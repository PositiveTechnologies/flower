(defproject flower/flower-integration-jira "0.3.0"
  :description "Flower integration with Atlassian Jira"
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
  :repositories {"atlassian-public" "https://maven.atlassian.com/content/repositories/atlassian-public/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.memoize "0.5.9"]
                 [joda-time/joda-time "2.9.9"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [com.atlassian.fugue/fugue "2.6.1"]
                 [com.atlassian.jira/jira-rest-java-client-core "4.0.0"]
                 [com.atlassian.jira/jira-rest-java-client-api "4.0.0"]
                 [clj-time "0.14.2"]
                 [flower/flower-common "0.3.0"]
                 [flower/flower-proto "0.3.0"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
