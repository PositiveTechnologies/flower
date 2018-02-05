(ns flower.tracker.tfs.common
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [flower.macros :as macros]
            [flower.tracker.proto :as proto]))


;;
;; Public definitions
;;

(macros/public-definition get-tfs-workitems-inner cached)
(macros/public-definition get-tfs-query-inner cached)
(macros/public-definition get-tfs-iterations-inner cached)
(macros/public-definition get-tfs-capacity-inner cached)
(macros/public-definition set-tfs-workitem-inner!)


;;
;; Macros
;;

(defmacro with-tfs-auth [full-url query-params key & body]
  `(let [response# (client/get ~full-url {:basic-auth [~'login (or ~'token
                                                                   ~'password)]
                                          :content-type :json
                                          :accept :json
                                          :query-params ~query-params})
         response-body# (get response# :body "{}")
         ~'result (get (json/read-str response-body#
                                      :key-fn keyword)
                       ~key [])]
     ~@body))


(defmacro with-tfs-function [tracker project? url query-params key & body]
  (let [full-url (if project?
                   `(str (proto/get-project-url ~tracker) ~@url)
                   `(str (proto/get-tracker-url ~tracker) ~@url))]
    `(let [auth# (get-in (proto/get-tracker-component ~tracker)
                         [:auth]
                         {})
           ~'login (get auth# :tfs-login)
           ~'password (get auth# :tfs-password)
           ~'token (get auth# :tfs-token)]
       (with-tfs-auth ~full-url ~query-params ~key
         ~@body))))


;;
;; Private definitions
;;

(defn- private-get-tfs-workitems-inner [tracker task-ids]
  (if-not (empty? (filter identity task-ids))
    (let [query-string {:ids (clojure.string/join "," task-ids)}]
      (with-tfs-function tracker false ("/_apis/wit/workitems") query-string :value
        result))))


(defn- private-get-tfs-query-inner [tracker query-id]
  (with-tfs-function tracker false ("/_apis/wit/wiql/" query-id) {} :workItems
    (private-get-tfs-workitems-inner tracker
                                     (map :id result))))


(defn- private-get-tfs-iterations-inner [tracker]
  (with-tfs-function tracker true ("/_apis/work/teamsettings") {} :_links
    (with-tfs-auth (get-in result [:teamIterations :href]) {} :value
      result)))


(defn- private-get-tfs-capacity-inner [tracker iteration]
  (with-tfs-function tracker true ("/_apis/work/teamsettings") {} :_links
    (with-tfs-auth (get-in result [:teamIterations :href]) {} :value
      (if-let [iteration-url (-> (filter (fn [iteration-inner]
                                           (= 0 (compare (proto/get-iteration-id iteration)
                                                         (get iteration-inner :id))))
                                         result)
                                 (first)
                                 (get :url))]
        (with-tfs-auth iteration-url {} :_links
          (with-tfs-auth (get-in result [:capacity :href]) {} :value
            result))))))


(defn- private-set-tfs-workitem-inner! [tracker task-id fields]
  (let [auth (get-in (proto/get-tracker-component tracker)
                     [:auth]
                     {})
        login (get auth :tfs-login)
        password (get auth :tfs-password)
        token (get auth :tfs-token)
        wit (get fields :System.WorkItemType "Task")
        operations (map (fn [[key value]] {:op :add
                                           :path (str "/fields/" (name key))
                                           :value value})
                        fields)
        operations-str (json/write-str operations :escape-slash false)
        task-url (str (if task-id
                        (proto/get-tracker-url tracker)
                        (proto/get-project-url tracker))
                      "/_apis/wit/workitems/"
                      (if (integer? task-id)
                        (str task-id)
                        (if-not (empty? task-id)
                          task-id
                          (str "$" wit)))
                      "?api-version=1.0")]
    (if (empty? operations)
      {:id task-id}
      (let [response (client/patch task-url
                                   {:basic-auth [login (or token
                                                           password)]
                                    :content-type :json-patch+json
                                    :accept :json
                                    :body operations-str})
            response-body (get response :body "{}")]
        (json/read-str response-body :key-fn keyword)))))
