(ns flower.tracker.jira.common
  (:require [flower.macros :as macros]
            [flower.tracker.proto :as proto])
  (:import (java.net URI)
           (com.atlassian.util.concurrent Promises)
           (com.atlassian.jira.rest.client.internal.async AsynchronousHttpClientFactory
                                                          AsynchronousJiraRestClient)
           (com.atlassian.jira.rest.client.auth BasicHttpAuthenticationHandler)))


;;
;; Public definitions
;;

(macros/public-definition get-jira-conn-inner cached)
(macros/public-definition get-jira-project-inner cached)
(macros/public-definition get-jira-projects-inner cached)
(macros/public-definition get-jira-workitems-inner cached)
(macros/public-definition get-jira-iterations-inner cached)
(macros/public-definition get-jira-capacity-inner cached)


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
                                  basic-auth (new BasicHttpAuthenticationHandler login password)
                                  http-client (.createClient (new AsynchronousHttpClientFactory)
                                                             tracker-uri
                                                             basic-auth)
                                  jira-conn (AsynchronousJiraRestClient. tracker-uri
                                                                         http-client)]
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


(defn- private-get-jira-workitems-inner [tracker task-ids]
  (let [issue-client (.getIssueClient (get-jira-conn-inner tracker))]
    (-> (Promises/when (map #(.getIssue issue-client %)
                            task-ids))
        (.claim))))


(defn- private-get-jira-iterations-inner [tracker]
  nil)


(defn- private-get-jira-capacity-inner [tracker iteration]
  nil)
