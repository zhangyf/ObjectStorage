package cn.zyf.handler;

import cn.zyf.ObjectStorageServer;
import cn.zyf.context.RequestInfo;
import cn.zyf.utils.Constants;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangyufeng on 2016/10/27.
 */
public class FilterRequestHandler extends BasicRequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FilterRequestHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RequestInfo requestInfo = (RequestInfo) msg;

        if (ObjectStorageServer.blackListManager.filter(requestInfo.getBucketName())) {
            LOG.error("bucket " + requestInfo.getBucketName() + " in black list requestID=" + requestInfo.getId() + ". skip it");
            sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
        } else {
            LOG.info("filter request OK requestID=" + requestInfo.getId());
            ctx.fireChannelRead(requestInfo);
        }
    }

}
