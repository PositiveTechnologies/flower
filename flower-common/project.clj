(defproject flower/flower-common "0.4.7"
  :description "Flower common utilities"
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
  :eastwood {:exclude-linters [:suspicious-expression]}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/core.memoize "1.0.236"]
                 [com.cemerick/pomegranate "1.1.0"]
                 [lambdaisland/uri "1.4.54"]
                 [cprop "0.1.17"]
                 [trptcolin/versioneer "0.2.0"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}})
