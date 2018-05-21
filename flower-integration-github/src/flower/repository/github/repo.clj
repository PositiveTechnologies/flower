(ns flower.repository.github.repo
  (:require [flower.repository.proto :as proto]
            [flower.repository.github.common :as common]
            [flower.repository.github.pr :as pr]))


;;
;; Private declarations
;;

(declare private-repository-name-only
         private-get-projects
         private-get-pull-requests)


;;
;; Public definitions
;;

(defrecord GithubRepository [repository-component repo-name repo-url repo-project]
  proto/RepositoryProto
  (get-repository-component [repository] repository-component)
  (repository-name-only [repository] (private-repository-name-only repository repo-name repo-url))
  (get-project-name [repository] repo-project)
  (get-projects [repository] (private-get-projects repository repo-name repo-url))
  (get-pull-requests [repository] (private-get-pull-requests repository repo-project {}))
  (get-pull-requests [repository options] (private-get-pull-requests repository repo-project options))
  (get-repository-url [repository] repo-url)
  (get-project-url [repository] (str repo-url "/" repo-project)))


;;
;; Private definitions
;;

(defn- private-repository-name-only [repository repo-name repo-url]
  (map->GithubRepository {:repository-component (proto/get-repository-component repository)
                          :repo-name repo-name
                          :repo-url repo-url
                          :repo-project nil}))


(defn- private-get-projects [repository repo-name repo-url]
  (map #(map->GithubRepository {:repository-component (proto/get-repository-component repository)
                                :repo-name repo-name
                                :repo-url repo-url
                                :repo-project (.getName %)})
       (common/get-github-projects-inner repository)))


(defn- private-get-pull-requests [repository repo-project options]
  (if (nil? repo-project)
    (list)
    (pr/get-github-pull-requests repository options)))
