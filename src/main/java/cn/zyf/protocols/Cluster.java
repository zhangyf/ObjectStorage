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

package cn.zyf.protocols;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

;

public abstract class Cluster {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public ClusterType getType() {
        return type;
    }

    public void setType(ClusterType type) {
        this.type = type;
    }

    public void addHost(String hostname, int port) {
        this.hosts.add(hostname + ":" + port);
    }

    public String getHost(int idx) {
        Iterator<String> iterator = hosts.iterator();
        int i=0;

        while (iterator.hasNext()) {
            if (i++ == idx) {
                return iterator.next();
            }
        }

        return null;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    private String name;
    private String extraInfo;
    private ClusterType type;
    private Set<String> hosts;

    public Cluster() {
        this.name = "";
        this.extraInfo = "";
        this.type = ClusterType.UNKNOWN;
        hosts = new ConcurrentSkipListSet<>();
    }

    public abstract void connect();
    public abstract Object get(Object ...args);


}
