package com.zhy.base.imageloader;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created by zhy on 15/7/31.
 */
public class ImageRequest
{
    private ImageLoader mImageLoader;
    /**
     * 软引用图片对象
     */
    private WeakReference<Object> mTarget;

    private Uri mUri;

    private String mCacheKey;

    private Bitmap mBitmap;

    private int mDefaultResId;

    private int mErrorResId;

    public int mExpectWidth;

    public int mExpectHeight;


    public ImageRequest(ImageLoader imageLoader, String url, Object imageView, int errorResId, int defaultResId)
    {
        mImageLoader = imageLoader;
        mUri = Uri.parse(url);
        mTarget = new WeakReference<Object>(imageView);
        mErrorResId = errorResId;
        mDefaultResId = defaultResId;

        if (imageView instanceof View)
        {
            ImageUtils.ImageSize viewSize = ImageUtils.getImageViewSize((View) imageView);
            mExpectWidth = viewSize.width;
            mExpectHeight = viewSize.height;
            mCacheKey = mUri.toString() + "_w" + mExpectWidth + "_h" + mExpectHeight;
        } else
        {
            throw new RuntimeException("暂不支持view以外的控件!");
        }

    }

    /**
     * 检测当前的任务是否有必要去执行
     *
     * @return
     */
    public boolean checkTaskNotActual()
    {
        return checkViewCollected() || checkViewReused();

    }

    /**
     * 检测当前的ImageView是否已经绑定了别的url
     */
    private boolean checkViewReused()
    {
        String cacheKey = mImageLoader.getCancelableRequestDelegate().getCacheKey(getTarget().hashCode());
        return !mCacheKey.equals(cacheKey);
    }

    /**
     * 检测当前view是否已经被回收
     */
    private boolean checkViewCollected()
    {
        return mTarget.get() == null;
    }


    public void setBitmap(Bitmap bitmap)
    {
        mBitmap = bitmap;
    }


    public Bitmap getBitmap()
    {
        return mBitmap;
    }


    public Object getTarget()
    {
        return mTarget.get();
    }


    public Uri getUri()
    {
        return mUri;
    }


    public ImageUtils.ImageSize getExpectSize()
    {
        return new ImageUtils.ImageSize(mExpectWidth, mExpectHeight);
    }


    public String getCacheKey()
    {
        return mCacheKey;
    }

    public String getUrl()
    {
        return mUri.toString();
    }

    public boolean setResBitmap()
    {
        if (checkTaskNotActual()) return false;
        Bitmap bm = mBitmap;
        ImageView imageView = (ImageView) getTarget();
        if (imageView == null) return false;
        if (bm == null) return false;
        imageView.setImageBitmap(bm);
        mImageLoader.getCancelableRequestDelegate().remove(getTarget().hashCode());
        return true;
    }

    public void setErrorImageRes()
    {
        ImageView imageView = (ImageView) mTarget.get();
        if (mTarget == null || imageView == null) return;
        if (mErrorResId == 0) return;
        imageView.setImageResource(mErrorResId);
    }


}
