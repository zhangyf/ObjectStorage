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

import cn.zyf.handler.ObjectStorageHandler;
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
import cn.zyf.protocols.AuthenticationManager;
import cn.zyf.protocols.AuthorizationManager;
import cn.zyf.protocols.BlackListManager;
import cn.zyf.protocols.impl.DefaultAuthenticationManager;
import cn.zyf.protocols.impl.DefaultAuthorizationManager;
import cn.zyf.protocols.impl.DefaultBlackListManager;
import cn.zyf.tasks.BlackListReloadTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangyufeng on 2016/10/21.
 */
public class ObjectStorageServer {
    private static Logger LOG = LoggerFactory.getLogger(ObjectStorageServer.class);

    private static final String DEFAULT_BIND_STR = "0.0.0.0:8400";
    private static final int DEFAULT_PACKAGE_SIZE = 65535;
    private static final long DEFAULT_RELOAD_BLACK_LIST_INTERVAL_IN_SECONDS = 60;

    public static BlackListManager blackListManager;
    public static AuthenticationManager authenticationManager;
    public static AuthorizationManager authorizationManager;

    private void run(String host, int port, int packageSize) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

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
                            ch.pipeline().addLast("req-handler", new ObjectStorageHandler());
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
        ConfigTreeNode classNode = configTreeNode.get("class");
        String className = classNode.get("name").getStringValue();

        LOG.info("custom class:\t" + className);

        Class<?>[] paramsTypes = null;
        Object[] paramsObjs = null;
        if (classNode.containsKey("params")) {
            ConfigTreeNode paramsNode = classNode.get("params");
            Set<String> paramsNames = paramsNode.getSubKeys();
            paramsTypes = new Class[paramsNames.size()];
            paramsObjs = new Object[paramsNames.size()];
            int idx = 0;
            for (String paramName : paramsNames) {
                paramsTypes[idx] = Class.forName("java.lang." + paramsNode.get(paramName).getAttribute("type"));
                Method method = paramsNode.get(paramName).getClass().getMethod("get"
                        + paramsNode.get(paramName).getAttribute("type")
                        + "Value");

                paramsObjs[idx] = method.invoke(paramsNode.get(paramName));
                idx++;

                LOG.info("custom class params:\t(" + paramsNode.get(paramName).getAttribute("type") + ") " + paramName
                        + "=" + method.invoke(paramsNode.get(paramName)));
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

    private static void initServer(ConfigTree config)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        assert config != null;

        String bindStr = config.containsKey("bind") ? config.get("bind").getCurrentValue() : DEFAULT_BIND_STR;
        int packageSize = config.containsKey("packageSize") ?
                Integer.parseInt(config.get("packageSize").getCurrentValue())
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
        ConfigTreeNode blacklistConfig = config.containsKey("blackList") ? config.get("blackList") : null;
        if (blacklistConfig != null) {

            if (blacklistConfig.containsKey("class")) {
                blackListManager = (BlackListManager) createObjectByReflection(blacklistConfig);
            } else {
                blackListManager = new DefaultBlackListManager();
                blackListFilePath = blacklistConfig.containsKey("path") ?
                        blacklistConfig.get("path").getCurrentValue()
                        :
                        null;

                reloadBlackListPeriod = blacklistConfig.containsKey("period") ?
                        Long.parseLong(blacklistConfig.get("period").getCurrentValue())
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
        ConfigTreeNode authenticationConfig = config.containsKey("authentication") ? config.get("authentication") : null;
        authenticationManager = ((authenticationConfig != null) && authenticationConfig.containsKey("class")) ?
                (AuthenticationManager) createObjectByReflection(authenticationConfig)
                :
                new DefaultAuthenticationManager();
        if (authenticationConfig == null) {
            LOG.info("use default Authentication");
        }

        // init authorizationManager
        ConfigTreeNode authorizationConfig = config.containsKey("authorization") ? config.get("authorization") : null;
        authorizationManager = ((authorizationConfig != null) && authorizationConfig.containsKey("class")) ?
                (AuthorizationManager) createObjectByReflection(authorizationConfig)
                :
                new DefaultAuthorizationManager();
        if (authorizationConfig == null) {
            LOG.info("use default Authorization");
        }

        LOG.info("================================");
        (new ObjectStorageServer()).run(entries[0], Integer.parseInt(entries[1]), packageSize);

    }

    public static void main(String[] args) {
        String configFilePath = (System.getProperties().getProperty("serviceConfig") == null) ?
                ObjectStorageServer.class.getResource("/serviceConfig.xml").toString()
                :
                System.getProperties().getProperty("serviceConfig");

        LOG.info("load config file from " + configFilePath);
        ConfigTree configuration = null;
        try {
            configuration = ConfigUtils.getConfig(configFilePath);
        } catch (IOException e) {
            LOG.error("load config from " + configFilePath + " occur IOException", e);
            System.exit(-1);
        }

        try {
            initServer(configuration);
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            LOG.error("initServer occur Exception", e);
            System.exit(-1);
        }
    }
}
