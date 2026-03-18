<property name="typeAliasesPackage" value="${entityPackage!}"/>
		<!-- pagehelper插件配置 -->
        <property name="plugins">
            <array>
                <bean class="com.github.pagehelper.PageInterceptor">
                    <property name="properties">
                        <value>
                            reasonable=true
                            supportMethodsArguments=true
                        </value>
                    </property>
                </bean>
            </array>
        </property>
	</bean>

	<!-- 自动扫描mapper配置和dao映射，无需写mybatis-config.xml -->
	<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
		<property name="basePackage" value="${mapperPackage!}" />
		<property name="sqlSessionFactory" ref="sqlSessionFactory"/>
	</bean>