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

package cn.zyf.tasks;

import cn.zyf.ObjectStorageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangyufeng on 2016/10/24.
 */
public class BlackListReloadTask implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(BlackListReloadTask.class);
    private String blPath;

    public BlackListReloadTask() {
        this("");
    }

    public BlackListReloadTask(String path) {
        blPath = path;
    }

    @Override
    public void run() {
        if (blPath != null) {
            LOG.info("time to reload black list " + blPath);
            ObjectStorageServer.blackListManager.reload(blPath);
        }
    }
}
