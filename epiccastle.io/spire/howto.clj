(require '[clojure.string :as string])

(selmer "../templates/site.html"
        {:title "Spire: How To"
         :extra-scripts
         ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-core.min.js"
          "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/autoloader/prism-autoloader.min.js"]
         :extra-styles
         ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/themes/prism-tomorrow.min.css"]
         :body (as-html
                [:div#main.wrapper {:style "padding-top: 32px;"}
                 [:div.container
                  [:section {:style "margin-top: 32px; margin-left: 32px; margin-right: 32px;"}
                   (-> (markdown "../../../spire/doc/howto.md")
                       (enlive/at [:code.clojure] (enlive/add-class "language-clojure")
                                  [:pre] (enlive/set-attr "style" "border-radius:8px;margin-bottom:32px;")
                                  [:pre] (enlive/add-class "language-")
                                  [:code] (enlive/set-attr "style" "padding:0px;")
                                  [:h1] (enlive/add-class "title-heading")
                                  ))]]])})
