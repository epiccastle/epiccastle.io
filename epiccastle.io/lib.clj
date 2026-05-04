(ns lib
  "Babashka compatibility library replacing bootleg built-in functions.
   Provides: as-html, markdown, selmer, yaml, symlink, mkdir, glob,
   convert-to, enlive/at, enlive/add-class, enlive/set-attr, and more."
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [babashka.fs :as fs]
            [clj-yaml.core :as clj-yaml]
            [hiccup2.core :as hiccup2]
            [selmer.parser :as selmer-parser]
            [nextjournal.markdown :as md]
            [edamame.core :as edamame]
            [clojure.data.xml :as xml]))

;; ---------------------------------------------------------------------------
;; Style map serialization (bootleg supported {:style {:color "red"}} in hiccup)
;; ---------------------------------------------------------------------------

(defn style-map->str
  "Convert a map of CSS properties to a CSS string.
   Keywords are converted to names: :margin-top -> \"margin-top\"."
  [m]
  (if (map? m)
    (->> m
         (map (fn [[k v]]
                (str (name k) ":" (if (keyword? v) (name v) v))))
         (string/join ";"))
    (str m)))

;; ---------------------------------------------------------------------------
;; Hiccup rendering (replacement for bootleg's `as-html`)
;; ---------------------------------------------------------------------------

(defn normalize-hiccup
  "Walk hiccup tree, converting style maps to strings and flattening
   embedded sequences/vectors-of-vectors so hiccup2 can render them.
   
   Bootleg allowed nested seqs and vectors-of-vectors inside hiccup;
   hiccup2 requires children to be either hiccup vectors, strings, or
   flat sequences of the same."
  [form]
  (cond
    (string? form) form
    (number? form) form
    (nil? form) nil
    (keyword? form) form

    ;; A hiccup element: [:tag {attrs} ...children]
    (and (vector? form) (seq form) (keyword? (first form)))
    (let [tag (first form)
          has-attrs? (and (> (count form) 1) (map? (second form)))
          attrs (if has-attrs? (second form) nil)
          children (if has-attrs? (drop 2 form) (rest form))
          ;; Normalize style maps in attrs
          attrs (if (and attrs (map? (:style attrs)))
                  (assoc attrs :style (style-map->str (:style attrs)))
                  attrs)
          ;; Flatten and normalize children
          norm-children (mapcat (fn [child]
                                  (cond
                                    ;; vector of vectors -> treat as multiple hiccup forms
                                    (and (vector? child) (seq child) (vector? (first child)))
                                    (map normalize-hiccup child)
                                    ;; lazy seq of children
                                    (seq? child)
                                    (map normalize-hiccup child)
                                    :else
                                    [(normalize-hiccup child)]))
                                children)]
      (if attrs
        (into [tag attrs] norm-children)
        (into [tag] norm-children)))

    ;; A vector of vectors (hiccup-seq at top level)
    (and (vector? form) (seq form) (vector? (first form)))
    (mapv normalize-hiccup form)

    ;; A seq
    (seq? form)
    (map normalize-hiccup form)

    ;; Attribute map at top level (shouldn't happen, but pass through)
    (map? form)
    (if (and (contains? form :style) (map? (:style form)))
      (assoc form :style (style-map->str (:style form)))
      form)

    :else form))

(defn render-hiccup
  "Render a single hiccup form to an HTML string."
  [form]
  (str (hiccup2/html (normalize-hiccup form))))

(defn as-html
  "Convert hiccup data to an HTML string.
   Accepts a single vector (hiccup form) or a seq/vector of forms.
   If the argument is a vector whose first element is also a vector,
   treat it as a sequence of forms (bootleg's as-html behaviour)."
  [data]
  (cond
    (string? data) data
    (nil? data) ""
    (and (vector? data) (seq data) (vector? (first data)))
    (apply str (map render-hiccup data))
    (vector? data)
    (render-hiccup data)
    (seq? data)
    (apply str (map render-hiccup data))
    :else (str data)))

;; ---------------------------------------------------------------------------
;; Markdown (replacement for bootleg's `markdown`)
;; ---------------------------------------------------------------------------

;; Custom hiccup renderers for nextjournal.markdown.
;; The default renderers don't handle :html-inline and :html-block
;; (raw HTML embedded in markdown). We use hiccup2's `raw` to splat the
;; literal HTML text into the output without escaping.
(def ^:private markdown-renderers
  (assoc md/default-hiccup-renderers
         :html-inline (fn [_ctx node] (hiccup2/raw (md/node->text node)))
         :html-block  (fn [_ctx node] (hiccup2/raw (md/node->text node)))
         :html        (fn [_ctx node] (hiccup2/raw (md/node->text node)))))

(defn markdown
  "Parse a markdown file (or string with :data flag) and return hiccup.
   Usage:
     (markdown \"file.md\")        -> seq of hiccup child elements
     (markdown \"# hi\" :data)     -> seq of hiccup child elements"
  ([source]
   (markdown source :file))
  ([source mode]
   (let [text (if (= mode :data) source (slurp source))
         hiccup (md/->hiccup markdown-renderers (md/parse text))]
     ;; nextjournal.markdown returns [:div ...children...]
     ;; bootleg returned a hiccup-seq (list of top-level elements)
     ;; We return the children (dropping the outer :div wrapper)
     (if (and (vector? hiccup) (= :div (first hiccup)))
       (rest hiccup)
       (list hiccup)))))

;; ---------------------------------------------------------------------------
;; YAML (replacement for bootleg's `yaml`)
;; ---------------------------------------------------------------------------

(defn yaml
  "Parse a YAML file and return a Clojure map with keyword keys."
  [filename]
  (clj-yaml/parse-string (slurp filename)))

;; ---------------------------------------------------------------------------
;; Selmer templating (replacement for bootleg's `selmer`)
;; ---------------------------------------------------------------------------

(defn selmer
  "Render a Selmer template file with the given variables.
   Returns an HTML string (matching bootleg's default output behaviour)."
  [template-path vars]
  (selmer-parser/render (slurp template-path) vars))

;; ---------------------------------------------------------------------------
;; File system utilities
;; ---------------------------------------------------------------------------

(defn symlink
  "Create a symbolic link, idempotent (like bootleg's symlink).
   Creates link-name pointing to target."
  [link-name target]
  (let [link-path (fs/path link-name)]
    (when-not (and (fs/sym-link? link-path)
                   (= (str (fs/read-link link-path)) (str target)))
      (when (fs/exists? link-path {:nofollow-links true})
        (fs/delete link-path))
      (fs/create-sym-link link-path target))))

(defn mkdir
  "Create a directory (idempotent)."
  [path]
  (fs/create-dirs path))

(defn glob
  "Glob for files matching a pattern.
   Bootleg's `glob` accepted a single pattern that could include directory
   components (including `..`). babashka.fs/glob requires a separate root-dir
   and does not support `..` inside the glob pattern itself.
   
   When the pattern starts with `..` or `/`, we split off the leading literal
   path components (everything up to the first wildcard) and use that as the
   root-dir. Otherwise we use `.` as the root and pass the pattern through."
  [pattern]
  (if (or (.startsWith ^String pattern "..")
          (.startsWith ^String pattern "/"))
    ;; Split into literal-prefix + glob-suffix at the first path segment
    ;; that contains a wildcard.
    (let [segs (clojure.string/split pattern #"/")
          [literal globby] (split-with #(not (re-find #"[*?\[\]{}]" %)) segs)
          root (if (seq literal) (clojure.string/join "/" literal) ".")
          pat  (clojure.string/join "/" globby)]
      (if (empty? pat)
        ;; Pattern was entirely literal - just check existence
        (if (fs/exists? root) [root] [])
        (map str (fs/glob root pat))))
    (map str (fs/glob "." pattern))))

;; ---------------------------------------------------------------------------
;; Parse string (replacement for bootleg's `parse-string`)
;; ---------------------------------------------------------------------------

(def ^:private edamame-opts
  "Options for edamame parsers used to read arbitrary Clojure source files."
  {:auto-resolve {:current 'user}
   :regex true
   :fn true
   :quote true
   :syntax-quote true
   :deref true
   :read-cond :allow
   :readers (fn [_tag] (fn [val] val))
   :features #{:clj}})

(defn parse-string
  "Parse a string as EDN/Clojure data. With {:all true}, parses all
   top-level forms and returns a sequence (used by spire/modules.clj)."
  ([s]
   (edamame/parse-string s edamame-opts))
  ([s opts]
   (if (:all opts)
     (edamame/parse-string-all s edamame-opts)
     (edamame/parse-string s (merge edamame-opts opts)))))

;; ---------------------------------------------------------------------------
;; Enlive-like hiccup transformations
;; ---------------------------------------------------------------------------
;;
;; Bootleg's enlive/at operates on hiccup data with CSS-like selectors.
;; We implement a subset that covers all selectors used in this project:
;;   - [:tag]          matches by tag name
;;   - [:tag.class]    matches tag with CSS class
;;   - [:.class]       matches any tag with CSS class (not used here, but supported)
;;
;; Transformations supported:
;;   - (enlive/add-class "cls1" "cls2")
;;   - (enlive/set-attr "key" "val")  or  (enlive/set-attr :key val)
;;

(defn- parse-selector
  "Parse a keyword selector like :img.blog-splash into {:tag :img :classes #{\"blog-splash\"}}"
  [sel]
  (let [s (name sel)
        parts (string/split s #"\." -1)
        tag (when (and (first parts) (not (empty? (first parts))))
              (keyword (first parts)))
        classes (set (rest parts))]
    {:tag tag :classes classes}))

(defn- get-attrs
  "Get the attribute map from a hiccup element, or nil."
  [el]
  (when (and (vector? el) (> (count el) 1) (map? (second el)))
    (second el)))

(defn- get-tag-and-classes
  "Extract the tag keyword and set of CSS classes from a hiccup element.
   Handles [:tag.class1.class2 ...] syntax."
  [el]
  (when (and (vector? el) (keyword? (first el)))
    (let [s (name (first el))
          ;; Also split on # for id syntax
          tag-part (first (string/split s #"[.#]" 2))
          ;; Extract classes from tag keyword itself (e.g. :div.foo.bar)
          inline-classes (let [parts (string/split s #"\." -1)]
                           (set (rest parts)))
          ;; Also get classes from :class attr
          attr-class (when-let [attrs (get-attrs el)]
                       (let [c (:class attrs)]
                         (if (string? c)
                           (set (string/split c #"\s+"))
                           #{})))
          all-classes (into (or inline-classes #{}) (or attr-class #{}))]
      {:tag (keyword tag-part)
       :classes all-classes})))

(defn- matches-selector?
  "Check if a hiccup element matches a parsed selector."
  [parsed-sel el]
  (when-let [{:keys [tag classes]} (get-tag-and-classes el)]
    (and (or (nil? (:tag parsed-sel)) (= (:tag parsed-sel) tag))
         (every? #(contains? classes %) (:classes parsed-sel)))))

(defn- ensure-attrs
  "Ensure a hiccup vector has an attribute map in position 1."
  [el]
  (if (and (> (count el) 1) (map? (second el)))
    el
    (into [(first el) {}] (rest el))))

(defn enlive-add-class
  "Return a transformation fn that adds CSS classes to matching elements."
  [& classes]
  (fn [el]
    (let [el (ensure-attrs el)
          attrs (second el)
          existing (or (:class attrs) "")
          existing-set (if (empty? existing) #{} (set (string/split existing #"\s+")))
          new-set (into existing-set classes)
          new-class (string/join " " (sort new-set))]
      (assoc el 1 (assoc attrs :class new-class)))))

(defn enlive-set-attr
  "Return a transformation fn that sets attributes on matching elements.
   Accepts key-value pairs: (enlive-set-attr \"style\" \"color:red\")
   or (enlive-set-attr :style {:padding \"8px\"})"
  [& kvs]
  (fn [el]
    (let [el (ensure-attrs el)
          attrs (second el)
          pairs (partition 2 kvs)
          new-attrs (reduce (fn [a [k v]]
                              (let [k (if (string? k) (keyword k) k)
                                    v (if (and (= k :style) (map? v))
                                        (style-map->str v)
                                        v)]
                                (assoc a k v)))
                            attrs pairs)]
      (assoc el 1 new-attrs))))

(defn- apply-transform
  "Apply a transformation function to a hiccup element."
  [el transform-fn]
  (if transform-fn
    (transform-fn el)
    el))

(defn enlive-at
  "Apply enlive-like selector+transformation pairs to hiccup data.
   Usage: (enlive-at data [:img] (enlive-add-class \"fit\")
                          [:pre] (enlive-set-attr \"style\" \"...\"))
   
   data can be a single hiccup vector, a seq of hiccup forms, or a vector
   of hiccup forms (i.e. [[:div ...] [:p ...]])."
  [data & selector-transform-pairs]
  (let [pairs (partition 2 selector-transform-pairs)
        parsed-pairs (mapv (fn [[sel transform-fn]]
                             [(parse-selector (first sel)) transform-fn])
                           pairs)
        transform-element (fn transform-element [el]
                            (if (and (vector? el) (keyword? (first el)))
                              ;; It's a hiccup element - check selectors
                              (let [transformed
                                    (reduce (fn [e [parsed-sel transform-fn]]
                                              (if (matches-selector? parsed-sel e)
                                                (apply-transform e transform-fn)
                                                e))
                                            el parsed-pairs)
                                    ;; Recursively transform children
                                    transformed (ensure-attrs transformed)
                                    tag (first transformed)
                                    attrs (second transformed)
                                    children (drop 2 transformed)
                                    new-children (mapv (fn [child]
                                                         (cond
                                                           (and (vector? child) (keyword? (first child)))
                                                           (transform-element child)
                                                           (seq? child)
                                                           (map transform-element child)
                                                           (and (vector? child) (seq child) (vector? (first child)))
                                                           (mapv transform-element child)
                                                           :else child))
                                                       children)]
                                (into [tag attrs] new-children))
                              el))]
    (cond
      ;; seq of hiccup forms
      (seq? data)
      (map transform-element data)
      ;; vector of hiccup vectors (hiccup-seq style)
      (and (vector? data) (seq data) (vector? (first data)))
      (mapv transform-element data)
      ;; single hiccup form
      (and (vector? data) (keyword? (first data)))
      (transform-element data)
      :else data)))

;; ---------------------------------------------------------------------------
;; convert-to (subset used in this project)
;; ---------------------------------------------------------------------------

(defn- hiccup->xml-element
  "Convert a hiccup form to a clojure.data.xml Element for XML output.
   Special-cases the [:-cdata \"...\"] form to emit a real CDATA section."
  [form]
  (cond
    (string? form)
    form

    (and (vector? form) (keyword? (first form)))
    (let [tag (first form)]
      (if (= :-cdata tag)
        ;; CDATA section: concatenate any string children verbatim
        (xml/cdata (apply str (filter string? (rest form))))
        (let [has-attrs? (and (> (count form) 1) (map? (second form)))
              attrs (if has-attrs? (second form) {})
              children (if has-attrs? (drop 2 form) (rest form))
              flat-children (mapcat (fn [c]
                                      (if (seq? c)
                                        (map hiccup->xml-element c)
                                        [(hiccup->xml-element c)]))
                                    children)]
          (apply xml/element tag attrs flat-children))))

    (number? form)
    (str form)

    :else
    (str form)))

(defn convert-to
  "Convert data between formats. Supports:
   - :html  - hiccup seq -> HTML string
   - :xml   - hiccup -> XML string
   - :hiccup-seq - HTML string -> seq of hiccup forms (via basic parsing)"
  [data format]
  (case format
    :html (as-html data)
    :xml (xml/emit-str (hiccup->xml-element data))
    :hiccup-seq data ;; if data is already hiccup, just return it
    (throw (ex-info (str "Unsupported convert-to format: " format) {:format format}))))

;; ---------------------------------------------------------------------------
;; Output helper - scripts print their result to stdout for Makefile capture
;; ---------------------------------------------------------------------------

(defn output!
  "Print the result string to stdout (no trailing newline).
   This replaces bootleg's implicit output-to-file behaviour."
  [s]
  (print s)
  (flush))
