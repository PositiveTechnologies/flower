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
        default-token-github (flower.credentials/get-credentials :token :github)
        default-token-tfs (flower.credentials/get-credentials :token :tfs)
        default-credentials (into (common/->ComponentAuth)
                                  {:github-login default-login
                                   :github-password default-password
                                   :github-token default-token-github
                                   :gitlab-login default-login
                                   :gitlab-password default-password
                                   :jira-login default-login
                                   :jira-password default-password
                                   :tfs-login (str default-domain "\\" default-login)
                                   :tfs-password default-password
                                   :tfs-token default-token-tfs
                                   :message-box-username default-login
                                   :message-box-password default-password
                                   :message-box-domain default-domain
                                   :message-box-email default-email})]
    (binding [common/*component-auth* default-credentials]
      (tracker.core/get-tracker tracker-url))))


(defn get-tracker [tracker-url auth-type]
  (apply (condp = auth-type
           false get-tracker-no-auth
           get-tracker-default-auth)
         [tracker-url]))


(defn- get-task-info-str [task long-version]
  (let [task-id (get task :task-id)
        task-url (when task
                   (str "flower:" task-id))]
    (when task-id
      (let [task-title (str (get task :task-type "Task") " "
                            task-id ": "
                            (get task :task-title "?"))
            task-assignee (get task :task-assignee "?")
            task-state (get task :task-state "?")]
        (str (if task-url
               (str "[[" task-url "][" task-title "]]")
               task-title)
             (if long-version
               (str "\n"
                    "Assignee: " task-assignee "\n"
                    "State: " task-state "\n\n"
                    (util.common/strip-html (get task :task-description "?")))
               (str " [" task-state "] - " task-assignee))
             "\n")))))


(defn get-task-info [tracker-url auth-type task-id]
  (let [tracker (get-tracker tracker-url auth-type)
        task (first (tracker.proto/get-tasks tracker [task-id]))]
    (get-task-info-str task true)))


(defn get-task-url [tracker-url auth-type task-id]
  (let [tracker (get-tracker tracker-url auth-type)
        task (first (tracker.proto/get-tasks tracker [task-id]))]
    (when task
      (tracker.proto/get-task-url task))))


(defn get-tasks [tracker-url auth-type query grouping]
  (let [tracker (get-tracker tracker-url auth-type)
        tasks (tracker.proto/get-tasks tracker (or query nil))]
    (apply str
           (if-not grouping
             (map (fn [task]
                    (str "* " (get-task-info-str task false)))
                  tasks)
             (map (fn [[key value]]
                    (str "* " grouping ": \"" key "\"\n"
                         (apply str (map (fn [task]
                                           (str "** " (get-task-info-str task false)))
                                         value))))
                  (group-by (keyword grouping) tasks))))))
