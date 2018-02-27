(defproject flower/flower-integration-gitlab "0.4.1-SNAPSHOT"
  :description "Flower integration with GitLab"
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
  :repositories {"sonatype" "https://oss.sonatype.org/content/repositories/releases"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [joda-time/joda-time "2.9.9"]
                 [org.gitlab/java-gitlab-api "4.0.0"]
                 [clj-time "0.14.2"]
                 [flower/flower-common "0.4.1-SNAPSHOT"]
                 [flower/flower-proto "0.4.1-SNAPSHOT"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
