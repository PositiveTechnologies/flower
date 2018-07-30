(ns flower.tracker.proto)


;;
;; Protocol definitions
;;

(defprotocol TrackerProto
  (get-tracker-component [tracker])
  (tracker-name-only [tracker])
  (get-tracker-type [tracker])
  (get-project-name [tracker])
  (get-projects [tracker])
  (get-tasks
    [tracker]
    [tracker query])
  (get-tracker-url [tracker])
  (get-project-url [tracker])
  (get-iterations [tracker]))


(defprotocol TrackerTaskProto
  (get-task-id [tracker-task])
  (get-task-url [tracker-task])
  (get-tracker [tracker-task])
  (get-state [tracker-task])
  (get-type [tracker-task])
  (get-related-tasks
    [tracker-task]
    [tracker-task relation-type])
  (get-related-task-types [tracker-task])
  (get-comments [tracker-task])
  (upsert! [tracker-task]))


(defprotocol TrackerTaskCommentProto
  (get-author [tracker-task-comment])
  (get-text [tracker-task-comment]))


(defprotocol TrackerIterationProto
  (get-iteration-id [iteration])
  (get-calendar [iteration])
  (get-capacity [iteration]))


(defprotocol TrackerCapacityProto
  (get-iteration [capacity])
  (get-teammember [capacity])
  (get-daysoff [capacity]))


(defprotocol TrackerCalendarProto
  (get-range-start-date [calendar])
  (get-range-finish-date [calendar])
  (get-ranges [calendar]))
