(ns flower.repository.core
  (:require [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [cemerick.url :as url]
            [tesser.core :as tesser]
            [flower.common :as common]
            [flower.resolver :as resolver]
            [flower.repository.proto :as proto]))


;;
;; Private declarations
;;

(declare private-projects-reducer-builder
         private-pull-requests-reducer-builder)


;;
;; Public definitions
;;

(defrecord RepositoryComponent [auth context]
  component/Lifecycle
  (start [component] (into component {:auth auth
                                      :context context}))
  (stop [component] (into component {:auth {}
                                     :context {}})))


(defn repositories [repository-component repos]
  (into {}
        (map (fn [[repo-name {repo-type :repo-type
                              repo-url :repo-url
                              repo-projects :repo-projects}]]
               (let [repo-projects-list (or repo-projects (list nil))]
                 [repo-name (map #((resolver/resolve-implementation repo-type :repository)
                                   {:repository-component repository-component
                                    :repo-name repo-name
                                    :repo-url repo-url
                                    :repo-project %})
                                 repo-projects-list)]))
             repos)))


(defn start-component [args]
  (map->RepositoryComponent args))


(def ^:dynamic *repository-type* nil)


(defmacro with-repository-type [repository-type & body]
  `(binding [flower.repository.core/*repository-type* ~repository-type]
     ~@body))


(defn get-repository-info [repository-full-url]
  (let [repository-url (url/url repository-full-url)
        repository-domain (get repository-url :host)
        repository-path-components (string/split (string/replace (get repository-url :path "/")
                                                                 #"(\.git)?/?$"
                                                                 "")
                                                 #"/")
        repository-path-first (string/join "/" (take 2 repository-path-components))
        repository-path-second (first (drop 2 repository-path-components))
        repository-path-third (first (drop 3 repository-path-components))
        repository-path-last (last repository-path-components)]
    (cond (or (= repository-domain "github.com")
              (= *repository-type* :github)) {:repo-type :github
                                              :repo-url (str (assoc repository-url
                                                                    :path repository-path-first))
                                              :repo-projects [repository-path-second]
                                              :repo-name (keyword (str "github-" repository-domain "-" repository-path-second))}
          (or (.contains repository-domain "gitlab")
              (= *repository-type* :gitlab)) {:repo-type :gitlab
                                              :repo-url (str (assoc repository-url
                                                                    :path ""))
                                              :repo-projects [repository-path-second]
                                              :repo-name (keyword (str "gitlab-" repository-domain "-" repository-path-second))})))


(defn get-repository [repository-full-url]
  (let [repository-info (get-repository-info repository-full-url)
        repository-name (get repository-info :repo-name :repository)]
    (first (get (repositories (start-component {:auth common/*component-auth*
                                                :context common/*component-context*})
                              {repository-name repository-info})
                repository-name))))


(defn get-projects-from-repositories
  ([repositories] (get-projects-from-repositories repositories {}))
  ([repositories options] (tesser/tesser repositories
                                         (tesser/fold {:reducer-identity list
                                                       :reducer (private-projects-reducer-builder options)
                                                       :post-reducer list
                                                       :combiner-identity list
                                                       :combiner into
                                                       :post-combiner identity}))))


(defn get-pull-requests-from-projects
  ([repositories] (get-pull-requests-from-projects repositories {}))
  ([repositories options] (tesser/tesser repositories
                                         (tesser/fold {:reducer-identity list
                                                       :reducer (private-pull-requests-reducer-builder options)
                                                       :post-reducer list
                                                       :combiner-identity list
                                                       :combiner into
                                                       :post-combiner identity}))))


;;
;; Private definitions
;;

(defn- private-projects-reducer-builder [options]
  (fn [acc repository]
    (let [repository-projects (-> repository
                                  (proto/repository-name-only)
                                  (proto/get-projects))
          project-names (map proto/get-project-name repository-projects)
          repository-project-name (proto/get-project-name repository)]
      (concat acc
              (cond (nil? repository-project-name) repository-projects
                    (.contains project-names repository-project-name) (list repository)
                    :else (list))))))


(defn- private-pull-requests-reducer-builder [options]
  (fn [acc repository]
    (concat acc (filter (get-in (proto/get-repository-component repository)
                                [:context :pull-requests-filter-function]
                                (fn [pull-request] true))
                        (proto/get-pull-requests repository options)))))
