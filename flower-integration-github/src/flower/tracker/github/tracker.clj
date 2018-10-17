(ns flower.tracker.github.tracker
  (:require [flower.tracker.proto :as proto]
            [flower.tracker.github.common :as common]
            [flower.tracker.github.task :as task]
            [flower.tracker.github.iteration :as iteration]))

;;
;; Private declarations
;;

(declare private-tracker-name-only
         private-get-projects
         private-get-tasks
         private-get-iterations)


;;
;; Public definitions
;;

(defrecord GithubTracker [tracker-component tracker-name tracker-url tracker-ns tracker-project]
  proto/TrackerProto
  (get-tracker-component [tracker] tracker-component)
  (tracker-name-only [tracker] (private-tracker-name-only tracker-name tracker-url))
  (get-tracker-type [tracker] :github)
  (get-namespace [tracker] tracker-ns)
  (get-project-name [tracker] tracker-project)
  (get-projects [tracker] (list))
  (get-tasks [tracker] (private-get-tasks tracker nil))
  (get-tasks [tracker query] (private-get-tasks tracker query))
  (get-tracker-url [tracker] tracker-url)
  (get-project-url [tracker] (str tracker-url "/" tracker-project))
  (get-iterations [tracker] (private-get-iterations tracker)))


;;
;; Private definitions
;;

(defn- private-tracker-name-only [tracker-name tracker-url]
  (->GithubTracker nil tracker-name tracker-url nil nil))


(defn- private-get-projects [tracker tracker-name tracker-url]
  (map #(map->GithubTracker {:tracker-component (proto/get-tracker-component tracker)
                             :tracker-name tracker-name
                             :tracker-url tracker-url
                             :tracker-ns (.getName (.getOwner %))
                             :tracker-project (.getName %)})
       (common/get-github-projects-inner tracker)))


(defn- private-get-tasks [tracker task-ids]
  (task/get-github-workitems tracker task-ids))


(defn- private-get-iterations [tracker]
  (iteration/get-github-iterations tracker))
