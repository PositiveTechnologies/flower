(ns flower.tracker.gitlab.common
  (:require [flower.macros :as macros]
            [flower.tracker.proto :as proto])
  (:import org.gitlab.api.GitlabAPI))


;;
;; Public definitions
;;

(macros/public-definition get-gitlab-conn-inner cached)
(macros/public-definition get-gitlab-project-inner cached)
(macros/public-definition get-gitlab-projects-inner cached)
(macros/public-definition get-gitlab-workitems-inner cached)
(macros/public-definition get-gitlab-iterations-inner cached)
(macros/public-definition get-gitlab-capacity-inner cached)


;;
;; Private definitions
;;

(defn- private-get-gitlab-conn-inner
  ([tracker] (let [auth (get-in (proto/get-tracker-component tracker)
                                [:auth]
                                {})
                   login (get auth :gitlab-login)
                   password (get auth :gitlab-password)]
               (private-get-gitlab-conn-inner tracker login password)))
  ([tracker token] (GitlabAPI/connect (:tracker-url tracker) token))
  ([tracker login password] (let [gitlab-conn (GitlabAPI/connect (:tracker-url tracker)
                                                                 login
                                                                 password)]
                              (private-get-gitlab-conn-inner tracker
                                                             (.getPrivateToken gitlab-conn)))))


(defn- private-get-gitlab-projects-inner [tracker]
  (.getProjects (get-gitlab-conn-inner tracker)))


(defn- private-get-gitlab-project-inner [tracker]
  (let [project-name (proto/get-project-name tracker)]
    (-> (filter (fn [project]
                  (= (.getName project)
                     project-name))
                (private-get-gitlab-projects-inner tracker))
        (first))))


(defn- private-get-gitlab-workitems-inner
  ([tracker] (let [conn-inner (get-gitlab-conn-inner tracker)
                   project-inner (get-gitlab-project-inner tracker)]
               (.getIssues conn-inner project-inner)))
  ([tracker task-ids] (let [task-ids-list (into [] (map #(Integer. %) task-ids))]
                        (into [] (filter (fn [issue]
                                           (.contains task-ids-list (.getIid issue)))
                                         (get-gitlab-workitems-inner tracker))))))


(defn- private-get-gitlab-iterations-inner [tracker]
  (let [conn-inner (get-gitlab-conn-inner tracker)
        project-inner (get-gitlab-project-inner tracker)]
    (.getMilestones conn-inner project-inner)))


(defn- private-get-gitlab-capacity-inner [tracker iteration]
  nil)
