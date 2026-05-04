The Spire code base is in a bit of a state. It was never the cleanest code but the addition of babashka pod support has increased its complexity a lot. There are multiple code paths, duplicated (but slightly different) namespaces, clojure code inside strings and namespace inspecting macros that test your ability to reason.

After the podification it got to the point that I couldn't bear to work on it anymore. I felt burnt out. Adding things or fixing bugs became a daunting prospect. I did not know how to make it right. So I ran away and hid.

This break from the project gave me some perspective. Slowly I realised what needed to be done. This post is to layout the future direction of spire development.

I plan for one more release of spire as it presently stands to set free the latest bug-fixes and improvements. Then I will undertake a large development effort to rebuild it from the ground up. At the end of this work it should work much as it does now. Perhaps it will do even more. But it will certainly be a much more stable foundation on which to build more features and it will be composed of parts that can be reused in different places and contexts.

#### Decomplect the Parts

The code base complects all of the things into one mess. It's nice that it's one binary but we can keep that and also have the separate components that can be used to make other nice things. Not everyone who wants SSH wants idempotent scripts. Not everyone wanting idempotent scripts wants them executed over SSH. Some people want its functionality in the JVM, some in babashka. Perhaps even in future it might run on a JavaScript platform.

#### Just the SSH bits.

I think the SSH support inside spire is very complete. I initially used clj-ssh but found bugs and unimplemented features. I found scp support in particular was buggy. These things all work in spire but you cannot use them outside of it. The SSH functionality should be available on its own.

So the first step is to break out the low level SSH support into a library. Because of how different writing a pod and writing a clojure library are, there will be two projects. These two libraries will strive to be as similar in API design as I can make them.

##### bbssh

This will be a babashka pod providing SSH functionality. It will *only* provide SSH functionality. I will try and expose as much of JSch in pod form as I can. I have begun this work. It is [underway here](https://github.com/epiccastle/bbssh).

##### clojuressh

This will be a clojure library exposing JSch functionality in a clojure friendly way. It will be uploaded to clojars to be used from your `deps.edn` or `project.clj`.

#### Just the Idempotent scripts

Similarly, not everyone who wants to run idempotent system scripts wants to do it over SSH. So this part of spire will be broken out into a library. This library will construct bash scripts that perform idempotent operations. How you consume them is optional. Potentially a whole bunch of them could be appended together into a big static uberscript that will do basic provisioning! Spire will become a consumer of this library.

#### Spire becomes a cljc library

Spire can then become a pure clojure library. By using cljc reader tags the differences between the babashka version and the clojure version can be elegantly expressed. Babashka will evaluate `:bb` tagged code if it comes before a `:clj` tag. The spire code will become clean and elegant!

#### Falling behind babashka

Spire's execution environment has fallen significantly behind babashka's. I would like spire to support the consumption of pods and clojure libraries. This is already present in babashka. Implementing this in spire is just duplicate work. And so it is likely I will eventually retire the spire binary. Spire will then not contain sci, or evaluate any clojure code. It will be a library.

To provide an upgrade path the spire binary will be replaced by a small shim script that will launch babashka. Using some pre-execution scripts the babashka execution environment could be made to `require` the namespaces that spire has by default. The hope is that spire scripts should continue to work without much need of change by running inside babashka.

That's the plan. Now for the hard work of doing it...
