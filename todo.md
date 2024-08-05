# Todos

* test `@Dynamic` with `@Dump` / figure out why `@Dump` can break unloading
* `__dynnsvars__` non-static vars asm rewriting
* dockerify hotreload tests

## Want:

* Single implemntation of test code
    * (*) Code selectively built based on java version being tested on
        * have some of this with the hotreload tests
    * JavaParser codegen to "update" hooks?
    * (x) Hooks built into separate JARs combined with scripting out vl attach/detach in phases
        * hotreload tests does this
    * Maybe dynamically generate docker entrypoint shell script?
* To figure out why logger crashed in `./gradlew test`
    * Does it happen w/ raw junit?
* More anti-debugging
* Packaging
    * For full release
* shouganaiyo-loader suppport
* (*) multi-release jar to get around eventual issues w/ isAccessible
    * have workaround using reflection compat polyfill


## Have

* Global no recursion (NoRecursion) wrapper
* Single hook no recursion (NoSelfRecursion) wrapper
* (untested) bypass no recursion for individual calls
* `@Dynamic` working
    * static fields in @Hook class are removed and rewired into `__dynvars__` map
    * dependency injection works with @Dynamic

# Tests

## Have:

* test harness (e.g. provide java version, gets plugged into docker script depending on version)
* premain (bootstrap + spring)
* agentmain (bootstrap + spring)
* java 6/7 (hotspot)
* java 8/9+ (hotspot/openj9, tested up to 21)
* Multiple attach/detach cycles
* Hot reload/In-place hook upgrade (including for bootstrap hooks)

## Need:

* CLI args
* Config file
* Detach cleanup
* Multiple JARs with colliding classes
* Anti-debugging

