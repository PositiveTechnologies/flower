;;; flower.el --- Emacs task tracker client. -*- lexical-binding: t -*-

;;; Copyright © 2017-2018 JSC Positive Technologies. All rights reserved.

;; Author: Sergey Sobko <SSobko@ptsecurity.com>
;; URL: https://github.com/PositiveTechnologies/flower
;; Keywords: hypermedia, outlines, tools, vc
;; Version: 0.4.5
;; Package-Requires: ((emacs "24.4")(clomacs "0.0.3"))

;; This file is not part of GNU Emacs.

;;; This program is licensed under the terms of the MIT license.
;;; For a copy, see <https://opensource.org/licenses/MIT>.

;;; Commentary:

;; flower provides simple interface to list tasks (issues) from Jira, TFS,
;; Gitlab and Github using clomacs under the hood for integration with
;; Flower Clojure library.

;; See README.md for detailed description.

;;; Code:

(require 'clomacs)
(require 'org)

(clomacs-defun flower-get-task-info
               get-task-info
               :lib-name "flower"
               :namespace flower.util.tracker
               :doc "Get task info from task tracker")

(clomacs-defun flower-get-task-url
               get-task-url
               :lib-name "flower"
               :namespace flower.util.tracker
               :doc "Get task URL from task tracker")

(clomacs-defun flower-get-tasks
               get-tasks
               :lib-name "flower"
               :namespace flower.util.tracker
               :doc "Get tasks from task tracker")

(defgroup flower nil
  "Flower customization group."
  :group 'applications)

(defcustom flower-tracker nil
  "Task tracker URL."
  :group 'flower
  :type 'string)

(defcustom flower-tracker-auth nil
  "Tracker authentication type."
  :group 'flower
  :type '(choice (const :tag "No authentication" nil)
                 (const :tag "Default authentication" t)))

(defcustom flower-tracker-query nil
  "Tracker default query."
  :group 'flower
  :type '(choice (const :tag "Default" nil)
                 (string :tag "Query")))

(defcustom flower-tracker-queries [nil]
  "All trackers and queries."
  :group 'flower
  :type '(vector (list (choice (string :tag "Tracker URL")
                               (const :tag "Same tracker" nil))
                       (choice (const :tag "No authentication" nil)
                               (const :tag "Default authentication" t))
                       (choice (const :tag "Default query" nil)
                               (string :tag "Query")))))

(defcustom flower-tracker-grouping nil
  "Task grouping."
  :group 'flower
  :type '(choice (const :tag "No grouping" nil)
                 (const :tag "Group by State" "task-state")
                 (const :tag "Group by Assignee" "task-assignee")))

(defcustom flower-open-command 'flower-browse-task
  "The Emacs command to be used to display 'flower:' links."
  :group 'flower
  :type '(choice (const :tag "Browse task" flower-browse-task)
                 (const :tag "Show task info" flower-show-task-info)))

(defvar flower-buffer "*flower*"
  "Buffer name to display list of tasks.")

(defvar flower-buffer-task "*flower-task*"
  "Buffer name to display contents of concrete task.")

(defun flower-show-buffer (contents switch-to-org-mode switch-back)
  "Pop to buffer specified by 'flower-buffer' variable and set buffer text.
Argument CONTENTS New contents of the buffer.
Argument SWITCH-TO-ORG-MODE Change buffer mode using function ‘org-mode’.
Argument SWITCH-BACK Switch current buffer back."
  (when contents
    (let ((oldbuf (current-buffer))
          (contents-stripped (replace-regexp-in-string "%" "%%" contents)))
      (save-current-buffer
        (unless (get-buffer-window flower-buffer 0)
          (pop-to-buffer flower-buffer nil t))
        (with-current-buffer flower-buffer
          (when switch-to-org-mode
            (org-mode)
            (flower-mode)
            (setq-local org-return-follows-link t)
            (setq-local org-support-shift-select t)
            (read-only-mode))
          (let ((inhibit-read-only t))
            (erase-buffer)
            (insert contents-stripped)
            (goto-char (point-min)))))
      (if switch-back
        (pop-to-buffer oldbuf nil t)
        (pop-to-buffer flower-buffer nil t)))))

(defun flower-check-tracker ()
  "Check if variable 'flower-tracker' is specified."
  (or flower-tracker
      (if (and (> (length flower-tracker-queries) 0)
               (aref flower-tracker-queries 0))
          (flower-cycle-query 0)
        (progn
          (message "%s" "Please specify `flower-tracker` or `flower-tracker-queries`")
          nil))))

;;;###autoload
(defun flower-show-task-info (task-id)
  "Show task info in new buffer.
Argument TASK-ID Task identifier."
  (interactive "sEnter task id: ")
  (message "Showing task: %s" task-id)
  (when (flower-check-tracker)
    (let ((flower-buffer flower-buffer-task)
          (nrepl-sync-request-timeout 30))
      (flower-show-buffer (flower-get-task-info flower-tracker
                                                flower-tracker-auth
                                                task-id)
                          t
                          t))))

;;;###autoload
(defun flower-browse-task (task-id)
  "Browse task in external browser by task id.
Argument TASK-ID Task identifier."
  (interactive "sEnter task id: ")
  (message "Browsing task: %s" task-id)
  (when (flower-check-tracker)
    (let ((nrepl-sync-request-timeout 30))
      (browse-url (flower-get-task-url flower-tracker
                                       flower-tracker-auth
                                       task-id)))))

;;;###autoload
(defun flower-list-tasks ()
  "Show list of tasks for task tracker specified by 'flower-tracker' variable."
  (interactive)
  (when (flower-check-tracker)
    (let ((nrepl-sync-request-timeout 30))
      (flower-show-buffer (flower-get-tasks flower-tracker
                                            flower-tracker-auth
                                            flower-tracker-query
                                            flower-tracker-grouping)
                          t
                          nil)
      (message "Listed all tasks for: %s -> %s" flower-tracker (or flower-tracker-query
                                                                   "Default query")))))

;;;###autoload
(defun flower-cycle-query (query-index)
  "Change current task tracker query.
Argument QUERY-INDEX Index of the query."
  (interactive "p")
  (let* ((values flower-tracker-queries)
         (index-before
          (if (get 'flower-cycle-query 'state)
              (get 'flower-cycle-query 'state)
            0))
         (index-after (% (+ index-before (length values) query-index)
                         (length values)))
         (next-value (aref values index-after)))
    (put 'flower-cycle-query 'state index-after)
    (when (first next-value)
      (setq flower-tracker (first next-value))
      (setq flower-tracker-auth (second next-value)))
    (setq flower-tracker-query (third next-value))
    (message "Flower tracker query set to: %s" next-value)))

;;;###autoload
(defun flower-cycle-query-and-go (query-index)
  "Change current task tracker query and show list of tasks.
Argument QUERY-INDEX Index of the query."
  (interactive "p")
  (flower-cycle-query query-index)
  (flower-list-tasks))

;;;###autoload
(defun flower-org-show-task-info ()
  "Show task info in buffer specified by 'flower-buffer-task' variable."
  (interactive)
  (let ((flower-open-command 'flower-show-task-info))
    (org-open-at-point)))

;;;###autoload
(defun flower-open (task-id)
  "Visit flower tracker task.
Argument TASK-ID Task identifier."
  (interactive "sEnter task id: ")
  (funcall flower-open-command task-id))

(org-add-link-type "flower" 'flower-open)

(define-minor-mode flower-mode
  "Flower mode for viewing issues from task trackers"
  :init-value nil
  :lighter " Flower"
  :keymap (let ((map (make-sparse-keymap)))
            (define-key map (kbd "M-.") 'flower-org-show-task-info)
            (define-key map (kbd "M-n") 'flower-cycle-query-and-go)
            map)
  :group 'flower)

(provide 'flower)

;;; flower.el ends here
