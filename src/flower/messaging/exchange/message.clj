(ns flower.messaging.exchange.message
  (:require [flower.macros :as macros]
            [flower.messaging.proto :as proto]
            [flower.messaging.exchange.common :as common]))


;;
;; Private declarations
;;

(declare private-search-exchange-messages-inner)


;;
;; Public definitions
;;

(defrecord ExchangeMessage [msg-box msg-source msg-recipients msg-title msg-body]
  proto/MessageProto
  (get-message-box [message] msg-box)
  (get-source [message] msg-source)
  (get-recipients [message] msg-recipients)
  (get-title [message] msg-title)
  (get-body [message] msg-body)
  (send-message [message] (common/send-exchange-message-inner message)))


(macros/public-definition search-exchange-messages cached)

;;
;; Private definitions
;;

(defn- private-search-exchange-messages-before-map [message-box params]
  (let [{load-body :load-body} params]
    (map #(map->ExchangeMessage {:msg-box message-box
                                 :msg-source (.toString (.getFrom %))
                                 :msg-recipients (map (fn [item] (.toString item))
                                                      (.getItems (.getToRecipients %)))
                                 :msg-title (.getConversationTopic %)
                                 :msg-body (if load-body
                                             (.toString (.getBody %))
                                             nil)})
         (private-search-exchange-messages-inner message-box params))))


(defn- private-search-exchange-messages [message-box params]
  (map (get-in (proto/get-message-box-component message-box)
               [:context :messages-map-function]
               (fn [message] message))
       (private-search-exchange-messages-before-map message-box params)))


(defn- private-search-exchange-messages-inner [message-box params]
  (let [conn-inner (common/get-message-box-conn-inner message-box)]
    (common/search-exchange-messages-inner conn-inner params)))
