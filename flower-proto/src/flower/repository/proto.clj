(ns flower.repository.proto)


;;
;; Protocol definitions
;;

(defprotocol RepositoryProto
  (get-repository-component [repository])
  (repository-name-only [repository])
  (get-project-name [repository])
  (get-projects [repository])
  (get-pull-requests
    [repository]
    [repository options])
  (get-repository-url [tracker]))


(defprotocol RepositoryPullRequestProto
  (get-repository [pull-request])
  (get-pull-request-id [pull-request])
  (get-state [pull-request])
  (get-source-branch [pull-request])
  (get-target-branch [pull-request])
  (get-title [pull-request])
  (get-counters [pull-request])
  (get-commits [pull-request])
  (get-files [pull-request])
  (merge-pull-request!
    [pull-request]
    [pull-request message]))


(defprotocol RepositoryPullRequestCounterProto
  (get-wips [pull-request-counters]))
