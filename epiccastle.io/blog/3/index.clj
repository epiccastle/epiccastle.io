(require '[clojure.string :as string])

(def words-per-minute 150)

(defn word-count [data]
  (-> data
      (convert-to :html)
      (string/replace #"<[^>]+>" " ")
      string/trim
      (string/split #"\s+")
      count))

(defn read-time-minutes [data]
  (let [words (word-count data)]
    (-> (/ words words-per-minute)
        Math/ceil
        int)))

(let [body (markdown "body.md")
      {:keys [title] :as vars} (yaml "vars.yml")
      vars (assoc vars
                  :read-time (read-time-minutes body)
                  :body (-> body
                            (enlive/at [:img] (enlive/add-class "image" "fit")
                                       [:pre] (enlive/set-attr "style" "border-radius:8px;margin-bottom:32px;")
                                       [:code] (enlive/set-attr "style" "padding:0px;")
                                       [:code.clojure] (enlive/add-class "language-clojure")
                                       [:pre] (enlive/add-class "language-")
                                       )
                            (convert-to :html)))
      ]
  (selmer "../../templates/site.html"
          {:title title
           :body (-> (selmer "../../templates/blog.html" vars)
                     (enlive/at [:img.blog-splash] (enlive/add-class "image" "fit")
                                [:h2] (enlive/add-class "title-heading"))
                     (as-html))
           :extra-scripts
           ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-core.min.js"
            "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/autoloader/prism-autoloader.min.js"]
           :extra-styles
           ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/themes/prism-tomorrow.min.css"]

           }

          ))
