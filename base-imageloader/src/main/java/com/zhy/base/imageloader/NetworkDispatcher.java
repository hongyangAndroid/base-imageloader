package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

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
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEAFULT_THREAD_COUNT = 3;
    private ImageDecorder mImageDecorder;
    //TODO 抽象
    private DiskLruCacheHelper mDiskLruCache;

    public NetworkDispatcher(Context context, Handler uiHandler, BlockingQueue<ImageRequest> nextworkQueue, DiskLruCacheHelper diskLruCache)
    {
        super(context, nextworkQueue, uiHandler,
                ImageLoader.MSG_HTTP_GET_SUCCESS,
                ImageLoader.MSG_HTTP_GET_ERROR);
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


    private Runnable buildTask(final ImageRequest request)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                String imageUrl = request.getUrl();
                try
                {
                    InputStream imageStream = mImageDecorder.getImageStream(imageUrl);
                    //存储到硬盘缓存
                    buildCacheToDisk(request.getCacheKey(), imageStream);
                    //网络加载
                    Bitmap bitmap = mImageDecorder.decodeByStream(imageStream, buildDecodeParams(request));
                    request.setBitmap(bitmap);
                    sendSuccessMsg(request);
                } catch (IOException e)
                {
                    e.printStackTrace();
                    L.w("network dispatcher : " + imageUrl + " decodeByStream failed , check network state and url .");
                    sendErrorMsg(request);
                }
            }
        };
    }

    private void buildCacheToDisk(String cacheKey, InputStream imageStream)
    {
        OutputStream outputStream = null;
        try
        {
            DiskLruCache.Editor editor = mDiskLruCache.editor(cacheKey);
            if (editor == null) return;
            outputStream = editor.newOutputStream(0);
            byte[] buf = new byte[2048];
            int len = 0;
            while ((len = imageStream.read(buf)) != -1)
            {
                outputStream.write(buf, 0, len);
            }
            outputStream.flush();
            editor.commit();
        } catch (IOException e)
        {
            e.printStackTrace();
            L.w("network dispatcher : buildCacheToDisk failed , check log .");
        } finally
        {
            if (outputStream != null)
            {
                try
                {
                    outputStream.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void dealRequest(ImageRequest request)
    {
        mThreadPool.execute(buildTask(request));
    }

}
