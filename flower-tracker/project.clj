(defproject flower/flower-tracker "0.4.7"
  :description "Flower trackers integration"
  :url "http://github.com/PositiveTechnologies/flower"
  :scm {:dir ".."}
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[org.clojure/core.unify "0.5.7"]
            [jonase/eastwood "0.3.11"]
            [lein-cljfmt "0.5.7"]
            [lein-bump-version "0.1.6"]]
  :cljfmt {:remove-consecutive-blank-lines? false}
  :aliases {"lint" ["do" ["cljfmt" "check"] ["eastwood"]]
            "test-all" ["with-profile" "default:+1.9" "test"]
            "lint-and-test-all" ["do" ["lint"] ["test-all"]]}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [com.stuartsierra/component "1.0.0"]
                 [flower/flower-common "0.4.7"]
                 [flower/flower-proto "0.4.7"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}})
