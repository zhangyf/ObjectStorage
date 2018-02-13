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

package cn.zyf.protocols.impl.cluster;

import cn.zyf.protocols.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangyufeng on 2016/11/1.
 */
public class CassandraClusterImpl extends Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraClusterImpl.class);
    private com.datastax.driver.core.Cluster cluster;
    private Session session;

    @Override
    public void connect() {
        com.datastax.driver.core.Cluster.Builder builder = com.datastax.driver.core.Cluster.builder();
        getHosts().forEach(e -> {
            String[] entries = e.split(":");
            if (entries.length == 2) {
                builder.addContactPoint(entries[0]);
            }
        });
        builder.withClusterName(getName());
        cluster = builder.build();
        session = cluster.connect();
        LOG.info("connect to " + getName() + " success");
    }

    @Override
    public void close() {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Override
    public Object get(Object... args) {
        ResultSet rs = session.execute("select release_version from system.local");
        return rs.one();
    }
}
