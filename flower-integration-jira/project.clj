(defproject flower/flower-integration-jira "0.4.7-SNAPSHOT"
  :description "Flower integration with Atlassian Jira"
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
  :repositories {"atlassian-public" "https://packages.atlassian.com/repository/public/"
                 "eclipse-releases" "https://repo.eclipse.org/content/groups/releases/"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/core.memoize "0.7.2"]
                 [joda-time/joda-time "2.10.3"]
                 [org.slf4j/slf4j-api "1.7.28"]
                 [com.atlassian.fugue/fugue "2.7.0"]
                 [com.atlassian.jira/jira-rest-java-client-core "5.0.4" :upgrade false]
                 [com.atlassian.jira/jira-rest-java-client-api "5.0.4" :upgrade false]
                 [clj-time "0.15.2"]
                 [flower/flower-common "0.4.7-SNAPSHOT"]
                 [flower/flower-proto "0.4.7-SNAPSHOT"]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
