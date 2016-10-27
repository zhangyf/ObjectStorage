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

package cn.zyf.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhangyufeng on 2016/10/25.
 */
public class Constants {
    public static final int ERR_CODE_BAD_REQUEST = 60400;
    public static final int ERR_CODE_AUTHEN_FAILED = 60401;
    public static final int ERR_CODE_AUTHOR_FAILED = 60402;
    public static final int ERR_CODE_INTERNAL_ERROR = 60500;

    public static final String ERR_MSG_DEFAULT = "INTERNAL SERVER ERROR";

    public static final Map<Integer, String> statusMap;

    static {
        Map<Integer, String> tmpMap = new HashMap<>();
        tmpMap.put(ERR_CODE_BAD_REQUEST, "Bad Request");
        tmpMap.put(ERR_CODE_AUTHEN_FAILED, "Authentication failed");
        tmpMap.put(ERR_CODE_AUTHOR_FAILED, "Authorization failed");
        tmpMap.put(ERR_CODE_INTERNAL_ERROR, "Internal Service Error");
        statusMap = Collections.unmodifiableMap(tmpMap);
    }

}
