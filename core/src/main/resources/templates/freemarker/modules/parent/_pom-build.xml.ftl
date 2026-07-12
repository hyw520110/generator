<build>
		<plugins>
			<!-- 编译插件：设置编译版本、编码 -->
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-compiler-plugin</artifactId>
					<version>3.13.0</version>
					<configuration>
						<release><#noparse>${maven.compiler.release}</#noparse></release>
						<encoding><#noparse>${project.build.sourceEncoding}</#noparse></encoding>
					</configuration>
				</plugin>
<#if springboot_version?has_content>
			<!-- Spring Boot 插件 -->
				<plugin>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-maven-plugin</artifactId>
					<version><#noparse>${spring-boot.version}</#noparse></version>
					<configuration>
						<mainClass><#noparse>${mainClass}</#noparse></mainClass>
					<#if (springBootMajor!'3')?string != '4'>
						<fork>true</fork>
					</#if>
					</configuration>
			</plugin>
</#if>
		</plugins>
	</build>
