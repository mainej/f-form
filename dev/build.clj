(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as d]
            [clojure.java.io :as jio]
            [clojure.string :as string]))

(def lib 'com.github.mainej/f-form)
(def git-revs (Integer/parseInt (b/git-count-revs nil)))
(defn format-version [revision] (format "0.2.%s" revision))
(def version (format-version git-revs))
(def next-version (format-version (inc git-revs)))
(def tag (str "v" version))
(def next-tag (str "v" next-version))
(def class-dir "target/classes")
(def basis (b/create-basis {:root    nil
                            :user    nil
                            :project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn current-tag [params]
  (println tag)
  params)

(defn preview-tag [params]
  (println next-tag)
  params)

(defn clean [params]
  (b/delete {:path "target"})
  params)

(defn die
  ([code message & args]
   (die code (apply format message args)))
  ([code message]
   (binding [*out* *err*]
     (println message))
   (System/exit code)))

(defn jar [params]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file})
  params)

(defn assert-changelog-updated [params]
  (when-not (string/includes? (slurp "CHANGELOG.md") tag)
    (die 10 "CHANGELOG.md must include tag %s." tag))
  params)

(defn assert-scm-clean [params]
  (when-not (-> {:command-args ["git" "status" "--porcelain"]
                 :out          :capture}
                b/process
                :out
                string/blank?)
    (die 11 "Git working directory must be clean."))
  params)

(defn assert-scm-tagged [params]
  (when-not (zero? (-> {:command-args ["git" "rev-list" tag]
                        :out          :ignore
                        :err          :ignore}
                       b/process
                       :exit))
    (die 12 "Git tag %s must exist." tag))
  (let [{:keys [exit out]} (b/process {:command-args ["git" "describe" "--tags" "--abbrev=0" "--exact-match"]
                                       :out          :capture})]
    (when (or (not (zero? exit))
              (not= (string/trim out) tag))
      (die 13 "Git tag %s must be on HEAD." tag)))
  params)

(defn git-push []
  (when-not (zero? (-> {:command-args ["git" "push" "--follow-tags"]
                        :out          :ignore
                        :err          :ignore}
                       b/process
                       :exit))
    (die 14 "Couldn't sync with github.")))

(defn release [params]
  (assert-scm-clean params)
  (assert-scm-tagged params)
  (assert-changelog-updated params)
  (jar params)
  (d/deploy {:installer :remote
             :artifact  jar-file
             :pom-file  (jio/file (b/resolve-path class-dir) "META-INF" "maven"
                                  (namespace lib) (name lib) "pom.xml")})
  (git-push)
  params)

(comment
  (clean nil)
  (jar nil)
  (:libs (b/create-basis {#_#_:root    nil
                          :user    nil
                          :project "deps.edn"
                          :aliases [:release]}))

  (str lib)


  )
