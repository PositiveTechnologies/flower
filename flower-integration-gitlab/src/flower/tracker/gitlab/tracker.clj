(ns flower.tracker.gitlab.tracker
  (:require [flower.tracker.proto :as proto]
            [flower.tracker.gitlab.common :as common]
            [flower.tracker.gitlab.task :as task]
            [flower.tracker.gitlab.iteration :as iteration]))

;;
;; Private declarations
;;

(declare private-tracker-name-only
         private-get-projects
         private-get-tasks
         private-get-iterations
         private-get-gitlab-project-path)


;;
;; Public definitions
;;

(defrecord GitlabTracker [tracker-component tracker-name tracker-url tracker-project]
  proto/TrackerProto
  (get-tracker-component [tracker] tracker-component)
  (tracker-name-only [tracker] (private-tracker-name-only tracker-name tracker-url))
  (get-tracker-type [tracker] :gitlab)
  (get-project-name [tracker] tracker-project)
  (get-projects [tracker] (list))
  (get-tasks [tracker] (private-get-tasks tracker nil))
  (get-tasks [tracker query] (private-get-tasks tracker query))
  (get-tracker-url [tracker] tracker-url)
  (get-project-url [tracker] (str tracker-url "/" (private-get-gitlab-project-path tracker)))
  (get-iterations [tracker] (private-get-iterations tracker)))


;;
;; Private definitions
;;

(defn- private-tracker-name-only [tracker-name tracker-url]
  (->GitlabTracker nil tracker-name tracker-url nil))


(defn- private-get-projects [tracker tracker-name tracker-url]
  (map #(map->GitlabTracker {:tracker-component (proto/get-tracker-component tracker)
                             :tracker-name tracker-name
                             :tracker-url tracker-url
                             :tracker-project (.getName %)})
       (common/get-gitlab-projects-inner tracker)))


(defn- private-get-tasks [tracker task-ids]
  (task/get-gitlab-workitems tracker task-ids))


(defn- private-get-iterations [tracker]
  (iteration/get-gitlab-iterations tracker))


(defn- private-get-gitlab-project-path [tracker]
  (.getPathWithNamespace (common/get-gitlab-project-inner tracker)))
