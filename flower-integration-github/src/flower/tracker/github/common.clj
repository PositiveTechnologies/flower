(ns flower.tracker.github.common
  (:require [clojure.string :as string]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto])
  (:import (java.net URL)
           (org.eclipse.egit.github.core Issue
                                         Label)
           (org.eclipse.egit.github.core.client GitHubClient
                                                RequestException)
           (org.eclipse.egit.github.core.service RepositoryService
                                                 IssueService
                                                 MilestoneService
                                                 UserService)))


;;
;; Public definitions
;;

(macros/public-definition get-github-conn-inner always-cached)
(macros/public-definition get-github-project-inner always-cached)
(macros/public-definition get-github-projects-inner cached)
(macros/public-definition get-github-workitems-inner cached)
(macros/public-definition get-github-workitem-comments-inner cached)
(macros/public-definition get-github-iterations-inner cached)
(macros/public-definition get-github-capacity-inner cached)
(macros/public-definition set-github-workitem-inner!)


;;
;; Private definitions
;;

(defn- private-get-github-conn-inner
  ([tracker] (let [auth (get-in (proto/get-tracker-component tracker)
                                [:auth]
                                {})
                   login (get auth :github-login)
                   password (get auth :github-password)
                   token (get auth :github-token)]
               (cond
                 token (private-get-github-conn-inner tracker token)
                 (and login password) (private-get-github-conn-inner tracker login password)
                 :else (GitHubClient/createClient (proto/get-tracker-url tracker)))))
  ([tracker token] (doto (GitHubClient/createClient (proto/get-tracker-url tracker))
                     (.setOAuth2Token token)))
  ([tracker login password] (doto (GitHubClient/createClient (proto/get-tracker-url tracker))
                              (.setCredentials login password))))


(defn- private-get-github-projects-inner [tracker]
  (let [user-name (-> (URL. (proto/get-tracker-url tracker))
                      (.getPath)
                      (rest)
                      (string/join))
        conn-inner (get-github-conn-inner tracker)
        repository-service (RepositoryService. conn-inner)]
    (try
      (.getOrgRepositories repository-service user-name)
      (catch RequestException re (.getRepositories repository-service user-name)))))


(defn- private-get-github-project-inner [tracker]
  (let [project-name (proto/get-project-name tracker)]
    (-> (filter (fn [project]
                  (= (.getName project)
                     project-name))
                (private-get-github-projects-inner tracker))
        (first))))


(defn- private-get-github-workitems-inner
  ([tracker] (let [conn-inner (get-github-conn-inner tracker)
                   project-inner (get-github-project-inner tracker)
                   issue-service (IssueService. conn-inner)]
               (.getIssues issue-service project-inner nil)))
  ([tracker task-ids] (let [task-ids-list (into [] (map #(Integer. %)
                                                        (filter identity task-ids)))]
                        (into [] (filter (fn [issue]
                                           (.contains task-ids-list (.getNumber issue)))
                                         (get-github-workitems-inner tracker))))))


(defn- private-get-github-workitem-comments-inner [tracker workitem-inner]
  (let [conn-inner (get-github-conn-inner tracker)
        project-inner (get-github-project-inner tracker)
        workitem-id (.getNumber workitem-inner)
        issue-service (IssueService. conn-inner)]
    (.getComments issue-service project-inner workitem-id)))


(defn- private-get-github-iterations-inner [tracker]
  (let [conn-inner (get-github-conn-inner tracker)
        repository-inner (get-github-project-inner tracker)
        milestone-service (MilestoneService. conn-inner)]
    (.getMilestones milestone-service repository-inner nil)))


(defn- private-get-github-capacity-inner [tracker iteration]
  nil)


(defn- private-set-github-workitem-inner! [tracker task-id fields]
  (let [conn-inner (get-github-conn-inner tracker)
        project-inner (get-github-project-inner tracker)
        issue-service (IssueService. conn-inner)
        user-service (UserService. conn-inner)
        workitem-inner (or (first (get-github-workitems-inner tracker [task-id]))
                           (Issue.))
        workitem-inner-new (doto workitem-inner
                             (.setTitle (if (contains? fields :task-title)
                                          (get fields :task-title)
                                          (.getTitle workitem-inner)))
                             (.setBody (if (contains? fields :task-description)
                                         (get fields :task-description)
                                         (.getBody workitem-inner)))
                             (.setUser (if (contains? fields :task-assignee)
                                         (let [assignee-name (get fields :task-assignee)]
                                           (and assignee-name
                                                (.getUser user-service assignee-name)))
                                         (.getUser workitem-inner)))
                             (.setLabels (if (contains? fields :task-tags)
                                           (let [tags (get fields :task-tags)]
                                             (and tags
                                                  (map (fn [label-name]
                                                         (doto (Label.)
                                                           (.setName label-name)))
                                                       tags)))
                                           (.getLabels workitem-inner)))
                             (.setState (if (contains? fields :task-state)
                                          (get fields :task-state)
                                          (.getState workitem-inner))))]
    (if task-id
      (.editIssue issue-service
                  project-inner
                  workitem-inner-new)
      (.createIssue issue-service
                    project-inner
                    workitem-inner-new))))
