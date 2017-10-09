(ns flower.tracker.tfs.iteration
  (:require [clj-time.core]
            [clj-time.format]
            [clj-time.local]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]
            [flower.tracker.tfs.common :as common]))


;;
;; Private declarations
;;

(declare private-get-tfs-capacity)


;;
;; Public definitions
;;

(macros/public-definition get-tfs-iterations cached)


(defrecord TFSTrackerIterationCalendar [it-cal-start-date it-cal-finish-date]
  proto/TrackerCalendarProto
  (get-range-start-date [calendar] it-cal-start-date)
  (get-range-finish-date [calendar] it-cal-finish-date)
  (get-ranges [calendar] [[it-cal-start-date it-cal-finish-date]]))


(defrecord TFSTrackerDaysOffCalendar [do-cal-ranges]
  proto/TrackerCalendarProto
  (get-range-start-date [calendar] (apply min (map first do-cal-ranges)))
  (get-range-finish-date [calendar] (apply max (map second do-cal-ranges)))
  (get-ranges [calendar] do-cal-ranges))


(defrecord TFSTrackerIteration [tracker it-id it-name it-url it-path it-calendar it-current]
  proto/TrackerIterationProto
  (get-id [iteration] it-id)
  (get-calendar [iteration] it-calendar)
  (get-capacity [iteration] (private-get-tfs-capacity tracker iteration)))


(defrecord TFSTrackerCapacity [cap-iteration cap-teammember cap-daysoff]
  proto/TrackerCapacityProto
  (get-iteration [capacity] cap-iteration)
  (get-teammember [capacity] cap-teammember)
  (get-daysoff [capacity] cap-daysoff))


;;
;; Private definitions
;;

(defn- private-get-tfs-iterations-before-map [tracker]
  (map #(map->TFSTrackerIteration
         (let [iteration-inner %
               parse (partial clj-time.format/parse (clj-time.format/formatters :date-time-no-ms))
               start-date (parse (get-in iteration-inner [:attributes :startDate]))
               finish-date (parse (get-in iteration-inner [:attributes :finishDate]))
               current-date (clj-time.local/local-now)]
           {:tracker tracker
            :it-id (get iteration-inner :id)
            :it-name (get iteration-inner :name)
            :it-path (get iteration-inner :path)
            :it-url (get iteration-inner :url)
            :it-calendar (map->TFSTrackerIterationCalendar {:it-cal-start-date start-date
                                                            :it-cal-finish-date finish-date})
            :it-current (and (<= (compare start-date current-date) 0)
                             (<= (compare current-date finish-date) 0))}))
       (common/get-tfs-iterations-inner tracker)))


(defn- private-get-tfs-iterations [tracker]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :iterations-map-function]
               (fn [iteration] iteration))
       (private-get-tfs-iterations-before-map tracker)))


(defn- private-get-tfs-capacity-before-map [tracker iteration]
  (map #(map->TFSTrackerCapacity
         {:cap-iteration iteration
          :cap-teammember (get-in % [:teamMember :displayName])
          :cap-daysoff (map->TFSTrackerDaysOffCalendar {:do-cal-ranges (map (fn [days-off-record]
                                                                              (vector (get days-off-record :start)
                                                                                      (get days-off-record :end)))
                                                                            (get % :daysOff))})})
       (common/get-tfs-capacity-inner tracker iteration)))


(defn- private-get-tfs-capacity [tracker iteration]
  (map (get-in (proto/get-tracker-component tracker)
               [:context :capacity-map-function]
               (fn [capacity] capacity))
       (private-get-tfs-capacity-before-map tracker iteration)))
