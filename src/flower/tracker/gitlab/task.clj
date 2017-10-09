(ns flower.tracker.gitlab.task
  (:require [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.gitlab.common :as common]))

;;
;; Public definitions
;;

(defrecord GitlabTrackerTask [tracker task-id task-title task-type task-state task-tags]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type))


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
          :task-tags (.getLabels %)})
       (if (empty? task-ids)
         (common/get-gitlab-workitems-inner tracker)
         (common/get-gitlab-workitems-inner tracker task-ids))))


(defn- private-get-gitlab-workitems [tracker task-ids]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :tasks-map-function]
               (fn [task] task))
       (private-get-gitlab-workitems-before-map tracker task-ids)))
