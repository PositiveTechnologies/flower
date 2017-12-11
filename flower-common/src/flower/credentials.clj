(ns flower.credentials
  (:require [cprop.core :as cprop]))


;;
;; Dynamic variables
;;

(def ^:dynamic *credentials-file*
  (clojure.string/join "/" [(System/getProperty "user.home")
                            ".credentials.edn"]))


;;
;; Public definitions
;;

(defn get-credentials [& path]
  (get-in (cprop/load-config :file *credentials-file*)
          path))
