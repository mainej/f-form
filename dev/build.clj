(ns build
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.java.io :as jio]
            [clojure.string :as string]
            [clojure.tools.build.api :as b]
            [clojure.zip :as zip]
            [deps-deploy.deps-deploy :as d]))

(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")

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
(def pom-dir (jio/file (b/resolve-path class-dir) "META-INF" "maven" (namespace lib) (name lib)))

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

(defn git-rev []
  (let [{:keys [exit out]} (b/process {:command-args ["git" "rev-parse" "HEAD"]
                                       :out          :capture})]
    (when (zero? exit)
      (string/trim out))))

(defn pom [params]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})

  ;; insert git revision as pom.xml > project > scm > tag
  (let [pom-file (jio/file pom-dir "pom.xml")
        rev-tag  (xml/sexp-as-element [::pom/tag (git-rev)])
        scm      (-> (xml/parse (jio/input-stream pom-file)
                                :skip-whitespace true)
                     zip/xml-zip
                     (zip-xml/xml1-> ::pom/project ::pom/scm))
        pom      (zip/root
                  (if-let [existing-child (zip-xml/xml1-> scm ::pom/tag)]
                    (zip/edit existing-child (constantly rev-tag))
                    (zip/append-child scm rev-tag)))]
    (spit pom-file (xml/indent-str pom)))

  params)

(defn jar [params]
  (pom params)

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
  (when-not (-> {:command-args ["git" "rev-list" tag]
                 :out          :ignore
                 :err          :ignore}
                b/process
                :exit
                zero?)
    (die 12 "Git tag %s must exist." tag))
  (let [{:keys [exit out]} (b/process {:command-args ["git" "describe" "--tags" "--abbrev=0" "--exact-match"]
                                       :out          :capture})]
    (when (or (not (zero? exit))
              (not= (string/trim out) tag))
      (die 13 "Git tag %s must be on HEAD." tag)))
  params)

(defn git-push [params]
  (when (or (-> {:command-args ["git" "push" "origin" tag]
                 :out          :ignore
                 :err          :ignore}
                b/process
                :exit
                zero?
                not)
            (-> {:command-args ["git" "push" "origin"]
                 :out          :ignore
                 :err          :ignore}
                b/process
                :exit
                zero?
                not))
    (die 14 "Couldn't sync with github."))
  params)

(defn release [params]
  (assert-scm-clean params)
  (assert-scm-tagged params)
  (assert-changelog-updated params)
  (jar params)
  (d/deploy {:installer :remote
             :artifact  jar-file
             :pom-file  (jio/file pom-dir "pom.xml")})
  (git-push params)
  params)

(comment
  (clean nil)
  (jar nil)
  (:libs (b/create-basis {#_#_:root nil
                          :user     nil
                          :project  "deps.edn"
                          :aliases  [:release]}))

  (str lib)

  (git-push nil)


  )
