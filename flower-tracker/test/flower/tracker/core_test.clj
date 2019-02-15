(ns flower.tracker.core-test
  (:require [clojure.test :as test]
            [flower.tracker.core :as tracker.core]))


(test/deftest test-tracker-info
  (test/testing "Getting tracker info"
    (test/testing "for GitHub"
      (test/is (= (tracker.core/get-tracker-info "https://github.com/PositiveTechnologies/flower")
                  {:tracker-type :github
                   :tracker-url "https://github.com/PositiveTechnologies"
                   :tracker-ns nil
                   :tracker-projects ["PositiveTechnologies/flower"]
                   :tracker-name :github-github.com-flower}))
      (test/is (= (tracker.core/get-tracker-info "https://github.com/example/test")
                  {:tracker-type :github
                   :tracker-url "https://github.com/example"
                   :tracker-ns nil
                   :tracker-projects ["example/test"]
                   :tracker-name :github-github.com-test})))

    (test/testing "for GitLab"
      (test/is (= (tracker.core/get-tracker-info "https://gitlab.com/example/test")
                  {:tracker-type :gitlab
                   :tracker-url "https://gitlab.com"
                   :tracker-ns nil
                   :tracker-projects ["example/test"]
                   :tracker-name :gitlab-gitlab.com-test}))
      (test/is (= (tracker.core/get-tracker-info "https://gitlab.example.com/example/test")
                  {:tracker-type :gitlab
                   :tracker-url "https://gitlab.example.com"
                   :tracker-ns nil
                   :tracker-projects ["example/test"]
                   :tracker-name :gitlab-gitlab.example.com-test}))
      (test/is (= (tracker.core/with-tracker-type :gitlab
                    (tracker.core/get-tracker-info "https://hello.example.com/example/test"))
                  {:tracker-type :gitlab
                   :tracker-url "https://hello.example.com"
                   :tracker-ns nil
                   :tracker-projects ["example/test"]
                   :tracker-name :gitlab-hello.example.com-test})))

    (test/testing "for Jira"
      (test/is (= (tracker.core/get-tracker-info "https://jira.example.com/browse/TEST")
                  {:tracker-type :jira
                   :tracker-url "https://jira.example.com"
                   :tracker-ns nil
                   :tracker-projects ["TEST"]
                   :tracker-name :jira-jira.example.com-TEST}))
      (test/is (= (tracker.core/with-tracker-type :jira
                    (tracker.core/get-tracker-info "https://hello.example.com/browse/TEST"))
                  {:tracker-type :jira
                   :tracker-url "https://hello.example.com"
                   :tracker-ns nil
                   :tracker-projects ["TEST"]
                   :tracker-name :jira-hello.example.com-TEST})))

    (test/testing "for TFS"
      (test/is (= (tracker.core/get-tracker-info "https://tfs.example.com/tfs/Example/TEST")
                  {:tracker-type :tfs
                   :tracker-url "https://tfs.example.com/tfs/Example"
                   :tracker-ns nil
                   :tracker-projects ["Example/TEST"]
                   :tracker-name :tfs-tfs.example.com-TEST}))
      (test/is (= (tracker.core/with-tracker-type :tfs
                    (tracker.core/get-tracker-info "https://hello.example.com/tfs/Example/TEST"))
                  {:tracker-type :tfs
                   :tracker-url "https://hello.example.com/tfs/Example"
                   :tracker-ns nil
                   :tracker-projects ["Example/TEST"]
                   :tracker-name :tfs-hello.example.com-TEST})))

    (test/testing "unrecognized"
      (test/is (= (tracker.core/get-tracker-info "https://hello.example.com/example/test")
                  {:tracker-type :default
                   :tracker-name :default-hello.example.com
                   :tracker-url "https://hello.example.com"})))))
