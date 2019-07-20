(defproject flower/flower-integration-gitlab "0.4.7-SNAPSHOT"
  :description "Flower integration with GitLab"
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
  :repositories {"sonatype" "https://oss.sonatype.org/content/repositories/releases"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [joda-time/joda-time "2.10.3"]
                 [org.gitlab/java-gitlab-api "4.1.0"]
                 [clj-time "0.15.1"]
                 [flower/flower-common "0.4.7-SNAPSHOT"]
                 [flower/flower-proto "0.4.7-SNAPSHOT"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
