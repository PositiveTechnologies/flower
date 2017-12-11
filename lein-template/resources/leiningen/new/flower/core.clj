(ns {{name}}.core
    (:require [clojure.string]
              [flower.tracker.core :as tracker.core]
              [flower.tracker.proto :as tracker.proto]
              [flower.repository.core :as repository.core]
              [flower.repository.proto :as repository.proto]))


(defn print-all-opened-tasks
  "Print all opened tasks in our task tracker"
  [url]
  (println "All opened tasks for" url)
  (loop [[task & other-tasks] (-> url
                                  (tracker.core/get-tracker)
                                  (tracker.proto/get-tasks))]
    (if task
      (let [task-parts (-> task
                           (select-keys [:task-type :task-id :task-title])
                           (vals))
            task-string (clojure.string/join " " task-parts)]
        (println "*" task-string)
        (recur other-tasks)))))



(defn print-all-pull-requests
  "Print all pull requests in our repository"
  [url]
  (println "All pull requests for" url)
  (loop [[pr & other-prs] (-> url
                              (repository.core/get-repository)
                              (repository.proto/get-pull-requests))]
    (if pr
      (let [pr-parts (-> pr
                         (select-keys [:pr-id :pr-title])
                         (vals))
            pr-string (clojure.string/join " " pr-parts)]
        (println "*" pr-string)
        (recur other-prs)))))


(defn -main []
  (let [url "https://github.com/PositiveTechnologies/flower"]
    (doto url
      (print-all-opened-tasks)
      (print-all-pull-requests))))
