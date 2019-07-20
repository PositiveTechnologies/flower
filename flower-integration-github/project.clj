(defproject flower/flower-integration-github "0.4.7-SNAPSHOT"
  :description "Flower integration with GitHub"
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
            "test-all" ["with-profile" "default:+1.9:+1.8" "test"]
            "lint-and-test-all" ["do" ["lint"] ["test-all"]]}
  :repositories {"eclipse-releases" "https://repo.eclipse.org/content/groups/releases/"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [com.google.code.gson/gson "2.8.6"]
                 [org.eclipse.mylyn.github/org.eclipse.egit.github.core "4.9.0.201710071750-r"]
                 [clj-time "0.15.2"]
                 [flower/flower-common "0.4.7-SNAPSHOT"]
                 [flower/flower-proto "0.4.7-SNAPSHOT"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
