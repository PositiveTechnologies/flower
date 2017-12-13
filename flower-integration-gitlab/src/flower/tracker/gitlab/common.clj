(ns flower.tracker.gitlab.common
  (:require [clojure.string :as string]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto])
  (:import (org.gitlab.api GitlabAPI)
           (org.gitlab.api.models GitlabIssue$Action)))


;;
;; Public definitions
;;

(macros/public-definition get-gitlab-conn-inner cached)
(macros/public-definition get-gitlab-project-inner cached)
(macros/public-definition get-gitlab-projects-inner cached)
(macros/public-definition get-gitlab-workitems-inner cached)
(macros/public-definition get-gitlab-iterations-inner cached)
(macros/public-definition get-gitlab-capacity-inner cached)
(macros/public-definition set-gitlab-workitem-inner!)


;;
;; Private definitions
;;

(defn- private-get-gitlab-conn-inner
  ([tracker] (let [auth (get-in (proto/get-tracker-component tracker)
                                [:auth]
                                {})
                   login (get auth :gitlab-login)
                   password (get auth :gitlab-password)]
               (private-get-gitlab-conn-inner tracker login password)))
  ([tracker token] (GitlabAPI/connect (:tracker-url tracker) token))
  ([tracker login password] (let [gitlab-conn (GitlabAPI/connect (:tracker-url tracker)
                                                                 login
                                                                 password)]
                              (private-get-gitlab-conn-inner tracker
                                                             (.getPrivateToken gitlab-conn)))))


(defn- private-get-gitlab-projects-inner [tracker]
  (.getProjects (get-gitlab-conn-inner tracker)))


(defn- private-get-gitlab-project-inner [tracker]
  (let [project-name (proto/get-project-name tracker)]
    (-> (filter (fn [project]
                  (= (.getName project)
                     project-name))
                (private-get-gitlab-projects-inner tracker))
        (first))))


(defn- private-get-gitlab-workitems-inner
  ([tracker] (let [conn-inner (get-gitlab-conn-inner tracker)
                   project-inner (get-gitlab-project-inner tracker)]
               (.getIssues conn-inner project-inner)))
  ([tracker task-ids] (let [task-ids-list (into [] (map #(Integer. %)
                                                        (filter identity task-ids)))]
                        (into [] (filter (fn [issue]
                                           (.contains task-ids-list (.getIid issue)))
                                         (get-gitlab-workitems-inner tracker))))))


(defn- private-get-gitlab-iterations-inner [tracker]
  (let [conn-inner (get-gitlab-conn-inner tracker)
        project-inner (get-gitlab-project-inner tracker)]
    (.getMilestones conn-inner project-inner)))


(defn- private-get-gitlab-capacity-inner [tracker iteration]
  nil)


(defn- private-set-gitlab-workitem-inner! [tracker task-id fields]
  (let [conn-inner (get-gitlab-conn-inner tracker)
        workitem-inner (first (get-gitlab-workitems-inner tracker [task-id]))
        assignee-name (get fields :task-assignee)
        assignee (and assignee-name
                      (first (.findUsers conn-inner assignee-name)))
        state (get fields :task-state)
        project-id (.getId (get-gitlab-project-inner tracker))
        assignee-id (if assignee
                      (.getId assignee)
                      0)]
    (if workitem-inner
      (let [milestone (.getMilestone workitem-inner)
            milestone-id (if milestone
                           (.getId milestone)
                           0)
            issue-id (.getId workitem-inner)
            labels (string/join ","
                                (if (contains? fields :task-tags)
                                  (get fields :task-tags)
                                  (seq (.getLabels workitem-inner))))
            title (if (contains? fields :task-title)
                    (get fields :task-title)
                    (.getTitle workitem-inner))
            description (if (contains? fields :task-description)
                          (get fields :task-description)
                          (.getDescription workitem-inner))
            action (if state
                     (if (= state "opened")
                       GitlabIssue$Action/REOPEN
                       GitlabIssue$Action/CLOSE)
                     GitlabIssue$Action/LEAVE)]
        (.editIssue conn-inner
                    project-id
                    issue-id
                    assignee-id
                    milestone-id
                    labels
                    description
                    title
                    action))
      (let [milestone-id (Integer. 0)
            description (get fields :task-description "")
            labels (get fields :task-tags "")
            title (get fields :task-title "")]
        (.createIssue conn-inner
                      project-id
                      assignee-id
                      milestone-id
                      labels
                      description
                      title)))))
