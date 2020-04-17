# seata 1.0入门教程

##### 本教程基于seata 1.0.0版本

### 技术选型及版本
spring-cloud-starter-alibaba-seata

spring-cloud-alibaba：1.5.1.RELEASE

springcloud：Edgware.SR4

seata-all：1.0.0

springboot：1.5.4

mybatis-spring-boot-starter.version：2.0.0

java：jdk8


### 1.启动seata server

下载包：https://github.com/seata/seata/releases

解压：tar -xzvf seata-server-1.0.0.tar.gz

修改配置：

#### 1.配置registry.conf

```java
[root@jr-test conf]# cat registry.conf 
//注册中心
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "eureka" //注册中心类型

  nacos {
    serverAddr = "localhost"
    namespace = ""
    cluster = "default"
  }
  eureka {
    serviceUrl = "http://192.xx.xx.xx:8761/eureka" //注册中心地址
    application = "fsp_tx" //tc注册时的名称
    weight = "1"
  }
  //......支持多种
}
//配置中心
config {
  # file、nacos 、apollo、zk、consul、etcd3
  type = "file" //配置中心类型
  file {
    name = "file.conf" //
  }
  //......支持多种
}

```

#### 2.配置file.conf

```java
[root@jr-test conf]# cat file.conf
service {
  #transaction service group mapping
  vgroup_mapping.default = "fsp_tx"  //事务分组，非常重要，client和tc一定要一致，default是个自定义的分组名称
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #disable seata
  disableGlobalTransaction = false
}

## transaction log store, only used in seata-server
store {
  ## store mode: file、db
  mode = "db" //事务日志存储模式

  ## file store property
  file {
    ## store location dir
    dir = "sessionStore"
  }

  ## database store property
  db {
    ## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp) etc.
    datasource = "dbcp"
    ## mysql/oracle/h2/oceanbase etc.
    db-type = "mysql"
    driver-class-name = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://192.xx.xx.xx:3306/seata" // tc的数据库,可自定义命名，对应就好
    user = "root"
    password = "xxx"
  }
}
```

#### 3.建表

全局事务会话信息由3块内容构成：

- 全局事务 global_table
- 分支事务 branch_table
- 全局锁  lock_table 

建表语句在：https://github.com/seata/seata/tree/develop/script/server/db

#### 4.启动tc

bin目录下：

```java
nohup sh seata-server.sh -h xx.xx.xx.xx -p 8091 -m db -n 1 &
```

这里是以nohup的方式后台启动，参数可选：

-h: 注册到注册中心的ip

 -p: Server rpc 监听端口 

-m: 全局事务会话信息存储模式，file、db，优先读取启动参数 

-n: Server node，多个Server时，需区分各自节点，用于生成不同区间的transactionId，以免冲突 

-e: 多环境配置参考 http://seata.io/en-us/docs/ops/multi-configuration-isolation.html 

### 2.client端引入seata依赖

目前有三种方式，相应的支持程度不同：

| 依赖                       | 支持yml配置 | 实现xid传递 | 支持数据源自动代理 | 自动初始化GlobalTransactionScanner入口 |
| -------------------------- | ----------- | ----------- | ------------------ | -------------------------------------- |
| seata-all                  | 否          | 否          | 是                 | 否                                     |
| seata-spring-boot-starter  | 是          | 否          | 是                 | 是                                     |
| spring-cloud-alibaba-seata | 否          | 是          | 是                 | 是                                     |

不建议用户仅引入seata-all，需要自行实现的东西太多。

spring-cloud-alibaba-seata，2.1.0内嵌seata-all 0.7.1，2.1.1内嵌seata-all 0.9.0。建议排除掉，引入1.0；

我们这里以引入spring-cloud-alibaba-seata 依赖为例，下面仅展示与seata相关的依赖：

```java
	<properties>
		<spring-cloud.version>Edgware.SR4</spring-cloud.version>
		<spring-cloud-alibaba.version>1.5.1.RELEASE</spring-cloud-alibaba.version>
		<seata-version>1.0.0</seata-version>
	</properties>
	<dependencies>
		<!--seata-all-->
		<dependency>
			<groupId>io.seata</groupId>
			<artifactId>seata-all</artifactId>
			<version>${seata-version}</version>
		</dependency>
		<!--sca-seata-->
		<dependency>
			<groupId>com.alibaba.cloud</groupId>
			<artifactId>spring-cloud-starter-alibaba-seata</artifactId>
		</dependency>
	</dependencies>

	<dependencyManagement>
		<dependencies>
			<!--Spring Cloud-->
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<!--Spring Cloud Alibaba，包含seata-all 0.9,这里排除掉，换为1.0-->
			<dependency>
				<groupId>com.alibaba.cloud</groupId>
				<artifactId>spring-cloud-alibaba-dependencies</artifactId>
				<version>${spring-cloud-alibaba.version}</version>
				<type>pom</type>
				<scope>import</scope>
				<exclusions>
					<exclusion>
						<artifactId>seata-all</artifactId>
						<groupId>io.seata</groupId>
					</exclusion>
				</exclusions>
			</dependency>
		</dependencies>
	</dependencyManagement>
```



### 3.client端引入配置文件

seata server端和client端的脚本和配置，都汇总在这里，client端整合时，去找对应的文件复制过来修改。

https://github.com/seata/seata/tree/develop/script

#### 1.修改bootstrap.yml

添加事务分组

```java
spring:
  cloud:
    alibaba:
      seata:
        tx-service-group: default //这个default是事务分组名称，与server端的事务分组名称保持一致
```

这个分组名称自定义，但是seata server端，client端要保持一致。

#### 2.配置file.conf

引入配置文件，并修改相关配置，由于有很多配置项，不是每个都需要去改一遍，这里只是改动少数，让你先能整合进去跑起来，其他的参数后面再自己调整。

file.conf

```java
//省略很多
service {
  #transaction service group mapping
  vgroup_mapping.default = "fsp_tx"  //这个default是事务分组名称，与server端的事务分组名称保持一致
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #degrade, current not support
  enableDegrade = false
  #disable seata
  disableGlobalTransaction = false
}
client {
    //省略很多
  rm {
    report.success.enable = true //一阶段成功后是否上报tc，这个配置可以提高性能
  }
  support {
    # auto proxy the DataSource bean //数据源自动代理
    spring.datasource.autoproxy = false
  }
}

```

#### 3.配置registry.conf

注册中心和配置中心，都支持多种，按照自己的技术选型，修改对应的配置。

```java
registry {
  # file 、nacos 、eureka、redis、zk
  type = "eureka"
  eureka {
    serviceUrl = "http://192.xx.xx.xx:8761/eureka"
    application = "fsp_tx" 
    weight = "1"
  }
    //省略
}

config {
  # file、nacos 、apollo、zk
  type = "file"
  file {
    name = "file.conf"
  }
    //省略
}

```

### 4.配置数据源头代理

如果支持自动代理，那就开启配置就好，这里示例下使用mysql和mybatis时，如何自行代理数据源。

```java
package io.seata.sample;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 数据源代理
 * @author IT云清
 */
@Configuration
public class DataSourceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource druidDataSource(){
        DruidDataSource druidDataSource = new DruidDataSource();
        return druidDataSource;
    }

    @Primary
    @Bean("dataSource")
    public DataSourceProxy dataSource(DataSource druidDataSource){
        return new DataSourceProxy(druidDataSource);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSourceProxy dataSourceProxy)throws Exception{
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSourceProxy);
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/*.xml"));
        sqlSessionFactoryBean.setTransactionFactory(new SpringManagedTransactionFactory());
        return sqlSessionFactoryBean.getObject();
    }
}

```



### 5.实现xid传递

如果你引入的依赖和技术选型，没有实现xid传递等逻辑，你需要参考源码integration文件夹下的各种rpc实现 module。

https://github.com/seata/seata/tree/develop/integration

### 6.实现scanner入口

如果你引入的依赖和技术选型，没有实现初始化GlobalTransactionScanner逻辑，可以自行实现如下：

1.SeataProperties.java

```java
@ConfigurationProperties("spring.cloud.alibaba.seata")
public class SeataProperties {
    private String txServiceGroup;
    public SeataProperties() {
    }
    public String getTxServiceGroup() {
        return this.txServiceGroup;
    }
    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }
}
```

2.初始化GlobalTransactionScanner

```java
package com.runlion.fsp.credit.seata.config;

import com.runlion.fsp.credit.seata.SeataProperties;
import io.seata.spring.annotation.GlobalTransactionScanner;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author IT云清
 */
@Configuration
@EnableConfigurationProperties({SeataProperties.class})
public class GlobalTransactionAutoConfiguration {
    private static final String APPLICATION_NAME_PREFIX = "spring.application.name";
    private static final String DEFAULT_TX_SERVICE_GROUP_SUFFIX = "-seata-service-group";
    private final ApplicationContext applicationContext;
    private final SeataProperties seataProperties;
    public GlobalTransactionAutoConfiguration(
            ApplicationContext applicationContext,
            SeataProperties seataProperties) {
        this.applicationContext = applicationContext;
        this.seataProperties = seataProperties;
    }

    /**
     * If there is no txServiceGroup,use the default
     * @return GlobalTransactionScanner the entrance
     */
    @Bean
    public GlobalTransactionScanner globalTransactionScanner(){
        String applicationName = this.applicationContext.getEnvironment().getProperty(APPLICATION_NAME_PREFIX);
        String txServiceGroup = seataProperties.getTxServiceGroup();
        if(StringUtils.isEmpty(txServiceGroup)){
            txServiceGroup = applicationName + DEFAULT_TX_SERVICE_GROUP_SUFFIX;
            this.seataProperties.setTxServiceGroup(txServiceGroup);
        }
        return new GlobalTransactionScanner(applicationName,txServiceGroup);
    }
}

```

### 7.建表

如果要使用seata分布式事务，当前服务就需要建一张undolog表。

建表语句参考：https://github.com/seata/seata/tree/develop/script/client/at/db

### 8.使用

- 1.@GlobalTransaction 全局事务注解
- 2.@GlobalLock 防止脏读和脏写，又不想纳入全局事务管理时使用。（不需要rpc和xid传递等成本）

### 9.辅助信息

##### 如果发现数据不一致，可以参考下面的一些典型日志，或者查找关键字，比如Successfully begin global transaction，Successfully register branch xid = 。。。可以辅助你确认你的配置是否正确有效，各分支事务是否注册，纳入全局管理。

#### 1.client端如果正常启动，在server端会有如下日志，否则，请再检查配置：
```java
2019-12-31 15:38:44.170 INFO [ServerHandlerThread_1_500]io.seata.core.rpc.DefaultServerMessageListenerImpl.onRegRmMessage:123 -rm register success,message:RegisterRMRequest{resourceIds='jdbc:mysql://116.xx.xx.xx/seata-account', applicationId='account-server', transactionServiceGroup='default'},channel:[id: 0xb58488ac, L:/192.xx.xx.xx:8091 - R:/192.xx.xx.xx:13641]
2019-12-31 15:38:44.968 INFO [NettyServerNIOWorker_1_8]io.seata.core.rpc.DefaultServerMessageListenerImpl.onRegTmMessage:140 -checkAuth for client:192.xx.xx.xx:13644,vgroup:default,applicationId:account-server

```

#### 2.全局事务提交成功,server端会有日志如下：
```java
2019-12-31 16:00:31.209 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage timeout=60000,transactionName=fsp-create-order
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:31.211 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.begin:154 -Successfully begin global transaction xid = 192.xx.xx.xx:8091:2031075692
2019-12-31 16:00:31.253 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075692,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-order,lockKey=order:877
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:31.257 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.lambda$branchRegister$0:98 -Successfully register branch xid = 192.xx.xx.xx:8091:2031075692, branchId = 2031075694
2019-12-31 16:00:31.278 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075692,branchId=2031075694,resourceId=null,status=PhaseOne_Done,applicationData=null
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:31.283 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.branchReport:126 -Successfully branch report xid = 192.xx.xx.xx:8091:2031075692, branchId = 2031075694
2019-12-31 16:00:31.339 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075692,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-storage,lockKey=storage:1
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:31.344 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.lambda$branchRegister$0:98 -Successfully register branch xid = 192.xx.xx.xx:8091:2031075692, branchId = 2031075697
2019-12-31 16:00:31.363 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075692,branchId=2031075697,resourceId=null,status=PhaseOne_Done,applicationData=null
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:31.366 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.branchReport:126 -Successfully branch report xid = 192.xx.xx.xx:8091:2031075692, branchId = 2031075697
2019-12-31 16:00:32.237 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075692,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-account,lockKey=account:1
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:32.242 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.lambda$branchRegister$0:98 -Successfully register branch xid = 192.xx.xx.xx:8091:2031075692, branchId = 2031075701
2019-12-31 16:00:32.294 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075692,branchId=2031075701,resourceId=null,status=PhaseOne_Done,applicationData=null
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:32.327 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.branchReport:126 -Successfully branch report xid = 192.xx.xx.xx:8091:2031075692, branchId = 2031075701
2019-12-31 16:00:32.368 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075692,extraData=null
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:00:32.775 INFO [AsyncCommitting_1]io.seata.server.coordinator.DefaultCore.doGlobalCommit:316 -Global[192.xx.xx.xx:8091:2031075692] committing is successfully done.

```

#### 3.全局事务提交失败,server端会有日志如下：
```java
2019-12-31 16:16:50.327 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage timeout=60000,transactionName=fsp-create-order
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:16:50.329 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.begin:154 -Successfully begin global transaction xid = 192.xx.xx.xx:8091:2031075709
2019-12-31 16:16:50.408 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075709,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-order,lockKey=order:878
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:16:50.412 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.lambda$branchRegister$0:98 -Successfully register branch xid = 192.xx.xx.xx:8091:2031075709, branchId = 2031075711
2019-12-31 16:16:50.432 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075709,branchId=2031075711,resourceId=null,status=PhaseOne_Done,applicationData=null
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:16:50.435 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.branchReport:126 -Successfully branch report xid = 192.xx.xx.xx:8091:2031075709, branchId = 2031075711
2019-12-31 16:16:50.520 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075709,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-storage,lockKey=storage:1
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:16:50.523 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.lambda$branchRegister$0:98 -Successfully register branch xid = 192.xx.xx.xx:8091:2031075709, branchId = 2031075714
2019-12-31 16:16:50.541 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075709,branchId=2031075714,resourceId=null,status=PhaseOne_Done,applicationData=null
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:16:50.544 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.branchReport:126 -Successfully branch report xid = 192.xx.xx.xx:8091:2031075709, branchId = 2031075714
2019-12-31 16:16:52.569 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:205 -SeataMergeMessage xid=192.xx.xx.xx:8091:2031075709,extraData=null
,clientIp:192.xx.xx.xx,vgroup:default
2019-12-31 16:16:52.626 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.doGlobalRollback:418 -Successfully rollback branch xid=192.xx.xx.xx:8091:2031075709 branchId=2031075714
2019-12-31 16:16:52.693 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.doGlobalRollback:418 -Successfully rollback branch xid=192.xx.xx.xx:8091:2031075709 branchId=2031075711
2019-12-31 16:16:52.696 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.doGlobalRollback:465 -Successfully rollback global, xid = 192.xx.xx.xx:8091:2031075709

```
