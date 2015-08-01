package com.zhy.base.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;

/**
 * Created by zhy on 15/7/31.
 */
public class ImageRequest
{
    private boolean mCancel;

    public void setBitmap(Bitmap bitmap)
    {
        mBitmap = bitmap;
    }

    public void setImageBitmap()
    {
        ImageView imageView = (ImageView) mTarget.get();
        if (mTarget == null || imageView == null) return;
        if (mBitmap == null) return;
//        if (imageView.getTag().equals(mKey))
        if (!mCancel)
        {
            imageView.setImageBitmap(mBitmap);
        }

    }

    public void setErrorImageRes()
    {
        ImageView imageView = (ImageView) mTarget.get();
        if (mTarget == null || imageView == null) return;
        if (mErrorResId == 0) return;
        imageView.setImageResource(mErrorResId);
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }

    public void setCancel(boolean cancel)
    {
        mCancel = cancel;
    }

    public boolean getCancel()
    {
        return mCancel;
    }

    public Object getTarget()
    {
        return mTarget.get();
    }


    public static class ImageSize
    {
        int width;
        int height;
    }

    /**
     * 软引用图片对象
     */
    private SoftReference<Object> mTarget;

    private Uri mUri;

    private String mKey;

    private Bitmap mBitmap;

    private int mDefaultResId;

    private int mErrorResId;

    public int mExpectWidth;

    public int mExpectHeight;

    public ImageRequest(String url, Object imageView)
    {
        mUri = Uri.parse(url);
        mTarget = new SoftReference<Object>(imageView);
        mKey = mUri.toString() + "\nw" + mExpectHeight + "\nh" + mExpectHeight;
        if (imageView instanceof View)
        {
            //((View) imageView).setTag(mKey);
            ImageSize viewSize = getImageViewSize((View) imageView);
            mExpectWidth = viewSize.width;
            mExpectHeight = viewSize.height;
        } else
        {
            throw new RuntimeException("暂不支持view以外的控件");
        }


    }


    public Uri getUri()
    {
        return mUri;
    }


    public Bitmap bytes2bitmap(byte[] bytes)
    {
        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mBitmap = bm;
        return bm;
    }


    public String getCacheKey()
    {
        return mKey;
    }

    public String getUrl()
    {
        return mUri.toString();
    }

    /**
     * 根据ImageView获适当的压缩的宽和高
     *
     * @param imageView
     * @return
     */
    public static ImageSize getImageViewSize(View imageView)
    {

        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources()
                .getDisplayMetrics();


        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth();// 获取imageview的实际宽度
        if (width <= 0)
        {
            width = lp.width;// 获取imageview在layout中声明的宽度
        }
        if (width <= 0)
        {
            //width = imageView.getMaxWidth();// 检查最大值
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0)
        {
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();// 获取imageview的实际高度
        if (height <= 0)
        {
            height = lp.height;// 获取imageview在layout中声明的宽度
        }
        if (height <= 0)
        {
            height = getImageViewFieldValue(imageView, "mMaxHeight");// 检查最大值
        }
        if (height <= 0)
        {
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;

        return imageSize;
    }

    /**
     * 通过反射获取imageview的某个属性值
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName)
    {
        int value = 0;
        try
        {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE)
            {
                value = fieldValue;
            }
        } catch (Exception e)
        {
        }
        return value;

    }
}
