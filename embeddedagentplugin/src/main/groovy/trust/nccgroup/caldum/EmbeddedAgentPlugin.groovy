package trust.nccgroup.caldum

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class EmbeddedAgentPlugin implements Plugin<Project> {

  void apply(Project project) {

    project.configure(project) {
      apply plugin: 'com.github.johnrengelman.shadow'

    }

    project.task('caldum-vl-embed') { task ->

      java.nio.file.Path agentjar = project.shadowJar.outputs.files[0].toPath()
      task.inputs.file(agentjar.toFile().getCanonicalPath())
      java.nio.file.Path finaljar = agentjar.getParent().resolve(agentjar.toFile().getName().replace(".jar", "-vl.jar"))
      task.outputs.file(finaljar.toFile().getCanonicalPath())


      doLast {
        def bbver = project.configurations.compileOnly.files.find { it.getName().startsWith("byte-buddy-") }.getParentFile().getParentFile().getName()
        def cvlver = project.configurations.compileOnly.files.find { it.getName().startsWith("caldum-") }.getParentFile().getName()
        println(project.configurations.compileOnly.files)
        println(bbver)
        println(cvlver)

        java.nio.file.Path tmpdir = Files.createTempDirectory("vl")
        java.nio.file.Path build_gradle = tmpdir.resolve("build.gradle")

        Files.write(build_gradle, ("""
apply plugin: 'java'

buildscript {
  repositories {
    jcenter()
  }

  dependencies {
    classpath 'com.github.jengelman.gradle.plugins:shadow:+'
  }
}

apply plugin: 'com.github.johnrengelman.shadow'

sourceCompatibility = 1.6
targetCompatibility = 1.6

repositories {
  mavenLocal()
  mavenCentral()
}

configurations.all {
  resolutionStrategy.failOnVersionConflict()
}

dependencies {
  compile fileTree(include: ['*.jar'], dir: 'tools') // for tools.jar on jdk < 9

  compile group: 'net.bytebuddy', name: 'byte-buddy', version: '${bbver}'
  compile group: 'trust.nccgroup', name: 'vulcanloader', version: '${cvlver}'
}

sourceSets {
  main {
    java {
      srcDirs = []
    }
  }
}

jar {
  manifest {
    attributes (
      "Manifest-Version": "1.0",
      "Can-Redefine-Classes": "true",
      "Can-Retransform-Classes": "true",
      "Can-Set-Native-Method-Prefix": "true",
      "Main-Class": "trust.nccgroup.vulcanloader.Main",
      "Premain-Class": "trust.nccgroup.vulcanloader.PreMain",
      "Agent-Class": "trust.nccgroup.vulcanloader.AgentMain"
    )
  }
}

shadowJar {
  archiveName = "vl.\${extension}"

  // removes -all from the build
  classifier = ''
}

defaultTasks 'shadowJar'
        """).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)

        File tools = project.file('./tools')
        if (tools.exists()) {
          java.nio.file.Path src = tools.toPath();
          java.nio.file.Path dest = tmpdir.resolve("tools")

          Files.walk(src).forEach({source ->
            Files.copy(source, dest.resolve(src.relativize(source)))
          })
        }

        ProjectConnection pc = GradleConnector.newConnector().forProjectDirectory(tmpdir.toFile()).connect()

        // note: if this following line of code errors with a specific
        // conflicting version, the specified version of byte-buddy conflicts
        // with the range of versions allowed by the specified version of
        // caldum. gradle simply reports the highest version within that range.
        pc.newBuild()
          .setStandardOutput(System.out)
          .setStandardError(System.err)
          .forTasks("shadowJar")
          .run()

        //println tmpdir.toString()


        java.nio.file.Path zipfile = tmpdir.resolve("build/libs/vl.jar")

        java.nio.file.FileSystem zipfs = FileSystems.newFileSystem(zipfile, null)

        java.nio.file.Path assets = zipfs.getPath("/assets")
        Files.createDirectory(assets)

        java.nio.file.Path pathInZipfile = assets.resolve("agent.jar")
        Files.copy(agentjar, pathInZipfile, StandardCopyOption.REPLACE_EXISTING)
        zipfs.close()

        Files.copy(zipfile, finaljar, StandardCopyOption.REPLACE_EXISTING)

        tmpdir.deleteDir()
      }

    }

    project.tasks['caldum-vl-embed'].dependsOn(project.tasks.shadowJar)
    project.tasks['test'].dependsOn(project.tasks['caldum-vl-embed'])

    //println(project.tasks.shadowJar.outputs.files.getFiles())
    //println(project.tasks['caldum-vl-embed'].outputs.files.getFiles())

    String finaljarpath = project.tasks['caldum-vl-embed'].outputs.files[0]
    String javaagent = '-javaagent:' + finaljarpath

    project.configure(project) {
      test {
        //dependsOn 'cleanTest'
        outputs.upToDateWhen {false}
        jvmArgs javaagent

        testLogging {
          showStandardStreams = true
          events "PASSED", "STARTED", "FAILED", "SKIPPED"
        }
      }
    }

    if (project.hasProperty('test.executable')) {
      project.configure(project) {
        test {
          executable project.property('test.executable')
        }
      }
    }

  }
}
