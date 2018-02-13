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

package cn.zyf.protocols.impl;

import cn.zyf.protocols.Cluster;
import cn.zyf.protocols.ClusterManager;
import cn.zyf.protocols.ClusterType;
import cn.zyf.protocols.impl.cluster.CassandraClusterImpl;
import com.zyf.utils.conf.ConfigTree;
import com.zyf.utils.conf.ConfigTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhangyufeng on 2016/10/28.
 */
public class DefaultClusterManager extends ClusterManager {
    private static Logger LOG = LoggerFactory.getLogger(DefaultClusterManager.class);
    private Map<String, Cluster> clusters;
    private Map<String, String> metaOption;
    private Cluster metaCluster;

    private void init(ConfigTree config) {
        Set<Object> subClusterNode = config.getRoot().getValue();

        for (Object obj : subClusterNode) {
            ConfigTreeNode ctn = (ConfigTreeNode) obj;
            ClusterType type;
            switch (ctn.getAttributes().get("type").toLowerCase()) {
                case "cassandra":
                    LOG.info("add cassandra cluster " + ctn.getName() +" into ClusterManager");
                    Cluster cluster = new CassandraClusterImpl();
                    cluster.setName(ctn.getName());
                    type = ClusterType.CASSANDRA;
                    for (ConfigTreeNode subCtn : ctn.getByName("node")) {
                        String[] entries = subCtn.getStringValue().split("\\.");
                        if (entries.length == 2) {
                            cluster.addHost(entries[0], Integer.parseInt(entries[1]));
                        } else {
                            cluster.addHost(subCtn.getStringValue(), 9160);
                        }
                    }
                    cluster.setType(type);
                    clusters.put(cluster.getName(), cluster);
                    break;
                case "hbase":
                    type = ClusterType.HBASE;
                    break;
                case "hdfs":
                    type = ClusterType.HDFS;
                    break;
                case "mysql":
                    type = ClusterType.MYSQL;
                    break;
                case "oracle":
                    type = ClusterType.ORACLE;
                    break;
                case "redis":
                    type = ClusterType.REDIS;
                    break;
                case "memcached":
                    type = ClusterType.MEMCACHED;
                    break;
                default:
                    break;
            }
        }
    }

    public DefaultClusterManager(ConfigTree configTree) {
        clusters = new ConcurrentHashMap<>();
        init(configTree);
    }

    @Override
    public void setMetaCluster(String name) {
        metaCluster = clusters.get(name);
    }

    @Override
    public Cluster getMetaCluster() {
        return metaCluster;
    }

    @Override
    public void setMetaOption(Map<String, String> option) {
        assert option != null;
        metaOption = option;
    }

    @Override
    public Map<String, String> getMetaOption() {
        return metaOption;
    }

    @Override
    public Cluster getClusterByName(String name) {
        return clusters.get(name);
    }
}
