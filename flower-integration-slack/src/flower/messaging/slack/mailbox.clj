(ns flower.messaging.slack.mailbox
  (:require [flower.macros :as macros]
            [flower.messaging.proto :as proto]
            [flower.messaging.slack.message :as message]))


;;
;; Private declarations
;;

(declare private-search-messages)
(declare private-subscribe)


;;
;; Public definitions
;;

(defrecord SlackMessagebox [msg-component msg-name msg-root]
  proto/MessageboxProto
  (get-message-box-component [message-box] msg-component)
  (get-messaging-type [message-box] :slack)
  (get-message-box-root [message-box] msg-root)
  (search-messages [message-box] (private-search-messages message-box {}))
  (search-messages [message-box params] (private-search-messages message-box params))
  (subscribe [message-box] (private-subscribe message-box {}))
  (subscribe [message-box params] (private-subscribe message-box params)))


;;
;; Private definitions
;;

(defn- private-search-messages [message-box params]
  (message/search-slack-messages message-box (merge {:msg-root (proto/get-message-box-root message-box)}
                                                    params)))


(defn- private-subscribe [message-box params]
  (message/subscribe message-box (merge {:msg-root (proto/get-message-box-root message-box)}
                                        params)))
