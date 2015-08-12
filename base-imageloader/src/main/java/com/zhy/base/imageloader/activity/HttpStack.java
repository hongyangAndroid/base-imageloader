package com.zhy.base.imageloader.activity;

import android.content.Context;
import android.net.Uri;


import com.zhy.base.imageloader.ContentLengthInputStream;
import com.zhy.base.imageloader.IoUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by zhy on 15/8/11.
 */
public class HttpStack
{

    /**
     * {@value}
     */
    public static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 5 * 1000; // milliseconds
    /**
     * {@value}
     */
    public static final int DEFAULT_HTTP_READ_TIMEOUT = 20 * 1000; // milliseconds

    /**
     * {@value}
     */
    protected static final int BUFFER_SIZE = 32 * 1024; // 32 Kb
    /**
     * {@value}
     */
    protected static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";


    protected final Context context;
    protected final int connectTimeout;
    protected final int readTimeout;

    public HttpStack(Context context)
    {
        this(context, DEFAULT_HTTP_CONNECT_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);
    }

    public HttpStack(Context context, int connectTimeout, int readTimeout)
    {
        this.context = context.getApplicationContext();
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    protected static final int MAX_REDIRECT_COUNT = 5;

    protected InputStream getStreamFromNetwork(String imageUri) throws IOException
    {
        HttpURLConnection conn = createConnection(imageUri);

        int redirectCount = 0;
        while (conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT)
        {
            conn = createConnection(conn.getHeaderField("Location"));
            redirectCount++;
        }

        InputStream imageStream;
        try
        {
            imageStream = conn.getInputStream();
        } catch (IOException e)
        {
            // Read all data to allow reuse connection (http://bit.ly/1ad35PY)
            IoUtils.readAndCloseStream(conn.getErrorStream());
            throw e;
        }
        //conn.getResponseCode() == 200;
        if (!shouldBeProcessed(conn))
        {
            IoUtils.closeSilently(imageStream);
            throw new IOException("Image request failed with response code " + conn.getResponseCode());
        }

        return new ContentLengthInputStream(new BufferedInputStream(imageStream, BUFFER_SIZE), conn.getContentLength());
    }

    protected HttpURLConnection createConnection(String url) throws IOException
    {
        String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
        HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        return conn;
    }

    protected boolean shouldBeProcessed(HttpURLConnection conn) throws IOException
    {
        return conn.getResponseCode() == 200;
    }
}
