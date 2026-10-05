plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
val verifyCore by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("ko.bura.core.CoreChecksKt")
}
tasks.check { dependsOn(verifyCore) }
