(load-file "../lib.clj")
(refer 'lib)
(require '[clojure.string :as string])

(defn trim-leading-trailing [s]
  (-> s
      (string/replace #"^\s+" "")
      (string/replace #"\s+$" "")))

(def module-ref
  [:div
   (let [modules
         (->>
          (glob "../../../spire/src/clj/spire/module/*.clj")
          (map (fn [fname]
                 [fname (->> (parse-string (slurp fname) {:all true})
                             (filter #(and (seq? %)
                                           (= 'def (first %))
                                           (= 'documentation (second %))))
                             (map #(nth % 2))
                             first)]))
          (into {}))]

     (mkdir "module")
     (doall
      (for [[filename doc] (sort-by first modules)]
       (let [{:keys [module blurb description opts examples form args]} doc
             fname (format "module/%s.html" module)
             module-doc
             [:section {:style {:margin-top "48px"
                                :margin-left "16px"
                                :margin-right "16px"}}
              [:a {:href "../"} "back"]
              [:div.container {:style {:margin-top "16px"
                                       :margin-left "16px"
                                       :margin-right "16px"}}
               [:h2 module]
               [:p blurb]
               [:h3 "Overview"]
               [:ul
                (for [desc description]
                  [:li
                   (enlive-at
                    (vec (markdown desc :data))
                    [:p]
                    (enlive-set-attr
                     :style {:margin-bottom "0px"}))])]
               [:h3 "Form"]
               [:pre.language-clojure
                [:code.language-clojure
                 form]]
               [:h3 "Arguments"]
                [:div
                 (for [{:keys [arg desc values]} args]
                   [:table
                    [:pre [:b arg]]
                    [:span (vec (markdown desc :data))]
                    (for [[val desc] values]
                      [:div {:style {:margin-bottom "4px"}}
                       [:code (str val)]
                       (enlive-at
                        (vec (markdown desc :data))
                        [:p]
                        (enlive-set-attr :style {:display :inline
                                                 :margin-left "16px"}))])])]
               [:h3 "Options"]
               [:table
                [:thead [:th "Option"] [:th "Description"]]
                [:tbody
                 (for [[option {:keys [description type aliases required]}] opts]
                   [:tr
                    [:td
                     [:div [:code (str option)]
                      [:div {:style {:margin-top "18px"}}
                       (when required
                         [:div {:style {:font-size "12px"}}
                          [:i {:style {:color "#606060"}} "required "]
                          [:code {:style {:font-size "10px"}} "true"]])
                       (when aliases
                         [:div {:style {:font-size "12px"}}
                          [:i {:style {:color "#606060"}} "aliases "]
                          (for [alias aliases]
                            [:code {:style {:font-size "10px"}} (str alias)])])
                       (when type
                         [:div {:style {:font-size "12px"}}
                          [:i {:style {:color "#606060"}} "type "]
                          (if (sequential? type)
                            (for [t type] [:code {:style {:font-size "10px"}} (name t)])
                            [:code {:style {:font-size "10px"}} (name type)])])]]]
                    [:td
                     [:ul
                      (for [desc description]
                        [:div
                         (enlive-at
                          (vec (markdown desc :data))
                          [:p] (enlive-set-attr
                                :style {:margin-bottom "4px"}))])]]])]]
               [:h3 {:style {:margin-top "16px"}} "Examples"]
               (for [{:keys [description form]} examples]
                 [:div {:style {:margin-top "16px"}}
                  [:h4 description]
                  [:pre.language-clojure
                   [:code.language-clojure (trim-leading-trailing form)]]])]]]

         ;; write out specific module page
         (spit
          fname
          (selmer "../templates/site.html"
                  {:title module
                   :extra-scripts
                   ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-core.min.js"
                    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js"]
                   :extra-styles
                   ["https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css"]
                   :body (as-html
                          (enlive-at
                           module-doc
                           [:td] (enlive-set-attr :style {:padding "8px"})
                           [:ul] (enlive-set-attr :style {:margin "0px"})
                           [:pre] (enlive-set-attr :style {:border-radius "16px"})))}))
         [:div
          [:b [:a {:href fname} module]]
          [:p blurb]]))))])

(output!
 (selmer "../templates/site.html"
         {:title "Module Reference"
          :body (as-html
                 [:div#main.wrapper {:style "padding-top: 32px;"}
                  [:div.container
                   [:section

                    [:h2 "Module Reference"]
                    module-ref]]])}))
