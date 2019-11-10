(def icon-layout
  [{:icon "fa-code-branch.png"
    :heading "Infrastructure as Code"
    :body "Capture the definition of your service infrastructure as source code. Bring development, staging and production into alignment. Enhance provenance and forensics."
    :class "4u 6u(medium) 12u$(xsmall)"}

   {:icon "fa-cogs.png"
    :heading "Continuous Integration"
    :body "Automatically run test suites upon code changes and inform the team immediately upon errors and regressions. Discover defects earlier when they are cheaper to fix and before your customers see them."
    :class "4u 6u$(medium) 12u$(xsmall)"}

   {:icon "fa-paper-plane.png"
    :heading "Continuous Deployment"
    :body "Increase the rate at which your software is delivered to your customers while providing safeguards and the ability to rollback."
    :class "4u$ 6u(medium) 12u$(xsmall)"}

   {:icon "fa-credit-card.png"
    :heading "Bill Reduction"
    :body "Are you paying more for your cloud usage than you need? Optimise your infrastructure and cut the excess services and costs."
    :class "4u 6u$(medium) 12u$(xsmall)"}

   {:icon "fa-cloud.png"
    :heading "Cloud Architecture"
    :body "Build a dynamic and scalable cloud architecture that guarantees uptime and gracefully handles change."
    :class "4u 6u(medium) 12u$(xsmall)"}

   {:icon "fa-bell.png"
    :heading "Monitoring and Alerting"
    :body "Instrument your application and monitor aspects of its performance you never thought possible. Build dashboards and live feedback for everyone in the organisation."
    :class "4u$ 6u$(medium) 12u$(xsmall)"}])

(def carousel-layout
  [{:image "aws.png"}
   {:width "80%"
    :margin-top "2em"
    :image "digitalocean.png"}
   {:image "elastic.png"}
   {:image "newrelic.png"}
   {:width "80%"
    :margin-top "2em"
    :image "prometheus.png"}
   {:width "80%"
    :margin-top "2em"
    :image "grafana.png"}
   {:width "50%"
    :margin-top "2.5em"
    :image "clojure.png"}
   {:width "60%"
    :margin-top "2em"
    :image "erlang.png"}
   {:width "40%"
    :margin-top "3em"
    :image "python.png"}
   {:width "80%"
    :margin-top "1.5em"
    :image "ansible.png"}
   {:width "70%"
    :margin-top "1.5em"
    :image "docker.png"}
   {:width "70%"
    :margin-top "1.5em"
    :image "nixos.png"}
   {:width "80%"
    :margin-top "1.7em"
    :image "rabbitmq.png"}
   {:width "90%"
    :margin-top "1.5em"
    :image "redis.png"}
   {:width "80%"
    :margin-top "1.5em"
    :image "concourse.png"}
   {:width "80%"
    :margin-top "1.5em"
    :image "postgres.png"}])

(selmer "templates/site.html"
        {:body
         (as-html
          [[:section#banner
            [:div.content
             [:header
              [:h2 "Building or scaling your enterprise?"]
              [:p "Reduce pain, free your time and grow" "\u00A0"
               [:br] "more quickly with smart devops."]
              [:ul.actions {:style "margin-top: 3em;"}
               [:li [:a.button {:href "/contact.html"} "Contact Us"]]]]]
            [:a.goto-next.scrolly {:href "#four"} "Next"]]

           [:section#four.wrapper.style1.special.fade-up
            [:div.container
             [:header.major
              [:h2 "Our Services"]]
             [:div.box.alt
              [:div.row.uniform

               (for [{:keys [icon heading body class]} icon-layout]
                 [:section {:class class}
                  [:span.icon.alt.major
                   [:img {:style "width:50%;vertical-align:middle;"
                          :src (str "/images/" icon)}]]
                  [:h3 heading]
                  [:p body]])]]

             [:footer.major
              [:ul.actions
               [:li [:a.button {:href "/contact.html"} "Contact"]]]]]]

           [:section#five.wrapper.style2.special.fade
            [:div.container
             [:header
              [:h2 "Our Toolkit"]
              [:p [:i "If I have seen further it is by standing on the shoulders of Giants."] " — Isaac Newton"]]
             [:div.crsl-items {:data-navigation "crsl-nav-tools"}
              [:div.crsl-wrap
               (for [{:keys [width margin-top image]} carousel-layout]
                 [:figure.crsl-item
                  [:img {:style (str "vertical-position:middle;width:" (or width "100%")
                                     ";margin-top:" (or margin-top "1em") ";")
                         :src (str "/images/logos/" image)}]])]]]]])})
