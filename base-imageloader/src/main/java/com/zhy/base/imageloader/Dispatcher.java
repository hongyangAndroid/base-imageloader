package com.zhy.base.imageloader;

import android.content.Context;

import java.util.concurrent.BlockingQueue;

/**
 * Created by zhy on 15/7/31.
 */
public class Dispatcher extends Thread
{
    protected Context mContext ;
    protected BlockingQueue<ImageRequest> mQueue;
    protected volatile boolean mQuit = false;

    public Dispatcher(Context context,BlockingQueue<ImageRequest> queue)
    {
        mContext = context ;
        mQueue = queue ;
    }



    public void quit()
    {
        mQuit = true;
        interrupt();
    }

}
