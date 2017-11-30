(ns flower.tracker.github.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.github.common :as common]))

;;
;; Private declarations
;;

(declare private-set-github-workitem!)


;;
;; Public definitions
;;

(defrecord GithubTrackerTask [tracker task-id task-title task-type task-state task-tags]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (update! [tracker-task] (private-set-github-workitem! tracker-task)))


(macros/public-definition get-github-workitems cached)


;;
;; Private definitions
;;

(defn- private-get-github-workitems-before-map [tracker task-ids]
  (map #(map->GithubTrackerTask
         {:tracker tracker
          :task-id (.getNumber %)
          :task-title (.getTitle %)
          :task-type "Issue"
          :task-assignee (let [assignee (.getAssignee %)]
                           (if assignee
                             (.getName assignee)
                             nil))
          :task-state (.getState %)
          :task-tags (map (fn [label]
                            (.toString label))
                          (.getLabels %))})
       (if (empty? task-ids)
         (common/get-github-workitems-inner tracker)
         (common/get-github-workitems-inner tracker task-ids))))


(defn- private-get-github-workitems [tracker task-ids]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :tasks-map-function]
               (fn [task] task))
       (private-get-github-workitems-before-map tracker task-ids)))


(defn- private-set-github-workitem! [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)
        old-workitem (first (proto/get-tasks tracker [task-id]))
        fields (second (data/diff old-workitem tracker-task))]
    (common/set-github-workitem-inner! tracker task-id fields)
    (common/get-github-workitems-inner-clear-cache!)
    (get-github-workitems-clear-cache!)
    tracker-task))
