(ns flower.repository.gitlab.pr
  (:require [flower.macros :as macros]
            [flower.repository.proto :as proto]
            [flower.repository.gitlab.common :as common]))


;;
;; Private declarations
;;

(declare private-get-gitlab-pull-requests-inner
         private-get-gitlab-pull-request-counters
         private-merge-pull-request)


;;
;; Public definitions
;;

(defrecord GitlabRepositoryPullRequestCounters [count-upvotes
                                                count-downvotes
                                                count-merge-conflicts
                                                count-merge-conflicts-notes
                                                count-reassigns
                                                count-lgtms
                                                count-wips]
  proto/RepositoryPullRequestCounterProto
  (get-wips [pull-request-counters] count-wips))


(defrecord GitlabRepositoryPullRequest [repository
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
  (get-state [pull-request] pr-state)
  (get-source-branch [pull-request] pr-source-branch)
  (get-target-branch [pull-request] pr-target-branch)
  (get-title [pull-request] pr-title)
  (get-counters [pull-request] pr-counters)
  (merge-pull-request [pull-request] (private-merge-pull-request repository
                                                                 pull-request
                                                                 pr-id
                                                                 nil))
  (merge-pull-request [pull-request message] (private-merge-pull-request repository
                                                                         pull-request
                                                                         pr-id
                                                                         message)))


(macros/public-definition get-gitlab-pull-requests cached)


;;
;; Private definitions
;;

(defn- private-get-gitlab-pull-requests-before-map [repository options]
  (map #(map->GitlabRepositoryPullRequest
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
          :pr-counters (private-get-gitlab-pull-request-counters repository %)
          :task-ids (list)})
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


(defn- private-get-gitlab-pull-request-counters [repository pull-request]
  (let [notes (.getNotes (common/get-gitlab-conn-inner repository) pull-request)
        notes-map (private-get-pull-request-notes-counters-by-patterns (map #(.getBody %) notes)
                                                                       ["merge conflict"
                                                                        "reassign"
                                                                        "lgtm"
                                                                        "wip"])
        merge-status-counters (-> (.getMergeStatus pull-request)
                                  (list)
                                  (frequencies))]
    (map->GitlabRepositoryPullRequestCounters
     {:count-upvotes (.getUpvotes pull-request)
      :count-downvotes (.getDownvotes pull-request)
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


(defn- private-merge-pull-request [repository pull-request pr-id message]
  (let [conn-inner (common/get-gitlab-conn-inner repository)
        project-inner (common/get-gitlab-project-inner repository)
        opened-pull-requests (.getOpenMergeRequests conn-inner project-inner)
        inner-pull-request (first (filter #(= (.getIid %) pr-id) opened-pull-requests))]
    (if inner-pull-request
      (.acceptMergeRequest conn-inner project-inner (.getId inner-pull-request) message))
    (assoc pull-request :pr-state "merged")))
