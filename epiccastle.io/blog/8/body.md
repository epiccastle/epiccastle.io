Bbssh is a Babashka pod that provides an SSH implementation to your Babashka code. It's compiled with GraalVM's native image into a single executable. Part of Bbssh is written in C. In the past, I integrated C code using JNI. I bundled the JNI shared library into the native image, and at runtime it was extracted to the file-system. The Java library path was updated to include that location, and the library was then loaded.

There are some down sides to this approach: it's cumbersome, it's not supported with a musl linked static image on Linux, the shared library has to be specially crafted for JNI to call it and the JNI calls are somewhat slower than what could be achieved with direct linking.

There is a new way to achieve this hybrid Java/C project. The GraalVM has a layer called the substrate that now allows direct linking to C code. Your C code can be statically or dynamically linked in with your executable. The coupling is efficient and fast. However it only works with native images.

I built a C library that can be linked directly into a GraalVM native image, but can also be loaded through JNI when running on a JVM. Thus I can continue to do development work with the JNI method and then build the release version with the substrate linking. This article is going to lay out how I achieved this. I am going to be simplifying things to clarify the important parts while also providing links to the full code for deeper investigation.

I found [HuaHui's post here](https://yyhh.org/blog/2021/02/writing-c-code-in-javaclojure-graalvm-specific-programming/) to be very helpful and should serve you as a useful reference. I did not use the method of copying the libs during static initialisation as he did, instead opting to alter the search paths of the compiler and linker themselves to find the associated files. I found that to be simpler (although this method was impossible in the case of Windows and I had to fall back to copying which I will discuss later).

#### The C library

I create a basic C library with no JNI. Composed of a number function like this in a file `bbssh.c`:

```C
int is_stdout_a_tty() {
  return isatty(STDOUT_FILENO);
}
```

And there is a matching header line in `bbssh.h`:

```C
int is_stdout_a_tty();
```

For all the functions present in the library I have to write a java file containing the substrate binding. I called my class `BbsshUtils` and the important part looks like this:

```java
@CContext(BbsshUtils.Directives.class)
public final class BbsshUtils {
    public static final class Directives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList("\"bbssh.h\"");
        }

        @Override
        public List<String> getLibraries() {
            return Arrays.asList("bbssh");
        }
    }
    @CFunction("is_stdout_a_tty")
    public static native int is_stdout_a_tty();
}
```

The `@CContext` annotation tells native-image which static class contains the information needed. It specifies the header file (or files), in this case `bbssh.h`. And it specifies the library to link with, in this case it is `bbssh`, so would link with bbssh.a, bbssh.lib, libbbssh.so, libbbssh.dylib depending on the method of linking and the platform.

The main body of the class contains methods annotated by `@CFunction`. This tells native-image which C function the method calls.

Sometimes the interface between Java and C required some extra convenience processing on the Java side. In these cases I named the direct native java method something slightly different to the C function, and then made another method with that name that did the various type conversions and processing. For example, to pass a Java string into a function requiring a C style char pointer I did the following:

```java
    @CFunction("ssh_auth_socket_read")
    public static native int cssh_auth_socket_read(int fd, CCharPointer buffer, int count);

    public static int ssh_auth_socket_read(int fd, byte[] buf, int count)
    {
        return cssh_auth_socket_read
            (
             fd,
             org.graalvm.nativeimage.c.type.CTypeConversion.toCBytes(buf).get(),
             count
             );
    }
```

#### Compiling

Now I can compile my C library. For static linking I want a static library file. This is done the same way on mac and Linux. You will need to adjust the GraalVM include folders for your situation and platform.

```shell
cc -I/home/crispin/graalvm-jdk-24.0.2+11.1/include -I/home/crispin/graalvm-jdk-24.0.2+11.1/include/linux -c src/c/bbssh.c -o libbbssh.a
```

On windows this is a little different. We must make a .lib archive. It goes something like this:

```powershell
cl /c src\c\bbssh.c
lib /out:bbssh.lib bbssh.obj
```

So on Linux and mac we have a `libbbssh.a` file. On windows we have a `bbssh.lib` file. These are what will be statically linked into the native-image.

#### native-image

To statically link this custom library file in the native image build you pass the `--native-compiler-options` flag to `native-image`, and it will include any flags you give it into the build and link steps. You will need to give it an extra `-I` flag for header includes and a `-L` flag to point to the location of the library file you just built. These are just directory locations. Remember the actual name of the header file and the name of the library file are specified in the Java binding source files under the `@CContext` annotation.

In short, you will pass in some extra arguments like:

```shell
native-image \
--native-compiler-options=-I/path/to/project/src/c \
--native-compiler-options=-L/path/to/project/build \
... many more args ...
```

Bbssh has so many different arrangements for these parameters for different platforms and static vs dynamically linked builds that I broke the varying parameters out into the [deps.edn](https://github.com/epiccastle/bbssh/blob/main/deps.edn#L23) inside the :aliases section, and launch the relevant native-image build code from my [build.clj](https://github.com/epiccastle/bbssh/blob/main/build.clj) with a `clojure -M:alias-name`

#### Windows difficulties

On windows you can pass in the location of extra include files to the underlying windows C compiler and linker with:

```shell
--native-compiler-options=/IC:\path\to\project\src\c
```

But the **Microsoft linker simply cannot** be made to look in more places for library files for linking. You can pass in a `/L` argument, and this is passed through by GraalVM's native-image, but the windows compiler does not pass it through to the linker. This appears to be a problem with Window's build tools themselves.

To work around this you have to copy the library file you built into the libs folder that GraalVM is using in the build before running native image. It's an ugly hack but it works. You will need a copy directive like the following in powershell:

```powershell
copy build\bbssh.lib C:\Users\myuser\project\graalvm\graalvm-jdk-24.0.2+11.1\lib\static\windows-amd64\
```

The lib will now be found and the `.exe` will be generated successfully.

#### Supporting JNI for Java based development

When developing day to day, it's faster to build the C code as a shared library and call it through Java's JNI. We can keep the static native‑image build for production and use the JNI version during development.

At this point it's pretty simple. As well as building the `build/libbbssh.a` file, we also build a `build/libbbssh.so` file from the same source. We will need the JNI binding file here (`src/c/jni/BbsshUtils.c` for us) to facilitate this build. You can find out about making this file from documentation on JNI.

Here are the two build commands side by side on Linux:

```bash
# build static lib
cc -I/home/crispin/graalvm-jdk-24.0.2+11.1/include -I/home/crispin/graalvm-jdk-24.0.2+11.1/include/linux -c src/c/bbssh.c -o build/libbbssh.a

# build JNI shared library
cc -I/home/crispin/graalvm-jdk-24.0.2+11.1/include -I/home/crispin/graalvm-jdk-24.0.2+11.1/include/linux -shared -Isrc/c src/c/jni/BbsshUtils.c src/c/bbssh.c -fPIC -o build/libbbssh.so
```

In your mainline code you can detect at runtime if you are using the Substrate VM and load the shared object if you are not:

```clojure
(defn native-image? []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(defn -main [& args]
  ;; parse args and setup

  ;; load a shared library with JNI in development
  (when-not (native-image?)
    (clojure.lang.RT/loadLibrary "bbssh"))

  ;; now run mainline
)
```

So in summary you will have **two** `BbsshUtils.java` binding files, one for JNI, and one for Substrate VM. In you code you will simply require BbsshUtils and call the functions there in. You need to make sure that the paths are setup so that development will see one, and native builds will see the other. I just did this with `:extra-paths` in my `deps.edn`.

```clojure
(BbsshUtils/is-stdout-a-tty)
```

For completeness, here is the [JNI binding file](https://github.com/epiccastle/bbssh/blob/main/src/c/jni/BbsshUtils.java) and here is the [Substrate VM binding file](https://github.com/epiccastle/bbssh/blob/main/src/c/native/BbsshUtils.java) for you to compare.

May your images be native and your builds be small.
