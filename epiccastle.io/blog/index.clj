(require '[clojure.string :as string])
(import [java.time OffsetDateTime]
        [java.time.format DateTimeFormatter])

(defn overview [body]
  (-> body
      (convert-to :hiccup-seq)
      (->> (filter #(= :p (first %))))
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

(defn string->datetime [s]
  (OffsetDateTime/parse s DateTimeFormatter/RFC_1123_DATE_TIME))

(defn datetime->string [dt]
  (.format dt DateTimeFormatter/RFC_1123_DATE_TIME))

(def now (OffsetDateTime/now))
(def now-string (datetime->string now))

(defn render-post [{:keys [splash author body snake-title]}]
  (-> [[:img.blog-splash {:src (:image splash) :alt author :style {:width "50%"}}]]
      (concat body)
      (enlive/at
       [:img] (el-update-in
               [:attrs :src] #(str "https://epiccastle.io/blog/" snake-title "/" %)))))

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
                               :body (markdown (str n "/body.md"))
                               )
                   ]
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
               [:img.image.fit.right
                {:src (str n "/" (:image splash))
                 :style {:width "25%"}
                 }]
               [:div {:style {:display "flex"
                              :justify-content "left"
                              :padding-bottom "32px"}}
                [:div {:style {:display "flex"}}
                 [:img {:src "/images/people/crispin.jpg"
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
               [:div {:style {:font-size  "10pt"}} (overview body)]]
              [:br]]))]]

  ;; rss feed
  (spit "feed.xml"
        (-> [:rss {:version "2.0"}
             [:channel
              [:title "Epiccastle Blog"]
              [:description "Epiccastle.io Updates"]
              [:link "https://epiccastle.io/blog/"]
              [:lastBuildDate now-string]
              [:pubDate now-string]
              [:ttl "1440"]
              (for [[n {:keys [snake-title
                               title
                               rss-date]
                        :as post-data}] (reverse (sort posts))]
                [:item
                 [:title title]
                 [:description (-> post-data
                                   render-post
                                   as-html
                                   escape-html)]
                 [:link (str "https://epiccastle.io/blog/" snake-title)]
                 [:gid snake-title]
                 [:pubDate rss-date]])]]
            (convert-to :xml)))

  ;; summary page
  (selmer "../templates/site.html"
          {:title "Blog Posts"
           :body (as-html
                  [:div#main.wrapper.style1 {:style {:padding-top "0px"}}
                   [:div.container
                    [:div {:style {:float "right" :padding-top "16px"}}
                     [:a {:href "feed.xml"
                          :style "border-bottom: none;"}
                      [:img {:alt "rss feed" :src "/images/rss-small.png"}]]]
                    [:h1.title-heading "Blog"]
                    summaries]])}))
