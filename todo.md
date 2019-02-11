# Have Tests For:

* premain (bootstrap + spring)
* agentmain (bootstrap + spring)
* java 6 (haven't actually run using 7 yet, see below)
* java 8/9+

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
* More generic test harness (e.g. provide java version, gets plugged into docker script depending on version)
* Global recursion (NoRecursion) vs single hook recursion ([Princess] MonoNoRecursion) wrappers
* To figure out why logger crashed in `./gradlew test`
    * Does it happen w/ raw junit?
* More anti-debugging
* MavenCentral
    * For full release
