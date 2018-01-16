(defproject flower/flower-tracker "0.3.6"
  :description "Flower trackers integration"
  :url "http://github.com/PositiveTechnologies/flower"
  :scm {:dir ".."}
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[jonase/eastwood "0.2.5"]
            [lein-cljfmt "0.5.7"]]
  :cljfmt {:remove-consecutive-blank-lines? false}
  :aliases {"lint" ["do" ["cljfmt" "check"] ["eastwood"]]
            "test-all" ["with-profile" "default:+1.7:+1.8" "test"]
            "lint-and-test-all" ["do" ["lint"] ["test-all"]]}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.cemerick/url "0.1.1"]
                 [flower/flower-common "0.3.6"]
                 [flower/flower-proto "0.3.6"]
                 [flower/flower-integration-github "0.3.6"]
                 [flower/flower-integration-gitlab "0.3.6"]
                 [flower/flower-integration-jira "0.3.6"]
                 [flower/flower-integration-tfs "0.3.6"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
