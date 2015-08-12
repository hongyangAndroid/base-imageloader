package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.zhy.base.imageloader.diskcache.disklrucache.DiskLruCacheHelper;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 图片加载类
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


    private ExecutorService mTaskDistribute = Executors.newFixedThreadPool(1);

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


    private CancelableRequestDelegate mCancelableRequestDelegate = new CancelableRequestDelegate();


    private int mErrorResId, mDefaultResId;
    private boolean mDiskCacheEnabled;
    private Type mType;

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
                    if (req.setResBitmap())
                    {
                        mLruCache.put(req.getCacheKey(), req.getBitmap());
                    }
                    break;
                case MSG_HTTP_GET_SUCCESS:
                    if (req.setResBitmap())
                    {
                        mLruCache.put(req.getCacheKey(), req.getBitmap());
                    }
                    break;
            }
        }
    };


    public CancelableRequestDelegate getCancelableRequestDelegate()
    {
        return mCancelableRequestDelegate;
    }


    private ImageLoader(Context context, LruCache<String, Bitmap> lrucahce, int errorResId, int defaultResId, boolean diskCacheEnabled, Type type)
    {
        mContext = context;
        mLruCache = lrucahce;
        mErrorResId = errorResId;
        mDefaultResId = defaultResId;
        mDiskCacheEnabled = diskCacheEnabled;
        mType = type;

        try
        {
            if (mDiskCacheEnabled)
                mDiskLruCacheHelper = new DiskLruCacheHelper(context, 50 * 1024 * 1024);
        } catch (IOException e)
        {
            e.printStackTrace();
        }


        //初始化CacheDispatcher
        if(mType==Type.LIFO)
        {
            mCacheQueue = new LIFOLinkedBlockingDeque<>();
        }else {
            mCacheQueue = new LinkedBlockingQueue<>();
        }
        mCacheDispatcher = new CacheDispatcher(context, HANDLER, mCacheQueue, mDiskLruCacheHelper);
        //初始化NetworkDispatcher
        mNetworkQueue = new LinkedBlockingQueue<>();
        mNetworkDispatcher = new NetworkDispatcher(context, HANDLER, mNetworkQueue, mDiskLruCacheHelper);

        //初始化LocalDispatcher
        //初始化CacheDispatcher
        if(mType==Type.LIFO)
        {
            mLocalQueue = new LIFOLinkedBlockingDeque<>();
        }else {
            mLocalQueue = new LinkedBlockingQueue<>();
        }
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
        /**
         * 内存缓存的大小，默认为app最大内存的1/8
         */
        private int mMaxMemCacheSize;
        /**
         * 发生错误时显示的图片
         */
        private int mErrorResId;
        /**
         * 默认显示的图片
         */
        private int mDefaultResId;

        /**
         * 是否开启硬盘缓存
         */
        private boolean mDiskCacheEnable = true;


        private Type mType = Type.LIFO;

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

            return new ImageLoader(context, defaultLruCache, mErrorResId, mDefaultResId, mDiskCacheEnable, mType);
        }

        public Builder memCacheSize(int memCacheSize)
        {
            mMaxMemCacheSize = memCacheSize;
            return this;
        }

        public Builder errorResId(int errorResId)
        {
            mErrorResId = errorResId;
            return this;
        }

        public Builder defaultResId(int defaultResId)
        {
            mDefaultResId = defaultResId;
            return this;
        }

        public Builder diskCacheEnbled(boolean cacheEnable)
        {
            mDiskCacheEnable = cacheEnable;
            return this;
        }

        public Builder loadType(Type type)
        {
            mType = type;
            return this;
        }


        private LruCache createDefaultLruCache()
        {
            int memCacheSize = mMaxMemCacheSize;
            int maxMemory = (int) Runtime.getRuntime().maxMemory();
            memCacheSize = memCacheSize <= 0 ? maxMemory / 8 : memCacheSize;
            return new LruCache<String, Bitmap>(memCacheSize)
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
        FIFO, LIFO;
    }


    /**
     * 根据path为imageview设置图片
     *
     * @param path
     * @param imageView
     */
    public void load(final String path, final ImageView imageView)
    {
        setDefaultImageRes(imageView);
        //L.e(TAG, "orign load :" + path);
        final ImageRequest req = buildImageRequest(path, imageView);
        final String cacheKey = req.getCacheKey();
        //记录最新的imageview -> cacheKey
        mCancelableRequestDelegate.putRequest(imageView.hashCode(), cacheKey);
        Bitmap bitmap = mLruCache.get(cacheKey);
        if (bitmap != null)
        {
            // L.e("get from lrcCache = " + path);
            imageView.setImageBitmap(bitmap);
            return;
        }

        //TODO 考虑改变分发策略，判断磁盘文件存在的方式分配任务
        mTaskDistribute.execute(new Runnable()
        {
            @Override
            public void run()
            {
                L.e("put hashCode = " + imageView.hashCode() + " , url = " + path);
                Uri uri = req.getUri();
                String scheme = uri.getScheme();
                if (SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme))
                {
                    mCacheQueue.offer(req);
                    return;
                }
                mLocalQueue.offer(req);
            }
        });


    }

    private void setDefaultImageRes(ImageView imageView)
    {
        if (mDefaultResId != 0)
        {
            imageView.setImageResource(mDefaultResId);
        }
    }

    private ImageRequest buildImageRequest(String path, ImageView imageView)
    {
        ImageRequest req = new ImageRequest(this, path, imageView, mErrorResId, mDefaultResId);
        return req;
    }


}
