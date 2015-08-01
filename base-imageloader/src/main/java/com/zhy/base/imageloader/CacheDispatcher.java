package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.zhy.base.cache.disk.DiskLruCacheHelper;

import java.util.concurrent.BlockingQueue;

/**
 * Created by zhy on 15/7/31.
 */
public class CacheDispatcher extends Dispatcher
{

    private DiskLruCacheHelper mDiskLruCacheHelper;
    private Handler mUiHandler;

    public CacheDispatcher(Context context, Handler uiHandler, BlockingQueue<ImageRequest> cacheQueue, DiskLruCacheHelper diskLruCacheHelper)
    {
        super(context, cacheQueue);
        mUiHandler = uiHandler;
        mDiskLruCacheHelper = diskLruCacheHelper;
    }

    @Override
    public void run()
    {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true)
        {
            try
            {
                ImageRequest request = mQueue.take();
                if (request.getCancel())
                {
                    continue;
                }
                Log.e(ImageLoader.TAG, "cache dispatcher :" + request.getUrl() + " , cancel =" + request.getCancel());

                String cacheKey = request.getCacheKey();
                Bitmap bitmap = mDiskLruCacheHelper.getAsBitmap(cacheKey);
                Message msg = null;
                //缓存命中
                if (bitmap != null)
                {
                    Log.e(ImageLoader.TAG, "cache hit " + cacheKey);
                    request.setBitmap(bitmap);
                    msg = Message.obtain(null, ImageLoader.MSG_CACHE_HINT);
                } else
                {
                    msg = Message.obtain(null, ImageLoader.MSG_CACHE_UN_HINT);
                }
                msg.obj = request;
                mUiHandler.sendMessage(msg);
            } catch (InterruptedException e)
            {
                //如果要求退出则退出，否则遇到异常继续
                if (mQuit) return;
                else continue;
            }
        }
    }


}
