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

## Building

Caldum is intended as a pure library. Currently, it may be built and installed
into the local Maven repository on a host with the following commands.

~~~bash
cd caldum && ./gradlew
~~~

# VulcanLoader

VulcanLoader (VL) is a thin-wrapper that builds Caldum as a Java agent and
provides a minimal interface to set up, reconfigure, and unload
Caldum-compatible Java agents. Agent state is maintained based on the JAR
filename, but not its entire file path; attempting to load a new agent JAR with
same filename as an existing agent JAR will result in the original being
unloaded prior to the new one being loaded.

## Building

VulcanLoader is intended to be rebuilt for each target host when targeting
JRE versions below 9. When doing so, the respective platform's `tools.jar`
(from its JDK, not JRE) must be copied to the `vulcanloader/tools/` directory
prior to building.

~~~bash
cd vulcanloader && ./gradlew
~~~

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

## Java Version Support

Caldum and VulcanLoader currently attempt to maintain support for the
same versions of Java supported by Byte Buddy (currently Java 6 through 9+).
Due to this, the codebases of Caldum and VulcanLoader themselves are
built as Java 6; however, Caldum-compatible agents may target newer versions of
Java supported by the JVM being instrumented.

# Annotations (`trust.nccgroup.caldum.annotation.*`)

## Class Annotations

### `@Hook`

This annotation identifies a Caldum hook class to be automatically
applied on load. 

Options:
* The `wrappers` value may be used to supply a list of hook wrapping classes
that will be applied in order to instrument the hook code.
* The `redefinition` value may be used to configure an
`AgentBuilder.RedefinitionStrategy` (e.g. `DISABLED` (new loads only),
`REDEFINITION`, or `RETRANSFORMATION`).

Hook classes are structured as follows:

~~~java
@Hook
class ExampleHook {

  static class SomeSettings {
    ... // type/method matchers, local dependency injection providers
  }

  ... // static fields (copied or dependency injected)

  @Advice.OnMethodEnter
  static void enter() {
    ...
  }

  @Advice.OnMethodExit
  static void exit() {
    ...
  }
}
~~~

### `@Debug`

This annotation is used to configure Byte Buddy to write events and exceptions
of an `@Hook`-annotated class to the standard output of the process being
hooked. This is implemented using a
`new AgentBuilder.Listener.StreamWriting(System.out)`.

### `@DI.Provider`

This annotation identifies a global dependency injection provider.

## Dependency Injection Annotations

Dependency injection in Caldum is designed to support local, in-file
configuration, with opt-in support for global defaults without violating the
principle of least surprise. By default, locally configured `@DI.Provide`-d
values will be applied to any static fields within the `@Hook` class that have
a matching name without validating type compatibility.

### `@DI.Provide`

This annotation may be applied to a static field or static method within a
global `@DI.Provider` class or a "settings" class nested within an `@Hook` class.

If applied to a field without an overriding `name` value, the name of the field
will be used to match against injection targets.

If applied to a static method returning a `Map<String,Object>`, the method will
be invoked to provide a dependency injection mapping of field names to values.

### `@DI.Inject`

This annotation may be applied to a static field within a `@Hook` class to
opt-in to dependency injection from a matching `@DI.Provider` class-supplied
`@DI.Provide`-d field. However, locally-declared `@DI.Provide`-d fields will
take precedence.

### `@DI.AgentClassLoader`

This annotation may be applied to a static field within a `@Hook` class to
inject the `ClassLoader` of the `@Hook` class (i.e. the `ClassLoader` of the
Caldum-compatible agent JAR).

## Matcher Annotations

Each of these annotations may be applied to a static Byte Buddy
`ElementMatcher` field or a static method returning an `ElementMatcher`.

### `@Matcher.Ignore`

The `ElementMatcher` will be used to configure `net.bytebuddy.agent.builder.AgentBuilder::ignore`.
`net.bytebuddy.agent.builder.AgentBuilder::ignore`.

### `@Matcher.Type`

The `ElementMatcher` will be used to configure the type matcher of
`net.bytebuddy.agent.builder.AgentBuilder::type`.

### `@Matcher.Loader`

The `ElementMatcher` will be used to configure the `ClassLoader` matcher of
`net.bytebuddy.agent.builder.AgentBuilder::type`.

### `@Matcher.Module`

The `ElementMatcher` will be used to configure the module matcher of
`net.bytebuddy.agent.builder.AgentBuilder::type`.

### `@Module.Raw`

The `ElementMatcher` will be used to configure
`net.bytebuddy.agent.builder.AgentBuilder::type(AgentBuilder.RawMatcher matcher)`.

### `@Module.Member`

The `ElementMatcher` will be used to configure
`net.bytebuddy.asm.Advice::on(ElementMatcher<? super MethodDescription> matcher)`.

## Hook Wrapper Annotations

Caldum supports the instrumentation of `@Hook` classes through the use of its
`wrappers` value. Classes specified in this manner must contain static nested
classes annotated with at least one of the following annotations.

### `@Wrapper.OnMethodEnter`

This class will be applied as Byte Buddy `Advice` to the
`@Advice.OnMethodEnter`-annotated method of the `@Hook` class. It should
specify at least one `@Advice.OnMethodEnter`-/`@Advice.OnMethodExit`-annotated
static method. Care should be taken when specifying `@Advice` dependency
injected method arguments; these will be applied to the `@Hook`-annotated class
itself and not the classes it instruments.

### `@Wrapper.OnMethodExit`

This class will be applied as Byte Buddy `Advice` to the
`@Advice.OnMethodExit`-annotated method of the `@Hook` class. It should
specify at least one `@Advice.OnMethodEnter`-/`@Advice.OnMethodExit`-annotated
static method. Care should be taken when specifying `@Advice` dependency
injected method arguments; these will be applied to the `@Hook`-annotated class
itself and not the classes it instruments.

# Included Hook Wrappers (`trust.nccgroup.caldum.wrappers.*`)

* `NoRecursion`: This wrapper ensures that a `@Hook`-annotated class configured
with it will not invoke the injected hook code when invoked from the call-stack
of a `NoRecursion`-wrapped `@Hook`-annotated class. This is useful for
performing invocations within hook code that may result in infinite recursion
otherwise.

# License

Caldum and VulcanLoader are licensed under the
[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).
Exceptions to this (i.e. for certain vendored source files) are explicitly
noted in the relevant source files.
