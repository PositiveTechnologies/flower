(ns flower.tracker.github.iteration
  (:require [clojure.reflect :as reflect]
            [clojure.set :as set]
            [clj-time.local :as local]
            [clj-time.coerce :as coerce]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.github.common :as common]))


;;
;; Private declarations
;;

(declare private-get-github-capacity)


;;
;; Public definitions
;;

(macros/public-definition get-github-iterations cached)


(defrecord GithubTrackerIterationCalendar [it-cal-start-date it-cal-finish-date]
  proto/TrackerCalendarProto
  (get-range-start-date [calendar] it-cal-start-date)
  (get-range-finish-date [calendar] it-cal-finish-date)
  (get-ranges [calendar] [[it-cal-start-date it-cal-finish-date]]))


(defrecord GithubTrackerDaysOffCalendar [do-cal-ranges]
  proto/TrackerCalendarProto
  (get-range-start-date [calendar] (apply min (map first do-cal-ranges)))
  (get-range-finish-date [calendar] (apply max (map second do-cal-ranges)))
  (get-ranges [calendar] do-cal-ranges))


(defrecord GithubTrackerIteration [tracker it-id it-name it-url it-path it-calendar it-current]
  proto/TrackerIterationProto
  (get-id [iteration] it-id)
  (get-calendar [iteration] it-calendar)
  (get-capacity [iteration] (private-get-github-capacity tracker iteration)))


(defrecord GithubTrackerCapacity [cap-iteration cap-teammember cap-daysoff]
  proto/TrackerCapacityProto
  (get-iteration [capacity] cap-iteration)
  (get-teammember [capacity] cap-teammember)
  (get-daysoff [capacity] cap-daysoff))


;;
;; Private definitions
;;

(defn- private-get-github-iterations-before-map [tracker]
  (map #(map->GithubTrackerIteration
         (let [iteration-inner %
               start-date nil
               finish-date (coerce/from-date (.getDueOn iteration-inner))
               current-date (local/local-now)]
           {:tracker tracker
            :it-id (.getNumber iteration-inner)
            :it-name (.getTitle iteration-inner)
            :it-path (.getTitle iteration-inner)
            :it-url (.getUrl iteration-inner)
            :it-calendar (map->GithubTrackerIterationCalendar {:it-cal-start-date start-date
                                                               :it-cal-finish-date finish-date})
            :it-current (and (<= (compare start-date current-date) 0)
                             (<= (compare current-date finish-date) 0))}))
       (common/get-github-iterations-inner tracker)))


(defn- private-get-github-iterations [tracker]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :iterations-map-function]
               (fn [iteration] iteration))
       (private-get-github-iterations-before-map tracker)))


(defn- private-get-github-capacity [tracker iteration]
  nil)
