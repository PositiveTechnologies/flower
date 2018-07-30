(ns flower.tracker.gitlab.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [flower.common :as common]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.gitlab.common :as gitlab.common]))

;;
;; Private declarations
;;

(declare private-get-gitlab-workitem-comments)
(declare private-set-gitlab-workitem!)
(declare private-get-gitlab-workitem-url)


;;
;; Public definitions
;;

(defrecord GitlabTrackerTaskComment [comment-author
                                     comment-text]
  proto/TrackerTaskCommentProto
  (get-author [tracker-task-comment] comment-author)
  (get-text [tracker-task-comment] comment-text))


(defrecord GitlabTrackerTask [tracker task-id task-title task-type task-state task-tags task-description task-comments-future]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-task-url [tracker-task] (private-get-gitlab-workitem-url tracker-task))
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (get-related-tasks [tracker-task] (list))
  (get-related-tasks [tracker-task relation-type] (list))
  (get-related-task-types [tracker-task] (list))
  (get-comments [tracker-task] @task-comments-future)
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
          :task-description (.getDescription %)
          :task-comments-future (macros/future-or-delay (private-get-gitlab-workitem-comments tracker %))})
       (if (empty? task-ids)
         (gitlab.common/get-gitlab-workitems-inner tracker)
         (gitlab.common/get-gitlab-workitems-inner tracker task-ids))))


(defn- private-get-gitlab-workitem-comments [tracker workitem-inner]
  (let [notes (try (.getNotes (gitlab.common/get-gitlab-conn-inner tracker) workitem-inner)
                   (catch java.io.IOException e nil))]
    (map #(map->GitlabTrackerTaskComment {:comment-author (.getUsername (.getAuthor %))
                                          :comment-text (.getBody %)})
         notes)))


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
        new-task-id (.getIid (gitlab.common/set-gitlab-workitem-inner! tracker task-id fields))]
    (when common/*behavior-implicit-cache-cleaning*
      (gitlab.common/get-gitlab-workitems-inner-clear-cache!)
      (get-gitlab-workitems-clear-cache!))
    (first (proto/get-tasks tracker [new-task-id]))))


(defn- private-get-gitlab-workitem-url [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        project-url (proto/get-project-url tracker)
        task-id (proto/get-task-id tracker-task)]
    (str project-url "/issues/" task-id)))
