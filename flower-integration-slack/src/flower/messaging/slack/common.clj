(ns flower.messaging.slack.common
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [clj-slack.channels :as channels]
            [clj-slack.chat :as chat]
            [clj-slack.groups :as groups]
            [clj-slack.rtm :as rtm]
            [clj-slack.users :as users]
            [gniazdo.core :as gniazdo]
            [flower.macros :as macros]
            [flower.messaging.proto :as proto]))


;;
;; Public definitions
;;

(macros/public-definition get-message-box-conn-inner always-cached)
(macros/public-definition get-slack-groups-inner cached)
(macros/public-definition get-slack-channels-inner cached)
(macros/public-definition is-slack-group-inner cached)
(macros/public-definition is-slack-channel-inner cached)
(macros/public-definition get-slack-channel-id-by-name-inner cached)
(macros/public-definition get-slack-channel-name-by-id-inner cached)
(macros/public-definition get-slack-user-name-by-id-inner cached)
(macros/public-definition search-slack-messages-inner cached)
(macros/public-definition send-slack-message-inner!)
(macros/public-definition subscribe-inner)


;;
;; Private definitions
;;

(defn- private-get-message-box-conn-inner
  ([message-box] (private-get-message-box-conn-inner message-box false))
  ([message-box is-bot] (let [auth (get-in (proto/get-message-box-component message-box)
                                           [:auth]
                                           {})
                              token (get auth (if is-bot
                                                :slack-bot-token
                                                :slack-token))]
                          {:api-url "https://slack.com/api"
                           :token token})))


(defn- private-get-slack-groups-inner [conn-inner]
  (groups/list conn-inner))


(defn- private-get-slack-channels-inner [conn-inner]
  (channels/list conn-inner))


(defn- private-search-slack-channel [channel-list outer-property filter-property filter-value inner-property]
  (get (first (filter #(= (get % filter-property) filter-value)
                      (get channel-list outer-property)))
       inner-property))


(defn- private-is-slack-group-inner [conn-inner group-name]
  (private-search-slack-channel (get-slack-groups-inner conn-inner) :groups :name group-name :is_group))


(defn- private-is-slack-channel-inner [conn-inner channel-name]
  (private-search-slack-channel (get-slack-channels-inner conn-inner) :channels :name channel-name :is_channel))


(defn- private-get-slack-channel-id-by-name-inner [conn-inner channel-name]
  (or (private-search-slack-channel (get-slack-groups-inner conn-inner) :groups :name channel-name :id)
      (private-search-slack-channel (get-slack-channels-inner conn-inner) :channels :name channel-name :id)))


(defn- private-get-slack-channel-name-by-id-inner [conn-inner channel-id]
  (or (private-search-slack-channel (get-slack-groups-inner conn-inner) :groups :id channel-id :name)
      (private-search-slack-channel (get-slack-channels-inner conn-inner) :channels :id channel-id :name)))


(defn- private-send-slack-message-inner! [message]
  (let [message-box (proto/get-message-box message)
        conn-inner (get-message-box-conn-inner message-box)
        title (proto/get-title message)]
    (loop [[recipient & rest] (filter identity
                                      (map (fn [rcpt]
                                             (get-slack-channel-id-by-name-inner conn-inner rcpt))
                                           (proto/get-recipients message)))]
      (chat/post-message conn-inner recipient title)
      (when rest
        (recur rest)))))


(defn- private-get-slack-message-inner [conn-inner message]
  (let [user-id-inner (get message :user)
        channel-id-inner (get message :channel)
        user-info (when user-id-inner
                    (get-slack-user-name-by-id-inner conn-inner
                                                     user-id-inner))
        username (get-in user-info [:user :name])
        channelname (when channel-id-inner
                      (get-slack-channel-name-by-id-inner conn-inner
                                                          channel-id-inner))]
    (merge message
           {:username username
            :channelname channelname})))


(defn- private-search-slack-messages-inner [conn-inner params]
  (let [{count :count
         load-body :load-body
         subject-filters :subject-filters
         folder-name :folder-name} params
        has-filters? (not (empty? subject-filters))
        folder (when folder-name
                 (get-slack-channel-id-by-name-inner conn-inner folder-name))]
    (when folder
      (map (partial private-get-slack-message-inner conn-inner)
           (get (apply (cond
                         (is-slack-channel-inner conn-inner folder-name) channels/history
                         (is-slack-group-inner conn-inner folder-name) groups/history)
                       [conn-inner folder {:count (str count)}])
                :messages [])))))


(defn- private-get-slack-user-name-by-id-inner [conn-inner user-id-inner]
  (users/info conn-inner user-id-inner))


(defn- private-connect-to-slack-websocket [conn-inner ws-url channel channel-inner]
  (gniazdo/connect ws-url
                   :on-close (fn [code reason]
                               (async/close! channel-inner))
                   :on-error (fn [])
                   :on-receive (fn [message]
                                 (let [json-message (json/read-str message
                                                                   :key-fn keyword)
                                       message-type (get json-message :type)
                                       message (private-get-slack-message-inner conn-inner
                                                                                json-message)]
                                   (when (and (not (.closed? channel-inner))
                                              (not (.closed? channel))
                                              (= message-type "message"))
                                     (async/go (async/>! channel-inner message)))))))


(defn- private-subscribe-inner [conn-inner params channel]
  (let [{subject-filters :subject-filters
         folder-name :folder-name} params
        has-filters? (not (empty? subject-filters))
        folder (when folder-name
                 (get-slack-channel-id-by-name-inner conn-inner folder-name))
        channel-inner (async/chan)]
    (when (and conn-inner
               folder)
      (let [connection-data (rtm/connect conn-inner)
            ws-url (get connection-data :url)]
        (if ws-url
          (future (let [ws-connection (private-connect-to-slack-websocket conn-inner
                                                                          ws-url
                                                                          channel
                                                                          channel-inner)]
                    (async/go-loop []
                      (if (or (.closed? channel-inner)
                              (.closed? channel))
                        (do (gniazdo/close ws-connection)
                            (async/close! channel-inner))
                        (recur)))))
          (async/close! channel-inner))))
    channel-inner))
