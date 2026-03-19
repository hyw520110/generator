<dependencyManagement>
		<dependencies>
			<!-- spring boot -->
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-dependencies</artifactId>
				<version><#noparse>${spring-boot.version}</#noparse></version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<!-- spring cloud alibaba -->
			<dependency>
				<groupId>com.alibaba.cloud</groupId>
				<artifactId>spring-cloud-alibaba-dependencies</artifactId>
				<version><#noparse>${spring-cloud-alibaba.version}</#noparse></version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-build-dependencies</artifactId>
				<version><#noparse>${spring-cloud-build.version}</#noparse></version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<!-- spring cloud zookeeper -->
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-starter-zookeeper-config</artifactId>
				<version><#noparse>${spring-cloud-zookeeper.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
				<version><#noparse>${spring-cloud-zookeeper.version}</#noparse></version>
			</dependency>
			<!-- apache curator -->
			<dependency>
				<groupId>org.apache.curator</groupId>
				<artifactId>curator-framework</artifactId>
				<version><#noparse>${curator.version}</#noparse></version>
			</dependency>
			<!-- dubbo -->
			<dependency>
				<groupId>org.apache.dubbo</groupId>
				<artifactId>dubbo-bom</artifactId>
				<version><#noparse>${dubbo.version}</#noparse></version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<!-- mybatis -->
			<dependency>
				<groupId>org.mybatis.spring.boot</groupId>
				<artifactId>mybatis-spring-boot-starter</artifactId>
				<version>2.1.0</version>
			</dependency>
			<!-- 分页插件 -->
			<dependency>
				<groupId>com.github.pagehelper</groupId>
				<artifactId>pagehelper-spring-boot-starter</artifactId>
				<version>1.2.10</version>
			</dependency>
			<!-- druid 数据库连接池 -->
			<dependency>
				<groupId>com.alibaba</groupId>
				<artifactId>druid-spring-boot-starter</artifactId>
				<version>1.1.18</version>
			</dependency>
			<!-- mysql -->
			<dependency>
				<groupId>mysql</groupId>
				<artifactId>mysql-connector-java</artifactId>
				<version>6.0.6</version>
			</dependency>
			<!-- swagger -->
			<dependency>
				<groupId>io.springfox</groupId>
				<artifactId>springfox-swagger2</artifactId>
				<version><#noparse>${swagger.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>io.springfox</groupId>
				<artifactId>springfox-bean-validators</artifactId>
				<version><#noparse>${swagger.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>io.springfox</groupId>
				<artifactId>springfox-swagger-ui</artifactId>
				<version><#noparse>${swagger.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>com.github.xiaoymin</groupId>
				<artifactId>swagger-bootstrap-ui</artifactId>
				<version><#noparse>${swagger.ui.version}</#noparse></version>
			</dependency>
			<!-- 其他 -->
			<dependency>
				<groupId>com.lmax</groupId>
				<artifactId>disruptor</artifactId>
				<version>3.4.2</version>
			</dependency>
			<dependency>
				<groupId>io.jsonwebtoken</groupId>
				<artifactId>jjwt</artifactId>
				<version><#noparse>${jwt.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>org.apache.shiro</groupId>
				<artifactId>shiro-spring</artifactId>
				<version><#noparse>${shiro.version}</#noparse></version>
			</dependency>
			<!-- lombok -->
			<dependency>
				<groupId>org.projectlombok</groupId>
				<artifactId>lombok</artifactId>
				<version>1.18.20</version>
			</dependency>
<#if mapperType?? && mapperType == "plus">
			<!-- mybatis-plus -->
			<dependency>
				<groupId>com.baomidou</groupId>
				<artifactId>mybatis-plus-core</artifactId>
				<version><#noparse>${mybatis.plus.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>com.baomidou</groupId>
				<artifactId>mybatis-plus-generator</artifactId>
				<version><#noparse>${mybatis.plus.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>com.baomidou</groupId>
				<artifactId>mybatis-plus-extension</artifactId>
				<version><#noparse>${mybatis.plus.version}</#noparse></version>
			</dependency>
			<dependency>
				<groupId>com.baomidou</groupId>
				<artifactId>mybatis-plus-boot-starter</artifactId>
				<version><#noparse>${mybatis.plus.version}</#noparse></version>
			</dependency>
</#if>
		</dependencies>
	</dependencyManagement>