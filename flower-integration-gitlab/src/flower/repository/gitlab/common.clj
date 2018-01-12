(ns flower.repository.gitlab.common
  (:require [flower.macros :as macros]
            [flower.repository.proto :as proto])
  (:import (java.util Arrays)
           (org.gitlab.api GitlabAPI)
           (org.gitlab.api.models GitlabCommit
                                  GitlabCommitDiff
                                  GitlabMergeRequest
                                  GitlabProject)))


;;
;; Public definitions
;;

(macros/public-definition get-gitlab-conn-inner cached)
(macros/public-definition get-gitlab-project-inner cached)
(macros/public-definition get-gitlab-projects-inner cached)
(macros/public-definition get-gitlab-pull-request-inner cached)
(macros/public-definition get-gitlab-commits-inner cached)
(macros/public-definition get-gitlab-changes-inner cached)


;;
;; Private definitions
;;

(defn- private-get-gitlab-conn-inner
  ([repository] (let [auth (get-in (proto/get-repository-component repository)
                                   [:auth]
                                   {})
                      login (get auth :gitlab-login)
                      password (get auth :gitlab-password)]
                  (private-get-gitlab-conn-inner repository login password)))
  ([repository token] (GitlabAPI/connect (:repo-url repository) token))
  ([repository login password] (let [gitlab-conn (GitlabAPI/connect (:repo-url repository)
                                                                    login
                                                                    password)]
                                 (private-get-gitlab-conn-inner repository
                                                                (.getPrivateToken gitlab-conn)))))


(defn- private-get-gitlab-projects-inner [repository]
  (.getProjects (get-gitlab-conn-inner repository)))


(defn- private-get-gitlab-project-inner [repository]
  (let [project-name (proto/get-project-name repository)]
    (-> (filter (fn [project] (= (.getName project)
                                 project-name))
                (private-get-gitlab-projects-inner repository))
        (first))))


(defn- private-get-gitlab-pull-request-inner [repository pull-request]
  (let [pr-id (proto/get-pull-request-id pull-request)
        conn-inner (get-gitlab-conn-inner repository)
        project-inner (get-gitlab-project-inner repository)
        all-pull-requests (.getMergeRequests conn-inner project-inner)
        pull-request-inner (first (filter #(= (.getIid %) pr-id) all-pull-requests))]
    pull-request-inner))


(defn- private-get-gitlab-commits-inner [repository pull-request]
  (let [conn-inner (get-gitlab-conn-inner repository)
        project-inner (get-gitlab-project-inner repository)
        pull-request-inner (get-gitlab-pull-request-inner repository pull-request)]
    (Arrays/asList (.to (.retrieve conn-inner)
                        (str GitlabProject/URL "/" (.getId project-inner)
                             GitlabMergeRequest/URL "/" (.getId pull-request-inner)
                             GitlabCommit/URL)
                        (class (into-array [(GitlabCommit.)]))))))


(defn- private-get-gitlab-changes-inner [repository pull-request]
  (let [conn-inner (get-gitlab-conn-inner repository)
        project-inner (get-gitlab-project-inner repository)
        pull-request-inner (get-gitlab-pull-request-inner repository pull-request)]
    (.getChanges (.getMergeRequestChanges conn-inner
                                          (.getId project-inner)
                                          (.getId pull-request-inner)))))
