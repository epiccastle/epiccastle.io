### Slash GraalVM Clojure Compilation Time With This One Weird Trick

Are you building a clojure project and compiling it to a native binary with GraalVM? As the project has grown, has the GraalVM compilation time gotten longer and longer? Here is the story of how I discovered [@borkdude](https://twitter.com/borkdude)'s trick to getting GraalVM native image times in under 5 minutes.

#### The Story

As I've been building [spire](https://epiccastle.io/spire/), compilation times have gradually been increasing. Then in order to bring a bunch of increased functionality I added a large number of Java class bindings to my sci environment. I check the code in and go to bed. When I wake I discover my [Circle CI builds](https://circleci.com/gh/epiccastle/spire/1055) have been killed in the night for taking too long. The build was underway for an hour when the CI system killed the build!

So I try compiling on my local linux workstation to see what is going on. After an eternity the build crashes with:

```shell
[build/spire:2202]     analysis: 12,784,610.52 ms,  6.22 GB
Fatal error:java.lang.OutOfMemoryError: GC overhead limit exceeded
Error: Image build request failed with exit status 1
com.oracle.svm.driver.NativeImage$NativeImageError: Image build request failed with exit status 1
    at com.oracle.svm.driver.NativeImage.showError(NativeImage.java:1527)
    at com.oracle.svm.driver.NativeImage.build(NativeImage.java:1289)
    at com.oracle.svm.driver.NativeImage.performBuild(NativeImage.java:1250)
    at com.oracle.svm.driver.NativeImage.main(NativeImage.java:1209)
    at com.oracle.svm.driver.NativeImage$JDK9Plus.main(NativeImage.java:1707)

```

**12,784,610ms** spent in analysis. That is over **three and a half hours**!

As I just added a bunch of new class bindings and reflection config for them all, I assume that must be the culprit. I remove 75% of them. And build again. I time this run. It succeeds, but it isn't pretty.

```shell
[build/spire:11341]    classlist:   5,953.42 ms,  0.94 GB
[build/spire:11341]        (cap):     800.78 ms,  0.94 GB
[build/spire:11341]        setup:   2,536.63 ms,  0.94 GB
[build/spire:11341]   (typeflow): 1,544,374.55 ms,  6.51 GB
[build/spire:11341]    (objects): 5,164,930.94 ms,  6.51 GB
[build/spire:11341]   (features):   4,320.62 ms,  6.51 GB
[build/spire:11341]     analysis: 6,724,448.94 ms,  6.51 GB
[build/spire:11341]     (clinit):   1,898.20 ms,  6.51 GB
[build/spire:11341]     universe:  47,567.23 ms,  6.51 GB
[build/spire:11341]      (parse):  12,451.68 ms,  4.17 GB
[build/spire:11341]     (inline):   9,098.24 ms,  3.94 GB
[build/spire:11341]    (compile):  75,592.43 ms,  3.59 GB
[build/spire:11341]      compile: 102,870.54 ms,  3.59 GB
[build/spire:11341]        image:  14,383.80 ms,  3.61 GB
[build/spire:11341]        write:   1,725.39 ms,  3.61 GB
[build/spire:11341]      [total]: 6,899,951.34 ms,  3.61 GB
cp build/spire spire

real	115m59.386s
user	839m24.587s
sys	1m56.493s

```

Wow! Analysis is not down to only 1 hour and 50 minutes! Good times!

So this leads me to wonder how Michiel ([@borkdude](https://twitter.com/borkdude)) is building his babashka images on Circle CI without hitting the wall. I go over to have a look at his [Circle CI builds](https://circleci.com/gh/borkdude/babashka/9392). They are building his images in **less that 5 minutes**.... whaaaaaaat?

#### That One Weird Trick

This leads me to a bunch of comparisons between the compilation and building of babashka with that of spire. I rule out reflection as the source of the problem. I rule out GraalVM options. And then I discover [the magical incantation](https://github.com/borkdude/babashka/blob/c3f9480efe08827dfa4ac0fb21f7376d80287ce6/project.clj#L53).

I had this setting on my GraalVM build, but I did not have it on when clojure was doing AOT compilation for the uberjar. Before we turn it on lets look into what it does.

#### Direct Linking

Clojure's direct linking can be activated by passing `-Dclojure.compiler.direct-linking=true` to the compiler. This feature is [documented here](https://clojure.org/reference/compilation#directlinking). From this discussion we read:

> "Normally, invoking a function will cause a var to be dereferenced to find the function instance implementing it, then invoking that function... *Direct linking* can be used to replace this indirection with a direct static invocation of the function instead. This will result in faster var invocation. Additionally, the compiler can remove unused vars from class initialization and direct linking will make many more vars unused. Typically this results in smaller class sizes and faster startup times."

And faster Graal compilation times to boot! This option will produce JVM byte code that will be much more like what a standard Java program will produce. Java is a statically typed language after all, and Java programs are not dereferencing vars every time they are invoked. The Graal compiler, being built primarily to compile Java programmes, is obviously having a very hard time with the dynamic nature of clojure's compiled byte code.

But do we lose anything if we compile our code with direct linking? According to the docs:

> "One consequence of direct linking is that var redefinitions will not be seen by code that has been compiled with direct linking (because direct linking avoids dereferencing the var). Vars marked as ^:dynamic will never be direct linked. If you wish to mark a var as supporting redefinition (but not dynamic), mark it with ^:redef to avoid direct linking."

As Michiel pointed out to me, things like a general use of `with-redefs` won't work with direct linking. But if we do want to do something dynamic like `with-redefs` in our code, we can individually mark those vars with `^:redef` meta data to allow them to work. Also, things like `with-redefs` is more commonly used in writing tests, so we can keep the option off in our test code and save direct linking for our uberjar builds.

#### The Fixed Build

Now taking the original problematic build that crashed after three and a half hours, I switch that setting on in my uberjar compilation and rebuild. Here's the result:

```shell
[build/spire:22871]    classlist:   4,379.73 ms,  0.96 GB
[build/spire:22871]        (cap):     739.03 ms,  0.96 GB
[build/spire:22871]        setup:   2,268.45 ms,  0.96 GB
[build/spire:22871]   (typeflow):  50,731.79 ms,  5.92 GB
[build/spire:22871]    (objects): 116,840.36 ms,  5.92 GB
[build/spire:22871]   (features):   2,682.61 ms,  5.92 GB
[build/spire:22871]     analysis: 173,423.16 ms,  5.92 GB
[build/spire:22871]     (clinit):   1,193.08 ms,  5.92 GB
[build/spire:22871]     universe:   3,049.85 ms,  5.92 GB
[build/spire:22871]      (parse):   7,224.44 ms,  5.79 GB
[build/spire:22871]     (inline):  13,408.30 ms,  4.19 GB
[build/spire:22871]    (compile):  77,773.85 ms,  4.16 GB
[build/spire:22871]      compile: 104,068.58 ms,  4.16 GB
[build/spire:22871]        image:  13,542.17 ms,  4.16 GB
[build/spire:22871]        write:   1,792.26 ms,  4.22 GB
[build/spire:22871]      [total]: 302,882.67 ms,  4.22 GB
cp build/spire spire

real	5m32.050s
user	33m7.865s
sys	0m12.756s

```

Down from 3½ hours... to a little under **3 minutes**. And amazing improvement!

#### Summary

Add `-Dclojure.compiler.direct-linking=true` to your clojure compilation JVM options when building your uberjar and when compiling with GraalVM.

In lein:

```clojure
(defproject foo "0.1.0-SNAPSHOT"

  ;; missing lines

  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
)
```

And in GraalVM native-image:

```shell
graalvm-ce-java11-20.1.0-dev/bin/native-image \
    ...
    -J-Dclojure.compiler.direct-linking=true \
    ...
```
