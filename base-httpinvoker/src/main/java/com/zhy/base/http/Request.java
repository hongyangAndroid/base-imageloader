/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhy.base.http;

import android.net.TrafficStats;
import android.net.Uri;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for all network requests.
 */
public class Request
{

    /**
     * Default encoding for POST or PUT parameters. See {@link #getParamsEncoding()}.
     */
    private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";


    /**
     * Params for POST
     */
    public static class Param
    {
        public String key;
        public String value;

        public Param(String key, String val)
        {
            this.key = key;
            this.value = val;
        }
    }

    /**
     * Supported request methods.
     */
    public interface Method
    {
        int DEPRECATED_GET_OR_POST = -1;
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int OPTIONS = 5;
        int TRACE = 6;
        int PATCH = 7;
    }

    /**
     * Request method of this request.  Currently supports GET, POST, PUT, DELETE, HEAD, OPTIONS,
     * TRACE, and PATCH.
     */
    private final int mMethod;

    /**
     * URL of this request.
     */
    private final String mUrl;

    /**
     * The redirect url to use for 3xx http responses
     */
    private String mRedirectUrl;


    /**
     * Default tag for {@link TrafficStats}.
     */
    private final int mDefaultTrafficStatsTag;

    /**
     * The retry policy for this request.
     */
    private RetryPolicy mRetryPolicy;

    /**
     * When a request can be retrieved from cache but must be refreshed from
     * the network, the cache entry will be stored here so that in the event of
     * a "Not Modified" response, we can be sure it hasn't been evicted from cache.
     */
    private Cache.Entry mCacheEntry = null;


    /**
     * Params for POST
     */
    private List<Param> mParams = new ArrayList<Param>();

    public Request setParams(List<Param> params)
    {
        mParams = params;
        return this;
    }

    public Request addParam(Param param)
    {
        mParams.add(param);
        return this;
    }

    public Request addParam(String key, String val)
    {
        mParams.add(new Param(key, val));
        return this;
    }


    /**
     * Creates a new request with the given method (one of the values from {@link Method}),
     * URL, and error listener.  Note that the normal response listener is not provided here as
     * delivery of responses is provided by subclasses, who have a better idea of how to deliver
     * an already-parsed response.
     */
    public Request(int method, String url)
    {
        mMethod = method;
        mUrl = url;
        setRetryPolicy(new DefaultRetryPolicy());
        mDefaultTrafficStatsTag = findDefaultTrafficStatsTag(url);
    }

    /**
     * Return the method for this request.  Can be one of the values in {@link Method}.
     */
    public int getMethod()
    {
        return mMethod;
    }


    /**
     * @return A tag for use with {@link TrafficStats#setThreadStatsTag(int)}
     */
    public int getTrafficStatsTag()
    {
        return mDefaultTrafficStatsTag;
    }

    /**
     * @return The hashcode of the URL's host component, or 0 if there is none.
     */
    private static int findDefaultTrafficStatsTag(String url)
    {
        if (!TextUtils.isEmpty(url))
        {
            Uri uri = Uri.parse(url);
            if (uri != null)
            {
                String host = uri.getHost();
                if (host != null)
                {
                    return host.hashCode();
                }
            }
        }
        return 0;
    }

    /**
     * Sets the retry policy for this request.
     *
     * @return This Request object to allow for chaining.
     */
    public Request setRetryPolicy(RetryPolicy retryPolicy)
    {
        mRetryPolicy = retryPolicy;
        return this;
    }


    /**
     * Returns the URL of this request.
     */
    public String getUrl()
    {
        return (mRedirectUrl != null) ? mRedirectUrl : mUrl;
    }

    /**
     * Returns the URL of the request before any redirects have occurred.
     */
    public String getOriginUrl()
    {
        return mUrl;
    }


    /**
     * Sets the redirect url to handle 3xx http responses.
     */
    public Request setRedirectUrl(String redirectUrl)
    {
        mRedirectUrl = redirectUrl;
        return this ;
    }

    /**
     * Returns the cache key for this request.  By default, this is the URL.
     */
    public String getCacheKey()
    {
        return getUrl();
    }

    /**
     * Annotates this request with an entry retrieved for it from cache.
     * Used for cache coherency support.
     *
     * @return This Request object to allow for chaining.
     */
    public Request setCacheEntry(Cache.Entry entry)
    {
        mCacheEntry = entry;
        return this;
    }

    /**
     * Returns the annotated cache entry, or null if there isn't one.
     */
    public Cache.Entry getCacheEntry()
    {
        return mCacheEntry;
    }

    /**
     * Returns a list of extra HTTP headers to go along with this request. Can
     * throw {@link AuthFailureError} as authentication may be required to
     * provide these values.
     *
     * @throws AuthFailureError In the event of auth failure
     */
    public Map<String, String> getHeaders() throws AuthFailureError
    {
        if (mHeaders != null && mHeaders.size() > 0)
        {
            return mHeaders;
        }
        return Collections.emptyMap();
    }

    private Map<String, String> mHeaders;

    public Request setHeaders(Map<String, String> header)
    {
        mHeaders = header;
        return this ;
    }
    /**
     * Returns a Map of parameters to be used for a POST or PUT request.  Can throw
     * {@link AuthFailureError} as authentication may be required to provide these values.
     * <p>
     * <p>Note that you can directly override {@link #getBody()} for custom data.</p>
     *
     * @throws AuthFailureError in the event of auth failure
     */
    public List<Param> getParams() throws AuthFailureError
    {
        return mParams;
    }


    private String mParamEncoding = DEFAULT_PARAMS_ENCODING;

    /**
     * Returns which encoding should be used when converting POST or PUT parameters returned by
     * {@link #getParams()} into a raw POST or PUT body.
     * <p>
     * <p>This controls both encodings:
     * <ol>
     * <li>The string encoding used when converting parameter names and values into bytes prior
     * to URL encoding them.</li>
     * <li>The string encoding used when converting the URL encoded parameters into a raw
     * byte array.</li>
     * </ol>
     */
    public String getParamsEncoding()
    {
        return mParamEncoding == null ?
                DEFAULT_PARAMS_ENCODING
                : mParamEncoding;
    }

    public void setParamsEncoding(String paramEncoding)
    {
        this.mParamEncoding = paramEncoding;
    }

    private String mBodyContentType;

    /**
     * Returns the content type of the POST or PUT body.
     */
    public String getBodyContentType()
    {
        return mBodyContentType == null ?
                "application/x-www-form-urlencoded; charset=" + getParamsEncoding()
                : mBodyContentType;
    }

    public Request setBodyContentType(String bodyContentType)
    {
        mBodyContentType = bodyContentType;
        return this ;
    }

    /**
     * Returns the raw POST or PUT body to be sent.
     * <p>
     * <p>By default, the body consists of the request parameters in
     * application/x-www-form-urlencoded format. When overriding this method, consider overriding
     * {@link #getBodyContentType()} as well to match the new body format.
     *
     * @throws AuthFailureError in the event of auth failure
     */
    public byte[] getBody() throws AuthFailureError
    {
        List<Param> params = getParams();
        if (params != null && params.size() > 0)
        {
            return encodeParameters(params, getParamsEncoding());
        }
        return null;
    }

    /**
     * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
     */
    private byte[] encodeParameters(List<Param> params, String paramsEncoding)
    {
        StringBuilder encodedParams = new StringBuilder();
        try
        {
            for (Param param : params)
            {
                encodedParams.append(URLEncoder.encode(param.key, paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(param.value, paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString().getBytes(paramsEncoding);
        } catch (UnsupportedEncodingException uee)
        {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

    /**
     * Returns the socket timeout in milliseconds per retry attempt. (This value can be changed
     * per retry attempt if a backoff is specified via backoffTimeout()). If there are no retry
     * attempts remaining, this will cause delivery of a {@link TimeoutError} error.
     */
    public final int getTimeoutMs()
    {
        return mRetryPolicy.getCurrentTimeout();
    }

    /**
     * Returns the retry policy that should be used  for this request.
     */
    public RetryPolicy getRetryPolicy()
    {
        return mRetryPolicy;
    }


}
