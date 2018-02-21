(ns flower.messaging.exchange.common
  (:require [clojure.core.async :as async]
            [flower.macros :as macros]
            [flower.messaging.proto :as proto]
            [flower.messaging.exchange.async :as exchange.async])
  (:import (microsoft.exchange.webservices.data.core ExchangeService PropertySet)
           (microsoft.exchange.webservices.data.core.enumeration.notification EventType)
           (microsoft.exchange.webservices.data.core.enumeration.property WellKnownFolderName)
           (microsoft.exchange.webservices.data.core.enumeration.search LogicalOperator)
           (microsoft.exchange.webservices.data.core.service.item EmailMessage)
           (microsoft.exchange.webservices.data.core.service.schema ItemSchema)
           (microsoft.exchange.webservices.data.credential WebCredentials)
           (microsoft.exchange.webservices.data.notification StreamingSubscriptionConnection)
           (microsoft.exchange.webservices.data.property.complex EmailAddress
                                                                 MessageBody)
           (microsoft.exchange.webservices.data.search ItemView)
           (microsoft.exchange.webservices.data.search.filter SearchFilter$ContainsSubstring
                                                              SearchFilter$SearchFilterCollection)))


;;
;; Public definitions
;;

(macros/public-definition get-message-box-conn-inner always-cached)
(macros/public-definition search-exchange-messages-inner cached)
(macros/public-definition send-exchange-message-inner!)
(macros/public-definition subscribe-inner)


;;
;; Private definitions
;;

(defn- private-get-message-box-conn-inner
  ([message-box] (let [auth (get-in (proto/get-message-box-component message-box)
                                    [:auth]
                                    {})
                       username (get auth :message-box-username)
                       password (get auth :message-box-password)
                       domain (get auth :message-box-domain)
                       email (get auth :message-box-email)]
                   (private-get-message-box-conn-inner message-box domain username email password)))
  ([message-box domain username email password] (let [credentials (WebCredentials. username password domain)]
                                                  (doto (new ExchangeService)
                                                    (.setCredentials credentials)
                                                    (.autodiscoverUrl email)))))


(defn- private-send-exchange-message-inner! [message]
  (let [message-box (proto/get-message-box message)
        conn-inner (get-message-box-conn-inner message-box)
        message-inner (doto (EmailMessage. conn-inner)
                        (.setSubject (proto/get-title message))
                        (.setBody (MessageBody. (proto/get-body message))))
        recipients-to-inner (.getToRecipients message-inner)
        recipients-cc-inner (.getCcRecipients message-inner)
        recipients (map #(EmailAddress. (re-find #"[^@<\s]+@[^@\s>]+" %))
                        (proto/get-recipients message))]
    (.add recipients-to-inner (first recipients))
    (if (> (count recipients) 1)
      (.addEmailRange recipients-cc-inner (.iterator (rest recipients))))
    (.send message-inner)
    message))


(defn- private-search-exchange-messages-inner [conn-inner params]
  (let [{count :count
         load-body :load-body
         subject-filters :subject-filters} params
        has-filters? (not (empty? subject-filters))
        filter-function (fn [subject-filter]
                          (SearchFilter$ContainsSubstring. ItemSchema/Subject
                                                           subject-filter))
        search-filter (if has-filters?
                        (SearchFilter$SearchFilterCollection. LogicalOperator/And
                                                              (map filter-function
                                                                   subject-filters)))]
    (loop [offset 0
           view (ItemView. 10)
           results (list)]
      (let [newoffset (+ offset 10)
            found-items (if has-filters?
                          (.findItems conn-inner
                                      WellKnownFolderName/Inbox
                                      search-filter
                                      view)
                          (.findItems conn-inner
                                      WellKnownFolderName/Inbox
                                      view))
            items (if load-body
                    (do (.loadPropertiesForItems conn-inner
                                                 found-items
                                                 PropertySet/FirstClassProperties)
                        (.getItems found-items))
                    (.getItems found-items))
            newresults (concat results items)]
        (if (or (not items)
                (and count
                     (> newoffset count)))
          (if count
            (take count newresults)
            newresults)
          (recur newoffset
                 (doto view
                   (.setOffset newoffset))
                 newresults))))))


(defn- private-subscribe-inner [conn-inner params channel]
  (let [{load-body :load-body} params
        channel-inner (async/chan)
        subscription (.subscribeToStreamingNotificationsOnAllFolders conn-inner
                                                                     (into-array EventType
                                                                                 [EventType/NewMail]))
        streaming-connection (StreamingSubscriptionConnection. conn-inner 30)]
    (future (let [streaming-connection' (exchange.async/connect-to-exchange-streaming conn-inner
                                                                                      streaming-connection
                                                                                      subscription
                                                                                      load-body
                                                                                      channel
                                                                                      channel-inner)]
              (async/go-loop []
                (if (or (.closed? channel-inner)
                        (.closed? channel))
                  (do (doto streaming-connection
                        .close
                        .clearNotificationEvent
                        .clearSubscriptionError
                        .clearDisconnect)
                      (.unsubscribe subscription))
                  (recur)))))
    channel-inner))
