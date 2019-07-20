(defproject flower/flower-team "0.4.7-SNAPSHOT"
  :description "Flower common utilities"
  :url "http://github.com/PositiveTechnologies/flower"
  :scm {:dir ".."}
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[jonase/eastwood "0.3.5"]
            [lein-cljfmt "0.5.7"]
            [lein-bump-version "0.1.6"]]
  :cljfmt {:remove-consecutive-blank-lines? false}
  :aliases {"lint" ["do" ["cljfmt" "check"] ["eastwood"]]
            "test-all" ["with-profile" "default:+1.9:+1.8" "test"]
            "lint-and-test-all" ["do" ["lint"] ["test-all"]]}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
