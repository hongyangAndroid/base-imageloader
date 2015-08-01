package com.zhy.base.http;

import android.content.Context;

import java.util.List;

/**
 * Created by zhy on 15/7/26.
 */
public class HttpHelper
{
    /**
     * execute http request
     */
    private Network mHttpInvoker;

    private volatile static HttpHelper sIntance;



    private HttpHelper(Context context)
    {
        mHttpInvoker = BasicNetwork.getDefaultNetwork(context);
    }

    /**
     * @param context
     * @return
     */
    public static HttpHelper getInstance(Context context)
    {
        if (sIntance == null)
        {
            synchronized (HttpHelper.class)
            {
                if (sIntance == null)
                {
                    if (context != null)
                        context = context.getApplicationContext();
                    sIntance = new HttpHelper(context);
                }
            }
        }

        return sIntance;
    }

    public static HttpHelper getInstance()
    {
        return getInstance(null);
    }


    private NetworkResponse _doRequest(Request req) throws HttpError
    {
        return mHttpInvoker.performRequest(req);
    }

    public static NetworkResponse doRequest(Request req) throws HttpError
    {
        return getInstance()._doRequest(req);
    }

    private NetworkResponse _doGet(String url) throws HttpError
    {
        return mHttpInvoker.performRequest(new Request(Request.Method.GET, url));
    }

    public static NetworkResponse doGet(String url) throws HttpError
    {
        return getInstance()._doGet(url);
    }

    private NetworkResponse _doPost(String url) throws HttpError
    {
        return mHttpInvoker.performRequest(new Request(Request.Method.POST, url));
    }

    public static NetworkResponse doPost(String url) throws HttpError
    {
        return getInstance()._doGet(url);
    }

    private NetworkResponse _doPost(String url, List<Request.Param> params) throws HttpError
    {
        return mHttpInvoker.performRequest(
                new Request(Request.Method.POST, url).setParams(params));
    }

    public static NetworkResponse doPost(String url, List<Request.Param> params) throws HttpError
    {
        return getInstance()._doPost(url, params);
    }

}
