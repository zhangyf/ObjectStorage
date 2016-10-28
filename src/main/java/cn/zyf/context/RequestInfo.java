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

package cn.zyf.context;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by zhangyufeng on 2016/10/24.
 */

enum Acl { PUBLIC_READ_WRITE, PUBLIC_READ, PRIVATE};

public class RequestInfo {
    private static Logger LOG = LoggerFactory.getLogger(RequestInfo.class);
    private HttpMethod verb;
    private Acl acl;
    private int contentLength;
    private String id;
    private String path;
    private String bucketName;
    private String objectName;
    private String contentType;
    private String contentMD5;
    private String dateStr;
    private Map<String, String> params;

    private String decodeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8").replaceAll("//+", "/");
            return uri;
        } catch (UnsupportedEncodingException e) {
            LOG.error("decode uri with UTF-8 encoder occur UnsupportedEncodingException ", e);
            return null;
        }
    }

    private String parsePath(String fullUri) {

        fullUri = decodeUri(fullUri);
        String ret = (fullUri != null && !fullUri.equals("?") && fullUri.indexOf('?') > 0) ?
                fullUri.substring(0, fullUri.indexOf('?'))
                :
                fullUri;

        return (ret != null && !ret.equals("/") && ret.charAt(ret.length()-1) == '/') ?
                ret.substring(0, ret.length() - 1)
                :
                ret;
    }

    private Map<String, String> getParameters(String input) {
        Map<String, String> ret = new HashMap<>();
        input = input.replaceAll("&+", "&");
        input = input.replaceAll("=+", "=");

        int tmpEnd = input.indexOf("&");
        if (tmpEnd < 0) {
            int idx = input.indexOf("=");
            if (idx >= 0) {
                ret.put(input.substring(0, idx), input.substring(idx + 1));
            }
        } else {
            String subInput = input.substring(0, tmpEnd);
            ret.put(subInput.substring(0, subInput.indexOf("=")), subInput.substring(subInput.indexOf("=") + 1));
            ret.putAll(getParameters(input.substring(tmpEnd + 1)));
        }

        return ret;
    }

    public RequestInfo() {
        this.verb = HttpMethod.GET;
        this.path = "";
        this.bucketName = "";
        this.objectName = "";
        this.params = null;
        this.acl = Acl.PRIVATE;
        this.contentLength = 0;
        this.contentMD5 = "";
        this.contentType = "";
        this.dateStr = "";
        setId("req_" + UUID.randomUUID());
    }


    public String getId() {
        return id;
    }

    private void setId(String id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentMD5() {
        return contentMD5;
    }

    public void setContentMD5(String contentMD5) {
        this.contentMD5 = contentMD5;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public HttpMethod getVerb() {
        return verb;
    }

    public void setVerb(HttpMethod verb) {
        this.verb = verb;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Acl getAcl() {
        return acl;
    }

    public void setAcl(Acl a) {
        this.acl = a;
    }

    public void parse(FullHttpRequest msg) throws UnsupportedOperationException{
        setVerb(msg.method());

        String decodedURI = decodeUri(msg.uri());
        setPath(parsePath(decodedURI));
        String [] entries = getPath().split("/");

        if (entries.length >= 2) {
            setBucketName(entries[1]);

            if (entries.length > 2) {
                setObjectName(entries[2]);
            }
        }

        if (decodedURI != null) {
            String paramStr = (decodedURI.indexOf("?") > 0) ? decodedURI.substring(decodedURI.indexOf("?") + 1) : "";
            this.params = getParameters(paramStr);
        }

        if (msg.headers().contains("x-oss-acl")) {
            switch (msg.headers().get("x-oss-acl")) {
                case "public-read-write" :
                    setAcl(Acl.PUBLIC_READ_WRITE);
                    break;
                case "public-read":
                    setAcl(Acl.PUBLIC_READ);
                    break;
                case "private" :
                    setAcl(Acl.PRIVATE);
                    break;
                default:
                    setAcl(Acl.PRIVATE);
                    break;
            }
        }

        if (msg.headers().contains("content-length")) {
            setContentLength(Integer.parseInt(msg.headers().get("content-length")));
        }

        if (msg.headers().contains("content-type")) {
            setContentType(msg.headers().get("content-type"));
        }

        if (msg.headers().contains("content-md5")) {
            setContentMD5(msg.headers().get("content-md5"));
        }

        if (msg.headers().contains("date")) {
            setDateStr(msg.headers().get("date"));
        }
    }

    public String toString() {
        return "{ verb=" + getVerb()
                + " path=" + getPath()
                + " bucketName=" + getBucketName()
                + " objectName=" + getObjectName()
                + " acl=" + getAcl()
                + " content-type=" + getContentType()
                + " content-length=" + getContentLength()
                + " content-md5=" + getContentMD5()
                + " date=" + getDateStr()
                + " params=" + this.params + " }";
    }
}
