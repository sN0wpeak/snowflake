# snowflake
分布式全局ID生成器，支持32位和64位ID生成。基于zookeeper实现的分布式选举和ID存储。
# 背景
由于数据库的表越来越大，所以说必须要分库分表了。在分库分表的时候会遇到一个必须解决的问题，就是分布式服务的情况下，自增ID的生成。有几种有效的策略可以解决这个问题。

1. 数据库：为每一个需要分表的业务在数据库中建立一个ID序列表，然后插入一条数据，通过LAST_INSERT_ID来获取分配好的ID。如果使用psql或者oracle的话，可以直接使用seq.
优点: 简单
缺点: 单点故障，数据库本身并发性能和连接数限制。

2. redis: 类似于数据库

# ID生成器 (待完善)
## 64位
ID生成策略采用twitter的[snowflake](https://github.com/twitter/snowflake)，本系统在此之上提供了自动分配workId的功能。
### 自动分配workId流程

## 32位
首先为不同的业务场景在zk中分配对应的sequence,服务本身每次去申请一段sequence缓存到本地，为客户端提供ID服务。

# 项目结构
1. snowflake-client 
客户端，用于和server通讯。
2. snowflake-core
公共核心类存放处。
3. snowflake-server
snowflakeServer，提供全局ID生成服务。


# quick-start
### 1. 部署安装zookeeper
### 2. 修改配置:
* 修改zkhosts，配置snowflake-server中的application-local.properties
``` bash
snowflake.zk.hosts=127.0.0.1:2181
```
* 修改服务端口（也可以启动的时候指定），配置snowflake-server中的application.properties
``` bash
server.port=8080
```
* 编译代码
```bash
mvn clean package -U -DskipTests=True
```
* 启动server
```bash
cd snowflake-server/target
java -jar snowflake-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=online --server.port=8080
```

* 添加partner
partnerKey、partnerSecret，useAuth用于客户端验证。start和rangeCount用于32位id生成，start相当于自增ID的起始ID，rangeCount是缓存区间。
```bash
curl http://127.0.0.1:8080/api/snowflake/add-biz -H 'Content-Type:application/json;charset=UTF-8' -d '{"partnerKey":"account","partnerSecret":"Xr&2Rd@1Ng","rangeCount":10000,"start":44900000,"useAuth":true}'
```


* test
```bash
# 64位ID获取
curl http://127.0.0.1:8080/api/snowflake/get-id?partnerKey=account

{"code":0,"data":{"id":126406183027220480},"msg":""}

# 32位ID获取
curl http://127.0.0.1:8080/api/snowflake/get-id32?partnerKey=account

{"code":0,"data":{"id":48150001},"msg":""}

```

# 集群部署
1. nginx
最简单的方式是使用nginx做负载均衡。

2. 注册中心方式 (待完善)