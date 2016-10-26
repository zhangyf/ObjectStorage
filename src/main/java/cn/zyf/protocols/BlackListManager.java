package cn.zyf.protocols;

/**
 * Created by zhangyufeng on 2016/10/25.
 */
public abstract class BlackListManager {

    public boolean filter(String target) { return false; }
    public void reload(String path) {}
}
