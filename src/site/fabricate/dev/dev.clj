(ns site.fabricate.dev
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :refer [sh]]
            [hiccup.core :refer [html]]))

;; ─── Configuration ────────────────────────────────────────────────

(def src-dir ".")
(def out-dir "public")
(def css-dir "src/site/fabricate/dev/templates")

;; ─── Frontmatter Parser ────────────────────────────────────────────

(defn parse-frontmatter
  "Parse key: value frontmatter from a markdown file.
   Format: fields at top, --- separator, then markdown body."
  [file-path]
  (let [content   (slurp file-path)
        lines     (str/split-lines content)
        ;; Find the --- separator
        sep-idx   (first (keep-indexed (fn [i line] 
                                         (when (= "---" (str/trim line)) i))
                                       lines))
        meta-lines (if sep-idx 
                     (take sep-idx lines) 
                     (take-while #(re-find #"^(\w[\w\s]*?)\s*:\s*(.*)" %) lines))
        body-lines (if sep-idx 
                     (drop (inc sep-idx) lines) 
                     (drop (count meta-lines) lines))
        body       (str/join "\n" body-lines)]

    ;; Parse key: value pairs
    (reduce (fn [acc line]
              (if-let [[_ k v] (re-find #"^(\w[\w\s]*?)\s*:\s*(.*)" line)]
                (assoc acc (keyword (str/lower-case (str/trim k)))
                       (str/trim v))
                acc))
            {:content body
             :slug    (-> file-path io/file .getName
                           (str/replace #"\.md$" ""))}
            meta-lines)))

;; ─── Markdown to HTML ──────────────────────────────────────────────

(defn markdown->html
  "Convert markdown string to HTML using pandoc."
  [markdown-content]
  (let [result (sh "pandoc" "-f" "markdown" "-t" "html5"
                   "--no-highlight"
                   :in markdown-content)]
    (if (= 0 (:exit result))
      (str/trim (:out result))
      (do
        (println "⚠️  Pandoc error:" (:err result))
        ;; Fallback: wrap in <p> tags
        (str "<p>" (str/escape markdown-content {\< "&lt;" \> "&gt;" \& "&amp;"}) "</p>")))))

;; ─── Hiccup Template: Single Post ──────────────────────────────────

(defn compile-d2-diagram [slug idx d2-code]
  (let [diagrams-dir (io/file out-dir "diagrams")
        _ (.mkdirs diagrams-dir)
        filename (str slug "-diagram-" idx "-" (Math/abs (.hashCode d2-code)) ".svg")
        file (io/file diagrams-dir filename)]
    ;; Only run compiler if the target file doesn't exist
    (when-not (.exists file)
      (let [escaped-d2 (str/replace d2-code #"(?<!\\)\$" "\\\\\\$")
            result (sh "d2" "--theme" "0" "--dark-theme" "0" "-l" "dagre" "-" (.getAbsolutePath file)
                       :in escaped-d2)]
        (when-not (= 0 (:exit result))
          (println "⚠️  D2 compilation failed for" filename ":" (:err result)))))
    (str "diagrams/" filename)))

(defn process-d2-blocks [slug markdown-content]
  (if-not markdown-content
    ""
    (let [pattern #"```d2\s*([\s\S]*?)\n```"
          matches (re-seq pattern markdown-content)]
      (loop [content markdown-content
             idx 1
             [[full-match d2-code] & remaining] matches]
        (if-not full-match
          content
          (let [svg-path (compile-d2-diagram slug idx d2-code)
                replacement (str "\n\n<div class=\"d2-diagram\"><img src=\"" svg-path "\" alt=\"Architecture Diagram\" /></div>\n\n")]
            (recur (str/replace-first content full-match replacement)
                   (inc idx)
                   remaining)))))))

(defn render-post-html
  "Render a single blog post page as HTML.
   Uses a placeholder approach to inject raw HTML from pandoc into Hiccup output."
  [{:keys [title date tags description content content-html slug] :as post}]
  (let [processed-content (process-d2-blocks slug content)
        body-html (or content-html (markdown->html processed-content))
        tag-list   (when tags
                     (map str/trim (str/split tags #",")))
        page-html (html
                    [:html {:lang "en"}
                     [:head
                      [:meta {:charset "UTF-8"}]
                      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
                      [:title (str title " — nurazhar.com")]
                      [:meta {:name "description" :content description}]
                      [:link {:rel "stylesheet" :href "styles.css"}]
                      [:link {:rel "stylesheet" :href "d2-mobile.css"}]]
                     [:body
                      [:nav {:class "nav"}
                       [:a {:href "index.html" :class "nav-link"} "← Home"]
                       [:a {:href "https://gitlab.com/nurazhar" :target "_blank" :rel "noopener noreferrer" :class "gitlab-link" :title "GitLab"}
                        "GitLab ↗"]]
                      [:article {:class "post"}
                       [:header {:class "post-header"}
                        [:h1 {:class "post-title"} title]
                        [:div {:class "post-meta"}
                         [:time {:datetime date} date]
                         (when (seq tag-list)
                           [:span {:class "post-tags"}
                            (for [t tag-list]
                              [:span {:class "post-tag"} t])])]]
                       [:div {:class "post-content"} "%%RAW_CONTENT%%"]]
                      [:footer {:class "post-footer"}
                       [:p "© 2026 nurazhar.com"]]]])]
    (let [[before after] (str/split page-html #"%%RAW_CONTENT%%" 2)]
      (str before body-html after))))

;; ─── Hiccup Template: Index Page ───────────────────────────────────

(defn render-index-html
  "Render the blog index page listing all posts."
  [posts]
  (html
   [:html {:lang "en"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     [:title "Nur Azhar — Blog"]
     [:meta {:name "description"
             :content "Singaporean Malay Technical Blog"}]
     [:link {:rel "stylesheet" :href "styles.css"}]
     [:link {:rel "stylesheet" :href "d2-mobile.css"}]]
    [:body
     [:nav {:class "nav"}
      [:div {:class "nav-left"}
       [:h1 "Nur Azhar"]
       [:p {:class "tagline"} "Singaporean Malay Technical Blog"]]
      [:a {:href "https://gitlab.com/nurazhar" :target "_blank" :rel "noopener noreferrer" :class "gitlab-link" :title "GitLab"}
       "GitLab ↗"]]
     [:main {:class "post-list"}
      (for [{:keys [title date description slug]} posts]
        [:a {:href (str slug ".html") :class "post-card"}
         [:h2 {:class "post-card-title"} title]
         [:div {:class "post-card-meta"}
          [:time {:datetime date} date]]
         (when description
           [:p {:class "post-card-description"} description])])]
     [:footer {:class "site-footer"}
      [:p "© 2026 nurazhar.com"]]]]))

;; ─── Asset Copying ─────────────────────────────────────────────────

(defn copy-assets
  "Copy static assets (CSS) from templates dir to public/."
  []
  (when (.isDirectory (io/file css-dir))
    (doseq [f (file-seq (io/file css-dir))
            :when (.isFile f)
            :let [name (.getName f)]
            :when (str/ends-with? name ".css")]
      (io/copy f (io/file out-dir name))
      (println "   📄 Copied" name))))

;; ─── Build ─────────────────────────────────────────────────────────

(defn clear-output
  "Clean and recreate the output directory."
  []
  (let [d (io/file out-dir)]
    (when (.exists d)
      (println "🧹 Clearing" out-dir "...")
      (let [all (reverse (vec (file-seq d)))]
        ;; Delete files depth-first so directories become empty
        (doseq [f all :when (.isFile f)]
          (io/delete-file f true))
        ;; Remove now-empty directories
        (doseq [f all :when (and (.isDirectory f) (not= f d))]
          (io/delete-file f true))))
    (.mkdirs d)
    (println "✅ Output directory ready.")))

(defn discover-posts
  "Find all .md files in the root directory (non-recursive),
   parse their frontmatter, return sorted list."
  []
  (let [root-dir (io/file src-dir)
        md-files (->> (.listFiles root-dir)
                      (filter #(and (.isFile %)
                                    (str/ends-with? (.getName %) ".md")
                                    (not (str/starts-with? (.getName %) "."))))
                      (map #(.getAbsolutePath %)))]
    (println (str "📄 Found " (count md-files) " markdown files."))
    (->> md-files
         (map parse-frontmatter)
         (sort-by :date)
         reverse)))

(defn build
  "Main build function: discover posts, render pages, write output."
  []
  (println "🏗️  Building site with Fabricate...")
  (clear-output)

  (let [posts (discover-posts)]
    ;; Generate individual post pages
    (doseq [post posts]
      (let [out-file (str out-dir "/" (:slug post) ".html")]
        (println "   ✍️ " (:slug post))
        (spit out-file (render-post-html post))))

    ;; Generate index page
    (println "   📋 Generating index...")
    (spit (str out-dir "/index.html") (render-index-html posts))

    ;; Copy static assets
    (println "📋 Copying assets...")
    (copy-assets))

  (println (str "✅ Site built! Output in " out-dir "/")))

;; ─── CLI Entry Point ───────────────────────────────────────────────

(defn -main
  "Entry point for 'bb build'"
  [& args]
  (build))

;; Allow running as script: bb -f dev.clj
(when (= *file* (System/getProperty "babashka.file"))
  (build))
