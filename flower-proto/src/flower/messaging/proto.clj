(ns flower.messaging.proto)


;;
;; Protocol definitions
;;

(defprotocol MessageboxProto
  (get-message-box-component [message-box])
  (get-messaging-type [message-box])
  (get-folder-name [message-box])
  (search-messages
    [message-box]
    [message-box params])
  (subscribe
    [message-box]
    [message-box params]))


(defprotocol MessageProto
  (get-message-box [message])
  (get-source [message])
  (get-recipients [message])
  (get-title [message])
  (get-body [message])
  (send-message! [message]))
