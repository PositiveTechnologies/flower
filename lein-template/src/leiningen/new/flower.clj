(ns leiningen.new.flower
  (:require [leiningen.new.templates :as templates]
            [leiningen.core.main :as main]))


(def render (templates/renderer "flower"))


(defn flower [name]
  (let [data {:name name
              :sanitized (templates/name-to-path name)
              :year (templates/year)}]
    (main/info "Generating fresh 'lein new' flower project.")
    (templates/->files data
                       [".gitignore"  (render "gitignore" data)]
                       ["project.clj" (render "project.clj" data)]
                       ["README.md"   (render "README.md" data)]
                       ["src/{{sanitized}}/core.clj" (render "core.clj" data)])))
