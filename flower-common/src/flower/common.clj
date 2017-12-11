(ns flower.common)


;;
;; Public definitions
;;

(defrecord ComponentAuth [])


(defmethod print-method ComponentAuth [value ^java.io.Writer writer]
  (.write writer (str (reduce-kv (fn [map key value]
                                   (assoc map key (if (.endsWith (str key) "password")
                                                    (apply str (repeat (count value) "*"))
                                                    value)))
                                 {}
                                 value))))


(def ^:dynamic *component-auth* (ComponentAuth.))


(def ^:dynamic *component-context* {})
