# ObjectStorage
An ObjectStorage based on netty. ObjectStorage APIs are partly compatible with Aliyun OSS API. 

## 1. License

**GPL v3.0**

## 2. Dependencies

### 2.1 Libraries
 + [netty-all](http://netty.io/)
 + log4j-api
 + log4j-core
 + log4j-slf4j-impl
 + [utils](https://github.com/zhangyf/util) 
 + json
 
### 2.2 Build-time requirement
 + Latest stable [Oracle JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
 + Latest stable [Apache Maven](http://maven.apache.org/)
 
## 3. How to Build

    mvn clean package

After building two jars will be generated under target directory:
    
    object-storage-[version]-jar-with-dependencies.jar
    object-storage-[version].jar
  

## 4. How to Use
### 4.1 Configuration

#### 4.1.1 Server Config


    <?xml version="1.0" encoding="UTF-8" ?>
    <configuration>
        <!-- bind server:port -->
        <bind>0.0.0.0:8484</bind>
    
        <!-- max request size in bytes -->
        <packageSize>65535</packageSize>
    
        <!-- 黑名单相关配置 -->
        <blackList>
            <!--<class>-->
            <!--<name>cn.zyf.protocols.impl.DefaultBlackListManager</name>-->
            <!--<params>-->
            <!--<paramName1 type="String" idx="0">paramValue</paramName1>-->
            <!--<paramName2 type="Integer" idx="1">10</paramName2>-->
            <!--<paramName3 type="Short" idx="2">20</paramName3>-->
            <!--<paramName4 type="Long" idx="3">30</paramName4>-->
            <!--<paramName5 type="Boolean" idx="4">false</paramName5>-->
            <!--</params>-->
            <!--</class>-->
            <path>xxxx</path>
            <!-- reload period in seconds -->
            <period>60</period>
        </blackList>
    
        <!-- 如果不使用外部认证，则不需要配置该项 -->
        <authentication>
            <!--
            <class>org.pacakge.classname</class>
            <params>
    
            </params>
            -->
        </authentication>
    
        <!-- 如果不使用外部鉴权，则不需要配置该项 -->
        <authorization>
            <!--
            <class>org.pacakge.classname</class>
            <params>
            </params>
            -->
        </authorization>
    
        <!-- 如果不使用外部cache，则不需要配置该项。xxx为cluster_conf.xml中已有的名字，且type仅为redis/memcached -->
        <cache>xxxx</cache>
    
        <!-- 设置系统的元数据信息存放的集群 -->
        <meta>
            <name>cluster1</name> <!-- cluster name in cluster_conf.xml  only hbase/cassandra/mysql/oracle -->
            <options>
                <dbName>db</dbName>
                <tableName>table</tableName>
                <cfName>cf</cfName>
                <cnName>cn</cnName>
                <ugi>ugi:ugi</ugi>
            </options>
        </meta>
    
    </configuration>

#### 4.1.2 Cluster Config

    <?xml version="1.0" encoding="UTF-8"?>
    <clusters>
        <cluster name="xxxx" type="cassandra">
            <node>hostname1:9160</node>
            <node>hostname2:9160</node>
            <node>hostname3:9160</node>
            <node>hostname4:9160</node>
        </cluster>

        <cluster name="xxxxx" type="hbase">
            <zookeeper>
                <zkHost>hostname1:2181,hostname2:2181,hostname3:2181</zkHost>
                <zkPath priority="3">/path/to/node</zkPath>
                <zkPath priority="2">/path/to/node</zkPath>
                <zkPath priority="1">/path/to/node</zkPath>
            </zookeeper>
        </cluster>

        <cluster name="xxxx" type="hdfs">
            <uri>hdfs://xxx.xxx.xxx.xxx:port</uri>
        </cluster>

        <cluster name="xxxx" type="redis/memcached">
            <node>hostname1:port,hostname2:port,hostname3:port</node>
        </cluster>
    </clusters>


### 4.2 Start ObjectStorageServer

#### 4.2.1 Basic cmd
**object-storage-[version]-jar-with-dependencies.jar** is an all-in-one jar file which contains object-server classes and all of the dependencies libraries. 

If you don't want to download the dependencies libraries, you could use it.

    java -cp object-storage-[version]-jar-with-dependencies.jar [optional-parameters] cn.zyf.ObjectStorageServer 


**object-storage-[version].jar** only contains ObjectStorage classes. If using it, you need download all the dependencies libraries according to **2.1 Libraries** and add them to classpath.

    java -cp object-storage-[version]-jar-with-dependencies.jar:[all-the-dependencies-libraries] [optional-parameters] cn.zyf.ObjectStorageServer

#### 4.2.2 optional parameters

 + -DserviceConfig=[/path/to/server/config/file]
 + -DclusterConfig=[/path/to/cluster/config/file]
 + -Dlog4j.configurationFile=[/path/to/log4j/config/file]


### 4.3 API
#### 4.3.1 Service API
#### 4.3.1.1 list buckets

#### 4.3.2 Bucket API
##### 4.3.2.1 create a bucket
##### 4.3.2.2 set bucket acl
##### 4.3.2.3 enable/disable bucket audit log
##### 4.3.2.4 set bucket website (unsupported temporarily)
##### 4.3.2.5 set bucket referer
##### 4.3.2.6 set bucket lifecycle
##### 4.3.2.7 list objects of a bucket
##### 4.3.2.8 get bucket acl
##### 4.3.2.9 get bucket location
##### 4.3.2.10 get bucket info
##### 4.3.2.11 get bucket audit log
##### 4.3.2.12 get bucket website (unsupported temporarily)
##### 4.3.2.13 get bucket referer
##### 4.3.2.14 get bucket lifecycle
##### 4.3.2.15 delete a bucket
##### 4.3.2.16 delete audit log of a bucket
##### 4.3.2.17 delete bucket website (unsupported temporarily)
##### 4.3.2.18 delete bucket lifecyc

#### 4.3.3 Object API
