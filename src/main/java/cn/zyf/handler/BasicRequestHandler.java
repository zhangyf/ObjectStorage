package cn.zyf.handler;

import cn.zyf.utils.Constants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.json.JSONObject;

/**
 * Created by zhangyufeng on 2016/10/28.
 */

class BasicRequestHandler extends SimpleChannelInboundHandler {
    void sendResponse(ChannelHandlerContext ctx, int errCode) {
        HttpResponseStatus status;

        switch (errCode) {
            case Constants.ERR_CODE_BAD_REQUEST:
                status = HttpResponseStatus.BAD_REQUEST;
                break;
            case Constants.ERR_CODE_AUTHEN_FAILED:
                status = HttpResponseStatus.FORBIDDEN;
                break;
            case Constants.ERR_CODE_AUTHOR_FAILED:
                status = HttpResponseStatus.METHOD_NOT_ALLOWED;
                break;
            default:
                status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                break;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("errCode", -errCode);
        jsonObject.put("errMsg", Constants.statusMap.get(errCode));
        innerSendResponse(ctx, status, jsonObject.toString());
    }

    private void innerSendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(content.getBytes()));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

    }
}
