(ns flower.messaging.proto)


;;
;; Protocol definitions
;;

(defprotocol MessageboxProto
  (get-message-box-component [message-box])
  (search-messages [message-box params]))


(defprotocol MessageProto
  (get-message-box [message])
  (get-source [message])
  (get-recipients [message])
  (get-title [message])
  (get-body [message])
  (send-message! [message]))
