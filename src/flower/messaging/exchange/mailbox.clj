(ns flower.messaging.exchange.mailbox
  (:require [flower.macros :as macros]
            [flower.messaging.proto :as proto]
            [flower.messaging.exchange.message :as message]))


;;
;; Private declarations
;;

(declare private-search-messages)


;;
;; Public definitions
;;

(defrecord ExchangeMessagebox [msg-component folder-name]
  proto/MessageboxProto
  (get-message-box-component [message-box] msg-component)
  (search-messages [message-box params] (private-search-messages message-box params)))


;;
;; Private definitions
;;

(defn- private-search-messages [message-box params]
  (message/search-exchange-messages message-box params))
