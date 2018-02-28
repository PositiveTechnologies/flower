(ns flower.messaging.default.mailbox
  (:require [clojure.core.async :as async]
            [flower.messaging.proto :as proto]))


;;
;; Private declarations
;;

(declare private-search-messages)
(declare private-subscribe)


;;
;; Public definitions
;;

(defrecord DefaultMessagebox [msg-component msg-name msg-root]
  proto/MessageboxProto
  (get-message-box-component [message-box] msg-component)
  (get-messaging-type [message-box] :default)
  (get-message-box-root [message-box] msg-root)
  (search-messages [message-box] (private-search-messages message-box {}))
  (search-messages [message-box params] (private-search-messages message-box params))
  (subscribe [message-box] (private-subscribe message-box {}))
  (subscribe [message-box params] (private-subscribe message-box params)))


;;
;; Private definitions
;;

(defn- private-search-messages [message-box params]
  nil)


(defn- private-subscribe [message-box params]
  (async/chan))
