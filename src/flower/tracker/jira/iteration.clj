(ns flower.tracker.jira.iteration
  (:require [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.jira.common :as common]))


;;
;; Private declarations
;;

(declare private-get-jira-capacity)


;;
;; Public definitions
;;

(macros/public-definition get-jira-iterations cached)


;;
;; Private definitions
;;

(defn- private-get-jira-iterations [tracker]
  nil)


(defn- private-get-jira-capacity [tracker iteration]
  nil)
