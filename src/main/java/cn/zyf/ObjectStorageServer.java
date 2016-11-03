/*
  Copyright 2016,2017 Yufeng Zhang

  This file is part of ObjectStorageServer.

  ObjectStorageServer is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  ObjectStorageServer is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with ObjectStorageServer.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.zyf;

import cn.zyf.handler.*;
import cn.zyf.protocols.AuthenticationManager;
import cn.zyf.protocols.AuthorizationManager;
import cn.zyf.protocols.BlackListManager;
import cn.zyf.protocols.ClusterManager;
import cn.zyf.protocols.impl.DefaultAuthenticationManager;
import cn.zyf.protocols.impl.DefaultAuthorizationManager;
import cn.zyf.protocols.impl.DefaultBlackListManager;
import cn.zyf.protocols.impl.DefaultClusterManager;
import cn.zyf.tasks.BlackListReloadTask;
import com.zyf.utils.conf.ConfigTree;
import com.zyf.utils.conf.ConfigTreeNode;
import com.zyf.utils.conf.ConfigUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangyufeng on 2016/10/21.
 */
public class ObjectStorageServer {
    private static Logger LOG = LoggerFactory.getLogger(ObjectStorageServer.class);

    private static final String DEFAULT_BIND_STR = "0.0.0.0:8484";
    private static final int DEFAULT_PACKAGE_SIZE = 65535;
    private static final long DEFAULT_RELOAD_BLACK_LIST_INTERVAL_IN_SECONDS = 60;

    public static BlackListManager blackListManager;
    public static AuthenticationManager authenticationManager;
    public static AuthorizationManager authorizationManager;
    public static ClusterManager clusterManager;

    private void run(String host, int port, int packageSize) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        EventLoopGroup backendGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                            ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(packageSize));
                            ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                            ch.pipeline().addLast("oss-decoder", new ParseRequestHandler());
                            ch.pipeline().addLast("oss-filter", new FilterRequestHandler());
                            ch.pipeline().addLast("oss-authen", new AuthenticationHandler());
                            ch.pipeline().addLast("oss-author", new AuthorizationHandler());
                            ch.pipeline().addLast(backendGroup, "oss-backend", new ObjectStorageHandler());
                        }
                    });

            ChannelFuture future = b.bind(host, port).sync();
            LOG.info("start Http Server with hostname=" + host + ":" + port);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOG.error("occur InterruptedException ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static Object createObjectByReflection(ConfigTreeNode configTreeNode)
            throws NoSuchMethodException, ClassNotFoundException,
            InvocationTargetException, IllegalAccessException, InstantiationException {

        ConfigTreeNode classNode = configTreeNode.getByName("class").stream().findFirst().orElse(null);
        String className = (classNode != null) ? classNode.getByName("name").stream().findFirst().orElse(null).getStringValue() : "";

        LOG.info("custom class:\t" + className);

        ConfigTreeNode paramsNode;
        String[] paramsNames;
        Class<?>[] paramsTypes = null;
        Object[] paramsObjs = null;
        if (classNode != null && classNode.containsByName("params")) {
            int idx = 0;
            paramsNode = classNode.getByName("params").stream().findFirst().orElse(null);

            if (paramsNode != null) {
                paramsNames = new String[paramsNode.getValue().size()];
                for (Object ctn : paramsNode.getValue()) {
                    paramsNames[idx++] = ((ConfigTreeNode) ctn).getName();
                }

                paramsTypes = new Class[paramsNames.length];
                paramsObjs = new Object[paramsNames.length];

                for (String paramName : paramsNames) {
                    for (ConfigTreeNode ctn : paramsNode.getByName(paramName)) {
                        idx = Integer.parseInt(ctn.getAttributes().get("idx"));
                        paramsTypes[idx] = Class.forName("java.lang." + ctn.getAttributes().get("type"));
                        Method method = ctn.getClass().getMethod("get" + ctn.getAttributes().get("type") + "Value");
                        paramsObjs[idx] = method.invoke(ctn);
                        LOG.info("custom class params: \t(" + paramsTypes[idx] + ") " + paramName + "=" + method.invoke(ctn));
                    }
                }
            }
        }

        Class<?> clazz = Class.forName(className);
        if (paramsTypes == null) {
            return clazz.newInstance();
        } else {
            Constructor constructor = clazz.getConstructor(paramsTypes);
            return constructor.newInstance(paramsObjs);
        }
    }

    private static void initServer(ConfigTree serviceConfig, ConfigTree clusterConfig)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        assert serviceConfig != null;
        assert clusterConfig != null;

        String bindStr = (serviceConfig.containsByName("bind") && serviceConfig.getByName("bind").stream().findAny().isPresent()) ?
                serviceConfig.getByName("bind").stream().findAny().orElse(null).getStringValue()
                :
                DEFAULT_BIND_STR;

        int packageSize = (serviceConfig.containsByName("packageSize")
                && serviceConfig.getByName("packageSize").stream().findAny().isPresent()) ?
                serviceConfig.getByName("packageSize").stream().findAny().orElse(null).getIntegerValue()
                :
                DEFAULT_PACKAGE_SIZE;


        String[] entries = bindStr.split(":");
        assert entries.length == 2;

        LOG.info("================================");
        LOG.info("server address:\t" + entries[0]);
        LOG.info("server port:\t" + entries[1]);
        LOG.info("packageSize:\t" + packageSize + " bytes");


        // init black list manager
        long reloadBlackListPeriod = DEFAULT_RELOAD_BLACK_LIST_INTERVAL_IN_SECONDS;
        String blackListFilePath = null;

        ConfigTreeNode blacklistConfig = serviceConfig.getByName("blackList").stream().findAny().orElse(null);

        if (blacklistConfig != null) {
            if (blacklistConfig.containsByName("class")) {
                blackListManager = (BlackListManager) createObjectByReflection(blacklistConfig);
            } else {
                blackListManager = new DefaultBlackListManager();
                blackListFilePath = blacklistConfig.getByName("path").stream().findAny().orElse(null).getStringValue();

                reloadBlackListPeriod = blacklistConfig.getByName("period").stream().findAny().isPresent() ?
                        blacklistConfig.getByName("period").stream().findAny().orElse(null).getLongValue()
                        :
                        DEFAULT_RELOAD_BLACK_LIST_INTERVAL_IN_SECONDS;

                LOG.info("blacklist file path:\t" + blackListFilePath);
                LOG.info("reload blacklist period:\t" + reloadBlackListPeriod + " sec");
            }
        }

        BlackListReloadTask blackListReloadTask = new BlackListReloadTask(blackListFilePath);
        ScheduledExecutorService reloadBlackListSerivce = Executors.newSingleThreadScheduledExecutor();
        reloadBlackListSerivce.scheduleAtFixedRate(blackListReloadTask,
                0, reloadBlackListPeriod, TimeUnit.SECONDS);

        // init authenticationManager
        authenticationManager =
                (serviceConfig.getByName("authentication").stream().findFirst().isPresent()
                        && serviceConfig.getByName("authentication").stream().findFirst().orElse(null)
                        .getByName("class").stream().findFirst().isPresent()) ?
                        (AuthenticationManager) createObjectByReflection(
                                serviceConfig.getByName("authentication").stream().findFirst().orElse(null))
                        :
                        new DefaultAuthenticationManager();


        // init authorizationManager
        authorizationManager =
                (serviceConfig.getByName("authorization").stream().findFirst().isPresent()
                        && serviceConfig.getByName("authorization").stream().findFirst().orElse(null)
                        .getByName("class").stream().findFirst().isPresent()) ?
                        (AuthorizationManager) createObjectByReflection(
                                serviceConfig.getByName("authorization").stream().findFirst().orElse(null))
                        :
                        new DefaultAuthorizationManager();

        // init clusterManager
        clusterManager = new DefaultClusterManager(clusterConfig);
        ConfigTreeNode metaNode = serviceConfig.getByName("meta").stream().findFirst().orElse(null);
        if (metaNode != null && metaNode.getByName("name").stream().findFirst().isPresent()) {
            String metaClusterName = metaNode.getByName("name").stream().findFirst().orElse(null).getStringValue();
            LOG.info("set meta cluster:\t" + metaClusterName);
            clusterManager.setMetaCluster(metaClusterName);

            Map<String, String> metaOptions = new HashMap<>();

            for (Object obj : metaNode.getByName("options").stream().findFirst().orElse(null).getValue()) {
                ConfigTreeNode ctn = (ConfigTreeNode) obj;
                LOG.info("set meta cluster options:\t" + ctn.getName() + "=" + ctn.getStringValue());
                metaOptions.put(ctn.getName(), ctn.getStringValue());
            }
            clusterManager.setMetaOption(metaOptions);

        } else {
            LOG.error("set meta cluster failed ");
            System.exit(-1);
        }

        LOG.info("================================");
        (new ObjectStorageServer()).run(entries[0], Integer.parseInt(entries[1]), packageSize);

    }

    private static ConfigTree loadConfigTree(String propertyName, String defaultFileName) {
        String configFilePath = (System.getProperties().getProperty(propertyName) == null) ?
                ObjectStorageServer.class.getResource(defaultFileName).toString()
                :
                System.getProperties().getProperty(propertyName);

        LOG.info("load config file from " + configFilePath);
        ConfigTree configuration = null;
        try {
            configuration = ConfigUtils.getConfig(configFilePath);
        } catch (IOException e) {
            LOG.error("load config from " + configFilePath + " occur IOException", e);
            System.exit(-1);
        }

        return configuration;
    }

    public static void main(String[] args) {

        ConfigTree serviceConfigTree = loadConfigTree("serviceConfig", "/serviceConfig.xml");
        ConfigTree clusterConfigTree = loadConfigTree("clusterConfig", "/clusterConfig.xml");

        try {
            initServer(serviceConfigTree, clusterConfigTree);
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            LOG.error("initServer occur Exception", e);
            System.exit(-1);
        }
    }
}
