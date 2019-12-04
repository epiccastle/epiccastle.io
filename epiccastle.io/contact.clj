(selmer "templates/site.html"
        {:title "Contact Us"
         :body
         (as-html
          [:div#main.wrapper.style1
           [:div.container
            [:header.major
             [:section
              [:h2 "Contact Us"]

              [:div#mask-overlay {:style {:width "100%"
                                          :height "400px"
                                          :position "absolute"
                                          :pointer-events "none"
                                          :background-color "#1C1D26"
                                          :opacity "0"
                                          :transition "opacity .3s ease-in-out"}}
               [:div.center {:style {:padding-top "9em"}}
                [:h3#result "Message Sent!"]
                [:h4#result-sub "Contact us "
                 [:a {:href "mailto:contact@epiccastle.io"
                      :style {:color "#D59563"
                              :text-decoration "underline"}}
                  "manually!"]]]]

              [:div#contact-us-form {:style {:transition "opacity 0.3s ease-in-out" }}
               [:form
                [:div.row.uniform.50%
                 [:div {:class "6u 12u$(xsmall)"}
                  [:input {:type "text"
                           :name "name"
                           :id "name"
                           :value ""
                           :placeholder "Name"}]]
                 [:div {:class "6u$ 12u$(xsmall)"}
                  [:input {:type "email"
                           :name "email"
                           :id "email"
                           :value ""
                           :placeholder "Email or Phone Number"}]]
                 [:div {:class "12u$"}
                  [:textarea {:name "message"
                              :id "message"
                              :placeholder "Enter your message"
                              :rows "6"}]]
                 [:div {:class "12u$"}
                  [:ul.actions
                   [:li [:input {:type "button"
                                 :value "Send Message"
                                 :class "special"
                                 :id "contact-submit"
                                 :onclick "contact_us();"}]]
                   [:li [:input {:type "reset"
                                 :value "Reset"}]]]]]]]]]]])})
