(ns flower.messaging.slack.message
  (:require [clojure.core.async :as async]
            [flower.macros :as macros]
            [flower.messaging.proto :as proto]
            [flower.messaging.slack.common :as common]))


;;
;; Private declarations
;;

(declare private-search-slack-messages-inner)
(declare private-subscribe-inner)


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
(macros/public-definition subscribe)

;;
;; Private definitions
;;

(defn- private-message-from-inner [message-box load-body message-inner]
  (map->SlackMessage {:msg-box message-box
                      :msg-source (get message-inner :username)
                      :msg-recipients (list (get message-inner :channelname))
                      :msg-title (get message-inner :text "")
                      :msg-body (if load-body
                                  (get message-inner :text "")
                                  "")}))


(defn- private-search-slack-messages-before-map [message-box params]
  (let [{load-body :load-body
         folder-name :folder-name} params]
    (map #(private-message-from-inner message-box
                                      load-body
                                      (merge % {:channelname folder-name}))
         (private-search-slack-messages-inner message-box params))))


(defn- private-search-slack-messages [message-box params]
  (map (get-in (proto/get-message-box-component message-box)
               [:context :messages-map-function]
               (fn [message] message))
       (private-search-slack-messages-before-map message-box params)))


(defn- private-subscribe [message-box params]
  (let [{load-body :load-body
         folder-name :folder-name} params
        conn-inner (common/get-message-box-conn-inner message-box true)
        channel (async/chan)
        channel-inner (common/subscribe-inner conn-inner params channel)]
    (if channel-inner
      (do (async/go-loop []
            (if (or (.closed? channel-inner)
                    (.closed? channel))
              (do (async/close! channel-inner)
                  (async/close! channel))
              (let [message-inner (async/<! channel-inner)
                    message (when message-inner
                              (private-message-from-inner message-box
                                                          folder-name
                                                          message-inner))]
                (when message
                  (async/>! channel message)
                  (recur))))))
      (async/close! channel))
    channel))


(defn- private-search-slack-messages-inner [message-box params]
  (let [conn-inner (common/get-message-box-conn-inner message-box)]
    (common/search-slack-messages-inner conn-inner params)))
