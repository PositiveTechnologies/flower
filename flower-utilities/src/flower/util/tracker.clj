(ns flower.util.tracker
  (:require [flower.macros :as macros]
            [flower.common :as common]
            [flower.credentials :as credentials]
            [flower.tracker.core :as tracker.core]
            [flower.tracker.proto :as tracker.proto]
            [flower.util.common :as util.common]))


(defn- get-tracker-no-auth [tracker-url]
  (tracker.core/get-tracker tracker-url))


(defn- get-tracker-default-auth [tracker-url]
  (let [default-login (credentials/get-credentials :account :login)
        default-password (credentials/get-credentials :account :password)
        default-domain (credentials/get-credentials :account :domain)
        default-email (credentials/get-credentials :account :email)
        default-credentials (into (common/->ComponentAuth)
                                  {:github-login default-login
                                   :github-password default-password
                                   :gitlab-login default-login
                                   :gitlab-password default-password
                                   :jira-login default-login
                                   :jira-password default-password
                                   :tfs-login (str default-domain "\\" default-login)
                                   :tfs-password default-password
                                   :message-box-username default-login
                                   :message-box-password default-password
                                   :message-box-domain default-domain
                                   :message-box-email default-email})]
    (binding [common/*component-auth* default-credentials]
      (tracker.core/get-tracker tracker-url))))


(defn get-tracker [tracker-url auth-type]
  (apply (condp = auth-type
           nil get-tracker-no-auth
           get-tracker-default-auth)
         [tracker-url]))


(defn get-task-info [tracker-url auth-type task-id]
  (let [tracker (get-tracker tracker-url auth-type)
        task (first (tracker.proto/get-tasks tracker [task-id]))
        task-id (get task :task-id)
        task-url (tracker.proto/get-task-url task)]
    (if task-id
      (str (get task :task-type "Task") " "
           (if task-url
             (str "[[" task-url "][" task-id "]]")
             task-id) ": "
           (get task :task-title "?") "\n"
           "Assignee: " (get task :task-assignee "?") "\n"
           "State: " (get task :task-state "?") "\n\n"
           (util.common/strip-html (get task :task-description "?"))))))


(defn get-task-url [tracker-url auth-type task-id]
  (let [tracker (get-tracker tracker-url auth-type)
        task (first (tracker.proto/get-tasks tracker [task-id]))]
    (tracker.proto/get-task-url task)))


(defn get-tasks [tracker-url auth-type query]
  (let [tracker (get-tracker tracker-url auth-type)]
    (apply str
           (map (fn [task]
                  (let [task-id (get task :task-id)
                        task-url (tracker.proto/get-task-url task)]
                    (str "* "
                         (get task :task-type "Task") " "
                         (if task-url
                           (str "[[" task-url "][" task-id "]]")
                           task-id) ": "
                         (get task :task-title "?") " ["
                         (get task :task-state "?") "] - "
                         (get task :task-assignee "?") "\n")))
                (tracker.proto/get-tasks tracker (or query
                                                     nil))))))
