(defproject {{name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [flower "0.4.5"]]
  :main ^:skip-aot {{name}}.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
