(ns flower.resolver
  (:require [clojure.string :as string]
            [cemerick.pomegranate :as pomegranate]
            [cemerick.pomegranate.aether :as aether]
            [trptcolin.versioneer.core :as versioneer.core]
            [flower.common :as common]
            [flower.macros :as macros])
  (:import (clojure.lang DynamicClassLoader)))

;;
;; Public definitions
;;

(macros/public-definition get-flower-version always-cached)
(macros/public-definition get-dependency-symbol always-cached)
(macros/public-definition get-require-name-type always-cached)
(macros/public-definition get-require-name-subtype always-cached)
(macros/public-definition get-require-symbol always-cached)
(macros/public-definition get-implementation-name-component always-cached)
(macros/public-definition get-implementation-symbol always-cached)
(macros/public-definition load-dependency)
(macros/public-definition resolve-implementation always-cached)


;;
;; Private definitions
;;

(defn- private-get-flower-version []
  (versioneer.core/get-version "flower" "flower-common"))


(defn- private-get-dependency-symbol [integration-name]
  (symbol (str "flower/flower-integration-" integration-name)))


(defn- private-get-require-name-type [integration-type]
  (case integration-type
    "task" "tracker"
    integration-type))


(defn- private-get-require-name-subtype [integration-type]
  (case integration-type
    "repository" "repo"
    "messaging" "mailbox"
    integration-type))


(defn- private-get-require-symbol [require-name-type integration-name require-name-subtype]
  (symbol (str "flower." require-name-type "." integration-name "." require-name-subtype)))


(defn- private-get-implementation-name-component [integration-type]
  (case integration-type
    "task" "TrackerTask"
    "messaging" "Messagebox"
    (string/capitalize integration-type)))


(defn- private-get-implementation-symbol [integration-name implementation-name-component]
  (symbol (str "map->"
               (if (> (count integration-name) 3)
                 (string/capitalize integration-name)
                 (string/upper-case integration-name))
               implementation-name-component)))


(defn private-ensure-dynamic-classloader []
  (let [thread (Thread/currentThread)
        cl (.getContextClassLoader thread)]
    (when-not (instance? DynamicClassLoader cl)
      (.setContextClassLoader thread (DynamicClassLoader. cl)))
    cl))


(defn- private-load-dependency [dependency-symbol]
  (private-ensure-dynamic-classloader)
  (cemerick.pomegranate/add-dependencies :coordinates [[dependency-symbol (get-flower-version)]]
                                         :repositories (merge cemerick.pomegranate.aether/maven-central
                                                              {"clojars" "https://clojars.org/repo"}
                                                              common/*resolver-additional-repositories*)))


(defn- private-resolve-implementation [integration-name integration-type]
  (let [integration-name (name integration-name)
        integration-type (name integration-type)
        dependency-symbol (get-dependency-symbol integration-name)
        require-name-type (get-require-name-type integration-type)
        require-name-subtype (get-require-name-subtype integration-type)
        require-symbol (get-require-symbol require-name-type integration-name require-name-subtype)
        implementation-name-component (get-implementation-name-component integration-type)
        implementation-symbol (get-implementation-symbol integration-name implementation-name-component)
        old-err-stream (System/err)]
    (when common/*behavior-suppress-warnings-on-loading-libraries*
      (System/setErr (java.io.PrintStream. (proxy [java.io.OutputStream] [] (write [& _])))))
    (load-dependency dependency-symbol)
    (require require-symbol)
    (let [temp-name (gensym)]
      (refer require-symbol :rename {implementation-symbol temp-name})
      (System/setErr old-err-stream)
      (resolve temp-name))))
