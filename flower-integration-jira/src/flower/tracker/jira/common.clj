(ns flower.tracker.jira.common
  (:require [flower.macros :as macros]
            [flower.tracker.proto :as proto])
  (:import (java.net URI)
           (com.atlassian.util.concurrent Promises)
           (com.atlassian.jira.rest.client.internal.async AsynchronousJiraRestClientFactory)
           (com.atlassian.jira.rest.client.api.domain IssueFieldId)
           (com.atlassian.jira.rest.client.api.domain.input IssueInputBuilder
                                                            TransitionInput)))


;;
;; Public definitions
;;

(macros/public-definition get-jira-conn-inner always-cached)
(macros/public-definition get-jira-project-inner always-cached)
(macros/public-definition get-jira-projects-inner cached)
(macros/public-definition get-jira-issue-type-inner cached)
(macros/public-definition get-jira-issue-types-inner cached)
(macros/public-definition get-jira-workitems-inner cached)
(macros/public-definition get-jira-query-inner cached)
(macros/public-definition get-jira-workitem-comments-inner cached)
(macros/public-definition get-jira-iterations-inner cached)
(macros/public-definition get-jira-capacity-inner cached)
(macros/public-definition set-jira-workitem-inner!)


;;
;; Private definitions
;;

(defn- private-get-jira-conn-inner
  ([tracker] (let [auth (get-in (proto/get-tracker-component tracker)
                                [:auth]
                                {})
                   login (get auth :jira-login)
                   password (get auth :jira-password)]
               (private-get-jira-conn-inner tracker login password)))
  ([tracker login password] (let [tracker-uri (new URI (:tracker-url tracker))
                                  jira-conn (.createWithBasicHttpAuthentication (AsynchronousJiraRestClientFactory.)
                                                                                tracker-uri
                                                                                login
                                                                                password)]
                              jira-conn)))


(defn- private-get-jira-projects-inner [tracker]
  (-> (get-jira-conn-inner tracker)
      (.getProjectClient)
      (.getAllProjects)
      (.claim)))


(defn- private-get-jira-project-inner [tracker]
  (let [project-name (proto/get-project-name tracker)]
    (-> (get-jira-conn-inner tracker)
        (.getProjectClient)
        (.getProject project-name)
        (.claim))))


(defn- private-get-jira-issue-types-inner [tracker]
  (-> (get-jira-conn-inner tracker)
      (.getMetadataClient)
      (.getIssueTypes)
      (.claim)))


(defn- private-get-jira-issue-type-inner [tracker issue-type]
  (let [issue-types (get-jira-issue-types-inner tracker)]
    (or (first (filter #(= issue-type (.getName %))
                       issue-types))
        (first issue-types))))


(defn- private-get-jira-workitems-inner [tracker task-ids]
  (let [issue-client (.getIssueClient (get-jira-conn-inner tracker))
        skip-on-exception (get (proto/get-tracker-component tracker) :skip-on-exception true)]
    (try
      (-> (Promises/when (filter identity
                                 (map #(try
                                         (.getIssue issue-client %)
                                         (catch Exception e
                                           (when-not skip-on-exception
                                             (throw e))))
                                      (filter identity task-ids))))
          (.claim))
      (catch Exception e
        (when-not skip-on-exception
          (throw e))))))


(defn- private-get-jira-query-inner [tracker query]
  (-> (get-jira-conn-inner tracker)
      (.getSearchClient)
      (.searchJql query)
      (.claim)
      (.getIssues)))


(defn- private-get-jira-workitem-comments-inner [tracker workitem-inner]
  (.getComments workitem-inner))


(defn- private-get-jira-iterations-inner [tracker]
  nil)


(defn- private-get-jira-capacity-inner [tracker iteration]
  nil)


(defn- private-set-jira-workitem-inner! [tracker task-id fields]
  (let [conn-inner (get-jira-conn-inner tracker)
        issue-client (.getIssueClient conn-inner)
        task-type (get fields :task-type)
        workitem-inner (first (get-jira-workitems-inner tracker [task-id]))
        workitem-input-builder (doto (IssueInputBuilder.)
                                 (.setProject (get-jira-project-inner tracker)))]
    (when (and workitem-inner
               (contains? fields :task-state))
      (let [transitions (-> issue-client
                            (.getTransitions workitem-inner)
                            (.claim))
            transition (first (filter #(= (get fields :task-state)
                                          (.getName %))
                                      transitions))]
        (when transition
          (-> issue-client
              (.transition workitem-inner
                           (TransitionInput. (.getId transition) nil))
              (.claim)))))
    (when (contains? fields :task-title)
      (.setSummary workitem-input-builder (get fields :task-title)))
    (when (contains? fields :task-type)
      (.setIssueType workitem-input-builder
                     (get-jira-issue-type-inner tracker
                                                (get fields :task-type))))
    (when (contains? fields :task-description)
      (.setDescription workitem-input-builder (get fields :task-description)))
    (when (contains? fields :task-assignee)
      (.setAssigneeName workitem-input-builder (get fields :task-assignee)))
    (when (contains? fields :task-tags)
      (.setFieldValue workitem-input-builder
                      (.id IssueFieldId/LABELS_FIELD)
                      (get fields :task-tags)))
    (if workitem-inner
      (-> issue-client
          (.updateIssue task-id
                        (.build workitem-input-builder))
          (.claim))
      (-> issue-client
          (.createIssue (.build workitem-input-builder))
          (.claim)))))
