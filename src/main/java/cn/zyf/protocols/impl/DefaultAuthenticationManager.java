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

import cn.zyf.context.RequestInfo;
import cn.zyf.protocols.AuthenticationManager;

/**
 * Created by zhangyufeng on 2016/10/26.
 */
public class DefaultAuthenticationManager extends AuthenticationManager {

    public boolean checkAuth(RequestInfo requestInfo) {
        return true;
    }
}
