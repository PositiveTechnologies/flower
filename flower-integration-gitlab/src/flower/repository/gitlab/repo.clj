(ns flower.repository.gitlab.repo
  (:require [flower.repository.proto :as proto]
            [flower.repository.gitlab.common :as common]
            [flower.repository.gitlab.pr :as pr]))


;;
;; Private declarations
;;

(declare private-repository-name-only
         private-get-projects
         private-get-pull-requests
         private-get-gitlab-project-path)


;;
;; Public definitions
;;

(defrecord GitlabRepository [repository-component repo-name repo-url repo-project]
  proto/RepositoryProto
  (get-repository-component [repository] repository-component)
  (repository-name-only [repository] (private-repository-name-only repository repo-name repo-url))
  (get-project-name [repository] repo-project)
  (get-projects [repository] (private-get-projects repository repo-name repo-url))
  (get-pull-requests [repository] (private-get-pull-requests repository repo-project {}))
  (get-pull-requests [repository options] (private-get-pull-requests repository repo-project options))
  (get-repository-url [repository] repo-url)
  (get-project-url [repository] (str repo-url "/" (private-get-gitlab-project-path repository))))


;;
;; Private definitions
;;

(defn- private-repository-name-only [repository repo-name repo-url]
  (map->GitlabRepository {:repository-component (proto/get-repository-component repository)
                          :repo-name repo-name
                          :repo-url repo-url
                          :repo-project nil}))


(defn- private-get-projects [repository repo-name repo-url]
  (map #(map->GitlabRepository {:repository-component (proto/get-repository-component repository)
                                :repo-name repo-name
                                :repo-url repo-url
                                :repo-project (.getName %)})
       (common/get-gitlab-projects-inner repository)))


(defn- private-get-pull-requests [repository repo-project options]
  (if (nil? repo-project)
    (list)
    (pr/get-gitlab-pull-requests repository options)))


(defn- private-get-gitlab-project-path [repository]
  (.getPathWithNamespace (common/get-gitlab-project-inner repository)))
