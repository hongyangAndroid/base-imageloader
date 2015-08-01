package com.zhy.base.http;

import android.test.InstrumentationTestCase;
import android.util.Log;

/**
 * Created by zhy on 15/7/26.
 */
public class HttpTest extends InstrumentationTestCase
{
    public void testGet() throws InterruptedException, HttpError
    {
        NetworkResponse response = HttpHelper.doGet("http://blog.csdn.net/lmj623565791");
        Log.e("TAG", new String(response.data));


    }

    public void testPost() throws InterruptedException, HttpError
    {

        NetworkResponse response = HttpHelper.doPost("http://blog.csdn.net/lmj623565791");
        Log.e("TAG", new String(response.data));

    }


}
