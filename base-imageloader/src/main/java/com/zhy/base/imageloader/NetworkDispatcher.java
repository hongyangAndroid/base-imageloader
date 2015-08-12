package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.zhy.base.imageloader.diskcache.disklrucache.DiskLruCache;
import com.zhy.base.imageloader.diskcache.disklrucache.DiskLruCacheHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhy on 15/7/31.
 */
public class NetworkDispatcher extends Dispatcher
{
    /**
     * UI线程中的Handler
     */
    private Handler mUiHandler;
    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEAFULT_THREAD_COUNT = 3;
    private ImageDecorder mImageDecorder;

    //TODO 抽象
    private DiskLruCacheHelper mDiskLruCache;

    public NetworkDispatcher(Context context, Handler uiHandler, BlockingQueue<ImageRequest> nextworkQueue, DiskLruCacheHelper diskLruCache)
    {
        super(context, nextworkQueue);
        mUiHandler = uiHandler;
        mImageDecorder = new ImageDecorder(context);
        mDiskLruCache = diskLruCache;
        // 创建线程池
        //mThreadPool = Executors.newFixedThreadPool(DEAFULT_THREAD_COUNT);

        mThreadPool = new ThreadPoolExecutor(
                DEAFULT_THREAD_COUNT,
                DEAFULT_THREAD_COUNT,
                0,
                TimeUnit.MILLISECONDS,
                new LIFOLinkedBlockingDeque<Runnable>());
    }


    @Override
    public void run()
    {
        //   L.e(ImageLoader.TAG, "network dispatcher start!!");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while (true)
        {
            try
            {
                final ImageRequest request = mQueue.take();
                if (request.checkTaskNotActual()) continue;
                //   L.e(ImageLoader.TAG, "network dispatcher path :" + request.getUrl());
                mThreadPool.execute(buildTask(request));

            } catch (InterruptedException e)
            {
                //如果要求退出则退出，否则遇到异常继续
                if (mQuit) return;
                else continue;
            }
        }
    }

    @NonNull
    private Runnable buildTask(final ImageRequest request)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String url = request.getUrl();
                    //   Log.e(ImageLoader.TAG, "network dispatcher :" + url);
                    InputStream imageStream = mImageDecorder.getImageStream(request.getUrl());
                    //存储到硬盘缓存
                    buildCacheToDisk(request.getCacheKey(), imageStream);
                    //网络加载
                    Bitmap bitmap = mImageDecorder.decodeByStream(imageStream, buildDecodeParams(request));
                    request.setBitmap(bitmap);
                    sendSuccessMsg(request);

                } catch (IOException e)
                {
                    e.printStackTrace();
                    sendErrorMsg(request);
                }
            }
        };
    }

    @NonNull
    private void buildCacheToDisk(String cacheKey, InputStream imageStream)
    {
        try
        {
            DiskLruCache.Editor editor = mDiskLruCache.editor(cacheKey);
            if (editor == null) return;
            OutputStream outputStream = editor.newOutputStream(0);
            byte[] buf = new byte[2048];
            int len = 0;
            while ((len = imageStream.read(buf)) != -1)
            {
                outputStream.write(buf, 0, len);
            }
            outputStream.flush();
            outputStream.close();
            editor.commit();
        } catch (IOException e)
        {
            e.printStackTrace();
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


    private void sendSuccessMsg(ImageRequest request)
    {
        Message msg = Message.obtain(null, ImageLoader.MSG_HTTP_GET_SUCCESS);
        msg.obj = request;
        mUiHandler.sendMessage(msg);
    }

    private void sendErrorMsg(ImageRequest request)
    {
        Message msg = Message.obtain(null, ImageLoader.MSG_HTTP_GET_ERROR);
        msg.obj = request;
        mUiHandler.sendMessage(msg);
    }
}
