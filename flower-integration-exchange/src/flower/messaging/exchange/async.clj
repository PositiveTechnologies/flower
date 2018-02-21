(ns flower.messaging.exchange.async
  (:require [clojure.core.async :as async]
            [flower.macros :as macros])
  (:import (microsoft.exchange.webservices.data.core ExchangeService PropertySet)
           (microsoft.exchange.webservices.data.core.service.item EmailMessage)
           (microsoft.exchange.webservices.data.notification StreamingSubscriptionConnection$ISubscriptionErrorDelegate
                                                             StreamingSubscriptionConnection$INotificationEventDelegate
                                                             StreamingSubscriptionConnection$ISubscriptionErrorDelegate)))

;;
;; Public definitions
;;

(macros/public-definition connect-to-exchange-streaming)


;;
;; Private definitions
;;

(defn- private-connect-to-exchange-streaming [conn-inner streaming-connection subscription load-body channel channel-inner]
  (let [on-disconnect (reify StreamingSubscriptionConnection$ISubscriptionErrorDelegate
                        (subscriptionErrorDelegate [_ sender args]
                          (when (and streaming-connection
                                     (not (.getIsOpen streaming-connection))
                                     (not (or (.closed? channel-inner)
                                              (.closed? channel))))
                            (.open streaming-connection))))
        on-notification-event (reify StreamingSubscriptionConnection$INotificationEventDelegate
                                (notificationEventDelegate [_ sender args]
                                  (if (or (.closed? channel-inner)
                                          (.closed? channel))
                                    (.close streaming-connection)
                                    (async/go-loop [[message-inner & rest] (map #(EmailMessage/bind conn-inner
                                                                                                    (.getItemId %))
                                                                                (.getEvents args))]
                                      (async/>! channel-inner (if load-body
                                                                (doto message-inner
                                                                  (.load PropertySet/FirstClassProperties))
                                                                message-inner))
                                      (when rest
                                        (recur rest))))))
        on-subscription-error (reify StreamingSubscriptionConnection$ISubscriptionErrorDelegate
                                (subscriptionErrorDelegate [_ sender args]
                                  (async/close! channel-inner)))]
    (doto streaming-connection
      (.addSubscription subscription)
      (.addOnDisconnect on-disconnect)
      (.addOnNotificationEvent on-notification-event)
      (.addOnSubscriptionError on-subscription-error)
      (.open))))
