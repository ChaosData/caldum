# Caldum

Caldum (Latin: *"hot"*) is a microframework for writing function hooks
targeting the Java Virtual Machine (JVM). It provides additional scaffolding on
top of the venerable [Byte Buddy](https://github.com/raphw/byte-buddy), and
provides several features that enable faster and easier development of
Byte Buddy-based Java agents.

Caldum's main feature is its hot-reloading capabilities that enable hooks to be
updated at runtime. It additionally provides annotation-based dependency
injection similar to JAX-RS, enabling simpler development workflows that do not
require application restarts. Lastly, Caldum provides an interface to extend
function hooks with dynamic instrumentation.

# VulcanLoader

VulcanLoader (VL) is a thin-wrapper that builds Caldum as a Java agent and
provides a minimal interface to set up, reconfigure, and unload
Caldum-compatible Java agents. Agent state is maintained based on the JAR
filename, but not its entire file path; attempting to load a new agent JAR with
an existing agent JAR's will result in the original being unloaded prior to the
new one being loaded.

## Interface

* `-javaagent:path/to/vl.jar=path/to/config`
* `-javaagent:path/to/vl.jar=path/to/agent.jar`
* `java -jar path/to/vl.jar <pid> -c path/to/config`
* `java -jar path/to/vl.jar <pid> <agent.jar...> -- <agent opts>`

## Configuration Format

~~~
path/to/agent1.jar: <agent1 opts>
path/to/agent2.jar: <agent2 opts>
...
~~~

## Unloading Agents

* Unload one agent:
    * `java -jar path/to/vl.jar <pid> -u path/to/agent.jar`
    * `java -jar path/to/vl.jar <pid> path/to/agent.jar -- unload`
* Unload all agents:
    * `java -jar path/to/vl.jar <pid> -u`


# Structure

Caldum and VulcanLoader are designed to directly expose a single bundled
version of Byte Buddy directly to compatible Java agents, such that they
do not need to bundle the entire Byte buddy library and may build to relatively
small JAR files. The ClassLoader used to load agent JARs will first check
itself when attempting to load classes. This enables developers to embed
generally whatever libraries they may need. However, care should be taken to
ensure that dependencies intended for use within hook code are injected
so that they are accessible from the ClassLoader of the instrumented class.

While it is not strictly necessary to include the normal JAR manifest
attributes used for Java agents (all loaded JARs inherit Caldum/VL's agent
capabilities), Caldum/VL will respect any `Premain-Class` and `Agent-Class`
attributes and invoke the associated entry points prior to scanning for
and applying annotated function hooks. This enables developers to provide
additional static configuration to annotated classes and support legacy
instrumentation code. Caldum/VL will additionally invoke any
`public static void unload()` method within the entry class when the
agent is unloaded (e.g. manually or when reloading).
