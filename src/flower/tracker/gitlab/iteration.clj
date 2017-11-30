(ns flower.tracker.gitlab.iteration
  (:require [clojure.reflect :as reflect]
            [clojure.set :as set]
            [clj-time.local :as local]
            [clj-time.coerce :as coerce]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.gitlab.common :as common]))


;;
;; Private declarations
;;

(declare private-get-gitlab-capacity)


;;
;; Public definitions
;;

(macros/public-definition get-gitlab-iterations cached)


(defrecord GitlabTrackerIterationCalendar [it-cal-start-date it-cal-finish-date]
  proto/TrackerCalendarProto
  (get-range-start-date [calendar] it-cal-start-date)
  (get-range-finish-date [calendar] it-cal-finish-date)
  (get-ranges [calendar] [[it-cal-start-date it-cal-finish-date]]))


(defrecord GitlabTrackerDaysOffCalendar [do-cal-ranges]
  proto/TrackerCalendarProto
  (get-range-start-date [calendar] (apply min (map first do-cal-ranges)))
  (get-range-finish-date [calendar] (apply max (map second do-cal-ranges)))
  (get-ranges [calendar] do-cal-ranges))


(defrecord GitlabTrackerIteration [tracker it-id it-name it-url it-path it-calendar it-current]
  proto/TrackerIterationProto
  (get-iteration-id [iteration] it-id)
  (get-calendar [iteration] it-calendar)
  (get-capacity [iteration] (private-get-gitlab-capacity tracker iteration)))


(defrecord GitlabTrackerCapacity [cap-iteration cap-teammember cap-daysoff]
  proto/TrackerCapacityProto
  (get-iteration [capacity] cap-iteration)
  (get-teammember [capacity] cap-teammember)
  (get-daysoff [capacity] cap-daysoff))


;;
;; Private definitions
;;

(defn- private-get-start-date [iteration-inner]
  (let [members (get (reflect/reflect iteration-inner) :members)]
    (if (empty? (set/select (fn [{name :name}]
                              (= name 'getStartDate))
                            members))
      nil
      (.getStartDate iteration-inner))))


(defn- private-get-gitlab-iterations-before-map [tracker]
  (map #(map->GitlabTrackerIteration
         (let [iteration-inner %
               start-date (coerce/from-date (private-get-start-date iteration-inner))
               finish-date (coerce/from-date (.getDueDate iteration-inner))
               current-date (local/local-now)]
           {:tracker tracker
            :it-id (.getIid iteration-inner)
            :it-name (.getTitle iteration-inner)
            :it-path (.getTitle iteration-inner)
            :it-url nil
            :it-calendar (map->GitlabTrackerIterationCalendar {:it-cal-start-date start-date
                                                               :it-cal-finish-date finish-date})
            :it-current (and (<= (compare start-date current-date) 0)
                             (<= (compare current-date finish-date) 0))}))
       (common/get-gitlab-iterations-inner tracker)))


(defn- private-get-gitlab-iterations [tracker]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :iterations-map-function]
               (fn [iteration] iteration))
       (private-get-gitlab-iterations-before-map tracker)))


(defn- private-get-gitlab-capacity [tracker iteration]
  nil)
