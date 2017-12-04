(ns flower.macros)


;;
;; Macro definitions
;;

(defmacro public-definition [public-fn & is-cached-args]
  (let [private-fn (symbol (str 'private- public-fn))
        public-clear-cache-fn (symbol (str public-fn '-clear-cache!))]
    (if (and is-cached-args
             (.contains '(true cached)
                        (first is-cached-args)))
      `(do (require 'clojure.core.memoize)
           (declare ~private-fn)
           (def ~public-fn
             (clojure.core.memoize/memo
              (fn [& args-list#]
                (apply ~private-fn args-list#))))
           (def ~public-clear-cache-fn (fn []
                                         (clojure.core.memoize/memo-clear! ~public-fn))))
      `(do (declare ~private-fn)
           (def ~public-fn
             (fn [& args-list#]
               (apply ~private-fn args-list#)))))))


(defmacro with-default-credentials [& body]
  `(do (require 'flower.common)
       (require 'flower.credentials)
       (def default-login# (flower.credentials/get-credentials :account :login))
       (def default-password# (flower.credentials/get-credentials :account :password))
       (def default-domain# (flower.credentials/get-credentials :account :domain))
       (def default-email# (flower.credentials/get-credentials :account :email))
       (def default-credentials# (into (flower.common/->ComponentAuth)
                                       {:github-login default-login#
                                        :github-password default-password#
                                        :gitlab-login default-login#
                                        :gitlab-password default-password#
                                        :jira-login default-login#
                                        :jira-password default-password#
                                        :tfs-login (str default-domain# "\\" default-login#)
                                        :tfs-password default-password#
                                        :message-box-username default-login#
                                        :message-box-password default-password#
                                        :message-box-domain default-domain#
                                        :message-box-email default-email#}))
       (binding [flower.common/*component-auth* default-credentials#]
         ~@body)))
