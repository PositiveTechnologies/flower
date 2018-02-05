;;; flower.el --- Emacs task tracker client. -*- lexical-binding: t -*-

;;; Copyright © 2017-2018 JSC Positive Technologies. All rights reserved.

;; Author: Sergey Sobko <SSobko@ptsecurity.com>
;; URL: https://github.com/PositiveTechnologies/flower
;; Keywords: hypermedia, outlines, tools, vc
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

(defgroup flower nil
  "Flower customization group."
  :group 'org)

(defcustom flower-tracker nil
  "Task tracker URL"
  :group 'flower
  :type 'string)

(defcustom flower-tracker-auth nil
  "Tracker authentication type"
  :group 'flower
  :type '(choice (const :tag "No authentication" nil)
                 (const :tag "Default authentication" t)))

(defcustom flower-tracker-query nil
  "Tracker default query"
  :group 'flower
  :type '(choice (const :tag "Default" nil)
                 (string :tag "Query")))

(defcustom flower-tracker-queries [nil]
  "All tracker default queries"
  :group 'flower
  :type '(vector (choice (const :tag "Default" nil)
                         (string :tag "Query"))))

(defcustom flower-tracker-grouping nil
  "Task default grouping"
  :group 'flower
  :type '(choice (const :tag "No grouping" nil)
                 (const :tag "Group by State" "task-state")
                 (const :tag "Group by Assignee" "task-assignee")))

(defvar flower-buffer "*flower*"
  "Buffer name to display results")

(defvar flower-buffer-task "*flower-task*"
  "Buffer name to display results")

(defun flower-show-buffer (text switch-to-org-mode switch-back)
  (when text
    (setq text-stripped (replace-regexp-in-string "%" "%%" text))
    (let ((oldbuf (current-buffer)))
      (save-current-buffer
        (unless (get-buffer-window flower-buffer 0)
          (pop-to-buffer flower-buffer nil t))
        (with-current-buffer flower-buffer
          (when switch-to-org-mode
            (org-mode)
            (setq-local org-return-follows-link t)
            (setq-local org-support-shift-select t)
            (read-only-mode))
          (let ((inhibit-read-only t))
            (setf (buffer-string) text-stripped)
            (goto-char (point-min)))))
      (if switch-back
        (pop-to-buffer oldbuf nil t)
        (pop-to-buffer flower-buffer nil t)))))

(defun flower-check-tracker ()
  (if flower-tracker
      t
    (progn
      (message "%s" "Please specify `flower-tracker`")
      nil)))

(defun flower-show-task-info (task-id)
  (interactive "sEnter task id: ")
  (message "Showing task: %s" task-id)
  (when (flower-check-tracker)
    (let ((flower-buffer flower-buffer-task)
          (nrepl-sync-request-timeout 30))
      (flower-show-buffer (flower-get-task-info-wrapper flower-tracker
                                                        flower-tracker-auth
                                                        task-id)
                          t
                          t))))

(defun flower-browse-task (task-id)
  (interactive "sEnter task id: ")
  (message "Browsing task: %s" task-id)
  (when (flower-check-tracker)
    (let ((nrepl-sync-request-timeout 30))
      (browse-url (flower-get-task-url-wrapper flower-tracker
                                               flower-tracker-auth
                                               task-id)))))

(defun flower-list-tasks ()
  (interactive)
  (when (flower-check-tracker)
    (let ((nrepl-sync-request-timeout 30))
      (flower-show-buffer (flower-get-tasks-wrapper flower-tracker
                                                    flower-tracker-auth
                                                    flower-tracker-query
                                                    flower-tracker-grouping)
                          t
                          nil)
      (message "Listed all tasks for query: %s" flower-tracker-query))))

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

(org-add-link-type "flower" 'flower-open)

(defcustom flower-open-command 'flower-browse-task
  "The Emacs command to be used to display flower link."
  :group 'flower
  :type '(choice (const flower-browse-task)
                 (const flower-show-task-info)))

(defun flower-open (task-id)
  "Visit flower tracker task."
  (funcall flower-open-command task-id))

(defun flower-org-show-task-info ()
  (interactive)
  (let ((flower-open-command 'flower-show-task-info))
    (org-open-at-point)))

(defun flower-org-mode-config ()
  (local-set-key (kbd "M-.") 'flower-org-show-task-info)
  (local-set-key (kbd "C-c F C") 'flower-cycle-query-and-go))

(add-hook 'org-mode-hook 'flower-org-mode-config)

(global-set-key (kbd "C-c F F") 'flower-list-tasks)

(provide 'flower)

;;; flower.el ends here
