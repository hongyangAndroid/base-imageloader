package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.zhy.base.cache.disk.DiskLruCacheHelper;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 图片加载类
 * http://blog.csdn.net/lmj623565791/article/details/41874561
 *
 * @author zhy
 */
public class ImageLoader
{
    public static final int MSG_CACHE_HINT = 0x110;
    public static final int MSG_CACHE_UN_HINT = MSG_CACHE_HINT + 1;
    public static final int MSG_HTTP_GET_ERROR = MSG_CACHE_UN_HINT + 1;
    public static final int MSG_HTTP_GET_SUCCESS = MSG_HTTP_GET_ERROR + 1;
    public static final int MSG_LOCAL_GET_SUCCESS = MSG_HTTP_GET_SUCCESS + 1;
    public static final int MSG_LOCAL_GET_ERROR = MSG_LOCAL_GET_SUCCESS + 1;
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private static ImageLoader mInstance;
    private Context mContext;

    /**
     * 图片缓存的核心对象
     */
    private LruCache<String, Bitmap> mLruCache;
    public static final String TAG = "ImageLoader";

    private LocalDispatcher mLocalDispatcher;
    private volatile BlockingQueue<ImageRequest> mLocalQueue;

    private CacheDispatcher mCacheDispatcher;
    private volatile BlockingQueue<ImageRequest> mCacheQueue;

    private NetworkDispatcher mNetworkDispatcher;
    private volatile BlockingQueue<ImageRequest> mNetworkQueue;

    private DiskLruCacheHelper mDiskLruCacheHelper;

    private Handler HANDLER = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            ImageRequest req = (ImageRequest) msg.obj;
            switch (msg.what)
            {
                case MSG_CACHE_UN_HINT:
                    mNetworkQueue.add(req);
                    break;
                case MSG_LOCAL_GET_ERROR:
                case MSG_HTTP_GET_ERROR:
                    req.setErrorImageRes();
                    break;
                case MSG_CACHE_HINT:
                case MSG_LOCAL_GET_SUCCESS:
                    mLruCache.put(req.getCacheKey(), req.getBitmap());
                    req.setImageBitmap();
                    break;
                case MSG_HTTP_GET_SUCCESS:
                    req.setImageBitmap();
                    mLruCache.put(req.getCacheKey(), req.getBitmap());
                    mDiskLruCacheHelper.put(req.getCacheKey(), req.getBitmap());
                    break;
            }
        }
    };


    private ImageLoader(Context context, LruCache<String, Bitmap> lrucahce)
    {
        mContext = context;
        mLruCache = lrucahce;

        try
        {
            mDiskLruCacheHelper = new DiskLruCacheHelper(context, 50 * 1024 * 1024);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        //初始化CacheDispatcher
        mCacheQueue = new LinkedBlockingQueue<>();
        mCacheDispatcher = new CacheDispatcher(context, HANDLER, mCacheQueue, mDiskLruCacheHelper);
        //初始化NetworkDispatcher
        mNetworkQueue = new LinkedBlockingQueue<>();
        mNetworkDispatcher = new NetworkDispatcher(context, HANDLER, mNetworkQueue);

        //初始化LocalDispatcher
        mLocalQueue = new LIFOLinkedBlockingDeque<>();
        mLocalDispatcher = new LocalDispatcher(context, HANDLER, mLocalQueue);

        mLocalDispatcher.start();
        mCacheDispatcher.start();
        mNetworkDispatcher.start();

    }

    public static ImageLoader with(Context context)
    {
        if (mInstance == null)
        {
            synchronized (ImageLoader.class)
            {
                if (mInstance == null)
                {
                    mInstance = new Builder(context).build();
                }
            }
        }
        return mInstance;
    }


    public static class Builder
    {
        private Context context;
        private int mMaxMemCacheSize;

        public Builder(Context context)
        {

            if (context == null)
            {
                throw new IllegalArgumentException("Context must not be null.");
            }
            this.context = context.getApplicationContext();
        }

        public ImageLoader build()
        {
            Context context = this.context;
            LruCache<String, Bitmap> defaultLruCache = createDefaultLruCache();
            return new ImageLoader(context, defaultLruCache);
        }

        public Builder setMemCacheSize(int memCacheSize)
        {
            mMaxMemCacheSize = memCacheSize;
            return this;
        }

        private LruCache createDefaultLruCache()
        {

            int maxMemory = (int) Runtime.getRuntime().maxMemory();
            mMaxMemCacheSize = maxMemory / 8;
            return new LruCache<String, Bitmap>(mMaxMemCacheSize)
            {
                @Override
                protected int sizeOf(String key, Bitmap value)
                {
                    return value.getRowBytes() * value.getHeight();
                }
            };
        }
    }

    public static enum Type
    {
        FIFO, LIFO, Type;
    }


    /**
     * TODO 增加各个参数定制
     * TODO 更好的方式实现LIFO
     */

    private Map<Object, ImageRequest> mRequests = new WeakHashMap<Object, ImageRequest>();

    /**
     * 根据path为imageview设置图片
     *
     * @param path
     * @param imageView
     */
    public void load(final String path, final ImageView imageView)
    {
        L.e(TAG, "orign load :" + path);
        //check ImageRequest exists
        ImageRequest imageRequest = mRequests.get(imageView);
        if (imageRequest != null)
        {
            imageRequest.setCancel(true);
        }
        ImageRequest req = buildImageRequest(path, imageView);
        mRequests.put(imageView, req);

        String cacheKey = req.getCacheKey();
        Bitmap bitmap = mLruCache.get(cacheKey);
        if (bitmap != null)
        {
            imageView.setImageBitmap(bitmap);
            return;
        }
        Uri uri = req.getUri();
        String scheme = uri.getScheme();
        if (SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme))
        {
            mCacheQueue.add(req);
            return;
        }
        mLocalQueue.offer(req);
    }

    private ImageRequest buildImageRequest(String path, ImageView imageView)
    {
        ImageRequest req = new ImageRequest(path, imageView);
        return req;
    }


}
