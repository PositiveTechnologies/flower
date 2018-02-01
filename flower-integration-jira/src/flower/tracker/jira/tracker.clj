(ns flower.tracker.jira.tracker
  (:require [flower.tracker.proto :as proto]
            [flower.tracker.jira.common :as common]
            [flower.tracker.jira.task :as task]
            [flower.tracker.jira.iteration :as iteration]))

;;
;; Private declarations
;;

(declare private-tracker-name-only
         private-get-projects
         private-get-tasks
         private-get-iterations)


;;
;; Public definitions
;;

(defrecord JiraTracker [tracker-component tracker-name tracker-url tracker-project]
  proto/TrackerProto
  (get-tracker-component [tracker] tracker-component)
  (tracker-name-only [tracker] (private-tracker-name-only tracker-name tracker-url))
  (get-tracker-type [tracker] :jira)
  (get-project-name [tracker] tracker-project)
  (get-projects [tracker] (private-get-projects tracker tracker-name tracker-url))
  (get-tasks [tracker] (list))
  (get-tasks [tracker query] (private-get-tasks tracker query))
  (get-tracker-url [tracker] tracker-url)
  (get-project-url [tracker] (str tracker-url "/projects/" tracker-project))
  (get-iterations [tracker] (private-get-iterations tracker)))


;;
;; Private definitions
;;

(defn- private-tracker-name-only [tracker-name tracker-url]
  (->JiraTracker nil tracker-name tracker-url nil))


(defn- private-get-projects [tracker tracker-name tracker-url]
  (map #(map->JiraTracker {:tracker-component (proto/get-tracker-component tracker)
                           :tracker-name tracker-name
                           :tracker-url tracker-url
                           :tracker-project (.getKey %)})

       (common/get-jira-projects-inner tracker)))


(defn- private-get-tasks [tracker task-ids]
  (task/get-jira-workitems tracker task-ids))


(defn- private-get-iterations [tracker]
  (list))
