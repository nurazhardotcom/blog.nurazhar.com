#!/usr/bin/env bb
;; tests/run-test.bb
;;
;; Smoke test for scripts/validate_links.clj.
;;
;; Setup:
;;   1. Wipe + rebuild tests/.test-workspace/public/
;;   2. Drop fixture.html (4 distinct hrefs) and good.html (a stub target)
;;
;; Execution:
;;   Run the validator against the temp public/ and capture its exit code
;;   plus stdout. We then regex-parse the printed counts.
;;
;; Assertion:
;;   The validator must report html=2 (fixture + good.html), md-links=1
;;   (source.md), broken=1 (broken-target.html), and exit=1 (broken was
;;   detected). If any of those numbers is wrong, the test fails loudly.
;;
;; Note on the bp/process deref: babashka.process/process returns a
;; future/promise — we MUST deref it with @ to actually wait for the
;; subprocess to finish. Bare `(let [p (bp/process ...)] (:exit p))`
;; returns nil because the process hasn't run yet.

(require '[babashka.fs :as fs]
         '[babashka.process :as bp])

(def script-dir (-> (System/getProperty "babashka.file")
                    fs/file .getParent str))

(def project-root (-> script-dir fs/file .getParent str))

(def validator (str project-root "/scripts/validate_links.clj"))

(defn setup-fixture! []
  (let [workspace (str script-dir "/.test-workspace")
        public    (str workspace "/public")]
    (when (fs/exists? workspace)
      (fs/delete-tree workspace))
    (fs/create-dirs public)
    (fs/copy (str script-dir "/fixture.html")
             (str public "/fixture.html"))
    ;; good.html exists so the "good.html" link in fixture.html resolves.
    (spit (str public "/good.html")
          "<!DOCTYPE html><html><body>good target exists</body></html>\n")
    public))

(defn parse-count [pattern text]
  (when-let [m (re-find (re-pattern pattern) text)]
    (try (Long/parseLong (second m))
         (catch Exception _ nil))))

(defn fail! [detail]
  (println "❌ test_validate_links FAILED:" detail)
  (System/exit 1))

(defn -main [& _args]
  (let [public-dir (setup-fixture!)
        result     (try
                     (let [p @(bp/process {:out :string
                                           :err :string
                                           :dir  project-root}
                                          "bb" validator public-dir)]
                       {:exit (:exit p)
                        :out  (or (:out p) "")
                        :err  (or (:err p) "")})
                     (catch Exception e
                       {:exit -1 :out "" :err (.getMessage e)}))]
    (println "=== Validator stdout ===")
    (println (:out result))
    (when (seq (:err result))
      (println "=== Validator stderr ===")
      (println (:err result)))

    (let [html   (parse-count "Validated\\s+(\\d+)\\s+HTML files" (:out result))
          md     (parse-count "MD Links count:\\s+(\\d+)" (:out result))
          broken (parse-count "Broken Links count:\\s+(\\d+)" (:out result))]
      (println (format "TEST PARSE: html-files=%s md-links=%s broken=%s exit=%s"
                       html md broken (:exit result)))
      (cond
        (nil? html)   (fail! "could not parse HTML count from validator output")
        (nil? md)     (fail! "could not parse MD links count")
        (nil? broken) (fail! "could not parse broken links count")
        (not= (:exit result) 1) (fail! (format "expected exit=1 (broken detected), got %d"
                                                (:exit result)))
        (not= html 2)   (fail! (format "expected html=2 (fixture + good.html), got %d" html))
        (not= md 1)     (fail! (format "expected md=1, got %d" md))
        (not= broken 1) (fail! (format "expected broken=1, got %d" broken))
        :else (do (println "✅ test_validate_links: all 4 link types handled correctly")
                  (System/exit 0))))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main))
