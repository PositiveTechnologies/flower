(ns flower.repository.core
  (:require [com.stuartsierra.component :as component]
            [tesser.core :as tesser]
            [flower.repository.proto :as proto]
            [flower.repository.gitlab.repo :as gitlab.repo]
            [flower.repository.github.repo :as github.repo]))


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
                 [repo-name (map #((case repo-type
                                     :gitlab gitlab.repo/map->GitlabRepository
                                     :github github.repo/map->GithubRepository)
                                   {:repository-component repository-component
                                    :repo-name repo-name
                                    :repo-url repo-url
                                    :repo-project %})
                                 repo-projects-list)]))
             repos)))


(defn start-component [args]
  (map->RepositoryComponent args))


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
