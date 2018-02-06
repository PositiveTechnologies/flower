(ns flower.messaging.slack.mailbox
  (:require [flower.macros :as macros]
            [flower.messaging.proto :as proto]
            [flower.messaging.slack.message :as message]))


;;
;; Private declarations
;;

(declare private-search-messages)


;;
;; Public definitions
;;

(defrecord SlackMessagebox [msg-component folder-name]
  proto/MessageboxProto
  (get-message-box-component [message-box] msg-component)
  (get-folder-name [message-box] folder-name)
  (search-messages [message-box params] (private-search-messages message-box params)))


;;
;; Private definitions
;;

(defn- private-search-messages [message-box params]
  (message/search-slack-messages message-box (merge {:folder-name (proto/get-folder-name message-box)}
                                                    params)))
