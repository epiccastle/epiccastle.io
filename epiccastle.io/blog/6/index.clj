(load-file "../../lib.clj")
(refer 'lib)
(require '[clojure.string :as string])

(def words-per-minute 150)

(defn word-count [data]
  (-> (as-html data)
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
                            (enlive-at [:img] (enlive-add-class "image" "fit")
                                       [:pre] (enlive-set-attr "style" "border-radius:8px;margin-bottom:32px;")
                                       [:code] (enlive-set-attr "style" "padding:5px;font-size:1em;")
                                       [:code.clojure] (enlive-add-class "language-clojure")
                                       [:code.cpp] (enlive-add-class "language-cpp")
                                       [:code.makefile] (enlive-add-class "language-makefile")
                                       [:pre] (enlive-add-class "language-"))
                            as-html))]
  (output!
   (selmer "../../templates/site.html"
           {:title title
            :body (-> (selmer "../../templates/blog.html" vars)
                      as-html)
            :extra-scripts
            ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-core.min.js"
             "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js"]
            :extra-styles
            ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css"]})))
