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

package cn.zyf.proxy;

import cn.zyf.ObjectStorageServer;
import cn.zyf.protocols.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangyufeng on 2016/11/1.
 */
public class MetaProxy {
    private static final Logger LOG = LoggerFactory.getLogger(MetaProxy.class);
    private static MetaProxy instance;
    private static String dbName;
    private static String tableName;
    private static String cfName;
    private static String cnName;
    private static String ugi;
    private Cluster metaCluster;

    private MetaProxy() {
        ObjectStorageServer.clusterManager.getMetaOption().entrySet().forEach(e -> {
            if (e.getKey().equals("dbName")) {
                dbName = e.getValue();
            } else if (e.getKey().equals("tableName")) {
                tableName = e.getValue();
            } else if (e.getKey().equals("cfName")) {
                cfName = e.getValue();
            } else if (e.getKey().equals("cnName")) {
                cnName = e.getValue();
            } else if (e.getKey().equals("ugi")) {
                ugi = e.getValue();
            }
        });
        metaCluster = ObjectStorageServer.clusterManager.getMetaCluster();
        metaCluster.connect();
    }

    public String getBucketMetaByName(String bucketName) {
        return (String) metaCluster.get(tableName, cfName, cnName, bucketName);
    }

    public static MetaProxy getInstance() {
        synchronized (MetaProxy.class) {
            if (instance == null) {
                synchronized (MetaProxy.class) {
                    instance = new MetaProxy();
                }
            }
        }


        return instance;
    }
}
