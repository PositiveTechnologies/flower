(ns flower.tracker.github.task
  (:require [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.github.common :as common]))

;;
;; Public definitions
;;

(defrecord GithubTrackerTask [tracker task-id task-title task-type task-state task-tags]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type))


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
          :task-tags (.getLabels %)})
       (if (empty? task-ids)
         (common/get-github-workitems-inner tracker)
         (common/get-github-workitems-inner tracker task-ids))))


(defn- private-get-github-workitems [tracker task-ids]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :tasks-map-function]
               (fn [task] task))
       (private-get-github-workitems-before-map tracker task-ids)))
