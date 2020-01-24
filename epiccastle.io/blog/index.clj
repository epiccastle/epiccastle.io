(require '[clojure.string :as string])

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
        (-> (as-html
             [:rss {:version "2.0"}
              [:channel
               [:title "Epiccastle Blog"]
               [:description "Epiccastle.io Updates"]
               [:link2 "https://epiccastle.io/blog/"]
               ;; date +"%a, %d %b %Y %H:%M:%S %z"
               [:last-build-date "Thu, 23 Jan 2020 15:58:40 +0800"]
               [:pubDate "Wed, 06 Nov 2019 16:20:00 +0000"]
               [:ttl "1800"]
               (for [[n {:keys [snake-title
                                title
                                blurb
                                author
                                date
                                splash
                                rss_date
                                body]}] (reverse (sort posts))]
                 [:item
                  [:title title]
                  [:description #_ blurb
                   (escape-html (as-html
                                 (-> (concat [[:img.blog-splash {:src (:image splash) :alt author :style {:width "50%"}}]] body)
                                     (enlive/at [:img] (el-update-in [:attrs :src] #(str "https://epiccastle.io/blog/" snake-title "/" %)) )
                                     )
                                 ))]
                  [:link2 (str "https://epiccastle.io/blog/" snake-title)]
                  [:gid snake-title]
                  [:pubDate rss_date]])]])
            (string/replace #"link2" "link")
            (string/replace #"last-build-date" "LastBuildDate")
            (string/replace #"pubdate" "pubDate")))

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
