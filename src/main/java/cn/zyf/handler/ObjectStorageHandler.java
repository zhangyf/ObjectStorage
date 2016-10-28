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

import cn.zyf.context.RequestInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhangyufeng on 2016/10/24.
 */

public class ObjectStorageHandler extends SimpleChannelInboundHandler<RequestInfo> {
    private static Logger LOG = LoggerFactory.getLogger(ObjectStorageHandler.class);

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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestInfo requestInfo) throws Exception {
        if (isListBuckets(requestInfo) || (requestInfo.getVerb() != HttpMethod.POST
                && !requestInfo.getBucketName().equals("")
                && requestInfo.getObjectName().equals(""))) {
            LOG.info("start process requestID=" + requestInfo.getId() + " with bucketProcessor");
            bucketProcessor(requestInfo);
        } else {
            LOG.info("start process requestID=" + requestInfo.getId() + " with objectProcessor");
            objectProcessor(requestInfo);
        }
    }
}
