### Hot loading C wasm with heap preservation

Wasm, or WebAssembly, is a very fast stack machine that happens to be buried in your browser. You can compile all kinds of low level code to wasm and have it execute at break-neck speed. But we like Clojure and clojurians love hot loading their code for maximal development cycles. Here is how I set up a hot loading wasm environment using clang, shadow-cljs and babashka. I also managed to preserve the WebAssembly heap between loads to really enhance that Clojure experience.

#### Clang

I'm using clang to compile my C. You could use any wasm language. The `makefile` I use to build it looks in part like:

```makefile
CLANG_DIR = $(HOME)/clang+llvm-13.0.1-x86_64-linux-gnu-ubuntu-18.04
CLANG = $(CLANG_DIR)/bin/clang
LLC = $(CLANG_DIR)/bin/llc
LD = $(CLANG_DIR)/bin/wasm-ld

C_SRC := $(wildcard src/c/*.c)
OBJ_SRC := $(patsubst %.c, %.o, $(C_SRC))

%.o: %.c # delete competing implicit rule

%.ll: %.c
    $(CLANG) \
        --target=wasm32	\
        -emit-llvm \
        -c \
        -S \
        -std=c99 \
        -o $@ \
        -nostdlib \
        $<

%.o: %.ll
    $(LLC) \
        -march=wasm32 \
        -filetype=obj \
        $<

resources/public/wasm/mymodule.wasm: $(OBJ_SRC)
    $(LD) \
        --no-entry \
        --strip-all \
        --import-memory \
        --export=__heap_base \
        -o $@ \
        $^

```

`-nostdlib` tells the compiler that we wont be linking with any libc (so no printf!). `--import-memory` tells the linker that we will be passing our memory into the module rather than letting the module create it for us. `--export=__heap_base` is needed so the module will report where the heap begins in memory when it is loaded.

With no other explicit `--export` arguments we need a way to choose what functions to export from our module. This is done directly in our C code with the `export_name` clang attribute like this:

```cpp
__attribute__((export_name("calc")))
int calc(int a, int b)
{
  return a*b;
}
```

And now we can compile the wasm code with

```shell
$ make resources/public/wasm/mymodule.wasm
```

#### Memory

I need to construct the WebAssembly Memory object myself as this will be reused between instances. I will pass this Memory object into the module instantiation later. Let's make some heap:

```clojure
(ns myproject.wasm.heap)

(def page-size (* 64 1024))
(def initial-pages 16) ;; 1 MiB
(def maximum-pages (* 16 32)) ;; 32 MiB

;; a heap that gets reused between reloaded wasm code
(defonce memory
  (js/WebAssembly.Memory.
   (clj->js {:initial initial-pages
             :maximum maximum-pages})))

;; all the different views into the wasm heap
(defonce heap-uint8 (js/Uint8Array. (.-buffer memory)))
(defonce heap-uint16 (js/Uint16Array. (.-buffer memory)))
(defonce heap-uint32 (js/Uint32Array. (.-buffer memory)))
(defonce heap-int8 (js/Int8Array. (.-buffer memory)))
(defonce heap-int16 (js/Int16Array. (.-buffer memory)))
(defonce heap-int32 (js/Int32Array. (.-buffer memory)))
```

Now we can use this when we load our wasm byte code. We pass the memory object in to the `instantiateStreaming` call:

#### Loader

```clojure
(ns myproject.wasm.loader
  (:require [myproject.wasm.heap :as heap]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer [<p!]]))

;; load and reload compiled wasm modules keeping memory between them
(defonce module (atom {}))

(defn load-streaming [{:keys [name url]}]
  (go
    (let [result
          (<p!
           (js/WebAssembly.instantiateStreaming
            (js/fetch url)
            (clj->js {:env {:memory heap/memory}})))
          instance (.-instance result)
          exports (.-exports instance)
          heap-base (.-value (aget exports "__heap_base"))]
      (swap! module assoc name
             {:module (.-module result)
              :instance (.-instance result)
              :exports exports})
      (heap/init-memory-map! heap-base)
      (js/console.log (str "wasm " name " reloaded from " url " with heap-base " heap-base)))))
```

Here `heap/memory` is passed to the `WebAssembly.instantiateStreaming` call.

**Note:** For this instantiate call to work your web server must return the Content-Type of the wasm file as `application/wasm`.

When the call succeeds we store the resulting instance in the `module` atom and then we initialise our heap with the `__heap_base` value. More about that in a minute.

Now if we call `(myproject.wasm.loader/load-streaming {:name :mymodule :url "wasm/mymodule.wasm"})` then the wasm module will load and the instance objects will be registered inside the `module` atom. And if we call it again, then it will reload the new wasm code, but keep the heap memory.

#### Heap allocator

Unfortunately, having no libc we cannot use dynamic memory allocation (eg. malloc/free) in our C code. However most of the C code I am writing is low level array buffer manipulations. Most of the general code lies on the ClojureScript side and the C code is merely for some heavy buffer processing. If I could partition the heap on the ClojureScript side I could just treat those allocations as buffers on the C side. Then reloading should preserve the state of that allocated memory. What I need is a ClojureScript heap allocator!

Now the glibc version of malloc does all kinds of wonderful things. My aim is not to out do it but to write a slow and inefficient imitation. My heap allocator is a simple first best fit allocator. It searches for the first free block of suitable size and allocates from that.

The allocator serves out 4-byte aligned sections of memory and it can coalesce chunks during free to reduce memory reallocation issues. If it runs out of memory, it just fails. It does not try to grow the heap.

In order to allocate out the right memory we have to be aware of where the heap is. The wasm stack starts just below the heap and grows downwards, and we certainly don't want to overwrite the stack, or have the stack overwrite our data. The stack by default is 64KB unless it's changed with the clang options. The location of the heap is exported from the WASM module in `__heap_base`.  We use this `__heap_base` value to initialise the memory map correctly in the `init-memory-map!` function.

The full allocator is a little large, [so you can see it here](https://gist.github.com/retrogradeorbit/3e2837e713b474b4ba98b9ff9fc9557d).

#### Babashka watcher

The last thing I need is something to trigger the C compile and hot load when a C file is saved. To do this I wrote the following babashka script:

```clojure
(require '[babashka.pods :as pods])
(pods/load-pod 'org.babashka/fswatcher "0.0.2")

(require '[pod.babashka.fswatcher :as fw]
         '[babashka.process :as p]
         '[clojure.string :as string])

(def path "src/c")
(def event-types #{:write})
(def path-extensions #{"c" "h"})
(def build-command "make resources/public/wasm/mymodule.wasm")
(def reload-hook-command "touch src/cljs/myproject/wasm/reloader.cljs")

(defn file-extension [s]
  (last (string/split s #"\.")))

(def watcher
  (fw/watch path
   (fn [{:keys [type path]}]
     (when (and (event-types type)
                (path-extensions (file-extension path)))
       (println "compiling...")
       (if (-> (p/process build-command {:inherit true})
               deref
               :exit
               zero?
               not)
         (println "failed!")
         (do
           (println "done.")
           @(p/process reload-hook-command)))))))

(.join (Thread/currentThread))
```

Whenever any `.c` or `.h` source file is written to, the build process is triggered to make a fresh `.wasm` file. If the build succeeds then the source file `reloader.cljs` is touched and this triggers shadow-cljs to reload the file. This little `reloader.cljs` stub looks like this:

```clojure
(ns myproject.wasm.reloader)

(myproject.wasm.loader/load-streaming
 {:name :mymodule
  :url "wasm/mymodule.wasm"})
```

Touch that and shadow-cljs pops up its little icon on your web page, reloads the name-space and makes the new `load-streaming` call to load in the fresh wasm code.

#### Calling C and passing buffers

Because our module instance lives in the `loader/module` atom, we can make a C call like this:

```clojure
(-> @loader/module :mymodule :exports (.calc 10 20))
```

We could make this more convenient with a little more work, but it shows the idea.

C pointers are represented as integer offsets into memory on the ClojureScript side, and they must be passed accordingly. However, on the ClojureScript side we will want to access the buffers via an array object. In other words, we need to use the array objects on the JS side, and integer offsets when calling and returning from C. I get around this inconvenience by returning both values from my malloc call.

```clojure
(heap/malloc 2048)
;; => {:buffer #object[Uint8Array 0,0,0...],
;;     :address 70656}
```

Lets make some C functions to see if we can investigate the heap. We add the following to our C code.

```cpp
__attribute__((export_name("get_byte")))
uint8 get_byte(uint8 *buffer, int i)
{
  return buffer[i];
}

__attribute__((export_name("set_byte")))
void set_byte(uint8 *buffer, int i, uint8 v)
{
  buffer[i]=v;
}
```

We save the file and the code is compiled and hot loaded into the browser. Our browser environment now has two new functions in the wasm module. Lets allocate some memory and see...

```clojure
(def b (heap/malloc 1024))

;; get the first memory location from the cljs and then C
(aget (:buffer b) 0) ;; => 0
(-> @module :mymodule :exports (.get_byte (:address buff) 0)) ;; => 0

;; set it to 50
(aset (:buffer b) 0 50)

;; check
(aget (:buffer b) 0) ;; => 50
(-> @module :mymodule :exports (.get_byte (:address buff) 0)) ;; => 50

;; go and edit the C file. Save it and wait for it to be hot loaded.
;; still the same memory
(aget (:buffer b) 0) ;; => 50
(-> @module :mymodule :exports (.get_byte (:address buff) 0)) ;; => 50

;; set on C side, read on cljs
(-> @module :mymodule :exports (.set_byte (:address buff) 0 100)) ;; => nil
(aget (:buffer b) 0) ;; => 100

;; set on cljs, read on C
(aset (:buffer b) 0 150) ;; => 150
(-> @module :mymodule :exports (.get_byte (:address buff) 0)) ;; => 150

;; release the memory
(heap/free b)
```

#### Conclusion, Limitations and Future Work

So how does it behave? Very well! Clang is a fast compiler and the new code is available in the browser almost instantly. For most C work the reloading behaviour and limitations are sufficient to work with the code changes without a browser refresh. Some things one has to be more careful of, however. If you change the layout of the heap, or change how the code interprets the memory packing of the heap, all bets are off! For example, adding a new field to a struct is going to generate new code that sees your existing heap data structures in incorrect ways. Luckily in these cases we just reload the browser to clean the wasm memory and allocate fresh again.

In the future it would be interesting to hook up our ClojureScript malloc and free as imports into the module. This way we could call malloc and free inside our C code as we saw fit and have the wasm use the ClojureScript allocator to do so. In addition the allocator could itself be improved in many ways. It could try and grow the heap if it cannot allocate. It could be faster. The glibc allocator does other tricks like keeping recently freed small blocks around to reallocate immediately for a malloc of the same size.

The complete set of files for this can be [found here](https://gist.github.com/retrogradeorbit/3e2837e713b474b4ba98b9ff9fc9557d).
