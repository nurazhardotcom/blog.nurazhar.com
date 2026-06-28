Title: Why Isn't My Static Site Build Instant? An Architectural Deep Dive
Date: 2026-06-28
Tags: clojure, systems-programming, optimization, compilers, rust, architecture
Description: Ever wondered why static site generation takes seconds or minutes instead of milliseconds? It's easy to blame Clojure or Babashka, but the true bottleneck is often the operating system's process boundary. Let's explore subprocess overhead, compiled vs. interpreted languages, and how to achieve sub-millisecond builds.

---

It is one of the ultimate ironies of modern development: we push our code to a continuous integration (CI) pipeline, and a "static site build" of a simple text-based blog takes several minutes. 

We write about compiled systems, sub-millisecond latencies, and high-performance engineering. Yet, when we click "Build," we stare at a spinner.

The natural instinct is to blame the orchestrating language. We think: *"Ah, Clojure is hosted on the JVM, which is slow to start,"* or *"Babashka is an interpreted dialect of Clojure, of course it’s slow. If I rewrote this in Rust or C++ with CMake, it would build in 50 milliseconds."*

But as a systems programmer, you must learn to **never guess where a bottleneck is—always measure it.** 

In this architectural deep dive, we are going to profile exactly why your static site build isn't instant, demystify the difference between interpreted runtimes and native compilation, and expose the silent performance killer of modern system automation: **the Operating System Process Boundary.**

---

## The Illusion: Blaming the Language

Let's address the elephant in the room. Your site is built using **Babashka (`bb`)**, a Clojure scripting engine. Clojure famously runs on the Java Virtual Machine (JVM), which is notoriously heavy and takes 1–2 seconds just to boot up.

But Babashka is different. It is **not** running on a standard JVM. Babashka is compiled using **GraalVM Native Image** into a native, self-contained machine-code binary. 

```d2
direction: right
GraalVM: "GraalVM Compiler"
ClojureSource: "Clojure AST Interpret"

GraalVM -> BabashkaBinary: "AOT Compile"
BabashkaBinary -> Execution: "Native Machine Instructions\n(Startup: ~10ms)"
```

When you type `bb`, it boots in **less than 10 milliseconds**. That is on par with compiled C or Go binaries. Babashka is blazingly fast. It is not the bottleneck.

So why does the build still take minutes?

---

## The Culprit: Subprocess Spawning (The OS Process Boundary)

To understand why the build slows down, we have to look at how markdown is compiled into HTML.

Your static site generator parses your article files one by one. For each article, it has to convert the markdown syntax into clean HTML. It does this by calling **Pandoc**, a highly optimized document converter written in Haskell.

But here is how it calls Pandoc:

```clojure
(defn markdown->html [content]
  (let [result (sh "pandoc" "-f" "markdown" "-t" "html5" :in content)]
    (:out result)))
```

The function `sh` tells the operating system: *"Spawn a new, separate process, run the Pandoc executable, feed this content into its standard input, wait for it to complete, and capture the output."*

This seems simple, but to your Operating System, spawning a subprocess is a **gargantuan task**. Let's visualize what the OS has to do every single time `sh` is called:

```d2
direction: down

OS_Kernel: "Operating System Kernel" {
  fork: "1. Fork Caller Process"
  exec: "2. Exec Pandoc Executable"
  mem: "3. Allocate Memory Pages"
  link: "4. Load & Link Dynamic C Libraries"
  sched: "5. Context Switch CPU to Pandoc Thread"
  cleanup: "6. Reclaim Memory & Tear Down Process"
}

Babashka -> OS_Kernel.fork: "sh \"pandoc\""
OS_Kernel.sched -> Pandoc: "Run Pandoc logic"
Pandoc -> OS_Kernel.cleanup: "Exit Process"
OS_Kernel.cleanup -> Babashka: "Return stdout string"
```

Spawning an OS subprocess requires a full context switch, kernel-level thread management, virtual memory mapping, dynamic library loading (linking `libc`, etc.), and process teardown.

This overhead takes roughly **50 to 100 milliseconds** per invocation. 

If you have 5 articles, you will never notice it (5 * 50ms = 250ms). But as your content library scales, this linear subprocess overhead compounds brutally:

$$\text{Total Subprocess Overhead} = N \times \text{OS Spawn Overhead}$$

For a multi-language, multi-category library of **1,500 pages**:

$$1,500 \text{ files} \times 80 \text{ms} = 120 \text{ seconds}$$

That is **2 full minutes** of your CPU doing nothing but creating and destroying operating system processes! The CPU core executing the code is running hot, yet 95% of its cycles are wasted on operating system bookkeeping instead of actual markdown parsing.

---

## Would Rust or C++ (CMake) Make This Faster?

Now, let's address the systems programming question: **If we rewrote your static site generator in Rust or C++, would it build in milliseconds?**

The answer is: **Only if you change the architecture.**

If you write a beautiful, blazing-fast Rust compiler, compile it to native machine instructions with maximum compiler optimizations (`--release`), but you **still** call `pandoc` via an external process spawn:

```rust
// A beautifully written, compiled Rust subprocess call
let mut child = Command::new("pandoc")
    .arg("-f").arg("markdown")
    .stdin(Stdio::piped())
    .spawn()
    .expect("Failed to spawn process");
```

Your Rust compiler will run into the **exact same operating system process bottleneck**. It will still take 2 minutes! 

The system boundary is the bottleneck, not the orchestration language.

---

## How to Achieve Instant (Sub-Millisecond) Builds

To make a build truly instant, we must eliminate the process boundary. We must keep all execution **In-Process (In-Memory)**. 

Instead of spawning a separate program, the markdown compiler should run directly inside the same memory address space as your static site generator.

### The In-Memory Architecture

```d2
direction: right

InProcess: "In-Memory Compilation (Sub-Millisecond)" {
  generator: "Static Site Generator Process" {
    ClojureHeap: "Clojure/Rust Shared Heap"
    Parser: "In-Process Markdown Parser\n(e.g., markdown-clj or pulldown-cmark)"
  }
}

InProcess.ClojureHeap -> InProcess.Parser: "Direct Memory Pointer Access"
InProcess.Parser -> InProcess.ClojureHeap: "Extremely Fast Return\n(Microsecond latency)"
```

If we use an in-memory parsing library:
1. There are **zero OS subprocess forks**.
2. There is **zero memory page mapping**.
3. All operations occur directly on the processor heap.

A compiled language like Rust can bind directly to a native parser library like `pulldown-cmark`. It compiles the markdown parser straight into your binary. When it processes 1,500 files, it doesn't talk to the OS kernel—it simply reads the files, parses them in-memory, and writes the output. 

This approach processes **1,500 files in under 100 milliseconds**!

We can achieve the exact same performance leap in Clojure/Babashka. By replacing the external `sh "pandoc"` call with a native Clojure library (like `markdown-clj`), your blog build would instantly drop from **2 minutes to 1.2 seconds**.

---

## Summary of Architectural Trade-offs

| Metric | Subprocess Model (Pandoc CLI) | In-Memory Model (Native Library) |
| :--- | :--- | :--- |
| **Orchestration Language** | Clojure (Babashka), Rust, Python, Go | Clojure (Babashka), Rust, Python, Go |
| **Engine Linkage** | External Subprocess (`fork`/`exec`) | Shared Memory Heap (Direct function call) |
| **Process Overhead** | 50ms - 100ms per file | **0.001ms (1 microsecond) per file** |
| **Build Time (1,500 files)** | ~120 seconds | **<1.5 seconds** |
| **Maintenance Cost** | Low (uses ready-made Pandoc binary) | Medium (requires compiling/binding libraries) |

## The Systems Lesson

The next time you profile a slow pipeline, **don't look at the programming language first.** Look at the **system boundaries**. 

Every time your data crosses a boundary—whether it's writing a file to disk, calling an external shell command, or making a network request—you pay a massive system tax. Collapsing those boundaries and keeping execution in-process is the single most powerful optimization you can make.
