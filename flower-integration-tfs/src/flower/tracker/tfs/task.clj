(ns flower.tracker.tfs.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [clojure.string :as string]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.tfs.common :as common]))

;;
;; Private declarations
;;

(declare private-set-tfs-workitem!)


;;
;; Public definitions
;;

(defrecord TFSTrackerTask [tracker task-id task-title task-type task-state task-tags task-description]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (upsert! [tracker-task] (private-set-tfs-workitem! tracker-task)))


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
            :task-tags (filter (complement empty?)
                               (string/split (get fields :System.Tags "") #"; "))
            :task-description (get fields :System.Description)}))
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


(defn- private-set-tfs-workitem! [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)
        old-workitem (first (proto/get-tasks tracker [task-id]))
        diff (into {} (filter second (second (data/diff old-workitem tracker-task))))
        fields (set/rename-keys diff {:task-title :System.Title
                                      :task-type :System.WorkItemType
                                      :task-assignee :System.AssignedTo
                                      :task-state :System.State
                                      :task-tags :System.Tags
                                      :task-description :System.Description})
        fields-with-tags (dissoc (if (contains? fields :System.Tags)
                                   (assoc fields :System.Tags (string/join "; "
                                                                           (get fields :System.Tags [])))
                                   fields)
                                 :tracker)
        new-task-id (get (common/set-tfs-workitem-inner! tracker task-id fields-with-tags) :id)]
    (common/get-tfs-workitems-inner-clear-cache!)
    (get-tfs-workitems-clear-cache!)
    (first (proto/get-tasks tracker [new-task-id]))))
