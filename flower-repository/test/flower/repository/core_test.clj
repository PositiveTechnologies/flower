(ns flower.repository.core-test
  (:require [clojure.test :as test]
            [flower.repository.core :as repository.core]))


(test/deftest test-repository-info
  (test/testing "Getting repository info"
    (test/testing "for GitHub"
      (test/is (= (repository.core/get-repository-info "https://github.com/PositiveTechnologies/flower")
                  {:repo-type :github
                   :repo-url "https://github.com/PositiveTechnologies"
                   :repository-ns nil
                   :repo-projects ["PositiveTechnologies/flower"]
                   :repo-name :github-github.com-flower}))
      (test/is (= (repository.core/get-repository-info "https://github.com/example/test")
                  {:repo-type :github
                   :repo-url "https://github.com/example"
                   :repository-ns nil
                   :repo-projects ["example/test"]
                   :repo-name :github-github.com-test})))

    (test/testing "for GitLab"
      (test/is (= (repository.core/get-repository-info "https://gitlab.com/example/test")
                  {:repo-type :gitlab
                   :repo-url "https://gitlab.com"
                   :repository-ns nil
                   :repo-projects ["example/test"]
                   :repo-name :gitlab-gitlab.com-test}))
      (test/is (= (repository.core/get-repository-info "https://gitlab.example.com/example/test")
                  {:repo-type :gitlab
                   :repo-url "https://gitlab.example.com"
                   :repository-ns nil
                   :repo-projects ["example/test"]
                   :repo-name :gitlab-gitlab.example.com-test}))
      (test/is (= (repository.core/with-repository-type :gitlab
                    (repository.core/get-repository-info "https://hello.example.com/example/test"))
                  {:repo-type :gitlab
                   :repo-url "https://hello.example.com"
                   :repository-ns nil
                   :repo-projects ["example/test"]
                   :repo-name :gitlab-hello.example.com-test})))

    (test/testing "unrecognized"
      (test/is (= (repository.core/get-repository-info "https://hello.example.com/example/test")
                  {:repo-type :default
                   :repo-name :default-hello.example.com
                   :repo-url "https://hello.example.com"})))))
