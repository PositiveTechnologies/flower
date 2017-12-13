(ns flower.tracker.gitlab.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.gitlab.common :as common]))

;;
;; Private declarations
;;

(declare private-set-gitlab-workitem!)


;;
;; Public definitions
;;

(defrecord GitlabTrackerTask [tracker task-id task-title task-type task-state task-tags task-description]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (upsert! [tracker-task] (private-set-gitlab-workitem! tracker-task)))


(macros/public-definition get-gitlab-workitems cached)


;;
;; Private definitions
;;

(defn- private-get-gitlab-workitems-before-map [tracker task-ids]
  (map #(map->GitlabTrackerTask
         {:tracker tracker
          :task-id (.getIid %)
          :task-title (.getTitle %)
          :task-type "Issue"
          :task-assignee (let [assignee (.getAssignee %)]
                           (if assignee
                             (.getUsername assignee)
                             nil))
          :task-state (.getState %)
          :task-tags (seq (.getLabels %))
          :task-description (.getDescription %)})
       (if (empty? task-ids)
         (common/get-gitlab-workitems-inner tracker)
         (common/get-gitlab-workitems-inner tracker task-ids))))


(defn- private-get-gitlab-workitems [tracker task-ids]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :tasks-map-function]
               (fn [task] task))
       (private-get-gitlab-workitems-before-map tracker task-ids)))


(defn- private-set-gitlab-workitem! [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)
        old-workitem (first (proto/get-tasks tracker [task-id]))
        fields (second (data/diff old-workitem tracker-task))
        new-task-id (.getIid (common/set-gitlab-workitem-inner! tracker task-id fields))]
    (common/get-gitlab-workitems-inner-clear-cache!)
    (get-gitlab-workitems-clear-cache!)
    (first (proto/get-tasks tracker [new-task-id]))))
