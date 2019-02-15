# 0.4.6 - 2019-02-15

- Disabled implicit cache for Emacs by default.
- Fixed issue with listing Jira issues.
- Added support for Clojure 1.10 and dropped for Clojure 1.7.
- Added initial tests for task trackers and repositories.
- Updated dependencies.

# 0.4.5 - 2018-10-25

- Added namespaces for task trackers and repositories.
- Added ability to skip unexistent issues for Jira task tracker.
- Simplified Leiningen template and README sample code.
- Fixed unconditional issue type setting for Jira.
- Updated dependencies.

# 0.4.3 - 2018-08-21

- Added ability to get related tasks in task trackers.
- Fixed task upsertion for TFS tracker.
- Upgraded dependencies.

# 0.4.2 - 2018-06-18

- Implemented comments getting for tasks in task trackers.
- Implemented comments getting for pull requests in repositories.
- Added ability to get project URLs for repositories.
- Added ability to assign reviewers for pull requests.
- Fixed issue with merging Gitlab pull requests.
- Fixed teams handling issue.

# 0.4.1 - 2018-03-16

- Implemented message creation.
- Deleted messaging integrations from cumulative package.
- Implemented getting credentials from environment.

# 0.4.0 - 2018-02-26

- Fixed url getting for tasks in task trackers and projects in repositories.
- Added JQL (Jira Query Language) support.
- Added Emacs integration.
- Added support for TFS query paths.
- Added Slack integration.
- Implemented loose coupling and dependency resolver for integrations.
- Added default mock integration for trackers, repositories, messaging.
- Improved support for Gitlab and Github token authentication.
- Implemented subscription for messaging systems.

# 0.3.6 - 2018-01-16

- Fixed issue with existing tags deletion in TFS tracker task upserting.

# 0.3.5 - 2018-01-15

- Fixed issue with TFS tracker task upserting.

# 0.3.3 - 2018-01-12

- Added ability to get pull request commits.
- Added ability to get pull request files.

# 0.3.1 - 2017-12-13

- Added ability to create new tasks in all task trackers.
- Added support for description field in tasks.

# 0.3.0 - 2017-12-11

- Added ability to fill in repository and tracker types manually for shorthand notation.
- Decomposed project into smaller components.
- Added Leiningen template to create new applications instantly.

# 0.2.1 - 2017-12-06

- Added ability to use GitHub user repositories along with organization ones.
- Changed project namespace to "flower" on Clojars.

# 0.2.0 - 2017-12-05

- Added shorthand notations to create single records for trackers, repositories and messaging.
- Added macro to supply default credentials while using shorthand notations.

# 0.1.6 - 2017-11-30

- Added ability to change state and attributes for tracker tasks.
- Fixed tracker protocols to avoid collisions.

# 0.1.5 - 2017-11-23

- Updated dependencies to the most recent versions.

# 0.1.4 - 2017-11-09

- Fixed issue with impure functions.
- Fixed issue with tags for GitHub issues.

# 0.1.3 - 2017-10-20

- Added ability to merge pull requests for GitLab and GitHub.

# 0.1.0 - 2017-10-09

- Added initial tracker support for Jira, GitLab, GitHub and TFS.
- Added initial repository support for GitLab and GitHub.
- Added initial messaging support for Exchange.
