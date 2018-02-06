(ns flower.messaging.core
  (:require [com.stuartsierra.component :as component]
            [flower.common :as common]
            [flower.messaging.proto :as proto]
            [flower.messaging.exchange.mailbox :as exchange.mailbox]
            [flower.messaging.slack.mailbox :as slack.mailbox]))


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
        (map (fn [[messaging-name {messaging-type :messaging-type}]]
               [messaging-name [((case messaging-type
                                   :exchange exchange.mailbox/map->ExchangeMessagebox
                                   :slack slack.mailbox/map->SlackMessagebox)
                                 {:msg-component messaging-component
                                  :folder-name (get-in messaging-component [:context :folder-name])})]])
             messaging)))


(defn start-component [args]
  (map->MessagingComponent args))


(def ^:dynamic *messaging-type* nil)


(defmacro with-messaging-type [messaging-type & body]
  `(binding [flower.messaging.core/*messaging-type* ~messaging-type]
     ~@body))


(defn get-messaging-info [& messaging-full-url]
  {:messaging-type *messaging-type*
   :messaging-name *messaging-type*})


(defn get-messaging [& messaging-full-url]
  (let [messaging-info (apply get-messaging-info messaging-full-url)
        messaging-name (get messaging-info :messaging-name :messaging)]
    (first (get (messaging (start-component {:auth common/*component-auth*
                                             :context common/*component-context*})
                           {messaging-name messaging-info})
                messaging-name))))
