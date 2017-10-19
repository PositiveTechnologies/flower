(ns flower.repository.github.common
  (:require [flower.macros :as macros]
            [flower.repository.proto :as proto])
  (:import (java.net URL)
           (org.eclipse.egit.github.core.client GitHubClient)
           (org.eclipse.egit.github.core.service PullRequestService
                                                 RepositoryService)))


;;
;; Public definitions
;;

(macros/public-definition get-github-conn-inner cached)
(macros/public-definition get-github-project-inner cached)
(macros/public-definition get-github-projects-inner cached)
(macros/public-definition get-github-pull-requests-inner cached)
(macros/public-definition get-github-pull-request-comments-inner cached)
(macros/public-definition merge-github-pull-request-inner)


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
                    (and login password) (private-get-github-conn-inner repository login password)
                    token (private-get-github-conn-inner repository token)
                    :else (GitHubClient/createClient (proto/get-repository-url repository)))))
  ([repository token] (doto (GitHubClient/createClient (proto/get-repository-url repository))
                        (.setOAuth2Token token)))
  ([repository login password] (doto (GitHubClient/createClient (proto/get-repository-url repository))
                                 (.setCredentials login password))))


(defn- private-get-github-projects-inner [repository]
  (let [organization-name (-> (URL. (proto/get-repository-url repository))
                              (.getPath)
                              (rest)
                              (clojure.string/join))
        conn-inner (get-github-conn-inner repository)
        repository-service (RepositoryService. conn-inner)]
    (.getOrgRepositories repository-service organization-name)))


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


(defn- private-merge-github-pull-request-inner [repository pull-request pr-id message]
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
