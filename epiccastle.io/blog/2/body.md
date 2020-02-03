### Modifying Markdown Document Links

I have some markdown in a project on github. And it contains relative links. These relative link take you to other markdown files in the github repo. When you click the links on github the browser navigates to `doc/tutorial.md` and shows it in the github styling. All good.

Now I wan't to render these markdown documents onto my own server, with my own styling, for the project's home page. This site is comprised of static files generated with [bootleg](https://github.com/retrogradeorbit/bootleg). All the links in the documents look like: `doc/tutorial.md`. But I need them to look like this: `tutorial.html`. I need an a-tag-url-modifier. One takes in the `*.md` href string, and transforms it to the equivalent `*.html` string.

What would be even more useful is a transformer that can modify _any_ part of the element, like clojure's `assoc-in` or an `update-in`.

It's very simple. Here is the implementation of an update-in transformer from my bootleg file:

```clojure
(defn el-update-in [path func & func-args]
  (fn [el]
    (apply update-in el path func func-args)))
```

I could append a html file extension to all the `<a>` tags with:

```clojure
[:section
 [:div.container
  (-> (markdown "body.md")
      (enlive/at [:a] (el-update-in [:attrs :href] str ".html")))]]
```

The `el-update-in` function returns the transformer. The transformer operates on a hickory element. These elements have the keys `:type`, `:tag`, `:attrs` and `:content`. Here I run the `str` function on the contents of the path `[:attrs :href]`. You could access any attr this way, like `[:attrs :class]` or even run on sub elements, with something like `[:content 0]`!

Here is my solution to my path transformation problem.

```clojure
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
```

I split the string up by `/` and take the last segment. Then hackishly remove the file extension. Then add the html extension. This turns `doc/tutorial.md` to `tutorial.html`.
