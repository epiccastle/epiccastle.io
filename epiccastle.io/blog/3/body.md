### Outputting XML with Bootleg

The latest release of [bootleg](https://github.com/retrogradeorbit/bootleg) (0.1.7) contains some new XML output functionality. I added this to bootleg to facilitate the generation of an RSS news feed form the epiccastle blog page that you are reading right now. Here is how I generate this RSS.

#### Background

In this blog I have post metadata stored in some short yaml files. I can gather all the relevant filenames in with the `glob` function:

```clojure
(for [filename (glob "*/vars.yml")]
  ...)
```

And then I can read these vars in with the `yaml` function:

```clojure
(let [vars (yaml filename)]
  ...)
```

I build all these yaml structures into a single hash map keyed by the post number.

```clojure
(let [posts
      (->> (for [filename (glob "*/vars.yml")]
             (let [post-num (-> filename
                                (string/split #"/")
                                first
                                parse-string)
                   vars (yaml filename)]
               [post-num vars]))
           (into {}))]
  ...)
```

#### Generating the RSS

I can now use this datastructure to generate an rss xml file. I do this by passing some hiccup to `(convert-to hiccup-data :xml)`

```clojure
(spit "feed.xml"
    (-> [:rss {:version "2.0"}
          [:channel
            [:title "Epiccastle Blog"]
            [:description "Epiccastle.io Updates"]
            [:link "https://epiccastle.io/blog/"]
            [:lastBuildDate now-string]
            [:pubDate now-string]
            [:ttl "1440"]
            (for [[n {:keys [snake-title
                             title
                             rss-date]
                      :as post-data}] (reverse (sort posts))]
              [:item
               [:title title]
               [:description (-> post-data
                                 render-post
                                 as-html
                                 escape-html)]
               [:link (str "https://epiccastle.io/blog/" snake-title)]
               [:gid snake-title]
               [:pubDate rss-date]])]]
          (convert-to :xml)))
```

#### Other helpful functions

I needed to escape the html markup to embed it in the xml description tag with this function:

```clojure
(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (-> text
      (string/replace "&" "&amp;")
      (string/replace "<" "&lt;")
      (string/replace ">" "&gt;")
      (string/replace "\"" "&quot;")))
```

And my `now-string` variable I generate with the following simple little time at the top of my file:

```clojure
(import [java.time OffsetDateTime]
        [java.time.format DateTimeFormatter])

(defn string->datetime [s]
  (OffsetDateTime/parse s DateTimeFormatter/RFC_1123_DATE_TIME))

(defn datetime->string [dt]
  (.format dt DateTimeFormatter/RFC_1123_DATE_TIME))

(def now (OffsetDateTime/now))
(def now-string (datetime->string now))

```

You can see the actual source for this process [here](https://github.com/epiccastle/epiccastle.io/blob/master/epiccastle.io/blog/index.clj).
