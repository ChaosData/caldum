# Have Tests For:

* premain (bootstrap + spring)
* agentmain (bootstrap + spring)
* java 6/7 (hotspot/openj9)
* java 8/9+ (hotspot/openj9, working up to 21)
* test harness (e.g. provide java version, gets plugged into docker script depending on version)

# Need Tests For:

* CLI args
* Config file
* Detach cleanup
* Multiple attach/detach cycles
* In-place hook upgrade (including for bootstrap hooks)
* Multiple JARs with colliding classes
* Anti-debugging

# Want:

* Single implemntation of test code
    * JavaParse codegen to "update" hooks?
* Code selectively built based on java version being tested on
* Hooks built into separate JARs combined with scripting out vl attach/detach in phases
    * Maybe dynamically generate docker entrypoint shell script?
* Global recursion (NoRecursion) vs single hook recursion ([Princess] MonoNoRecursion) wrappers
* To figure out why logger crashed in `./gradlew test`
    * Does it happen w/ raw junit?
* More anti-debugging
* MavenCentral
    * For full release
* shouganaiyo-loader suppport
* multi-release jar to get around eventual issues w/ isAccessible
