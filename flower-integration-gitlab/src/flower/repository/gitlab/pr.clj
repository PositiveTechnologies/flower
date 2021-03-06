(ns flower.repository.gitlab.pr
  (:require [clojure.string :as string]
            [flower.macros :as macros]
            [flower.repository.proto :as proto]
            [flower.repository.gitlab.common :as common])
  (:import (java.io FileNotFoundException)
           (org.gitlab.api GitlabAPIException)))


;;
;; Private declarations
;;

(declare private-get-gitlab-pull-requests-inner
         private-get-gitlab-pull-request-comments
         private-get-gitlab-pull-request-counters
         private-get-gitlab-pull-request-commits
         private-get-gitlab-pull-request-files
         private-set-assignee!
         private-merge-pull-request!)


;;
;; Public definitions
;;

(defrecord GitlabRepositoryPullRequestComments [comment-author
                                                comment-text]
  proto/RepositoryPullRequestCommentProto
  (get-author [pull-request-comments] comment-author)
  (get-text [pull-request-comments] comment-text))


(defrecord GitlabRepositoryPullRequestCounters [count-upvotes
                                                count-downvotes
                                                count-merge-conflicts
                                                count-merge-conflicts-notes
                                                count-reassigns
                                                count-lgtms
                                                count-wips]
  proto/RepositoryPullRequestCounterProto
  (get-wips [pull-request-counters] count-wips))


(defrecord GitlabRepositoryPullRequestCommit [commit-id
                                              commit-name
                                              commit-author-name
                                              commit-author-email
                                              commit-author-date
                                              commit-committer-name
                                              commit-committer-email
                                              commit-committer-date])


(defrecord GitlabRepositoryPullRequestFile [file-name
                                            file-additions
                                            file-deletions
                                            file-changes])


(defrecord GitlabRepositoryPullRequest [repository
                                        pr-id
                                        pr-title
                                        pr-state
                                        pr-target-branch
                                        pr-source-branch
                                        pr-author
                                        pr-assignee
                                        pr-comments-future
                                        pr-counters-future
                                        task-ids]
  proto/RepositoryPullRequestProto
  (get-repository [pull-request] repository)
  (get-pull-request-id [pull-request] pr-id)
  (get-state [pull-request] pr-state)
  (get-source-branch [pull-request] pr-source-branch)
  (get-target-branch [pull-request] pr-target-branch)
  (get-title [pull-request] pr-title)
  (get-comments [pull-request] @pr-comments-future)
  (get-counters [pull-request] @pr-counters-future)
  (get-commits [pull-request] (private-get-gitlab-pull-request-commits repository
                                                                       pull-request))
  (get-files [pull-request] (private-get-gitlab-pull-request-files repository
                                                                   pull-request))
  (set-assignee! [pull-request assignee] (private-set-assignee! repository
                                                                pull-request
                                                                pr-id
                                                                assignee))
  (merge-pull-request! [pull-request] (private-merge-pull-request! repository
                                                                   pull-request
                                                                   pr-id
                                                                   nil))
  (merge-pull-request! [pull-request message] (private-merge-pull-request! repository
                                                                           pull-request
                                                                           pr-id
                                                                           message)))


(macros/public-definition get-gitlab-pull-requests cached)


;;
;; Private definitions
;;

(defn- private-get-gitlab-pull-requests-before-map [repository options]
  (map #(map->GitlabRepositoryPullRequest
         (let [pr-comments-future (macros/future-or-delay (private-get-gitlab-pull-request-comments repository %))]
           {:repository repository
            :pr-id (.getIid %)
            :pr-title (.getTitle %)
            :pr-state (.getState %)
            :pr-target-branch (.getTargetBranch %)
            :pr-source-branch (.getSourceBranch %)
            :pr-author (.getUsername (.getAuthor %))
            :pr-assignee (let [assignee (.getAssignee %)]
                           (if assignee
                             (.getUsername assignee)
                             nil))
            :pr-comments-future pr-comments-future
            :pr-counters-future (macros/future-or-delay (private-get-gitlab-pull-request-counters repository
                                                                                                  %
                                                                                                  pr-comments-future))
            :task-ids (list)}))
       (private-get-gitlab-pull-requests-inner repository options)))


(defn- private-get-gitlab-pull-requests [repository options]
  (map (get-in (proto/get-repository-component repository)
               [:context :pull-requests-map-function]
               (fn [pull-request] pull-request))
       (private-get-gitlab-pull-requests-before-map repository options)))


(defn- private-get-pull-request-notes-counters-by-patterns [notes-list note-patterns]
  (let [full-text (apply str notes-list)
        counter-list (map #(vector % (-> (str "(?i)" %)
                                         (re-pattern)
                                         (re-seq full-text)
                                         (count)))
                          note-patterns)]
    (into (sorted-map) counter-list)))


(defn- private-get-gitlab-pull-request-commits [repository pull-request]
  (let [commits (common/get-gitlab-commits-inner repository pull-request)]
    (map #(map->GitlabRepositoryPullRequestCommit
           (let [result {:commit-id (.getId %)
                         :commit-name (.getTitle %)
                         :commit-author-name (.getAuthorName %)
                         :commit-author-email (.getAuthorEmail %)
                         :commit-author-date (.getAuthoredDate %)
                         :commit-committer-name (.getAuthorName %)
                         :commit-committer-email (.getAuthorEmail %)
                         :commit-committer-date (.getCommittedDate %)}]
             result))
         commits)))


(defn- private-get-gitlab-pull-request-files [repository pull-request]
  (let [conn-inner (common/get-gitlab-conn-inner repository)
        project-inner (common/get-gitlab-project-inner repository)
        project-id (.getId project-inner)
        changes (common/get-gitlab-changes-inner repository pull-request)
        flat-list (map (fn [diff]
                         (let [splitted (string/split (.getDiff diff) #"\n")
                               grouped (group-by first splitted)
                               added (count (get grouped \+ []))
                               deleted (count (get grouped \- []))]
                           [(.getNewPath diff) added deleted]))
                       changes)
        grouped-list (group-by first flat-list)]
    (map (fn [[file-name counters]]
           (reduce (fn [acc [filename additions deletions]]
                     (-> acc
                         (update :file-additions #(+ (get % :file-additions 0) additions))
                         (update :file-deletions #(+ (get % :file-deletions 0) deletions))
                         (update :file-changes #(+ (get % :file-changes 0) (+ additions
                                                                              deletions)))))
                   (map->GitlabRepositoryPullRequestFile {:file-name file-name})
                   counters))
         grouped-list)))


(defn- private-get-gitlab-pull-request-comments [repository pull-request-inner]
  (let [notes (try (.getNotes (common/get-gitlab-conn-inner repository) pull-request-inner)
                   (catch java.io.IOException e nil))]
    (map #(map->GitlabRepositoryPullRequestComments {:comment-author (.getUsername (.getAuthor %))
                                                     :comment-text (.getBody %)})
         notes)))


(defn- private-get-gitlab-pull-request-counters [repository pull-request-inner pr-comments-future]
  (let [notes @pr-comments-future
        notes-map (private-get-pull-request-notes-counters-by-patterns (map #(proto/get-text %) notes)
                                                                       ["merge conflict"
                                                                        "reassign"
                                                                        "lgtm"
                                                                        "wip"])
        merge-status-counters (-> (.getMergeStatus pull-request-inner)
                                  (list)
                                  (frequencies))]
    (map->GitlabRepositoryPullRequestCounters
     {:count-upvotes (.getUpvotes pull-request-inner)
      :count-downvotes (.getDownvotes pull-request-inner)
      :count-merge-conflicts (get merge-status-counters "cannot_be_merged" 0)
      :count-merge-conflicts-notes (get notes-map "merge conflict" 0)
      :count-reassigns (get notes-map "reassign" 0)
      :count-lgtms (get notes-map "lgtm" 0)
      :count-wips (get notes-map "wip" 0)})))


(defn- private-get-gitlab-pull-requests-inner [repository options]
  (let [conn-inner (common/get-gitlab-conn-inner repository)
        project-inner (common/get-gitlab-project-inner repository)]
    (case (get options :pr-state)
      "opened" (.getOpenMergeRequests conn-inner project-inner)
      "merged" (.getMergedMergeRequests conn-inner project-inner)
      "closed" (.getClosedMergeRequests conn-inner project-inner)
      nil (.getMergeRequests conn-inner project-inner))))


(defn- private-set-assignee! [repository pull-request pr-id assignee]
  (let [conn-inner (common/get-gitlab-conn-inner repository)
        project-inner (common/get-gitlab-project-inner repository)
        opened-pull-requests (.getOpenMergeRequests conn-inner project-inner)
        inner-pull-request (first (filter #(= (.getIid %) pr-id) opened-pull-requests))]
    (if inner-pull-request
      (try
        (or (when-let [assignee' (and assignee
                                      (first (.findUsers conn-inner assignee)))]
              (let [pull-request-inner (.updateMergeRequest conn-inner
                                                            (.getId project-inner)
                                                            (int pr-id)
                                                            nil
                                                            (.getId assignee')
                                                            nil
                                                            nil
                                                            nil
                                                            nil)]
                (assoc pull-request
                       :pr-assignee (.getUsername (.getAssignee pull-request-inner)))))
            pull-request)
        (catch GitlabAPIException e
          pull-request))
      pull-request)))


(defn- private-merge-pull-request! [repository pull-request pr-id message]
  (let [conn-inner (common/get-gitlab-conn-inner repository)
        project-inner (common/get-gitlab-project-inner repository)
        opened-pull-requests (.getOpenMergeRequests conn-inner project-inner)
        inner-pull-request (first (filter #(= (.getIid %) pr-id) opened-pull-requests))]
    (if inner-pull-request
      (try
        (assoc pull-request
               :pr-state (.getState (try
                                      (.acceptMergeRequest conn-inner project-inner (.getId inner-pull-request) message)
                                      (catch FileNotFoundException e
                                        (.acceptMergeRequest conn-inner project-inner pr-id message)))))
        (catch GitlabAPIException e
          pull-request))
      pull-request)))
