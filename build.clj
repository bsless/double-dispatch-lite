(ns build
  (:refer-clojure :exclude [test compile])
  (:require
   [clojure.tools.build.api :as b]
   [org.corfield.build :as bb]))

(def lib 'io.github.bsless/double-dispatch-lite)
(def version (format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))

(defn compile [opts]
  (b/javac {:src-dirs ["src/main/java"]
            :class-dir class-dir
            :basis basis})
  opts)

(defn test
  "Run the tests."
  [opts]
  (bb/run-tests opts))

(defn ci
  "Run the CI pipeline of tests (and build the JAR)."
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/clean)
      (compile)
      (bb/run-tests)
      (bb/jar)))

(defn clean [opts] (bb/clean opts))

(defn install
  "Install the JAR locally."
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy
  "Deploy the JAR to Clojars."
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))
