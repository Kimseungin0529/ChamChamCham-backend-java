plugins {
	java
	id("org.springframework.boot") version "3.5.4" apply false
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.chamchamcham"
version = "0.0.1-SNAPSHOT"
description = "Server project for Spring Boot"

allprojects {
	repositories {
		mavenCentral()
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
