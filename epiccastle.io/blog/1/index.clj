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
                                       [:code] (enlive/set-attr "style" "adding:0px;"))
                            (convert-to :html)))
      ]
  (selmer "../../templates/site.html"
          {:title title
           :body (-> (selmer "../../templates/blog.html" vars)
                     (enlive/at [:img.blog-splash] (enlive/add-class "image" "fit"))
                     (as-html))}))
