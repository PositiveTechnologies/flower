(ns flower.repository.github.pr
  (:require [flower.macros :as macros]
            [flower.repository.proto :as proto]
            [flower.repository.github.common :as common]))


;;
;; Private declarations
;;

(declare private-get-github-pull-requests-inner
         private-get-github-pull-request-counters
         private-get-github-pull-request-commits
         private-get-github-pull-request-files
         private-merge-pull-request!)


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


(defrecord GithubRepositoryPullRequestCommit [commit-id
                                              commit-name
                                              commit-author-name
                                              commit-author-email
                                              commit-author-date
                                              commit-committer-name
                                              commit-committer-email
                                              commit-committer-date])


(defrecord GithubRepositoryPullRequestFile [file-name
                                            file-additions
                                            file-deletions
                                            file-changes])


(defrecord GithubRepositoryPullRequest [repository
                                        pr-id
                                        pr-title
                                        pr-state
                                        pr-target-branch
                                        pr-source-branch
                                        pr-author
                                        pr-assignee
                                        pr-counters
                                        task-ids]
  proto/RepositoryPullRequestProto
  (get-repository [pull-request] repository)
  (get-pull-request-id [pull-request] pr-id)
  (get-state [pull-request] pr-state)
  (get-source-branch [pull-request] pr-source-branch)
  (get-target-branch [pull-request] pr-target-branch)
  (get-title [pull-request] pr-title)
  (get-counters [pull-request] pr-counters)
  (get-commits [pull-request] (private-get-github-pull-request-commits repository
                                                                       pull-request))
  (get-files [pull-request] (private-get-github-pull-request-files repository
                                                                   pull-request))
  (merge-pull-request! [pull-request] (private-merge-pull-request! repository
                                                                   pull-request
                                                                   pr-id
                                                                   nil))
  (merge-pull-request! [pull-request message] (private-merge-pull-request! repository
                                                                           pull-request
                                                                           pr-id
                                                                           message)))


(macros/public-definition get-github-pull-requests cached)


;;
;; Private definitions
;;

(defn- private-get-github-pull-requests-before-map [repository options]
  (map #(map->GithubRepositoryPullRequest
         {:repository repository
          :pr-id (.getNumber %)
          :pr-title (.getTitle %)
          :pr-state (let [state (.getState %)]
                      (if (= state "open")
                        "opened"
                        state))
          :pr-target-branch (.getRef (.getBase %))
          :pr-source-branch (.getRef (.getHead %))
          :pr-author (.getName (.getUser %))
          :pr-assignee (let [assignee (.getAssignee %)]
                         (if assignee
                           (.getName assignee)
                           nil))
          :pr-counters (private-get-github-pull-request-counters repository %)
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


(defn- private-get-github-pull-request-commits [repository pull-request]
  (let [commits (common/get-github-pull-request-commits-inner repository pull-request)]
    (map #(map->GithubRepositoryPullRequestCommit
           (let [commit (.getCommit %)
                 author (.getAuthor commit)
                 committer (.getCommitter commit)
                 stats (.getStats %)
                 result {:commit-id (.getSha %)
                         :commit-name (.getMessage commit)
                         :commit-author-name (.getName author)
                         :commit-author-email (.getEmail author)
                         :commit-author-date (.getDate author)
                         :commit-committer-name (.getName committer)
                         :commit-committer-email (.getEmail committer)
                         :commit-committer-date (.getDate committer)}]
             (if stats
               (assoc result
                      :commit-additions (.getAdditions stats)
                      :commit-deletions (.getDeletions stats)
                      :commit-total (.getTotal stats))
               result)))
         commits)))


(defn- private-get-github-pull-request-files [repository pull-request]
  (let [files (common/get-github-pull-request-files-inner repository pull-request)]
    (map #(map->GithubRepositoryPullRequestFile
           {:file-name (.getFilename %)
            :file-additions (.getAdditions %)
            :file-deletions (.getDeletions %)
            :file-changes (.getChanges %)})
         files)))


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
                                         (case (get options :pr-state)
                                           "opened" "open"
                                           "merged" "closed"
                                           "closed" "closed"
                                           nil "all")))


(defn- private-merge-pull-request! [repository pull-request pr-id message]
  (common/merge-github-pull-request-inner! repository pull-request pr-id message)
  (assoc pull-request :pr-state "merged"))
