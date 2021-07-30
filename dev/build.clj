(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as d]
            [clojure.java.io :as jio]))

(def lib 'com.github.mainej/f-form)
(def version (format "0.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:root    nil
                            :user    nil
                            :project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))

(comment
  (clean nil)
  (jar nil)
  (:libs (b/create-basis {#_#_:root    nil
                          :user    nil
                          :project "deps.edn"
                          :aliases [:release]}))

  (str lib)

  (d/deploy {:installer :local
             :artifact  jar-file
             :pom-file  (jio/file (b/resolve-path class-dir) "META-INF" "maven"
                                  (namespace lib) (name lib) "pom.xml")})

  )
