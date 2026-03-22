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
				<artifactId>spring-cloud-dependencies</artifactId>
				<version><#noparse>${spring-cloud.version}</#noparse></version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
<#if ZOOKEEPER?? && ZOOKEEPER>
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
</#if>
<#if DUBBO?? && DUBBO>
			<!-- dubbo -->
			<dependency>
				<groupId>org.apache.dubbo</groupId>
				<artifactId>dubbo-bom</artifactId>
				<version><#noparse>${dubbo.version}</#noparse></version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
</#if>
<#if ZOOKEEPER?? && ZOOKEEPER>
			<!-- sentinel datasource zookeeper -->
			<dependency>
				<groupId>com.alibaba.csp</groupId>
				<artifactId>sentinel-datasource-zookeeper</artifactId>
				<version><#noparse>${sentinel.version}</#noparse></version>
			</dependency>
</#if>
			<!-- mybatis -->
			<dependency>
				<groupId>org.mybatis.spring.boot</groupId>
				<artifactId>mybatis-spring-boot-starter</artifactId>
				<version><#noparse>${mybatis-spring-boot.version}</#noparse></version>
			</dependency>
			<!-- 分页插件 -->
			<dependency>
				<groupId>com.github.pagehelper</groupId>
				<artifactId>pagehelper-spring-boot-starter</artifactId>
				<version><#noparse>${pagehelper.version}</#noparse></version>
			</dependency>
			<!-- druid 数据库连接池 -->
			<dependency>
				<groupId>com.alibaba</groupId>
				<artifactId>druid-spring-boot-starter</artifactId>
				<version><#noparse>${druid.version}</#noparse></version>
			</dependency>
			<!-- mysql -->
			<dependency>
				<groupId>com.mysql</groupId>
				<artifactId>mysql-connector-j</artifactId>
				<version><#noparse>${mysql-connector.version}</#noparse></version>
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
				<version><#noparse>${disruptor.version}</#noparse></version>
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
				<classifier>jakarta</classifier>
			</dependency>
			<dependency>
				<groupId>org.apache.shiro</groupId>
				<artifactId>shiro-core</artifactId>
				<version><#noparse>${shiro.version}</#noparse></version>
				<classifier>jakarta</classifier>
			</dependency>
			<dependency>
				<groupId>org.apache.shiro</groupId>
				<artifactId>shiro-web</artifactId>
				<version><#noparse>${shiro.version}</#noparse></version>
				<classifier>jakarta</classifier>
			</dependency>
			<!-- lombok -->
			<dependency>
				<groupId>org.projectlombok</groupId>
				<artifactId>lombok</artifactId>
				<version><#noparse>${lombok.version}</#noparse></version>
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
				<artifactId>mybatis-plus-spring-boot3-starter</artifactId>
				<version><#noparse>${mybatis.plus.version}</#noparse></version>
			</dependency>
</#if>
		</dependencies>
	</dependencyManagement>