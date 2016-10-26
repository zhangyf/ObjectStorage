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
