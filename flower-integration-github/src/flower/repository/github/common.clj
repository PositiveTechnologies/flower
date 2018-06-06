(ns flower.repository.github.common
  (:require [clojure.string :as string]
            [flower.macros :as macros]
            [flower.repository.proto :as proto])
  (:import (java.net URL)
           (org.eclipse.egit.github.core.client GitHubClient
                                                RequestException)
           (org.eclipse.egit.github.core.service PullRequestService
                                                 RepositoryService
                                                 UserService)))


;;
;; Public definitions
;;

(macros/public-definition get-github-conn-inner always-cached)
(macros/public-definition get-github-project-inner always-cached)
(macros/public-definition get-github-projects-inner cached)
(macros/public-definition get-github-pull-requests-inner cached)
(macros/public-definition get-github-pull-request-comments-inner cached)
(macros/public-definition get-github-pull-request-commits-inner cached)
(macros/public-definition get-github-pull-request-files-inner cached)
(macros/public-definition set-assignee-inner!)
(macros/public-definition merge-github-pull-request-inner!)


;;
;; Private definitions
;;

(defn- private-get-github-conn-inner
  ([repository] (let [auth (get-in (proto/get-repository-component repository)
                                   [:auth]
                                   {})
                      login (get auth :github-login)
                      password (get auth :github-password)
                      token (get auth :github-token)]
                  (cond
                    token (private-get-github-conn-inner repository token)
                    (and login password) (private-get-github-conn-inner repository login password)
                    :else (GitHubClient/createClient (proto/get-repository-url repository)))))
  ([repository token] (doto (GitHubClient/createClient (proto/get-repository-url repository))
                        (.setOAuth2Token token)))
  ([repository login password] (doto (GitHubClient/createClient (proto/get-repository-url repository))
                                 (.setCredentials login password))))


(defn- private-get-github-projects-inner [repository]
  (let [user-name (-> (URL. (proto/get-repository-url repository))
                      (.getPath)
                      (rest)
                      (string/join))
        conn-inner (get-github-conn-inner repository)
        repository-service (RepositoryService. conn-inner)]
    (try
      (.getOrgRepositories repository-service user-name)
      (catch RequestException re (.getRepositories repository-service user-name)))))


(defn- private-get-github-project-inner [repository]
  (let [project-name (proto/get-project-name repository)]
    (-> (filter (fn [project]
                  (= (.getName project)
                     project-name))
                (private-get-github-projects-inner repository))
        (first))))


(defn- private-get-github-pull-requests-inner [repository state]
  (let [conn-inner (get-github-conn-inner repository)
        project-inner (get-github-project-inner repository)
        pull-request-service (PullRequestService. conn-inner)]
    (.getPullRequests pull-request-service project-inner state)))


(defn- private-get-github-pull-request-comments-inner [repository pull-request]
  (let [conn-inner (get-github-conn-inner repository)
        project-inner (get-github-project-inner repository)
        pull-request-service (PullRequestService. conn-inner)
        pull-request-id (.getNumber pull-request)]
    (.getComments pull-request-service project-inner pull-request-id)))


(defn- private-get-github-pull-request-commits-inner [repository pull-request]
  (let [conn-inner (get-github-conn-inner repository)
        project-inner (get-github-project-inner repository)
        pull-request-service (PullRequestService. conn-inner)
        pull-request-id (proto/get-pull-request-id pull-request)]
    (.getCommits pull-request-service project-inner pull-request-id)))


(defn- private-get-github-pull-request-files-inner [repository pull-request]
  (let [conn-inner (get-github-conn-inner repository)
        project-inner (get-github-project-inner repository)
        pull-request-service (PullRequestService. conn-inner)
        pull-request-id (proto/get-pull-request-id pull-request)]
    (.getFiles pull-request-service project-inner pull-request-id)))


(defn- private-set-assignee-inner! [repository pull-request pr-id assignee]
  (let [conn-inner (get-github-conn-inner repository)
        project-inner (get-github-project-inner repository)
        pull-request-service (PullRequestService. conn-inner)
        pull-request-inner (.getPullRequest pull-request-service pr-id)
        user-service (UserService. conn-inner)]
    (if (and assignee
             pull-request-inner)
      (if-let [found-user (.getUser user-service assignee)]
        (.editPullRequest pull-request-service
                          (.setAssignee pull-request-inner found-user))
        pull-request-inner)
      pull-request-inner)))


(defn- private-merge-github-pull-request-inner! [repository pull-request pr-id message]
  (let [conn-inner (get-github-conn-inner repository)
        project-inner (get-github-project-inner repository)
        pull-request-service (PullRequestService. conn-inner)
        commit-message (or message
                           (str "Merge branch '"
                                (proto/get-source-branch pull-request)
                                "' into '"
                                (proto/get-target-branch pull-request)
                                "'"))]
    (.merge pull-request-service project-inner pr-id commit-message)))
