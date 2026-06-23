;; src/site/fabricate/dev/dev.clj
(ns site.fabricate.dev
  (:require [site.fabricate.api :as api]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

def clear-output []
  (when (.exists (io/file "public"))
    (println "🧹 Clearing output directory...")
    (io/delete-file (io/file "public")))
  (io/make-parents (io/file "public")))

def build-site []
  (println "🏗️  Building site with Fabricate...")
  (clear-output)
  (api/construct! {})
  (println "✅ Site build complete!")

def parse-post-content [content]
  (let [lines (str/split-lines content)
        frontmatter-end (first (map-indexed (fn [i line] 
                                             (when (str/starts-with? line "---") i)) lines))
        frontmatter (take frontmatter-end lines)
        body-content (drop (inc frontmatter-end) lines)]
    
    ;; Parse frontmatter
    (let [metadata (reduce (fn [acc line]
                            (if (str/contains? line ":")
                              (let [[k v] (str/split line #": " 2)]
                                (assoc acc (keyword (str/trim k)) (str/trim v)))
                              acc))
                          {}
                          frontmatter)]
      
      {:title (:title metadata)
       :date (:date metadata)
       :tags (if (:tags metadata) (str/split (:tags metadata) #", ") [])
       :description (:description metadata)
       :d2-code (:d2 metadata)
       :content (str/join "\n" body-content)}))

def generate-post-html [post]
  (let [d2-code (:d2-code post)
        diagram-type (analyze-diagram-type d2-code)]
    [:html {:lang "en"}
     [:head
      [:meta {:charset "UTF-8"}]
      [:meta {:name "viewport" 
              :content "width=device-width, initial-scale=1.0"}]
      [:title {:content (str (:title post) " - nurazhar.com")}]

      [:link {:rel "stylesheet" :href "/styles.css"}]
      [:link {:rel "stylesheet" :href "/d2-mobile.css"}]
      [:script {:src "https://cdn.jsdelivr.net/npm/d2lib/dist/d2.min.js"}]]
     
     [:body
      [:nav {:class "nav"}
       [:a {:href "/" :class "nav-link"} "Home"]
       [:a {:href "/about" :class "nav-link"} "About"]
       [:a {:href "/contact" :class "nav-link"} "Contact"]]

      [:article {:class "post"}
       [:header {:class "post-header"}
        [:h1 {:class "post-title"} (:title post)]
        [:div {:class "post-meta"}
         [:time {:datetime (:date post)} (:date post)]
         [:span {:class "post-tags"} (str/join ", " (:tags post))]]]
       
       [:div {:class "post-content"} (:content post)]
       
       ;; D2 Diagram Section
       (when d2-code
         [:section {:class "diagram-section"}
          [:h2 "Architecture Diagram"]
          [:div {:class "diagram-container"}
           (case (:diagram-type post)
             :cards (render-cards d2-code)
             :d2 (render-d2 d2-code)
             :table (render-table d2-code)
             (render-d2 d2-code))]])]

      [:footer {:class "post-footer"}
       [:hr]
       [:p "© 2026 nurazhar.com • Built with Fabricate"]]]]])

def analyze-diagram-type [d2-code]
  (cond
    (should-render-as-cards? d2-code) :cards
    (str/includes? d2-code "graph TD") :d2
    (str/includes? d2-code "|") :table
    :else :d2))

def should-render-as-cards? [d2-code]
  (let [lines (str/split-lines d2-code)
        node-count (count (filter #(str/includes? % ":") lines))
        edge-count (count (filter #(str/includes? % "-->") lines))]
    (<= node-count 4)))

def render-as-d2-diagram [d2-code]
  [:div {:class "d2-diagram"}
   [:script {:type "text/d2"}
    d2-code]
   [:div {:class "d2-output" 
          :dangerouslySetInnerHTML {:__html (d2-render d2-code)}}]])

def d2-render [d2-code]
  (str "<img src=\"https://kroki.io/d2/svg/" 
       (java.net.URLEncoder/encode d2-code "UTF-8")
       "\" alt=\"D2 Diagram\" style=\"max-width:100%;\">")

def render-as-html-cards [d2-code]
  [:div {:class "diagram-cards"}
   [:div {:class "card-row"}
    [:div {:class "card"}
     [:h3 "Human Coach"]
     [:p "Accountability · Emotional support"]
     [:div {:class "arrow"} "↓"]]
    
    [:div {:class "card"}
     [:h3 "AI Coach"]
     [:p "Data-driven analysis · 24/7 availability"]
     [:div {:class "arrow"} "↓"]]
    
    [:div {:class "card"}
     [:h3 "Best Together"]
     [:p "AI does the audit. Human does the grounding."]]]]])

def render-as-comparison-table [d2-code]
  [:table {:class "comparison-table"}
   [:thead
    [:tr [:th "Dimension"] [:th "Human Coach"] [:th "AI Coach"]]]
   [:tbody
    [:tr [:td "Cost"] [:td "$200-500/session"] [:td "~$0.50/hr"]]
    [:tr [:td "Context"] [:td "~50 min memory"] [:td "1M token context"]]
    [:tr [:td "Availability"] [:td "Weekly sessions"] [:td "24/7"]]]])]

def build []
  (println "🏗️  Building site...")
  (clear-output)
  (api/construct! {})
  (println "✅ Build complete!"))

(when (= *file* (System/getProperty "babashka.script"))
  (let [args (System/getenv "BABASHKA_ARGS")]
    (cond
      (str/includes? args "--build") (build)
      (str/includes? args "--migrate") (migrate-posts)
      :else (build))))