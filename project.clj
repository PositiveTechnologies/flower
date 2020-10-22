(defproject flower "0.4.7"
  :description "Flower is a library for integration with task trackers, repositories, messaging systems and more"
  :url "http://github.com/PositiveTechnologies/flower"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[org.clojure/core.unify "0.5.7"]
            [jonase/eastwood "0.3.11"]
            [lein-sub "0.3.0"]
            [lein-ancient "0.6.15" :exclusions [[rewrite-clj]]]
            [lein-bump-version "0.1.6"]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.unify "0.5.7"]
                 [flower/flower-tracker "0.4.7"]
                 [flower/flower-repository "0.4.7"]
                 [flower/flower-messaging "0.4.7"]
                 [flower/flower-team "0.4.7"]
                 [flower/flower-utilities "0.4.7"]
                 [flower/flower-integration-default "0.4.7"]
                 [flower/flower-integration-github "0.4.7"]
                 [flower/flower-integration-gitlab "0.4.7"]
                 [flower/flower-integration-jira "0.4.7"]
                 [flower/flower-integration-tfs "0.4.7"]]
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
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :dev {:dependencies [[org.clojure/tools.namespace "1.0.0"]]}})

