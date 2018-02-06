(ns flower.messaging.slack.message
  (:require [flower.macros :as macros]
            [flower.messaging.proto :as proto]
            [flower.messaging.slack.common :as common]))


;;
;; Private declarations
;;

(declare private-search-slack-messages-inner)


;;
;; Public definitions
;;

(defrecord SlackMessage [msg-box msg-source msg-recipients msg-title msg-body]
  proto/MessageProto
  (get-message-box [message] msg-box)
  (get-source [message] msg-source)
  (get-recipients [message] msg-recipients)
  (get-title [message] msg-title)
  (get-body [message] msg-body)
  (send-message! [message] (common/send-slack-message-inner! message)))


(macros/public-definition search-slack-messages cached)

;;
;; Private definitions
;;

(defn- private-search-slack-messages-before-map [message-box params]
  (let [{load-body :load-body
         folder-name :folder-name} params]
    (map #(map->SlackMessage {:msg-box message-box
                              :msg-source (get % :username)
                              :msg-recipients (list folder-name)
                              :msg-title (get % :text)
                              :msg-body ""})
         (private-search-slack-messages-inner message-box params))))


(defn- private-search-slack-messages [message-box params]
  (map (get-in (proto/get-message-box-component message-box)
               [:context :messages-map-function]
               (fn [message] message))
       (private-search-slack-messages-before-map message-box params)))


(defn- private-search-slack-messages-inner [message-box params]
  (let [conn-inner (common/get-message-box-conn-inner message-box)]
    (common/search-slack-messages-inner conn-inner params)))
