(ns flower.repository.gitlab.mr
  (:require [flower.macros :as macros]
            [flower.repository.proto :as proto]
            [flower.repository.gitlab.common :as common]))


;;
;; Private declarations
;;

(declare private-get-gitlab-pull-requests-inner
         private-get-gitlab-pull-request-counters)


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


(macros/public-definition get-gitlab-pull-requests cached)


;;
;; Private definitions
;;

(defn- private-get-gitlab-pull-requests-before-map [repository options]
  (map #(map->GitlabRepositoryPullRequest
         {:repository repository
          :mr-id (.getIid %)
          :mr-title (.getTitle %)
          :mr-state (.getState %)
          :mr-target-branch (.getTargetBranch %)
          :mr-source-branch (.getSourceBranch %)
          :mr-author (.getUsername (.getAuthor %))
          :mr-assignee (let [assignee (.getAssignee %)]
                         (if assignee
                           (.getUsername assignee)
                           nil))
          :mr-counters (private-get-gitlab-pull-request-counters repository %)
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
    (case (get options :mr-state)
      "opened" (.getOpenMergeRequests conn-inner project-inner)
      "merged" (.getMergedMergeRequests conn-inner project-inner)
      "closed" (.getClosedMergeRequests conn-inner project-inner)
      nil (.getMergeRequests conn-inner project-inner))))
