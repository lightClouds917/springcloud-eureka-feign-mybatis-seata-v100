# seata 1.2.0整合教程

东半球最好用的分布式事务框架seata 1.2.0正式发布，本文将详细的介绍在SpringCloud全家桶技术选型下，如何快速整合使用seata.

如有问题请留issue，或者加微信：w1186355422反馈。

### 最常见问题

#### 1.no available service 'default' found或no available service 'null' found

99%的可能是client和server的事务分组配置不一致导致的！！！先检查事务分组配置！！！

#### 2.发生异常事务不会滚

比如A-B,C的场景，B或者C异常，全局却没回滚。

- 1.确保A,B,C都成功的整合了seata,被调用方也是需要纳入seata管理的。
- 2.如果服务都成功纳入全局管理，那么发起方是有全局事务开启日志，被调用方是有注册分支事务日志的。如果没有，看1.
- 3.如果1，2都没问题，检查被调用方的异常是否是被拦截掉了，比如全局异常拦截，这样发起方是感知不到异常的，就不会回滚。
- 4.可能数据源代理有问题，不同的版本有点区别，但是1.2.0都可以自动代理了，或者配合注解或者配合配置项。

其他问题，请先看官方的faq文档：https://seata.io/zh-cn/docs/overview/faq.html

事务分组详解：https://seata.io/zh-cn/docs/user/transaction-group.html

##### 第一次整合，大部分问题都是配置不对，配置不对的情况下大部分又是事务分组不对！！！可以先自己检查下。

##### 学会有效提问，比如版本，比如场景，比如技术选型，不要开局一张图或者200行日志，然后等着别人一句一句追问，问你版本，问你调用链，问你技术选型！！！

##### 学会搜群聊天记录，你遇到的问题，解决方案可能在之前的聊天记录里，搜关键词去看记录也许可以找到答案！

##### 本教程基于seata 1.2.0版本

### 技术选型及版本

springcloud：Greenwich.SR1

spring-cloud-alibaba：2.1.1.RELEASE

spring-cloud-alibaba-seata：2.2.0.RELEASE

seata-spring-boot-starter：1.2.0

springboot：2.2.0.RELEASE

mybatis-spring-boot-starter.version：2.0.0

java：jdk8


### 1.启动seata server

下载包：https://github.com/seata/seata/releases

解压：tar -xzvf seata-server-1.2.0.tar.gz

修改配置：

#### 1.配置registry.conf

```java
//注册中心配置
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "eureka"
  eureka {
    serviceUrl = "http://192.xx.xx.xx:8761/eureka"
    application = "fsp_tx" //tc注册时服务名
    weight = "1"
  }

}

//配置中心配置
config {
  # file、nacos 、apollo、zk、consul、etcd3
  type = "file"
  file {
    name = "file.conf"
  }
}

```
注册中心和配置中心均支持多种。

#### 2.配置file.conf

```java
## transaction log store, only used in seata-server
store {
  ## store mode: file、db
  mode = "db"

  ## database store property
  db {
    ## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp) etc.
    datasource = "druid"
    ## mysql/oracle/postgresql/h2/oceanbase etc.
    dbType = "mysql"
    driverClassName = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://192.168.173.95:3306/seata-server" //tc的数据库,可自定义命名，对应就好
    user = "mysql"
    password = "runlion@123"
    minConn = 5
    maxConn = 30
    globalTable = "global_table"
    branchTable = "branch_table"
    lockTable = "lock_table"
    queryLimit = 100
    maxWait = 5000
  }
}

```

#### 3.建表

全局事务过程中，会涉及3块内容：

- 全局事务 global_table
- 分支事务 branch_table
- 全局锁  lock_table 

脚本在：https://github.com/seata/seata/tree/1.2.0/script，请选择对应数据库和对应版本！

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

高可用部署可参考：https://seata.io/zh-cn/docs/ops/deploy-ha.html

### 2.client端引入seata依赖

我们这里以引入spring-cloud-alibaba-seata 依赖为例，下面仅展示与seata相关的依赖：

```java
<!--seata-->
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
  <version>2.2.0.RELEASE</version>
//如果内嵌最新版本starter，我们不用排除再引入  
  <exclusions>
    <exclusion>
      <groupId>io.seata</groupId>
      <artifactId>seata-spring-boot-starter</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<dependency>
  <groupId>io.seata</groupId>
  <artifactId>seata-spring-boot-starter</artifactId>
  <version>1.2.0</version>
</dependency>
```

### 3.client端引入配置文件

seata server端和client端的脚本和配置，都汇总在这里，client端整合时，去找对应版本和技术选型的文件复制过来修改。

https://github.com/seata/seata/tree/1.2.0/script

#### 1.修改配置文件

1.1.0开始，支持Springboot的配置文件application.yml,我们不用再单独创建file.conf和registry.conf，极大的解放了生产力。
这里仅贴上我们快速启动的必要配置，全配置项，请参考此项目配置文件。

注意:很多配置我们采用默认值即可，入门测试时需要关注的配置项并不多，每个配置项的作用请参考：https://seata.io/zh-cn/docs/user/configurations.html
```java
# -----------seata--------------
seata:
    enabled: true
    application-id: storage-server #服务名
    tx-service-group: default # default是自定义的事务分组名称
    enable-auto-data-source-proxy: true # 启用自动数据源代理
    use-jdk-proxy: false
    service:
        vgroup-mapping:
            default: fsp_tx # default是自定义的事务分组名称，fsp_tx是tc注册到注册中心的服务名称
        #        grouplist:
        #            default: 127.0.0.1:8091 # 	仅注册中心为file时使用
        enable-degrade: false # 是否启用降级
        disable-global-transaction: false # 是否禁用全局事务
    config:
        type: file # 配置中心为file模式
    registry:
        type: eureka # 注册中心为eureka
        eureka:
            weight: 1
            service-url: http://192.168.173.95:8761/eureka # 注册中心地址

# -----------seata--------------

```
#### 请注意server和client端的事务分组配置一致！！！
#### 请注意server和client端的事务分组配置一致！！！
#### 请注意server和client端的事务分组配置一致！！！
#### 请注意server和client端的事务分组配置一致！！！
#### 请注意server和client端的事务分组配置一致！！！
#### 请注意server和client端的事务分组配置一致！！！
！！！这里强调1万遍！！！不懂事务分组配置作用的请仔细读读：https://seata.io/zh-cn/docs/user/transaction-group.html
事务分组配置不对，会出现类似
```java
no available service 'null' found......
```
```java
no available service 'default' found......
```

### 7.建表

如果要使用seata分布式事务，当前服务就需要建一张undolog表。

建表语句参考：https://github.com/seata/seata/tree/1.2.0/script

### 8.使用

- 1.@GlobalTransaction 全局事务注解
- 2.@GlobalLock 防止脏读和脏写，又不想纳入全局事务管理时使用。（不需要rpc和xid传递等成本）

### 9.典型日志

##### 如果发现数据不一致，可以参考下面的一些典型日志，或者查找关键字，比如Successfully begin global transaction，Successfully register branch xid = 。。。可以辅助你确认你的配置是否正确有效，各分支事务是否注册，纳入全局管理。

#### 1.client端正常启动日志
server端日志：
```java
2020-04-21 20:28:49.192 INFO [ServerHandlerThread_1_500]io.seata.core.rpc.DefaultServerMessageListenerImpl.onRegRmMessage:127 -RM register success,message:RegisterRMRequest{resourceIds='jdbc:mysql://116.xx.xx.xx/seata-order', applicationId='order-server', transactionServiceGroup='default'},channel:[id: 0x4e101b0b, L:/192.168.158.247:8091 - R:/192.168.158.80:7482]
2020-04-21 20:29:43.532 INFO [NettyServerNIOWorker_1_8]io.seata.core.rpc.DefaultServerMessageListenerImpl.onRegTmMessage:153 -TM register success,message:RegisterTMRequest{applicationId='order-server', transactionServiceGroup='default'},channel:[id: 0x82ea0fcd, L:/192.168.158.247:8091 - R:/192.168.158.80:7529]

```
client日志：
```java
有删减
......
2020-04-21 20:28:43.824  INFO 26032 --- [           main] .s.s.a.d.SeataAutoDataSourceProxyCreator : Auto proxy of [dataSource]
......
2020-04-21 20:28:46.879  INFO 26032 --- [           main] i.s.c.r.netty.NettyClientChannelManager  : will connect to 192.168.158.247:8091
2020-04-21 20:28:46.879  INFO 26032 --- [           main] io.seata.core.rpc.netty.RmRpcClient      : RM will register :jdbc:mysql://116.62.62.26/seata-order
2020-04-21 20:28:46.882  INFO 26032 --- [           main] i.s.core.rpc.netty.NettyPoolableFactory  : NettyPool create channel to transactionRole:RMROLE,address:192.168.158.247:8091,msg:< RegisterRMRequest{resourceIds='jdbc:mysql://116.xx.xx.xx/seata-order', applicationId='order-server', transactionServiceGroup='default'} >
2020-04-21 20:28:48.548  INFO 26032 --- [           main] io.seata.core.rpc.netty.RmRpcClient      : register RM success. server version:1.2.0,channel:[id: 0xa6491f48, L:/192.168.158.80:7482 - R:/192.168.158.247:8091]
2020-04-21 20:28:48.557  INFO 26032 --- [           main] i.s.core.rpc.netty.NettyPoolableFactory  : register success, cost 102 ms, version:1.2.0,role:RMROLE,channel:[id: 0xa6491f48, L:/192.168.158.80:7482 - R:/192.168.158.247:8091]
2020-04-21 20:28:50.848  INFO 26032 --- [           main] o.s.c.n.eureka.InstanceInfoFactory       : Setting initial instance status as: STARTING
......
2020-04-21 20:29:42.845  INFO 26032 --- [imeoutChecker_1] io.seata.core.rpc.netty.TmRpcClient      : register TM success. server version:1.2.0,channel:[id: 0xb872c071, L:/192.168.158.80:7529 - R:/192.168.158.247:8091]
2020-04-21 20:29:42.845  INFO 26032 --- [imeoutChecker_1] i.s.core.rpc.netty.NettyPoolableFactory  : register success, cost 7 ms, version:1.2.0,role:TMROLE,channel:[id: 0xb872c071, L:/192.168.158.80:7529 - R:/192.168.158.247:8091]


```

#### 2.全局事务提交成功,server端会有日志如下：
```java
2020-04-21 20:36:30.992 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage timeout=60000,transactionName=fsp_create_order
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:36:30.993 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCoordinator.doGlobalBegin:159 -Begin new global transaction applicationId: order-server,transactionServiceGroup: default, transactionName: fsp_create_order,timeout:60000,xid:192.168.158.247:8091:2009659537
2020-04-21 20:36:31.026 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage xid=192.168.158.247:8091:2009659537,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-order,lockKey=order:1226
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:36:31.031 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.AbstractCore.lambda$branchRegister$0:87 -Register branch successfully, xid = 192.168.158.247:8091:2009659537, branchId = 2009659539, resourceId = jdbc:mysql://116.62.62.26/seata-order ,lockKeys = order:1226
2020-04-21 20:36:31.108 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage xid=192.168.158.247:8091:2009659537,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-storage,lockKey=storage:1
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:36:31.113 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.AbstractCore.lambda$branchRegister$0:87 -Register branch successfully, xid = 192.168.158.247:8091:2009659537, branchId = 2009659541, resourceId = jdbc:mysql://116.62.62.26/seata-storage ,lockKeys = storage:1
2020-04-21 20:36:31.192 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage xid=192.168.158.247:8091:2009659537,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-account,lockKey=account:1
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:36:31.200 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.AbstractCore.lambda$branchRegister$0:87 -Register branch successfully, xid = 192.168.158.247:8091:2009659537, branchId = 2009659543, resourceId = jdbc:mysql://116.62.62.26/seata-account ,lockKeys = account:1
2020-04-21 20:36:31.235 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage xid=192.168.158.247:8091:2009659537,extraData=null
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:36:31.788 INFO [AsyncCommitting_1]io.seata.server.coordinator.DefaultCore.doGlobalCommit:240 -Committing global transaction is successfully done, xid = 192.168.158.247:8091:2009659537.

```

#### 3.全局事务提交失败,server端会有日志如下：
```java
2020-04-21 20:35:09.036 INFO [NettyServerNIOWorker_1_8]io.seata.core.rpc.DefaultServerMessageListenerImpl.onRegTmMessage:153 -TM register success,message:RegisterTMRequest{applicationId='order-server', transactionServiceGroup='default'},channel:[id: 0x1531fc7f, L:/192.168.158.247:8091 - R:/192.168.158.80:7822]
2020-04-21 20:35:09.041 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage timeout=60000,transactionName=fsp_create_order
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:35:09.043 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCoordinator.doGlobalBegin:159 -Begin new global transaction applicationId: order-server,transactionServiceGroup: default, transactionName: fsp_create_order,timeout:60000,xid:192.168.158.247:8091:2009659516
2020-04-21 20:35:09.456 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage xid=192.168.158.247:8091:2009659516,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-order,lockKey=order:1224
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:35:09.462 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.AbstractCore.lambda$branchRegister$0:87 -Register branch successfully, xid = 192.168.158.247:8091:2009659516, branchId = 2009659518, resourceId = jdbc:mysql://116.62.62.26/seata-order ,lockKeys = order:1224
2020-04-21 20:35:10.382 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage xid=192.168.158.247:8091:2009659516,branchType=AT,resourceId=jdbc:mysql://116.62.62.26/seata-storage,lockKey=storage:1
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:35:10.388 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.AbstractCore.lambda$branchRegister$0:87 -Register branch successfully, xid = 192.168.158.247:8091:2009659516, branchId = 2009659521, resourceId = jdbc:mysql://116.62.62.26/seata-storage ,lockKeys = storage:1
2020-04-21 20:35:10.721 INFO [batchLoggerPrint_1]io.seata.core.rpc.DefaultServerMessageListenerImpl.run:214 -SeataMergeMessage xid=192.168.158.247:8091:2009659516,extraData=null
,clientIp:192.168.158.80,vgroup:default
2020-04-21 20:35:10.862 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.doGlobalRollback:290 -Rollback branch transaction  successfully, xid = 192.168.158.247:8091:2009659516 branchId = 2009659521
2020-04-21 20:35:10.967 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.doGlobalRollback:290 -Rollback branch transaction  successfully, xid = 192.168.158.247:8091:2009659516 branchId = 2009659518
2020-04-21 20:35:10.971 INFO [ServerHandlerThread_1_500]io.seata.server.coordinator.DefaultCore.doGlobalRollback:334 -Rollback global transaction successfully, xid = 192.168.158.247:8091:2009659516.

```


