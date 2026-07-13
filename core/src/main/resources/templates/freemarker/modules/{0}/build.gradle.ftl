<#if "GRADLE"=="${global.projectBuilder}">
apply plugin: 'java'
apply plugin: 'eclipse'

group = '${rootPackage!}'
version = '${version!}'

description = ''

sourceCompatibility = JavaVersion.VERSION_${(bytecodeRelease!javaVersion!)?replace(".", "_")}
targetCompatibility = JavaVersion.VERSION_${(bytecodeRelease!javaVersion!)?replace(".", "_")}
tasks.withType(JavaCompile) {
	options.encoding = 'UTF-8'
}



repositories {
     mavenCentral()
}
dependencies {
    implementation group: 'org.apache.commons', name: 'commons-lang3', version:'3.14.0'
    testImplementation group: 'org.junit.jupiter', name: 'junit-jupiter', version:'5.10.2'
}
</#if>
