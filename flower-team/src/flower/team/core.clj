(ns flower.team.core)


;;
;; Macro definitions
;;

(defmacro defteammember [login fullname & description]
  `(def ~login (into (map->TeamMember {:tm-login '~login :tm-fullname '~fullname})
                     (apply hash-map [~@description]))))


(defmacro defteam [name & members]
  `(def ~name (list ~@members)))


;;
;; Record definitions
;;

(defrecord TeamMember [tm-login tm-fullname])
