(ns flower.messaging.exchange.message
  (:require [clojure.core.async :as async]
            [flower.macros :as macros]
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
  (send-message! [message] (common/send-exchange-message-inner! message)))


(macros/public-definition search-exchange-messages cached)
(macros/public-definition subscribe)

;;
;; Private definitions
;;

(defn- private-message-from-inner [message-box load-body message-inner]
  (map->ExchangeMessage {:msg-box message-box
                         :msg-source (.toString (.getFrom message-inner))
                         :msg-recipients (map (fn [item] (.toString item))
                                              (.getItems (.getToRecipients message-inner)))
                         :msg-title (.getConversationTopic message-inner)
                         :msg-body (if load-body
                                     (.toString (.getBody message-inner))
                                     nil)}))


(defn- private-search-exchange-messages-before-map [message-box params]
  (let [{load-body :load-body} params]
    (map (partial private-message-from-inner message-box load-body)
         (private-search-exchange-messages-inner message-box params))))


(defn- private-search-exchange-messages [message-box params]
  (map (get-in (proto/get-message-box-component message-box)
               [:context :messages-map-function]
               (fn [message] message))
       (private-search-exchange-messages-before-map message-box params)))


(defn- private-subscribe [message-box params]
  (let [{load-body :load-body
         folder-name :folder-name} params
        conn-inner (common/get-message-box-conn-inner message-box)
        channel (async/chan)
        channel-inner (common/subscribe-inner conn-inner params channel)]
    (async/go-loop []
      (if (or (.closed? channel-inner)
              (.closed? channel))
        (do (println channel-inner channel)
            (async/close! channel-inner)
            (async/close! channel))
        (let [message-inner (async/<! channel-inner)
              message (when message-inner
                        (private-message-from-inner message-box
                                                    load-body
                                                    message-inner))]
          (when message
            (async/>! channel message)
            (recur)))))
    channel))


(defn- private-search-exchange-messages-inner [message-box params]
  (let [conn-inner (common/get-message-box-conn-inner message-box)]
    (common/search-exchange-messages-inner conn-inner params)))
