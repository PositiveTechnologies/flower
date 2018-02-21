(ns flower.messaging.core
  (:require [com.stuartsierra.component :as component]
            [cemerick.url :as url]
            [flower.common :as common]
            [flower.resolver :as resolver]
            [flower.messaging.proto :as proto]))


;;
;; Public definitions
;;

(defrecord MessagingComponent [auth context]
  component/Lifecycle
  (start [component] (into component {:auth auth
                                      :context context}))
  (stop [component] (into component {:auth {}
                                     :context {}})))


(defn messaging [messaging-component messaging]
  (into {}
        (map (fn [[messaging-name {messaging-type :messaging-type
                                   messaging-url :messaging-url}]]
               (let [folder-name (get-in messaging-component [:context :folder-name])]
                 [messaging-name [((resolver/resolve-implementation messaging-type :messaging)
                                   (merge {:msg-component messaging-component
                                           :msg-name messaging-name}
                                          (when messaging-url
                                            {:msg-url messaging-url})
                                          (when folder-name
                                            {:folder-name folder-name})))]]))
             messaging)))


(defn start-component [args]
  (map->MessagingComponent args))


(def ^:dynamic *messaging-type* nil)
(def ^:dynamic *messaging-url* nil)


(defmacro with-messaging-type [messaging-type & body]
  `(binding [flower.messaging.core/*messaging-type* ~messaging-type]
     ~@body))


(defn get-messaging-info
  ([] (get-messaging-info nil))
  ([messaging-full-url] (merge {:messaging-type *messaging-type*
                                :messaging-name *messaging-type*}
                               (when messaging-full-url
                                 (let [messaging-url (url/url messaging-full-url)
                                       messaging-domain (get messaging-url :host)]
                                   {:messaging-type *messaging-type*
                                    :messaging-name (keyword (str (name *messaging-type*) "-" messaging-domain))
                                    :messaging-url (or *messaging-url* (str messaging-url))})))))


(defn get-messaging
  ([] (get-messaging nil))
  ([messaging-full-url] (let [messaging-info (get-messaging-info messaging-full-url)
                              messaging-name (get messaging-info :messaging-name :messaging)]
                          (first (get (messaging (start-component {:auth common/*component-auth*
                                                                   :context common/*component-context*})
                                                 {messaging-name messaging-info})
                                      messaging-name)))))
