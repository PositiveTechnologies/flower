(ns flower.repository.gitlab.common
  (:require [flower.macros :as macros]
            [flower.repository.proto :as proto])
  (:import org.gitlab.api.GitlabAPI))


;;
;; Public definitions
;;

(macros/public-definition get-gitlab-conn-inner cached)
(macros/public-definition get-gitlab-project-inner cached)
(macros/public-definition get-gitlab-projects-inner cached)


;;
;; Private definitions
;;

(defn- private-get-gitlab-conn-inner
  ([repository] (let [auth (get-in (proto/get-repository-component repository)
                                   [:auth]
                                   {})
                      login (get auth :gitlab-login)
                      password (get auth :gitlab-password)]
                  (private-get-gitlab-conn-inner repository login password)))
  ([repository token] (GitlabAPI/connect (:repo-url repository) token))
  ([repository login password] (let [gitlab-conn (GitlabAPI/connect (:repo-url repository)
                                                                    login
                                                                    password)]
                                 (private-get-gitlab-conn-inner repository
                                                                (.getPrivateToken gitlab-conn)))))


(defn- private-get-gitlab-projects-inner [repository]
  (.getProjects (get-gitlab-conn-inner repository)))


(defn- private-get-gitlab-project-inner [repository]
  (let [project-name (proto/get-project-name repository)]
    (-> (filter (fn [project] (= (.getName project)
                                 project-name))
                (private-get-gitlab-projects-inner repository))
        (first))))
