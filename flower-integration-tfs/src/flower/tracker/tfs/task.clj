(ns flower.tracker.tfs.task
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [clojure.string :as string]
            [flower.common :as common]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.tfs.common :as tfs.common]))

;;
;; Private declarations
;;

(declare private-get-tfs-workitem-comments)
(declare private-set-tfs-workitem!)
(declare private-get-tfs-workitem-url)


;;
;; Public definitions
;;

(defrecord TFSTrackerTaskComment [comment-author
                                  comment-text]
  proto/TrackerTaskCommentProto
  (get-author [tracker-task-comment] comment-author)
  (get-text [tracker-task-comment] comment-text))


(defrecord TFSTrackerTask [tracker task-id task-title task-type task-state task-tags task-description task-comments-future]
  proto/TrackerTaskProto
  (get-tracker [tracker-task] tracker)
  (get-task-id [tracker-task] task-id)
  (get-task-url [tracker-task] (private-get-tfs-workitem-url tracker-task))
  (get-state [tracker-task] task-state)
  (get-type [tracker-task] task-type)
  (get-comments [tracker-task] @task-comments-future)
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
            :task-description (get fields :System.Description)
            :task-comments-future (future (private-get-tfs-workitem-comments tracker %))}))
       (if (string? query)
         (tfs.common/get-tfs-query-inner tracker query)
         (tfs.common/get-tfs-workitems-inner tracker query))))


(defn- private-get-tfs-workitem-comments [tracker workitem-inner]
  (let [notes (try (tfs.common/get-tfs-workitem-comments-inner tracker (get workitem-inner :id))
                   (catch java.io.IOException e nil))]
    (map #(map->TFSTrackerTaskComment {:comment-author (get-in % [:revisedBy :name])
                                       :comment-text (get % :text)})
         notes)))


(defn- private-get-tfs-workitems [tracker query]
  (if (empty? query)
    []
    (map (get-in (proto/get-tracker-component tracker)
                 [:context :tasks-map-function]
                 (fn [task] task))
         (private-get-tfs-workitems-before-map tracker query))))


(defn- private-join-tags [tags]
  (into {}
        (map (fn [[key value]]
               [key (if (= key :task-tags)
                      (clojure.string/join "; " (sort value))
                      value)])
             tags)))


(defn- private-set-tfs-workitem! [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)
        old-workitem (private-join-tags (first (proto/get-tasks tracker [task-id])))
        new-workitem (private-join-tags tracker-task)
        diff (into {} (filter second (second (data/diff old-workitem new-workitem))))
        fields (set/rename-keys diff {:task-title :System.Title
                                      :task-type :System.WorkItemType
                                      :task-assignee :System.AssignedTo
                                      :task-state :System.State
                                      :task-tags :System.Tags
                                      :task-description :System.Description})
        fields-without-tracker (dissoc fields :tracker)
        new-task-id (get (tfs.common/set-tfs-workitem-inner! tracker task-id fields-without-tracker) :id)]
    (when common/*behavior-implicit-cache-cleaning*
      (tfs.common/get-tfs-workitems-inner-clear-cache!)
      (get-tfs-workitems-clear-cache!))
    (first (proto/get-tasks tracker [new-task-id]))))


(defn- private-get-tfs-workitem-url [tracker-task]
  (let [tracker (proto/get-tracker tracker-task)
        task-id (proto/get-task-id tracker-task)]
    (str (proto/get-project-url tracker) "/_workitems/edit/" task-id)))
