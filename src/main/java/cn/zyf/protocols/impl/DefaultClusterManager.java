package cn.zyf.protocols.impl;

import cn.zyf.protocols.Cluster;
import cn.zyf.protocols.ClusterManager;
import cn.zyf.protocols.ClusterType;
import com.zyf.utils.conf.ConfigTree;
import com.zyf.utils.conf.ConfigTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhangyufeng on 2016/10/28.
 */
public class DefaultClusterManager extends ClusterManager {
    private static Logger LOG = LoggerFactory.getLogger(DefaultClusterManager.class);
    private Map<String, Cluster> clusters;

    private void init(ConfigTree config) {
        Set<Object> subClusterNode = config.getRoot().getValue();

        for (Object obj : subClusterNode) {
            Cluster cluster = new Cluster();
            ConfigTreeNode ctn = (ConfigTreeNode) obj;
            cluster.setName(ctn.getName());
            ClusterType type = ClusterType.UNKNOWN;
            switch (ctn.getAttributes().get("type").toLowerCase()) {
                case "cassandra":
                    type = ClusterType.CASSANDRA;
                    for (ConfigTreeNode subCtn : ctn.getByName("node")) {
                        String[] entries = subCtn.getStringValue().split("\\.");
                        if (entries.length == 2) {
                            cluster.addHost(entries[0], Integer.parseInt(entries[1]));
                        } else {
                            cluster.addHost(subCtn.getStringValue(), 9160);
                        }
                    }
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
            cluster.setType(type);
            clusters.put(cluster.getName(), cluster);
        }
    }

    public DefaultClusterManager(ConfigTree configTree) {
        clusters = new ConcurrentHashMap<>();
        init(configTree);
    }

    @Override
    public void connect() {

    }
}
