(ns flower.tracker.tfs.task
  (:require [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.tfs.common :as common]))

;;
;; Public definitions
;;

(defrecord TFSTrackerTask [tracker task-id task-title task-type task-state task-tags]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type))


(macros/public-definition get-tfs-workitems cached)


;;
;; Private definitions
;;

(defn- private-get-tfs-workitems-before-map [tracker query]
  (map #(map->TFSTrackerTask
         (let [fields (get % :fields {})]
           {:tracker tracker
            :task-id (get % :id)
            :task-title (get fields :System.Title)
            :task-type (get fields :System.WorkItemType)
            :task-assignee (get fields :System.AssignedTo)
            :task-state (get fields :System.State)
            :task-tags (get fields :System.Tags)}))
       (if (string? query)
         (common/get-tfs-query-inner tracker query)
         (common/get-tfs-workitems-inner tracker query))))


(defn- private-get-tfs-workitems [tracker query]
  (if (empty? query)
    []
    (map (get-in (proto/get-tracker-component tracker)
                 [:context :tasks-map-function]
                 (fn [task] task))
         (private-get-tfs-workitems-before-map tracker query))))
