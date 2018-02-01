(defproject flower "0.4.0-SNAPSHOT"
  :description "Flower is a library for integration with task trackers, repositories, messaging systems and more"
  :url "http://github.com/PositiveTechnologies/flower"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[lein-sub "0.3.0"]
            [lein-bump-version "0.1.6"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [flower/flower-tracker "0.4.0-SNAPSHOT"]
                 [flower/flower-repository "0.4.0-SNAPSHOT"]
                 [flower/flower-messaging "0.4.0-SNAPSHOT"]
                 [flower/flower-team "0.4.0-SNAPSHOT"]]
  :aliases {"test" ["sub" "lint-and-test-all"]
            "bump-all" ["do" ["bump-version"] ["sub" "bump-version"]]}
  :sub ["flower-proto"
        "flower-common"
        "flower-integration-github"
        "flower-integration-gitlab"
        "flower-integration-jira"
        "flower-integration-tfs"
        "flower-integration-exchange"
        "flower-tracker"
        "flower-repository"
        "flower-messaging"
        "flower-team"
        "lein-template"]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
