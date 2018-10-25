(defproject flower "0.4.5"
  :description "Flower is a library for integration with task trackers, repositories, messaging systems and more"
  :url "http://github.com/PositiveTechnologies/flower"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[lein-sub "0.3.0"]
            [lein-ancient "0.6.15"]
            [lein-bump-version "0.1.6"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [flower/flower-tracker "0.4.5"]
                 [flower/flower-repository "0.4.5"]
                 [flower/flower-messaging "0.4.5"]
                 [flower/flower-team "0.4.5"]
                 [flower/flower-utilities "0.4.5"]
                 [flower/flower-integration-default "0.4.5"]
                 [flower/flower-integration-github "0.4.5"]
                 [flower/flower-integration-gitlab "0.4.5"]
                 [flower/flower-integration-jira "0.4.5"]
                 [flower/flower-integration-tfs "0.4.5"]]
  :aliases {"test" ["do" ["ancient-all"] ["sub" "lint-and-test-all"]]
            "bump-all" ["do" ["bump-version"] ["sub" "bump-version"]]
            "ancient-all" ["do" ["ancient"] ["sub" "ancient"]]
            "deploy-all" ["do" ["sub" "deploy" "clojars"] ["deploy" "clojars"]]}
  :sub ["flower-proto"
        "flower-common"
        "flower-integration-default"
        "flower-integration-github"
        "flower-integration-gitlab"
        "flower-integration-jira"
        "flower-integration-tfs"
        "flower-integration-exchange"
        "flower-integration-slack"
        "flower-tracker"
        "flower-repository"
        "flower-messaging"
        "flower-team"
        "flower-utilities"
        "lein-template"]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]}})
