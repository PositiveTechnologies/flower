(ns flower.messaging.default.message
  (:require [flower.messaging.proto :as proto]))


;;
;; Public definitions
;;

(defrecord DefaultMessage [msg-box msg-source msg-recipients msg-title msg-body]
  proto/MessageProto
  (get-message-box [message] msg-box)
  (get-source [message] msg-source)
  (get-recipients [message] msg-recipients)
  (get-title [message] msg-title)
  (get-body [message] msg-body)
  (send-message! [message] message))
