package cn.zyf.protocols.impl;

import cn.zyf.protocols.BlackListManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by zhangyufeng on 2016/10/25.
 */
public class DefaultBlackListManager extends BlackListManager {
    private static Logger LOG = LoggerFactory.getLogger(DefaultBlackListManager.class);
    private Set<String> blackList;

    public DefaultBlackListManager() {
        blackList = new ConcurrentSkipListSet<>();
    }

    public synchronized void reload(String path) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                blackList.add(line);
            }
        } catch (IOException e) {
            LOG.error("reload black list file " + path + " occur IOException", e);
        }

    }

    public boolean filter(String target) {
        return blackList.contains(target);
    }
}
