(require '[clojure.string :as string])

(defn overview [body]
  (-> body
      (convert-to :hiccup-seq)
      (->> (filter #(= :p (first %))))
      first))

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
      [:div#main.wrapper.style1
       [:div.container
        (for [[n {:keys [snake-title
                         title
                         blurb
                         author
                         date
                         splash
                         body]}] (sort posts)]
          (do (symlink snake-title (str n))
              [:div {:style "cursor:pointer;"
                     :onclick (str "window.location='" snake-title "'")}
               [:h2 {:style "margin-bottom: 16px;"} title]
               [:div {:style "display:flex;justify-content:left;padding-bottom:32px;"}
                [:div {:style "display:flex;"}
                 [:img {:src "/images/people/crispin.jpg"
                        :style "width: 48px; height: 48px; border-radius: 50%;"}]
                 [:div {:style "text-align:left;margin-left:16px;"}
                  [:div author]
                  [:div {:style "font-size: 14px;margin-top:-8px;"}
                   date]]]]
               [:img.image.fit.right
                {:src (str n "/" (:image splash))
                 :style "width: 25%;"
                 }]
               [:div [:b blurb]]
               [:div {:style "font-size: 10pt;"} (overview body)]
               ]))]]]
  (selmer "../templates/site.html"
          {:title "Blog Posts"
           :body (as-html summaries)}))
