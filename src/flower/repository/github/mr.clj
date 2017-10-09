(ns flower.repository.github.mr
  (:require [flower.macros :as macros]
            [flower.repository.proto :as proto]
            [flower.repository.github.common :as common]))


;;
;; Private declarations
;;

(declare private-get-github-pull-requests-inner
         private-get-github-pull-request-counters)


;;
;; Public definitions
;;

(defrecord GithubRepositoryPullRequestCounters [count-upvotes
                                                count-downvotes
                                                count-merge-conflicts
                                                count-merge-conflicts-notes
                                                count-reassigns
                                                count-lgtms
                                                count-wips]
  proto/RepositoryPullRequestCounterProto
  (get-wips [pull-request-counters] count-wips))


(defrecord GithubRepositoryPullRequest [repository
                                        mr-id
                                        mr-title
                                        mr-state
                                        mr-target-branch
                                        mr-source-branch
                                        mr-author
                                        mr-assignee
                                        mr-counters
                                        task-ids]
  proto/RepositoryPullRequestProto
  (get-repository [pull-request] repository)
  (get-state [pull-request] mr-state)
  (get-source-branch [pull-request] mr-source-branch)
  (get-target-branch [pull-request] mr-target-branch)
  (get-title [pull-request] mr-title)
  (get-counters [pull-request] mr-counters))


(macros/public-definition get-github-pull-requests cached)


;;
;; Private definitions
;;

(defn- private-get-github-pull-requests-before-map [repository options]
  (map #(map->GithubRepositoryPullRequest
         {:repository repository
          :mr-id (.getNumber %)
          :mr-title (.getTitle %)
          :mr-state (let [state (.getState %)]
                      (if (= state "open")
                        "opened"
                        state))
          :mr-target-branch (.getRef (.getBase %))
          :mr-source-branch (.getRef (.getHead %))
          :mr-author (.getName (.getUser %))
          :mr-assignee (let [assignee (.getAssignee %)]
                         (if assignee
                           (.getName assignee)
                           nil))
          :mr-counters (private-get-github-pull-request-counters repository %)
          :task-ids (list)})
       (private-get-github-pull-requests-inner repository options)))


(defn- private-get-github-pull-requests [repository options]
  (map (get-in (proto/get-repository-component repository)
               [:context :pull-requests-map-function]
               (fn [pull-request] pull-request))
       (private-get-github-pull-requests-before-map repository options)))


(defn- private-get-pull-request-notes-counters-by-patterns [notes-list note-patterns]
  (let [full-text (apply str notes-list)
        counter-list (map #(vector % (-> (str "(?i)" %)
                                         (re-pattern)
                                         (re-seq full-text)
                                         (count)))
                          note-patterns)]
    (into (sorted-map) counter-list)))


(defn- private-get-github-pull-request-counters [repository pull-request]
  (let [notes (common/get-github-pull-request-comments-inner repository pull-request)
        notes-map (private-get-pull-request-notes-counters-by-patterns (map #(.getBody %) notes)
                                                                       ["merge conflict"
                                                                        "reassign"
                                                                        "lgtm"
                                                                        "wip"])]
    (map->GithubRepositoryPullRequestCounters
     {:count-upvotes 0
      :count-downvotes 0
      :count-merge-conflicts 0
      :count-merge-conflicts-notes (get notes-map "merge conflict" 0)
      :count-reassigns (get notes-map "reassign" 0)
      :count-lgtms (get notes-map "lgtm" 0)
      :count-wips (get notes-map "wip" 0)})))


(defn- private-get-github-pull-requests-inner [repository options]
  (common/get-github-pull-requests-inner repository
                                         (case (get options :mr-state)
                                           "opened" "open"
                                           "merged" "closed"
                                           "closed" "closed"
                                           nil "all")))
