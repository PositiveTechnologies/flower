(ns flower.messaging.slack.common
  (:require [clj-slack.channels :as channels]
            [clj-slack.chat :as chat]
            [clj-slack.groups :as groups]
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
(macros/public-definition search-slack-messages-inner cached)
(macros/public-definition send-slack-message-inner!)


;;
;; Private definitions
;;

(defn- private-get-message-box-conn-inner [message-box]
  (let [auth (get-in (proto/get-message-box-component message-box)
                     [:auth]
                     {})
        token (get auth :slack-token)]
    {:api-url "https://slack.com/api"
     :token token}))


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


(defn- private-search-slack-messages-inner [conn-inner params]
  (let [{count :count
         load-body :load-body
         subject-filters :subject-filters
         folder-name :folder-name} params
        has-filters? (not (empty? subject-filters))
        folder (when folder-name
                 (get-slack-channel-id-by-name-inner conn-inner folder-name))]
    (when folder
      (get (apply (cond
                    (is-slack-channel-inner conn-inner folder-name) channels/history
                    (is-slack-group-inner conn-inner folder-name) groups/history)
                  [conn-inner folder {:count (str count)}])
           :messages))))
