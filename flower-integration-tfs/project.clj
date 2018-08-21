(defproject flower/flower-integration-tfs "0.4.3"
  :description "Flower integration with Microsoft TFS"
  :url "http://github.com/PositiveTechnologies/flower"
  :scm {:dir ".."}
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[jonase/eastwood "0.2.5"]
            [lein-cljfmt "0.5.7"]
            [lein-bump-version "0.1.6"]]
  :cljfmt {:remove-consecutive-blank-lines? false}
  :aliases {"lint" ["do" ["cljfmt" "check"] ["eastwood"]]
            "test-all" ["with-profile" "default:+1.7:+1.8" "test"]
            "lint-and-test-all" ["do" ["lint"] ["test-all"]]}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [joda-time/joda-time "2.10"]
                 [clj-http "3.9.1"]
                 [clj-time "0.14.4"]
                 [flower/flower-common "0.4.3"]
                 [flower/flower-proto "0.4.3"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
