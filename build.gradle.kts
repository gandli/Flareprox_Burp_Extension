plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:2025.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("net.portswigger.burp.extensions:montoya-api:2025.8")
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().filter { it.isDirectory })
    from(configurations.runtimeClasspath.get().filterNot { it.isDirectory }.map { zipTree(it) })
}

tasks.test {
    useJUnitPlatform()
}

// 注册 JavaExec 任务以运行自检主程序
tasks.register<JavaExec>("runSelfTest") {
    group = "verification"
    description = "运行 MultipartBuilder 自检 main"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("MultipartBuilderTest")
    dependsOn("testClasses")
}