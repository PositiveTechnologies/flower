# Flower

[![Clojars](https://img.shields.io/clojars/v/flower.svg)](https://clojars.org/flower)
[![Travis](https://img.shields.io/travis/PositiveTechnologies/flower.svg)](https://travis-ci.org/PositiveTechnologies/flower)
[![Dependencies Status](https://versions.deps.co/PositiveTechnologies/flower/status.svg)](https://versions.deps.co/PositiveTechnologies/flower)

Handles all your issue tracking tools and version control systems for you!

<img src="/images/logo/flower-logo.png" width="200px" height="209px"
    alt="Flower logo" align="right" />

The Flower library is a set of common protocols for repositories, task trackers, and messaging
systems that includes integrations with the most common ones like Jira, TFS, GitLab, GitHub, and
Exchange. It may be useful for creating external automation scenarios.

Initially, it was designed to handle all routine operations of the
[PT Application Firewall](https://www.ptsecurity.com/ww-en/products/af/) development team:

* Merge opened interlinked pull requests in different GitLab repositories or servers, once MRs
are marked with 'LGTM', and then close issues in TFS.
* Notify about team members' birthdays.
* Create email requests to the company’s IT Helpdesk in order to give access rights
to new team members.
* Search for current 'In Progress' issues from the CLI.
* And much more.

If you need separate Python libraries with similar functionality, you may visit
[DevOpsHQ](https://github.com/DevOpsHQ).

## Installation

To install, add the following to your project `:dependencies`:

    [flower "0.2.1"]

## Usage

This library supports multiple integrations with various systems related to development flow.

So the best way to showcase its potential is to demonstrate how it handles each of them by example.
We will keep adding to the existing examples upon introducing new functionality.

However, there is a certain caveat you should be aware of before we go any further. Any function in
the library may be considered pure only on the top level. Underlying code implicitly uses cache
that may be explicitly cleared by a user after each query if so needed. To do it, the user needs to
call `(function-name-clear-cache!)` where `function-name` is a function defined with the
`public-definition` macro.


### For the impatient

```clj
(require '[clojure.string])
(require '[flower.tracker.core :as tracker.core]
         '[flower.tracker.proto :as tracker.proto])

;; Print all opened tasks in our task tracker
(loop [[task & other-tasks] (-> "https://github.com/PositiveTechnologies/flower"
                                (tracker.core/get-tracker)
                                (tracker.proto/get-tasks))]
  (if task
    (let [task-parts (-> task
                         (select-keys [:task-type :task-id :task-title])
                         (vals))
          task-string (clojure.string/join " " task-parts)]
      (println task-string)
      (recur other-tasks))))
```

### Task trackers

First, include the required parts of the library and indicate our task trackers and specific
projects:

```clj
(require '[flower.tracker.core :as tracker.core]
         '[flower.tracker.proto :as tracker.proto])

;; Our trackers definition
(def pt-trackers (let [organization-url "https://github.com/PositiveTechnologies"]
                   (tracker.core/trackers (tracker.core/start-component {})
                                          {:pt-github {:tracker-type :github
                                                       :tracker-url organization-url
                                                       :tracker-projects ["flower"]}})))
```

Here we created the `pt-trackers` hash map with `:pt-github` as a key and a list of
`flower.tracker.github.tracker.GithubTracker` instances as a value
(defined by `:tracker-type :github`). You may indicate your own tracker type
(`:github`, `:gitlab`, `:jira`, or `:tfs`) in `:tracker-type`, your company URL for
the tracker in `:tracker-url`, and your current projects in `:tracker-projects`.
Make sure you specified the `:auth` key in `start-component`.

Here is a shorthand notation to create a single tracker record:

```clj
(def pt-github-tracker (tracker.core/get-tracker "https://github.com/PositiveTechnologies/flower"))
```

Now you are all set to check your list of issues on GitHub:

```clj
(tracker.proto/get-tasks pt-github-tracker)
```

The next call for this method will give you the same result at lightning speed. As we mentioned in
the beginning, if you want to get new values from the task tracker, clear cache by doing this:

```clj
(require '[flower.tracker.github.common :as github.common]
         '[flower.tracker.github.task :as github.task])

(github.task/get-github-workitems-clear-cache!)
(github.common/get-github-workitems-inner-clear-cache!)

;; This time GitHub APIs will be called again. Pay attention to the API rate limit!
(tracker.proto/get-tasks pt-github-tracker)
```

This behavior may change in the future: these functions may be integrated into the corresponding
protocols or they will become part of the context macro.

To change the state or some attributes of the the task (and implicitly clear cache) you
may do the following (you should be authenticated beforehand, see next section):

```clj
(def some-task (first (tracker.proto/get-tasks pt-github-tracker)))

(tracker.proto/update! (assoc some-task
                              :task-title "New title"
                              :task-tags ["feature"]
                              :task-assignee "your_login_here"
                              :task-state "close"))
```


### Authentication

Not all task trackers allow unauthenticated access. Even GitHub penalizes unauthenticated users by
drastically dropping its API rate limit. Let's fix this by specifying your username and password
for our servers.

Create the `.credentials.edn` file in your home directory so you can access it with
`cat ~/.credentials.edn`:

```edn
{:account
 {:login "your_login_here"
  :domain "YOURDOMAIN"
  :password "YourSecretPassword"
  :email "your_login_here@example.com"}}
```

This is not the only way to specify your credentials and it's definitely not the most secure, but
we will use it here as the simplest one. If necessary, feel free to change it by redefining the
`flower.credentials.get-credentials` function.

So, let's get iterations from some company's internal task tracker
(GitLab in the following example):

```clj
(require '[flower.credentials :as credentials])
(require '[flower.tracker.core :as tracker.core]
         '[flower.tracker.proto :as tracker.proto])

(def login (credentials/get-credentials :account :login))
(def password (credentials/get-credentials :account :password))


;; Initialize the tracker component for authentication
(def tracker-component (tracker.core/start-component {:auth {:gitlab-login login
                                                             :gitlab-password password}
                                                      :context {}}))

;; Our trackers definition
(def our-trackers (tracker.core/trackers tracker-component
                                         {:inner-gitlab {:tracker-type :gitlab
                                                         :tracker-url "https://gitlab.example.com"
                                                         :tracker-projects ["example-project"]}}))

;; Get iterations for our project using auth from tracker-component
(tracker.proto/get-iterations (first (:inner-gitlab our-trackers)))
```

ATTENTION! All further examples will not contain the `:auth` key. However, it is implied.
It is your responsibility to ensure you have the right credentials.

You may use the following properties for `:auth`:
* `:jira-login` and `:jira-password` for Jira
* `:tfs-login` and `:tfs-password` for TFS
* `:github-login` and `:github-password` or `:github-token` for GitHub
* `:gitlab-login` and `:gitlab-password` for GitLab

Here is a convenient macro that uses `flower.credentials` under the hood and supplies necessary
credentials to `start-component` functions. It is supposed to be used together with
`flower.tracker.core/get-tracker`, `flower.repository.core/get-repository`, and
`flower.messaging.core/get-messaging` definitions:

```clj
(require '[flower.macros :as macros])
(require '[flower.tracker.core :as tracker.core])

(macros/with-default-credentials
  (def inner-gitlab-tracker (tracker.core/get-tracker "https://gitlab.example.com/example/example-project")))
```

### Repositories

Repository protocols are very similar to tracker ones. Let's create the definitions from the
section above, but this time for our repositories:

```clj
(require '[flower.repository.core :as repository.core]
         '[flower.repository.proto :as repository.proto])

;; Our repositories definition
(def pt-repos (let [organization-url "https://github.com/PositiveTechnologies"]
                (repository.core/repositories (repository.core/start-component {})
                                              {:pt-github {:repo-type :github
                                                           :repo-url organization-url
                                                           :repo-projects ["flower"]}})))
```

Here is a shorthand notation to create a single repository record:

```clj
(def pt-github-repo (repository.core/get-repository "https://github.com/PositiveTechnologies/flower"))
```

Let's find out the title and the corresponding source branch for every pull request in our
repository:

```clj
(map #(list (repository.proto/get-title %)
            (repository.proto/get-source-branch %))
     (repository.proto/get-pull-requests pt-github-repo))
```

Let's merge some opened pull request (make sure you specified `:auth` beforehand,
see previous section):

```clj
(let [first-opened-pr (first (repository.proto/get-pull-requests pt-github-repo
                                                                 {:pr-state "opened"}))]
  (repository.proto/merge-pull-request! first-opened-pr))
```

### Messaging

To send or receive emails, do this:

```clj
(require '[flower.credentials :as credentials])
(require '[flower.messaging.core :as messaging.core]
         '[flower.messaging.proto :as messaging.proto])

(def login (credentials/get-credentials :account :login))
(def password (credentials/get-credentials :account :password))
(def domain (credentials/get-credentials :account :domain))
(def email (credentials/get-credentials :account :email))

;; Initialize our messaging component for authentication
(def msg-component (messaging.core/start-component {:auth {:message-box-username login
                                                           :message-box-password password
                                                           :message-box-domain domain
                                                           :message-box-email email}
                                                    :context {}}))

;; Our exchange servers definition
(def our-messaging (messaging.core/messaging msg-component
                                             {:our-mail {:messaging-type :exchange}}))
```

Or this:

```clj
(require '[flower.macros :as macros])
(require '[flower.messaging.core :as messaging.core]
         '[flower.messaging.proto :as messaging.proto])

(macros/with-default-credentials
  (def our-mail (messaging.core/get-messaging)))
```

Now let's search for the first message from the top in our inbox:

```clj
(def top-message (first (messaging.proto/search-messages our-mail
                                                         {:count 1 :load-body true})))
```

The structure of the message is very simple. You can even change its recipients and resend it to a
specific email with this:

```clj
(messaging.proto/send-message! (assoc top-message
                                      :msg-recipients
                                      '("new_recipient@example.com")))
```

## License

Copyright © 2017 JSC Positive Technologies. All rights reserved.

Distributed under the MIT License. See LICENSE.

All the libraries and systems are licensed and remain the property of their respective owners.
