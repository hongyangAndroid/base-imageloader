package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

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
    private ImageDecorder mImageDecorder;

    public CacheDispatcher(Context context, Handler uiHandler, BlockingQueue<ImageRequest> cacheQueue, DiskLruCacheHelper diskLruCacheHelper)
    {
        super(context, cacheQueue, uiHandler,
                ImageLoader.MSG_CACHE_HINT,
                ImageLoader.MSG_CACHE_UN_HINT);
        mDiskLruCacheHelper = diskLruCacheHelper;
        mImageDecorder = new ImageDecorder(context);
    }


    @Override
    protected void dealRequest(ImageRequest request)
    {
        String cacheKey = request.getCacheKey();
        InputStream imageStream = mDiskLruCacheHelper.get(cacheKey);
        Bitmap bitmap = null;
        try
        {
            bitmap = mImageDecorder.decodeByStream(imageStream, buildDecodeParams(request));
        } catch (IOException e)
        {
            e.printStackTrace();
            L.w("cache dispatcher :" + request.getUrl() +" decodeByStream failed , something wrong happened in disklrucache . " );
        }
        request.setBitmap(bitmap);
        if (bitmap == null) sendErrorMsg(request);
        sendSuccessMsg(request);
    }


}
