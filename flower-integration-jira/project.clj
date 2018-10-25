(defproject flower/flower-integration-jira "0.4.4"
  :description "Flower integration with Atlassian Jira"
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
  :repositories {"atlassian-public" "https://maven.atlassian.com/content/repositories/atlassian-public/"
                 "eclipse-releases" "https://repo.eclipse.org/content/groups/releases/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.memoize "0.7.1"]
                 [joda-time/joda-time "2.10"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [com.atlassian.fugue/fugue "2.6.1" :upgrade false]
                 [com.atlassian.jira/jira-rest-java-client-core "5.0.4" :upgrade false]
                 [com.atlassian.jira/jira-rest-java-client-api "5.0.4" :upgrade false]
                 [clj-time "0.15.0"]
                 [flower/flower-common "0.4.4"]
                 [flower/flower-proto "0.4.4"]]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
