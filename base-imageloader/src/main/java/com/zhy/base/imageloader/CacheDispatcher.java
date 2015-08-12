package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.widget.ImageView;

import com.zhy.base.imageloader.diskcache.disklrucache.DiskLruCacheHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

/**
 * Created by zhy on 15/7/31.
 */
public class CacheDispatcher extends Dispatcher
{

    private DiskLruCacheHelper mDiskLruCacheHelper;
    private Handler mUiHandler;

    private ImageDecorder mImageDecorder;

    public CacheDispatcher(Context context, Handler uiHandler, BlockingQueue<ImageRequest> cacheQueue, DiskLruCacheHelper diskLruCacheHelper)
    {
        super(context, cacheQueue);
        mUiHandler = uiHandler;
        mDiskLruCacheHelper = diskLruCacheHelper;
        mImageDecorder = new ImageDecorder(context);
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
                if (request.checkTaskNotActual())
                {
                    //  L.e(request.getUrl() + " canceled in cacheDispatcher");
                    continue;
                }

                String cacheKey = request.getCacheKey();
                InputStream imageStream = mDiskLruCacheHelper.get(cacheKey);
                Bitmap bitmap = null;
                try
                {
                    bitmap = mImageDecorder.decodeByStream(imageStream, buildDecodeParams(request));
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                Message msg = null;
                //缓存命中
                if (bitmap != null)
                {
                  //  Log.e(ImageLoader.TAG, "cache hit " + cacheKey);
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


    /**
     * 根据ImageRequest构造decode需要的参数
     *
     * @param request
     * @return
     */
    private ImageDecorder.ImageDecorderParams buildDecodeParams(ImageRequest request)
    {
        ImageDecorder.ImageDecorderParams params = new ImageDecorder.ImageDecorderParams();
        params.imageView = (ImageView) request.getTarget();
        params.orginSize = request.getExpectSize();
        params.url = request.getUrl();
        return params;
    }


}
