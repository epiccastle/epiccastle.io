### Update

"load/recur" is _not_ a solution. It just fails far less often. Extensive testing shows it failing about 4% of the time (as opposed to 50% of the time with doseq). The problem is not related to doseq... but lies deeper. It is possibly a timing issue. Another blog post will be forthcoming when the root cause is found.

### Combining doseq and loadLibrary Considered Harmful

Loading multiple C libraries with loadLibrary inside a `doseq` will lead to lots of strange crashes and issues.

### Qt and javacpp

I have been building a [test stack using clojure, javacpp and Qt](https://github.com/retrogradeorbit/mullion) to build a clojure GUI and compile it into a single binary with GraalVM. All was going well until I started to experience strange problems as the code grew. Strangely, the compiled Graal native image continued to function perfectly, but various problems started to occur when running under the JVM. What was most concerning is sometimes it would work fine, and sometimes it would not.

When running under the JVM I would get one of the following problems when running the program.

- Program prints `QWidget: Must construct a QApplication before a QWidget` and exits.
- Program prints `QPixmap: Must construct a QGuiApplication before a QPixmap` and exits.
- Program prints `QApplication::exec: Please instantiate the QApplication object first` and exits.
- Program prints `QGuiApplication::font(): no QGuiApplication instance and no application font set.` multiple times and then exits.
- The program runs fine, but then crashes when closing with a `SIGSEGV`
- The program segfaults and crashes with `A fatal error has been detected by the Java Runtime Environment:SIGSEGV` and one of the following:
     * `# C  [libQt5Widgets.so.5+0x19c075]  QWidget::show()+0x15`, or...
     * `# C  0x0000000000000000`

Or... it runs fine! The [mandelbug](http://catb.org/jargon/html/M/mandelbug.html) nature of this makes it very strange indeed.

### Introducing Delays Works Around the Problem

The error messages mentioning constructing one thing before another start to pique my curiosity. Why is that error appearing when I quite clearly create the QApplication first? What if I add a `(Thread/sleep 1000)` after the creation of the QApplication?

Funnily enough, that stops the errors and crashes and the program now works reliably. Then after a while I just try sleeps in different locations. I move the sleep higher and higher up the code and it continues to work. I move it _before_ the creation of QApplication, and it works! And then I move it before the loading of the Qt libraries, and it no longer works!

It seems to be related to the way I am loading libraries. Maybe when the routine to load the libraries returns, the libraries aren't actually fully loaded? Maybe they are being loaded in another thread? Clojure, after all, is a highly parallel language.

### The Problematic Loader

Here is the loader as it was:

```clojure
(defn load-libs []
  (doseq [name library-load-list]
    (clojure.lang.RT/loadLibrary name)))
```

Seems innocuous enough. But what if `doseq` is actually multi-threaded and loading from multiple threads is causing issues? Lets try replacing it with something single threaded.

### The Replacement Loader

I remove the debug sleeps and replaced the loader implementation with this:

```clojure
(defn load-libs []
  (loop [[name & remain] library-load-list]
    (when name
      (clojure.lang.RT/loadLibrary name)
      (recur remain))))
```

And the problem instantly disappears. Launch after launch the program works perfectly.

In this working implementation, each `loadLibrary` call is issued and the program waits for the call to complete before loading the next shared library.

### The Culprit?

Looking at the source of `doseq` [here](https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L3216) we see that doseq is not inherently multi-threaded, but it does contain code to support chunked sequences. I don't have time to unravel exactly what `doseq` is doing here that is causing the crisis, but let it be known: **keep your loadLibrary calling code single threaded**
