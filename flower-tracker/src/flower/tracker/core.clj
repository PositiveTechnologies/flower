(ns flower.tracker.core
  (:require [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [cemerick.url :as url]
            [flower.common :as common]
            [flower.resolver :as resolver]
            [flower.tracker.proto :as proto]))


;;
;; Public definitions
;;

(defrecord TrackerComponent [auth context]
  component/Lifecycle
  (start [component] (into component {:auth auth
                                      :context context}))
  (stop [component] (into component {:auth {}
                                     :context {}})))


(defn trackers [tracker-component trackers]
  (into {}
        (map (fn [[tracker-name {tracker-type :tracker-type
                                 tracker-url :tracker-url
                                 tracker-projects :tracker-projects}]]
               (let [tracker-projects-list (or tracker-projects (list nil))
                     tracker-pair [tracker-name (map #((resolver/resolve-implementation tracker-type :tracker)
                                                       {:tracker-component tracker-component
                                                        :tracker-name tracker-name
                                                        :tracker-url tracker-url
                                                        :tracker-project %})
                                                     tracker-projects-list)]]
                 tracker-pair))
             trackers)))


(defn start-component [args]
  (map->TrackerComponent args))


(def ^:dynamic *tracker-type* nil)
(def ^:dynamic *tracker-url* nil)
(def ^:dynamic *tracker-project* nil)


(defmacro with-tracker-type [tracker-type & body]
  `(binding [flower.tracker.core/*tracker-type* ~tracker-type]
     ~@body))


(defmacro with-tracker-url [tracker-url & body]
  `(binding [flower.tracker.core/*tracker-url* ~tracker-url]
     ~@body))


(defmacro with-tracker-project [tracker-project & body]
  `(binding [flower.tracker.core/*tracker-project* ~tracker-project]
     ~@body))


(defn get-tracker-info [tracker-full-url]
  (let [tracker-url (url/url tracker-full-url)
        tracker-domain (get tracker-url :host)
        tracker-path-components (string/split (string/replace (get tracker-url :path "/")
                                                              #"(\.git)?/?$"
                                                              "")
                                              #"/")
        tracker-path-first (string/join "/" (take 2 tracker-path-components))
        tracker-path-first-and-second (string/join "/" (take 3 tracker-path-components))
        tracker-path-second (first (drop 2 tracker-path-components))
        tracker-path-third (first (drop 3 tracker-path-components))
        tracker-path-last (last tracker-path-components)]
    (cond (or (= tracker-domain "github.com")
              (= *tracker-type* :github)) {:tracker-type :github
                                           :tracker-url (or *tracker-url*
                                                            (str (assoc tracker-url
                                                                        :path tracker-path-first)))
                                           :tracker-projects [(or *tracker-project* tracker-path-second)]
                                           :tracker-name (keyword (str "github-" tracker-domain "-" (or *tracker-project*
                                                                                                        tracker-path-second)))}
          (or (.contains tracker-domain "gitlab")
              (= *tracker-type* :gitlab)) {:tracker-type :gitlab
                                           :tracker-url (or *tracker-url*
                                                            (str (assoc tracker-url
                                                                        :path "")))
                                           :tracker-projects [(or *tracker-project* tracker-path-second)]
                                           :tracker-name (keyword (str "gitlab-" tracker-domain "-" (or *tracker-project*
                                                                                                        tracker-path-second)))}
          (or (.contains tracker-domain "tfs")
              (= *tracker-type* :tfs)) {:tracker-type :tfs
                                        :tracker-url (or *tracker-url*
                                                         (str (assoc tracker-url
                                                                     :path tracker-path-first-and-second)))
                                        :tracker-projects [(or *tracker-project* tracker-path-third)]
                                        :tracker-name (keyword (str "tfs-" tracker-domain "-" (or *tracker-project*
                                                                                                  tracker-path-third)))}
          (or (.contains tracker-domain "jira")
              (= *tracker-type* :jira)) {:tracker-type :jira
                                         :tracker-url (or *tracker-url*
                                                          (str (assoc tracker-url
                                                                      :path "")))
                                         :tracker-projects [(or *tracker-project* tracker-path-second)]
                                         :tracker-name (keyword (str "jira-" tracker-domain "-" (or *tracker-project*
                                                                                                    tracker-path-second)))}
          :else (merge {:tracker-type *tracker-type*
                        :tracker-name (keyword (str (name *tracker-type*) "-" tracker-domain (when *tracker-project*
                                                                                               (str "-" *tracker-project*))))
                        :tracker-url (or *tracker-url*
                                         (str (assoc tracker-url
                                                     :path "")))}
                       (when *tracker-project*
                         {:tracker-projects [*tracker-project*]})))))


(defn get-tracker [tracker-full-url]
  (let [tracker-info (get-tracker-info tracker-full-url)
        tracker-name (get tracker-info :tracker-name :tracker)]
    (first (get (trackers (start-component {:auth common/*component-auth*
                                            :context common/*component-context*})
                          {tracker-name tracker-info})
                tracker-name))))


(defn task [task-data]
  (let [tracker (get task-data :tracker)
        tracker-type (proto/get-tracker-type tracker)
        task-function (resolver/resolve-implementation tracker-type :task)]
    (task-function task-data)))
