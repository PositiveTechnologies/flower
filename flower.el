;;; flower.el --- Emacs task tracker client. -*- lexical-binding: t -*-

;;; Copyright © 2017-2018 JSC Positive Technologies. All rights reserved.

;; Author: Sergey Sobko <SSobko@ptsecurity.com>
;; URL: https://github.com/PositiveTechnologies/flower
;; Keywords: clojure, tfs, jira, atlassian, gitlab, github, integration
;; Version: 0.4.0
;; Package-Requires: ((emacs "24.4")(clomacs "0.0.2")(cider "0.14"))

;; This file is not part of GNU Emacs.

;;; This program is licensed under the terms of the MIT license.
;;; For a copy, see <https://opensource.org/licenses/MIT>.

;;; Commentary:

;; flower provides simple interface to list tasks (issues) from Jira, TFS,
;; Gitlab and Github. It uses clomacs under the hood for integration with
;; Flower Clojure library.

;; See README.md for detailed description.

;;; Code:

(require 'clomacs)
(require 'org)

(clomacs-defun flower-get-task-info-wrapper
               get-task-info
               :lib-name "flower"
               :namespace flower.util.tracker
               :doc "Get task info from task tracker")

(clomacs-defun flower-get-task-url-wrapper
               get-task-url
               :lib-name "flower"
               :namespace flower.util.tracker
               :doc "Get task info from task tracker")

(clomacs-defun flower-get-tasks-wrapper
               get-tasks
               :lib-name "flower"
               :namespace flower.util.tracker
               :doc "Get tasks from task tracker")

(defvar flower-tracker nil
  "Task tracker URL")

(defvar flower-tracker-auth nil
  "Tracker authentication type (nil, t)")

(defvar flower-tracker-query nil
  "Tracker default query")

(defvar flower-tracker-queries [nil]
  "All tracker default queries")

(defvar flower-buffer "*flower*"
  "Buffer name to display results")

(defvar flower-buffer-task "*flower-task*"
  "Buffer name to display results")

(defun flower-show-buffer (text switch-to-org-mode)
  (setq text-stripped (replace-regexp-in-string "%" "%%" text))
  (progn
    (unless (get-buffer-window flower-buffer 0)
      (pop-to-buffer flower-buffer nil t))
    (with-current-buffer flower-buffer
      (when switch-to-org-mode
        (org-mode))
      (setf (buffer-string) text-stripped))))

(defun flower-check-tracker ()
  (if flower-tracker
      t
    (progn
      (message "%s" "Please specify `flower-tracker`")
      f)))

(defun flower-show-task-info (task-id)
  (interactive "sEnter task id: ")
  (when (flower-check-tracker)
    (let ((flower-buffer flower-buffer-task))
      (flower-show-buffer (flower-get-task-info-wrapper flower-tracker
                                                        flower-tracker-auth
                                                        task-id)
                          t))))

(defun flower-browse-task (task-id)
  (interactive "sEnter task id: ")
  (when (flower-check-tracker)
    (browse-url (flower-get-task-url-wrapper flower-tracker
                                             flower-tracker-auth
                                             task-id))))

(defun flower-list-tasks ()
  (interactive)
  (when (flower-check-tracker)
    (flower-show-buffer (flower-get-tasks-wrapper flower-tracker
                                                  flower-tracker-auth
                                                  flower-tracker-query)
                        t)
    (message "Listed all tasks for query: %s" flower-tracker-query)))

(defun flower-cycle-query (@n)
  (interactive "p")
  (let* (($values flower-tracker-queries)
         ($index-before
          (if (get 'flower-cycle-query 'state)
              (get 'flower-cycle-query 'state)
            0))
         ($index-after (% (+ $index-before (length $values) @n) (length $values)))
         ($next-value (aref $values $index-after)))
    (put 'flower-cycle-query 'state $index-after)
    (setq flower-tracker-query $next-value)
    (message "Flower tracker query set to: %s" $next-value)))

(defun flower-cycle-query-and-go (@n)
  (interactive "p")
  (flower-cycle-query @n)
  (flower-list-tasks))

(defun org-mode-show-task-info ()
  (interactive)
  (setq task-id (thing-at-point 'word))
  (flower-show-task-info task-id))

(defun flower-org-mode-config ()
  (local-set-key (kbd "M-.") 'org-mode-show-task-info))

(add-hook 'org-mode-hook 'flower-org-mode-config)

(provide 'flower)
