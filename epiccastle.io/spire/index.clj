(require '[clojure.string :as string])

(defn el-update-in
  "an update-in transformation for enlive/at processing

  eg.
  (enlive/at doc [:a] (el-update-in [:attr :href] str \".html\"))

  appends .html to all links hrefs
  "
  [path func & func-args]
  (fn [el]
    (apply update-in el path func func-args)))

(defn markdown? [s]
  (-> s
      (string/split #"\n")
      (->> (map string/trim)
           (filter #(not (empty? %))))
      first
      (string/starts-with? "#")))

(defn trim-leading-trailing [s]
  (-> s
      (string/replace #"^\s+" "")
      (string/replace #"\s+$" "")))

(selmer "../templates/site.html"
        {:title "Spire: Pragmatic Provisioning"

         :extra-scripts
         ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-core.min.js"
          "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/autoloader/prism-autoloader.min.js"]
         :extra-styles
         ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/themes/prism-tomorrow.min.css"]

         :body
         (as-html
          [[:section#spire-banner {:style "header:50vh;"}
            [:div.content
             [:header
              [:h2.title-heading {:style {:font-family "'Varela Round', sans-serif"}} "Spire"]
              [:p "Pragmatic Provisioning"]]]]

           [:section {:style "margin-top: 20px; margin-left: 20px; margin-right: 20px;"}
            [:div.container
             [:h3 "Overview"]
             [:img.fit-width {:src "cast.svg"
                              :style "border-radius: 16px;"}]]]

           [:section {:style "margin-top: 8px; margin-left: 20px; margin-right: 20px;"}
            [:div.container
             (-> (->> (markdown "../../../spire/README.md")
                      (drop-while #(not= :h2 (first %)))
                      (drop 1))

                 (enlive/at [:code.shell-session] (enlive/add-class "language-shell-session")
                            [:pre] (enlive/add-class "language-")
                            [:a] (el-update-in
                                  [:attrs :href]
                                  (fn [href]
                                    (if (string/starts-with? href "https://github.com/")
                                      href
                                      (-> href
                                          (string/split #"/" -1)
                                          last
                                          (string/split #"\." -1)
                                          butlast
                                          (->> (string/join "."))
                                          (str ".html")))))
                            [:code.clojure] (enlive/add-class "language-clojure")
                            [:pre] (enlive/set-attr "style" "border-radius:8px;margin-bottom:32px;")
                            [:pre] (enlive/add-class "language-")
                            [:code] (enlive/set-attr "style" "padding:0px;")
                            ))]]])})



#_

[:section
 [:div.container
  (-> (markdown "../../../spire/README.md")
      (enlive/at [:a] (el-update-in [:attrs :href] str ".html")))]]

#_

[:section
 [:div.container
  (-> (markdown "body.md")
      (enlive/at
       [:a] (el-update-in [:attrs :href] #(-> %
                                              (string/split #"/" -1)
                                              last
                                              (string/split #"\." -1)
                                              butlast
                                              (->> (string/join "."))
                                              (str ".html")))))]]
