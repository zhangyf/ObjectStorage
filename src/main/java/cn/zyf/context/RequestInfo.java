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

package cn.zyf.context;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhangyufeng on 2016/10/24.
 */
public class RequestInfo {
    private static Logger LOG = LoggerFactory.getLogger(RequestInfo.class);
    private HttpMethod verb;

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

    private String path;
    private String bucketName;
    private String objectName;
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

    private String getPath(String fullUri) {

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
    }

    public void parse(FullHttpRequest msg) {
        String decodedURI = decodeUri(msg.uri());
        path = getPath(decodedURI);
        String [] entries = path.split("/");

        if (entries.length >= 2) {
            bucketName = entries[1];

            if (entries.length > 2) {
                objectName = entries[2];
            }
        }

        if (decodedURI != null) {
            String paramStr = (decodedURI.indexOf("?") > 0) ? decodedURI.substring(decodedURI.indexOf("?") + 1) : "";
            this.params = getParameters(paramStr);
        }
    }

    public String toString() {
        return "{ verb=" + this.verb
                + " path=" + this.path
                + " bucketName=" + this.bucketName
                + " objectName=" + this.objectName
                + " params=" + this.params + " }";
    }
}
