<build>
		<plugins>
			<!-- 编译插件：设置编译版本、编码 -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.3</version>
				<configuration>
					<source><#noparse>${maven.compiler.source}</#noparse></source>
					<target><#noparse>${maven.compiler.target}</#noparse></target>
					<encoding><#noparse>${project.build.sourceEncoding}</#noparse></encoding>
				</configuration>
			</plugin>
<#if springboot_version?has_content>
			<!-- Spring Boot 插件 -->
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<fork>true</fork>
				</configuration>
			</plugin>
</#if>
		</plugins>
	</build>