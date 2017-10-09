(ns flower.tracker.core
  (:require [com.stuartsierra.component :as component]
            [flower.tracker.proto :as proto]
            [flower.tracker.github.tracker :as github.tracker]
            [flower.tracker.gitlab.tracker :as gitlab.tracker]
            [flower.tracker.jira.tracker :as jira.tracker]
            [flower.tracker.tfs.tracker :as tfs.tracker]))


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
               (let [tracker-projects-list (or tracker-projects (list nil))]
                 [tracker-name (map #((case tracker-type
                                        :tfs tfs.tracker/map->TFSTracker
                                        :gitlab gitlab.tracker/map->GitlabTracker
                                        :jira jira.tracker/map->JiraTracker
                                        :github github.tracker/map->GithubTracker)
                                      {:tracker-component tracker-component
                                       :tracker-name tracker-name
                                       :tracker-url tracker-url
                                       :tracker-project %})
                                    tracker-projects-list)]))
             trackers)))


(defn start-component [args]
  (map->TrackerComponent args))
