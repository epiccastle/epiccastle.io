(require '[clojure.string :as string])

(selmer "../templates/site.html"
        {:title "Spire: Tutorial"
         :extra-scripts
         ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-core.min.js"
          "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/autoloader/prism-autoloader.min.js"]
         :extra-styles
         ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/themes/prism-tomorrow.min.css"]
         :body (as-html
                [:div#main.wrapper
                 [:div.container
                  [:section
                   (-> (markdown "../../../spire/doc/tutorial.md")
                       (enlive/at [:code.clojure] (enlive/add-class "language-clojure")
                                  [:code.jinja2] (enlive/add-class "language-jinja2")
                                  [:code.shell-session] (enlive/add-class "language-shell-session")
                                  [:code.handlebars] (enlive/add-class "language-handlebars")
                                  [:pre] (enlive/set-attr "style" "border-radius:8px;margin-bottom:32px;")
                                  [:pre] (enlive/add-class "language-")
                                  [:code] (enlive/set-attr "style" "padding:0px;")
                                  [:h1] (enlive/add-class "title-heading")
                                  ))]]])})
