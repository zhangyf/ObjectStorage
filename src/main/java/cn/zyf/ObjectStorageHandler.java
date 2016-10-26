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
  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.zyf;

import cn.zyf.context.RequestInfo;
import cn.zyf.utils.Constants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by zhangyufeng on 2016/10/24.
 */
class ObjectStorageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static Logger LOG = LoggerFactory.getLogger(ObjectStorageHandler.class);

    private void sendResponse(ChannelHandlerContext ctx, int errCode) {
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

    private RequestInfo parseRequest(FullHttpRequest msg) {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.parse(msg);
        return requestInfo;
    }

    private String process(String path, Map<String, String> params) {
        return "";
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (!msg.decoderResult().isSuccess()) {
            LOG.info("Bad Request");
            sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
            return;
        }

        RequestInfo request = parseRequest(msg);
        if (ObjectStorageServer.blackListManager.filter(request.getBucketName())) {
            LOG.info("bucket " + request.getBucketName() + " in black list. skip it");
            sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
            return;
        }

        if (!ObjectStorageServer.authenticationManager.checkAuth()) {
            LOG.warn("bucket " + request.getBucketName() + " authentication failed");
            sendResponse(ctx, Constants.ERR_CODE_AUTHEN_FAILED);
        }

        if (!ObjectStorageServer.authorizationManager.checkAuth()) {
            LOG.warn("bucket " + request.getBucketName() + " authorization failed");
            sendResponse(ctx, Constants.ERR_CODE_AUTHOR_FAILED);
        }

        if (msg.method() == HttpMethod.GET) {
            LOG.info(request.toString());
            //LOG.info("process GET request path=" + path + " paramStr=" + parameters);
            //sendResponse(ctx, HttpResponseStatus.OK, process(path, parameters));
        } else if (msg.method() == HttpMethod.POST || msg.method() == HttpMethod.PUT) {

        } else {
            LOG.error("unsupported http verb " + msg.method());
            sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
        }
    }
}
