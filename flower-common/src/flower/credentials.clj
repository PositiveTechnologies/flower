(ns flower.credentials
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [cprop.core :as cprop]
            [cprop.source]))


;;
;; Dynamic variables
;;

(def ^:dynamic *credentials-file*
  (string/join "/" [(System/getProperty "user.home")
                    ".credentials.edn"]))


;;
;; Public definitions
;;

(defn get-credentials [& path]
  (get-in (if (.exists (io/as-file *credentials-file*))
            (cprop/load-config :file *credentials-file*)
            (cprop/load-config :merge [{:account {}
                                        :token {}
                                        :url {}}
                                       (select-keys (cprop.source/from-env)
                                                    [:account :token :url])]))
          path))
