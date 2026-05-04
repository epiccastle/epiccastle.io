(load-file "../lib.clj")
(refer 'lib)
(require '[clojure.string :as string])
(import [java.time OffsetDateTime]
        [java.time.format DateTimeFormatter DateTimeFormatterBuilder]
        [java.util Locale])

(defn overview [body]
  (->> body
       (filter #(and (vector? %) (= :p (first %))))
       first))

(defn el-update-in [path func & func-args]
  (fn [el]
    (apply update-in el path func func-args)))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (-> text
      (string/replace "&" "&amp;")
      (string/replace "<" "&lt;")
      (string/replace ">" "&gt;")
      (string/replace "\"" "&quot;")))

;; Lenient RFC-1123-ish parsers: accept both abbreviated ("Aug") and full
;; ("August") month names, and 1- or 2-digit days.
(def ^:private lenient-rfc1123-parsers
  (mapv #(-> (DateTimeFormatterBuilder.)
             (.parseCaseInsensitive)
             (.appendPattern %)
             (.toFormatter Locale/ENGLISH))
        ["EEE, d MMM yyyy HH:mm:ss Z"
         "EEE, d MMMM yyyy HH:mm:ss Z"]))

(defn string->datetime [s]
  (loop [[fmt & more] lenient-rfc1123-parsers
         last-exc nil]
    (if (nil? fmt)
      (throw last-exc)
      (let [result (try
                     (OffsetDateTime/parse s fmt)
                     (catch java.time.format.DateTimeParseException e e))]
        (if (instance? java.time.format.DateTimeParseException result)
          (recur more result)
          result)))))

(defn datetime->string [dt]
  (.format dt DateTimeFormatter/RFC_1123_DATE_TIME))

(def now (OffsetDateTime/now))
(def now-string (datetime->string now))
(def atom-now-string (.format now DateTimeFormatter/ISO_OFFSET_DATE_TIME))

(defn render-post [{:keys [splash author body snake-title]}]
  (-> (concat [[:img.blog-splash {:src (:image splash) :alt author :style {:width "50%"}}]]
              body)
      (enlive-at
       [:img] (fn [el]
                (let [el (if (map? (second el))
                           el
                           (into [(first el) {}] (rest el)))]
                  (update-in el [1 :src] #(str "https://epiccastle.io/blog/" snake-title "/" %)))))))

(let [posts
      (->> (for [filename (glob "*/vars.yml")]
             (let [n (-> filename
                         (string/split #"/")
                         first
                         parse-string)
                   {:keys [title] :as vars} (yaml filename)
                   snake-title (-> title
                                   string/lower-case
                                   (string/split #"\s+")
                                   (->> (string/join "-")))
                   vars (assoc vars
                               :snake-title snake-title
                               :body (markdown (str n "/body.md")))]
               [n vars]))
           (into {}))

      summaries
      [:div.container
       (for [[n {:keys [snake-title
                        title
                        blurb
                        author
                        date
                        splash
                        body]}] (reverse (sort posts))]
         (do (symlink snake-title (str n))
             [:div
              [:hr]
              [:div {:style {:cursor "pointer"}
                     :onclick (str "window.location='" snake-title "'")}
               [:h2 {:style {:margin-bottom "16px"}} title]
               [:img.image.right
                {:src (str n "/" (:image splash))
                 :style {:width "25%"
                         :margin-top "6rem"}}]
               [:div {:style {:display "flex"
                              :justify-content "left"
                              :padding-bottom "32px"}}
                [:div {:style {:display "flex"}}
                 [:img {:src "/images/people/crispin-wellington.jpg"
                        :style {:width "48px"
                                :height "48px"
                                :border-radius "50%"}}]
                 [:div {:style {:text-align "left"
                                :margin-left "16px"}}
                  [:div author]
                  [:div {:style {:font-size "14px"
                                 :margin-top "-8px"}}
                   date]]]]

               [:div [:b blurb]]
               [:div {:style {:font-size "1.2rem"}} (overview body)]]
              [:br]]))]]

  ;; atom feed
  (spit "feed.xml"
        (convert-to
         [:feed {:xmlns "http://www.w3.org/2005/Atom"}
          [:title "Epiccastle Blog"]
          [:subtitle "Epiccastle.io Updates"]
          [:link {:href "https://epiccastle.io/blog/feed.xml" :rel "self"}]
          [:link {:href "https://epiccastle.io/blog/"}]
          [:id "https://epiccastle.io/blog/"]
          [:updated atom-now-string]
          [:author
           [:name "Crispin Wellington"]]
          (for [[n {:keys [snake-title
                           title
                           rss-date
                           author]
                    :as post-data}] (reverse (sort posts))]
            (let [entry-url (str "https://epiccastle.io/blog/" snake-title)
                  entry-updated (.format (string->datetime rss-date)
                                         DateTimeFormatter/ISO_OFFSET_DATE_TIME)]
              [:entry
               [:title title]
               [:link {:href entry-url}]
               [:id entry-url]
               [:updated entry-updated]
               [:published entry-updated]
               (when author [:author [:name author]])
               [:content {:type "html"}
                [:-cdata (-> post-data
                             render-post
                             as-html)]]]))]
         :xml))

  ;; summary page
  (output!
   (selmer "../templates/site.html"
           {:title "Blog Posts"
            :body (as-html
                   [:div#main.wrapper.style1
                    [:div.container
                     [:div {:style {:float "right" :padding-top "16px"}}
                      [:a {:href "feed.xml"
                           :style "border-bottom: none;"}
                       [:img {:alt "rss feed" :src "/images/rss-small.png"}]]]
                     [:h1.title-heading "Blog"]
                     summaries]])})))
