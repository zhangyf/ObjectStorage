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

    public static final String ERR_MSG_DEFAULT = "INTERNAL SERVER ERROR";

    public static final Map<Integer, String> statusMap;

    static {
        Map<Integer, String> tmpMap = new HashMap<>();
        tmpMap.put(ERR_CODE_BAD_REQUEST, "Bad Request");
        tmpMap.put(ERR_CODE_AUTHEN_FAILED, "Authentication failed");
        tmpMap.put(ERR_CODE_AUTHOR_FAILED, "Authorization failed");
        statusMap = Collections.unmodifiableMap(tmpMap);
    }

}
