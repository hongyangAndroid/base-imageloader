package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import java.util.concurrent.BlockingQueue;

/**
 * Created by zhy on 15/7/31.
 */
public class LocalDispatcher extends Dispatcher
{

    private Handler mUiHandler;

    public LocalDispatcher(Context context, Handler uiHandler, BlockingQueue<ImageRequest> cacheQueue)
    {
        super(context, cacheQueue);
        mUiHandler = uiHandler;
    }

    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    protected Bitmap decodeSampledBitmapFromPath(String path, int width,
                                                 int height)
    {
        // 获得图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = caculateInSampleSize(options,
                width, height);

        // 使用获得到的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     */
    public int caculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                    int reqHeight)
    {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        if (width > reqWidth || height > reqHeight)
        {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);
        }

        return inSampleSize;
    }

    @Override
    public void run()
    {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true)
        {
            try
            {
                ImageRequest request = mQueue.take();
                if (request.getCancel())
                {
                    continue;
                }
                String path = request.getUrl();
                L.e(ImageLoader.TAG,"local dispatcher path : " + path);
                Bitmap bitmap = decodeSampledBitmapFromPath(path, request.mExpectWidth, request.mExpectHeight);
                Message msg = null;
                if (bitmap != null)
                {
                    request.setBitmap(bitmap);
                    msg = Message.obtain(null, ImageLoader.MSG_LOCAL_GET_SUCCESS);
                } else
                {

                    msg = Message.obtain(null, ImageLoader.MSG_LOCAL_GET_ERROR);
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
