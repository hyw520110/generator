<#if "GRADLE"=="${global.projectBuilder}">
	apply plugin: 'java'
	//eclipse users only
	apply plugin: 'eclipse'
//apply plugin: 'idea'
//provided 依赖支持，方式三
//apply plugin: 'propdeps'

group = '${rootPackage!}'
version = '${version!}'

description = '${projectName!}-${moduleName!}'

	sourceCompatibility = JavaVersion.VERSION_${(bytecodeRelease!javaVersion!)?replace(".", "_")}
	targetCompatibility = JavaVersion.VERSION_${(bytecodeRelease!javaVersion!)?replace(".", "_")}
tasks.withType(JavaCompile) {
	options.encoding = 'UTF-8'
}

configurations {
    provided
}
//provided 依赖支持 (引入 eclipse 插件),方式一
sourceSets {
    main.compileClasspath += configurations.provided
    test.compileClasspath += configurations.provided
    test.runtimeClasspath += configurations.provided
}
//eclipse users only
eclipse.classpath.plusConfigurations += configurations.provided

//idea { module { scopes.PROVIDED.plus += [configurations.provided] } }

	repositories {
	     mavenCentral()
	}
	dependencies {
	    implementation group: '${rootPackage!}', name: '${projectName!}-${moduleName!}', version:'${version!}'
	    implementation group: 'org.mybatis', name: 'mybatis', version:'3.4.4'
<#if enableCache?has_content>
	    implementation group: 'net.sf.ehcache', name: 'ehcache', version:'2.8.3'
	    implementation group: 'org.mybatis.caches', name: 'mybatis-ehcache', version:'1.1.0'
</#if>
	    implementation group: 'org.springframework', name: 'spring-context', version:'4.3.9.RELEASE'
	    implementation group: 'org.springframework', name: 'spring-web', version:'4.3.9.RELEASE'
<#if "mysql"=="${dbType}">
	    runtimeOnly group: 'com.mysql', name: 'mysql-connector-j', version:'${mysql_connector_version!"8.0.33"}'
</#if>
    //方式一
    provided group: '${servletApiGroupId}', name: '${servletApiArtifactId}', version:'3.0-alpha-1'
    //方式二
//  compileOnly '${servletApiGroupId}:${servletApiArtifactId}:3.0-alpha-1'
    //方式三
//    provided('${servletApiGroupId}:${servletApiArtifactId}:3.0-alpha-1')
	    testImplementation group: 'org.junit.jupiter', name: 'junit-jupiter', version:'5.10.2'
	}
</#if>
