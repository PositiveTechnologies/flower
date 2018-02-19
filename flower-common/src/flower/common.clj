(ns flower.common)


;;
;; Public definitions
;;

(defrecord ComponentAuth [])


(defmethod print-method ComponentAuth [value ^java.io.Writer writer]
  (.write writer (str (reduce-kv (fn [map key value]
                                   (assoc map key (if (or (.endsWith (str key) "password")
                                                          (.endsWith (str key) "token"))
                                                    (apply str (repeat (count value) "*"))
                                                    value)))
                                 {}
                                 value))))


(def ^:dynamic *component-auth* (ComponentAuth.))
(def ^:dynamic *component-context* {})


(def ^:dynamic *behavior-implicit-cache* true)
(def ^:dynamic *behavior-implicit-cache-cleaning* true)
(def ^:dynamic *behavior-suppress-warnings-on-loading-libraries* true)
