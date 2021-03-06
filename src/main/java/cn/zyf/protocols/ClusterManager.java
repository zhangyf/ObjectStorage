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

import java.util.Map;

/**
 * Created by zhangyufeng on 2016/10/28.
 */
public abstract class ClusterManager {
    public abstract Cluster getClusterByName(String name);
    public abstract void setMetaCluster(String name);
    public abstract Cluster getMetaCluster();
    public abstract void setMetaOption(Map<String, String> option);
    public abstract Map<String, String> getMetaOption();
}
