plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

group = "com.chamchamcham"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    testRuntimeOnly("com.h2database:h2")
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.8"))
    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.5.4"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
    implementation("com.cloudinary:cloudinary-http5:2.4.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

// 명시적으로 요청할 때만 test 소스의 로컬 실습 실행기를 사용한다. bootJar에는 포함되지 않는다.
tasks.register<JavaExec>("ragApiLabRun") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.chamchamcham.lab.RagApiLabApplication")
    maxHeapSize = "768m"
}

tasks.register<JavaExec>("ragApiLabIngest") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.chamchamcham.lab.RagApiLabIngest")
    maxHeapSize = "512m"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// DB/API를 시작하지 않는 정책 비교 입력 내보내기. 제품 PromptBuilder를 그대로 사용한다.
tasks.register<JavaExec>("coachingPolicyExport") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.chamchamcham.lab.CoachingPolicyExport")
    workingDir = rootProject.projectDir
    maxHeapSize = "256m"
}

// 보존 자료의 조건 선별만 비교한다. DB/API/모델은 시작하지 않는다.
tasks.register<JavaExec>("coachingEvidenceEvaluate") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.chamchamcham.lab.CoachingEvidenceEvaluation")
    workingDir = rootProject.projectDir
    maxHeapSize = "256m"
    doFirst {
        val run = providers.gradleProperty("evidenceRun").orNull
            ?: throw GradleException("-PevidenceRun=<new-run-name>을 지정하세요")
        require(Regex("[a-z0-9][a-z0-9-]{0,63}").matches(run)) { "잘못된 evidenceRun" }
        args(run)
    }
}
