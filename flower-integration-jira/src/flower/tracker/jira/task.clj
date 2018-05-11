(ns flower.tracker.jira.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [flower.common :as common]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.jira.common :as jira.common]))

;;
;; Private declarations
;;

(declare private-get-jira-workitem-comments)
(declare private-set-jira-workitem!)
(declare private-get-jira-workitem-url)


;;
;; Public definitions
;;

(defrecord JiraTrackerTaskComment [comment-author
                                   comment-text]
  proto/TrackerTaskCommentProto
  (get-author [tracker-task-comment] comment-author)
  (get-text [tracker-task-comment] comment-text))


(defrecord JiraTrackerTask [tracker task-id task-title task-type task-state task-tags task-description task-comments-future]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-task-url [tracker-task] (private-get-jira-workitem-url tracker-task))
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (get-comments [tracker-task] @task-comments-future)
  (upsert! [tracker-task] (private-set-jira-workitem! tracker-task)))


(macros/public-definition get-jira-workitems cached)


;;
;; Private definitions
;;

(defn- private-get-jira-workitems-before-map [tracker query]
  (map #(map->JiraTrackerTask
         {:tracker tracker
          :task-id (.getKey %)
          :task-title (.getSummary %)
          :task-type (.getName (.getIssueType %))
          :task-assignee (some-> %
                                 .getAssignee
                                 .getName)
          :task-state (.getName (.getStatus %))
          :task-tags (.getLabels %)
          :task-description (.getDescription %)
          :task-comments-future (future (private-get-jira-workitem-comments tracker %))})
       (if (string? query)
         (jira.common/get-jira-query-inner tracker query)
         (if (empty? query)
           (jira.common/get-jira-query-inner tracker (str "project=\"" (proto/get-project-name tracker) "\""))
           (jira.common/get-jira-workitems-inner tracker query)))))


(defn- private-get-jira-workitem-comments [tracker workitem-inner]
  (let [notes (try (jira.common/get-jira-workitem-comments-inner tracker workitem-inner)
                   (catch java.io.IOException e nil))]
    (map #(map->JiraTrackerTaskComment {:comment-author (some-> %
                                                                .getAuthor
                                                                .getName)
                                        :comment-text (.getBody %)})
         notes)))


(defn- private-get-jira-workitems [tracker query]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :tasks-map-function]
               (fn [task] task))
       (private-get-jira-workitems-before-map tracker query)))


(defn- private-set-jira-workitem! [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)
        old-workitem (first (proto/get-tasks tracker [task-id]))
        fields (second (data/diff old-workitem tracker-task))
        task-inner (jira.common/set-jira-workitem-inner! tracker task-id fields)
        new-task-id (if task-inner
                      (.getKey task-inner)
                      task-id)]
    (when common/*behavior-implicit-cache-cleaning*
      (jira.common/get-jira-workitems-inner-clear-cache!)
      (jira.common/get-jira-query-inner-clear-cache!)
      (get-jira-workitems-clear-cache!))
    (first (proto/get-tasks tracker [new-task-id]))))


(defn- private-get-jira-workitem-url [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        project-url (proto/get-project-url tracker)
        task-id (proto/get-task-id tracker-task)]
    (str project-url "/issues/" task-id)))
