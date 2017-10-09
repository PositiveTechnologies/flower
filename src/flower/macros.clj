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
