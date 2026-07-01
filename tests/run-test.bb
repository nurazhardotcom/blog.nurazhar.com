#!/usr/bin/env bb
;; tests/run-test.bb
;;
;; Smoke test for scripts/validate_links.clj.
;;
;; Two scenarios give us "happy + sad" coverage so refactors that
;; *over-flag* links (false positives) or *under-flag* links (miss
;; broken refs) BOTH fail the suite:
;;
;;   :sad   — tests/fixture.html          expected html=2 md=1 broken=1 exit=1
;;   :happy — tests/fixtures-happy/       expected html=2 md=1 broken=0 exit=0
;;
;; Mechanism: each scenario drops its fixture(s) into a clean
;; tests/.test-workspace/public/ and invokes the real validator with
;; that path as argv[1]. We regex-parse the printed counts and assert.
;;
;; Why the bp/process deref: babashka.process/process returns a future/promise
;; — we MUST deref it with @ to actually wait for the subprocess to finish.
;; Bare `(let [p (bp/process ...)] (:exit p))` returns nil because the
;; process hasn't run yet.

(require '[babashka.fs :as fs]
         '[babashka.process :as bp])

(def script-dir (-> (System/getProperty "babashka.file")
                    fs/file .getParent str))

(def project-root (-> script-dir fs/file .getParent str))

(def validator (str project-root "/scripts/validate_links.clj"))

(def workspace (str script-dir "/.test-workspace"))
(def workspace-public (str workspace "/public"))

;; ─── Scenarios ─────────────────────────────────────────────────────

(defn setup-sad-fixture! []
  ;; Wipe workspace.
  (when (fs/exists? workspace)
    (fs/delete-tree workspace))
  (fs/create-dirs workspace-public)
  ;; Drop the fixture and the stub target it links to.
  (fs/copy (str script-dir "/fixture.html")
           (str workspace-public "/fixture.html"))
  (spit (str workspace-public "/good.html")
        "<!DOCTYPE html><html><body>good target exists</body></html>\n"))

(defn setup-happy-fixture! []
  (when (fs/exists? workspace)
    (fs/delete-tree workspace))
  (fs/create-dirs workspace-public)
  ;; Copy every file from tests/fixtures-happy/ into the temp public/.
  ;; We derive the destination's relative path by stripping the source
  ;; dir prefix from each glob result's string form — this avoids the
  ;; `.getName`/`.getFileName` Path-vs-String gotchas and works for any
  ;; basename function (or none at all).
  (let [src-dir (str script-dir "/fixtures-happy")
        prefix  (str src-dir "/")]
    (doseq [src (->> (fs/glob src-dir "**")
                     (filter fs/regular-file?))]
      (let [rel (str/replace (str src) prefix "")]
        (fs/copy src (fs/path workspace-public rel))))))

(def scenarios
  {:sad   {:setup! setup-sad-fixture!
           :expect {:html 2 :md 1 :broken 1 :exit 1}
           :label  "sad (1 known-broken internal href → must report broken=1 exit=1)"}
   :happy {:setup! setup-happy-fixture!
           :expect {:html 2 :md 1 :broken 0 :exit 0}
           :label  "happy (all internal hrefs resolve → must report broken=0 exit=0)"}})

;; ─── Run + parse one scenario ──────────────────────────────────────

(defn parse-count [pattern text]
  (when-let [m (re-find (re-pattern pattern) text)]
    (try (Long/parseLong (second m))
         (catch Exception _ nil))))

(defn run-scenario! [scenario-key {:keys [setup! expect label]}]
  (let [result (try
                 ;; Setup lives INSIDE the try so a thrown setup error
                 ;; fails the scenario with non-zero exit rather than
                 ;; silently propagating up to babashka's top-level
                 ;; (which exits 0 in some versions).
                 (setup!)
                 (let [p @(bp/process {:out :string
                                       :err :string
                                       :dir  project-root}
                                      "bb" validator workspace-public)]
                   {:exit (:exit p)
                    :out  (or (:out p) "")
                    :err  (or (:err p) "")})
                 (catch Exception e
                   {:exit -1 :out "" :err (.getMessage e)}))
        html   (parse-count "Validated\\s+(\\d+)\\s+HTML files" (:out result))
        md     (parse-count "MD Links count:\\s+(\\d+)" (:out result))
        broken (parse-count "Broken Links count:\\s+(\\d+)" (:out result))
        observed {:html html :md md :broken broken :exit (:exit result)}]
    (println (format "\n=== Scenario: %s — %s ==="
                     (name scenario-key) label))
    (println (format "TEST PARSE: html-files=%s md-links=%s broken=%s exit=%s"
                     html md broken (:exit result)))
    (when (seq (:err result))
      (println "=== Validator stderr ===")
      (println (:err result)))
    (let [mismatches (->> expect
                          (map (fn [[k v]]
                                 (when (not= (get observed k) v)
                                   [k (get observed k) v])))
                          (remove nil?))]
      (if (seq mismatches)
        {:passed?  false
         :key      scenario-key
         :label    label
         :observed observed
         :mismatches mismatches}
        {:passed? true
         :key     scenario-key
         :label   label
         :observed observed}))))

;; ─── Entry point ───────────────────────────────────────────────────

(defn fail! [summaries]
  (println "\n❌ test_validate_links FAILED")
  (doseq [s (filter #(false? (:passed? %)) summaries)]
    (println (format "   Scenario %s: %s" (name (:key s)) (:label s)))
    (println (format "     observed: %s" (:observed s)))
    (doseq [[field got expected] (:mismatches s)]
      (println (format "     ✗ %s: got %s, expected %s" field got expected))))
  (System/exit 1))

(defn -main [& _args]
  (let [results (mapv (fn [[k v]] (assoc (run-scenario! k v) :key k)) scenarios)]
    (if (every? :passed? results)
      (do (println "\n✅ test_validate_links: all scenarios pass (happy + sad).")
          (System/exit 0))
      (fail! results))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main))
