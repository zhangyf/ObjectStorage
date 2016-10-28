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
public class AuthorizationHandler extends BasicRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RequestInfo requestInfo = (RequestInfo) msg;

        if (!ObjectStorageServer.authorizationManager.checkAuth(requestInfo)) {
            LOG.error("bucket " + requestInfo.getBucketName() + " authorization failed requestID=" + requestInfo.getId());
            sendResponse(ctx, Constants.ERR_CODE_AUTHOR_FAILED);
        } else {
            LOG.info("request authorization OK requestID=" + requestInfo.getId());
            ctx.fireChannelRead(requestInfo);
        }

    }

}
