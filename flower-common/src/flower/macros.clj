(ns flower.macros)


;;
;; Macro definitions
;;

(defmacro public-definition [public-fn & is-cached-args]
  (let [private-fn (symbol (str 'private- public-fn))
        cached-fn (symbol (str 'cached- public-fn))
        public-clear-cache-fn (symbol (str public-fn '-clear-cache!))
        cache-type (when is-cached-args
                     (keyword (first is-cached-args)))]
    (if cache-type
      `(do (require 'clojure.core.memoize)
           (require 'flower.common)
           (declare ~private-fn)
           (def ~cached-fn (clojure.core.memoize/memo
                            (fn [& cache-args-list#]
                              (apply ~private-fn cache-args-list#))))
           (def ~public-fn
             (fn [& args-list#]
               (apply (if (or flower.common/*behavior-implicit-cache*
                              (= ~cache-type :always-cached))
                        ~cached-fn
                        ~private-fn)
                      args-list#)))
           (def ~public-clear-cache-fn (fn []
                                         (clojure.core.memoize/memo-clear! ~cached-fn))))
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
       (def default-token-github# (flower.credentials/get-credentials :token :github))
       (def default-token-gitlab# (flower.credentials/get-credentials :token :gitlab))
       (def default-token-tfs# (flower.credentials/get-credentials :token :tfs))
       (def default-token-slack# (flower.credentials/get-credentials :token :slack))
       (def default-token-slack-bot# (flower.credentials/get-credentials :token :slack-bot))
       (def default-credentials# (into (flower.common/->ComponentAuth)
                                       {:github-login default-login#
                                        :github-password default-password#
                                        :github-token default-token-github#
                                        :gitlab-login default-login#
                                        :gitlab-password default-password#
                                        :gitlab-token default-token-gitlab#
                                        :jira-login default-login#
                                        :jira-password default-password#
                                        :tfs-login (str default-domain# "\\" default-login#)
                                        :tfs-password default-password#
                                        :tfs-token default-token-tfs#
                                        :slack-token default-token-slack#
                                        :slack-bot-token default-token-slack-bot#
                                        :message-box-username default-login#
                                        :message-box-password default-password#
                                        :message-box-domain default-domain#
                                        :message-box-email default-email#}))
       (binding [flower.common/*component-auth* default-credentials#]
         ~@body)))


(defmacro with-auth [auth & body]
  `(do (with-default-credentials
         (binding [flower.common/*component-auth* (merge flower.common/*component-auth*
                                                         ~auth)]
           ~@body))))


(defmacro with-component-context [component-context & body]
  `(do (require 'flower.common)
       (binding [flower.common/*component-context* ~component-context]
         ~@body)))


(defmacro without-implicit-cache [& body]
  `(do (require 'flower.common)
       (binding [flower.common/*behavior-implicit-cache* false]
         ~@body)))


(defmacro without-implicit-cache-cleaning [& body]
  `(do (require 'flower.common)
       (binding [flower.common/*behavior-implicit-cache-cleaning* false]
         ~@body)))


(defmacro future-or-delay [& body]
  `(do (require 'flower.common)
       (if flower.common/*behavior-do-long-operations-in-parallel*
         (future ~@body)
         (delay ~@body))))


(defmacro with-long-operations-in-parallel [& body]
  `(do (require 'flower.common)
       (binding [flower.common/*behavior-do-long-operations-in-parallel* true]
         ~@body)))
