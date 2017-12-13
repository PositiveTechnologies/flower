(ns flower.tracker.tfs.tracker
  (:require [flower.tracker.proto :as proto]
            [flower.tracker.tfs.common :as common]
            [flower.tracker.tfs.task :as task]
            [flower.tracker.tfs.iteration :as iteration]))

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

(defrecord TFSTracker [tracker-component tracker-name tracker-url tracker-project]
  proto/TrackerProto
  (get-tracker-component [tracker] tracker-component)
  (tracker-name-only [tracker] (private-tracker-name-only tracker-name tracker-url))
  (get-tracker-type [tracker] :tfs)
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
  (->TFSTracker nil tracker-name tracker-url nil))


(defn- private-get-projects [tracker tracker-name tracker-url]
  (map #(->TFSTracker (proto/get-tracker-component tracker)
                      tracker-name
                      tracker-url
                      (.getName %))
       (list)))


(defn- private-get-tasks [tracker query]
  (task/get-tfs-workitems tracker query))


(defn- private-get-iterations [tracker]
  (iteration/get-tfs-iterations tracker))
