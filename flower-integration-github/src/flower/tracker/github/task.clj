(ns flower.tracker.github.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [flower.common :as common]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.github.common :as github.common]))

;;
;; Private declarations
;;

(declare private-get-github-workitem-comments)
(declare private-set-github-workitem!)
(declare private-get-github-workitem-url)


;;
;; Public definitions
;;

(defrecord GithubTrackerTaskComment [comment-author
                                     comment-text]
  proto/TrackerTaskCommentProto
  (get-author [tracker-task-comment] comment-author)
  (get-text [tracker-task-comment] comment-text))


(defrecord GithubTrackerTask [tracker task-id task-title task-type task-state task-tags task-description task-comments-future]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-task-url [tracker-task] (private-get-github-workitem-url tracker-task))
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (get-related-tasks [tracker-task] (list))
  (get-related-tasks [tracker-task relation-type] (list))
  (get-related-task-types [tracker-task] (list))
  (get-comments [tracker-task] @task-comments-future)
  (upsert! [tracker-task] (private-set-github-workitem! tracker-task)))


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
                          (.getLabels %))
          :task-description (.getBody %)
          :task-comments-future (macros/future-or-delay (private-get-github-workitem-comments tracker %))})
       (if (empty? task-ids)
         (github.common/get-github-workitems-inner tracker)
         (github.common/get-github-workitems-inner tracker task-ids))))


(defn- private-get-github-workitem-comments [tracker workitem-inner]
  (let [notes (try (github.common/get-github-workitem-comments-inner tracker workitem-inner)
                   (catch java.io.IOException e nil))]
    (map #(map->GithubTrackerTaskComment {:comment-author (.getLogin (.getUser %))
                                          :comment-text (.getBody %)})
         notes)))


(defn- private-get-github-workitems [tracker task-ids]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :tasks-map-function]
               (fn [task] task))
       (private-get-github-workitems-before-map tracker task-ids)))


(defn- private-set-github-workitem! [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)
        old-workitem (first (proto/get-tasks tracker [task-id]))
        fields (second (data/diff old-workitem tracker-task))
        new-task-id (.getNumber (github.common/set-github-workitem-inner! tracker task-id fields))]
    (when common/*behavior-implicit-cache-cleaning*
      (github.common/get-github-workitems-inner-clear-cache!)
      (get-github-workitems-clear-cache!))
    (first (proto/get-tasks tracker [new-task-id]))))


(defn- private-get-github-workitem-url [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        project-url (proto/get-project-url tracker)
        task-id (proto/get-task-id tracker-task)]
    (str project-url "/issues/" task-id)))
