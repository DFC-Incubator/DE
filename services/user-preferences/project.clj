(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/user-preferences "5.2.5.0"
  :description "DE API for managing user preferences."
  :url "https://github.com/iPlantCollaborativeOpenSource/DE"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [user-preferences.core]
  :main user-preferences.core
  :uberjar-name "user-preferences-standalone.jar"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/common-cli "5.2.5.0"]
                 [org.iplantc/common-cfg "5.2.5.0"]
                 [org.iplantc/kameleon "5.2.5.0"]
                 [org.iplantc/service-logging "5.2.5.0"]
                 [io.aviso/pretty "0.1.17"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.5.0"
                   :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                                [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                                [com.fasterxml.jackson.core/jackson-annotations]
                                [com.fasterxml.jackson.core/jackson-databind]
                                [com.fasterxml.jackson.core/jackson-core]]]
                 [compojure "1.1.6"]
                 [midje "1.6.3"]
                 [ring "1.4.0"]
                 [ring/ring-json "0.3.1"]]
  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.1.1"]])
