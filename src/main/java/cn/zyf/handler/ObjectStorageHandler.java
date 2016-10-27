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

package cn.zyf.handler;

import cn.zyf.ObjectStorageServer;
import cn.zyf.context.RequestInfo;
import cn.zyf.excption.AuthenticationFailureException;
import cn.zyf.excption.AuthorizationFailureException;
import cn.zyf.excption.InvalidBucketException;
import cn.zyf.excption.UnsupportedVerbException;
import cn.zyf.utils.Constants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangyufeng on 2016/10/24.
 */

public class ObjectStorageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
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

        if (ObjectStorageServer.blackListManager.filter(requestInfo.getBucketName())) {
            throw new InvalidBucketException("bucket " + requestInfo.getBucketName() + " in black list. skip it");
        }

        if (!ObjectStorageServer.authenticationManager.checkAuth(requestInfo)) {
            throw new AuthenticationFailureException("bucket " + requestInfo.getBucketName() + " authentication failed");
        }

        if (!ObjectStorageServer.authorizationManager.checkAuth(requestInfo)) {
            throw new AuthorizationFailureException("bucket " + requestInfo.getBucketName() + " authorization failed");
        }

        return requestInfo;
    }

    private boolean isListBuckets(RequestInfo requestInfo) {
        return requestInfo.getVerb() == HttpMethod.GET
                && requestInfo.getPath().equals("/")
                && requestInfo.getBucketName().equals("")
                && requestInfo.getObjectName().equals("");
    }

    private String bucketProcessor(RequestInfo requestInfo) {
        return "";
    }

    private String objectProcessor(RequestInfo requestInfo) {
        return "";
    }

    private String dispatchRequest(RequestInfo requestInfo) {
        if (isListBuckets(requestInfo) || (requestInfo.getVerb() != HttpMethod.POST
                && !requestInfo.getBucketName().equals("")
                && requestInfo.getObjectName().equals(""))) {
            return bucketProcessor(requestInfo);
        } else {
            return objectProcessor(requestInfo);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (!msg.decoderResult().isSuccess()) {
            LOG.info("Bad Request");
            sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
            return;
        }

        RequestInfo requestInfo;
        try {
            requestInfo = parseAndValidateRequest(msg);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            if (e instanceof UnsupportedVerbException) {
                sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
            } else if (e instanceof AuthenticationFailureException) {
                sendResponse(ctx, Constants.ERR_CODE_AUTHEN_FAILED);
            } else if (e instanceof AuthorizationFailureException) {
                sendResponse(ctx, Constants.ERR_CODE_AUTHOR_FAILED);
            } else if (e instanceof InvalidBucketException) {
                sendResponse(ctx, Constants.ERR_CODE_BAD_REQUEST);
            } else {
                sendResponse(ctx, Constants.ERR_CODE_INTERNAL_ERROR);
            }

            return ;
        }

        LOG.info(requestInfo.toString());
        dispatchRequest(requestInfo);

    }
}
