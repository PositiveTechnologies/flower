(ns flower.tracker.jira.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.jira.common :as common]))

;;
;; Private declarations
;;

(declare private-set-jira-workitem!)


;;
;; Public definitions
;;

(defrecord JiraTrackerTask [tracker task-id task-title task-type task-state task-tags]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (update! [tracker-task] (private-set-jira-workitem! tracker-task)))


(macros/public-definition get-jira-workitems cached)


;;
;; Private definitions
;;

(defn- private-get-jira-workitems-before-map [tracker task-ids]
  (map #(map->JiraTrackerTask
         {:tracker tracker
          :task-id (.getKey %)
          :task-title (.getSummary %)
          :task-type (.getName (.getIssueType %))
          :task-assignee (let [assignee (.getAssignee %)]
                           (if assignee
                             (.getName assignee)
                             nil))
          :task-state (.getName (.getStatus %))
          :task-tags (.getLabels %)})
       (if (empty? task-ids)
         (list)
         (common/get-jira-workitems-inner tracker task-ids))))


(defn- private-get-jira-workitems [tracker task-ids]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :tasks-map-function]
               (fn [task] task))
       (private-get-jira-workitems-before-map tracker task-ids)))


(defn- private-set-jira-workitem! [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)
        old-workitem (first (proto/get-tasks tracker [task-id]))
        fields (second (data/diff old-workitem tracker-task))]
    (common/set-jira-workitem-inner! tracker task-id fields)
    (common/get-jira-workitems-inner-clear-cache!)
    (get-jira-workitems-clear-cache!)
    tracker-task))
