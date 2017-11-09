(defproject com.ptsecurity/flower "0.1.4"
  :description "Flower is a library for integration with task trackers, repositories, messaging systems and more"
  :url "http://github.com/PositiveTechnologies/flower"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[jonase/eastwood "0.2.4"]]
  :aliases {"test" ["eastwood"]}
  :repositories {"atlassian-public" "https://maven.atlassian.com/content/repositories/atlassian-public/"
                 "sonatype" "https://oss.sonatype.org/content/repositories/releases"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/data.json "0.2.6"]
                 [com.stuartsierra/component "0.3.2"]
                 [tesser.core "1.0.2"]
                 [joda-time/joda-time "2.9.7"]
                 [org.gitlab/java-gitlab-api "1.2.8"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [com.atlassian.fugue/fugue "2.6.1"]
                 [com.atlassian.jira/jira-rest-java-client-core "4.0.0"]
                 [com.atlassian.jira/jira-rest-java-client-api "4.0.0"]
                 [org.eclipse.mylyn.github/org.eclipse.egit.github.core "2.1.5"]
                 [com.microsoft.ews-java-api/ews-java-api "2.0"]
                 [clj-http "3.6.1"]
                 [clj-time "0.13.0"]
                 [cprop "0.1.11"]])
