(ns {{name}}.core
    (:require [flower.tracker.core :as tracker.core]
              [flower.repository.core :as repository.core]))


(defn print-all-opened-tasks
  "Print all opened tasks in our task tracker"
  [url]
  (println "All opened tasks for" url)
  (doall (map (comp (partial apply println)
                    (juxt (constantly "*")
                          :task-type
                          :task-id
                          :task-title))
              (.get-tasks (tracker.core/get-tracker url))))
  nil)


(defn print-all-pull-requests
  "Print all pull requests in our repository"
  [url]
  (println "All pull requests for" url)
  (doall (map (comp (partial apply println)
                    (juxt (constantly "*")
                          :pr-id
                          :pr-title))
              (.get-pull-requests (repository.core/get-repository url))))
  nil)


(defn -main []
  (let [url "https://github.com/PositiveTechnologies/flower"]
    (doto url
      (print-all-opened-tasks)
      (print-all-pull-requests)))
  nil)
