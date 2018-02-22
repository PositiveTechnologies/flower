(ns flower.repository.default.repo
  (:require [flower.repository.proto :as proto]))


;;
;; Private declarations
;;

(declare private-repository-name-only
         private-get-projects
         private-get-pull-requests)


;;
;; Public definitions
;;

(defrecord DefaultRepository [repository-component repo-name repo-url repo-project]
  proto/RepositoryProto
  (get-repository-component [repository] repository-component)
  (repository-name-only [repository] (private-repository-name-only repository repo-name repo-url))
  (get-project-name [repository] repo-project)
  (get-projects [repository] (private-get-projects repository repo-name repo-url))
  (get-pull-requests [repository] (private-get-pull-requests repository repo-project {}))
  (get-pull-requests [repository options] (private-get-pull-requests repository repo-project options))
  (get-repository-url [repository] repo-url))


;;
;; Private definitions
;;

(defn- private-repository-name-only [repository repo-name repo-url]
  (map->DefaultRepository {:repository-component (proto/get-repository-component repository)
                           :repo-name repo-name
                           :repo-url repo-url
                           :repo-project nil}))


(defn- private-get-projects [repository repo-name repo-url]
  nil)


(defn- private-get-pull-requests [repository repo-project options]
  nil)
