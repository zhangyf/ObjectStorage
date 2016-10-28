package cn.zyf.handler;

import cn.zyf.context.RequestInfo;
import cn.zyf.excption.UnsupportedVerbException;
import cn.zyf.utils.Constants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangyufeng on 2016/10/27.
 */
public class ParseRequestHandler extends BasicRequestHandler {
    private static Logger LOG = LoggerFactory.getLogger(ParseRequestHandler.class);

    private RequestInfo parseAndValidateRequest(FullHttpRequest msg) throws RuntimeException {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.parse(msg);

        if (requestInfo.getVerb() != HttpMethod.GET &&
                msg.method() != HttpMethod.PUT &&
                msg.method() != HttpMethod.POST &&
                msg.method() != HttpMethod.HEAD &&
                msg.method() != HttpMethod.DELETE) {
            throw new UnsupportedVerbException("unsupported http verb " + msg.method());
        }

        return requestInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

        if (!fullHttpRequest.decoderResult().isSuccess()) {
            LOG.info("Bad Request");
            sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
            return;
        }

        RequestInfo requestInfo;
        try {
            requestInfo = parseAndValidateRequest(fullHttpRequest);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            if (e instanceof UnsupportedVerbException) {
                sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
            } else {
                sendResponse(ctx, Constants.ERR_CODE_INTERNAL_ERROR);
            }
            return;
        }

        LOG.info("parse request OK requestID=" + requestInfo.getId());

        ctx.fireChannelRead(requestInfo);

    }
}
