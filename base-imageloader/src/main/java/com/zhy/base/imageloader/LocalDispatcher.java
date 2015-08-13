package com.zhy.base.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * Created by zhy on 15/7/31.
 */
public class LocalDispatcher extends Dispatcher
{

    private ImageDecorder mImageDecorder;

    public LocalDispatcher(Context context, Handler uiHandler,
                           BlockingQueue<ImageRequest> cacheQueue)
    {
        super(context, cacheQueue, uiHandler,
                ImageLoader.MSG_LOCAL_GET_SUCCESS,
                ImageLoader.MSG_LOCAL_GET_ERROR);
        mImageDecorder = new ImageDecorder(context);
    }


    @Override
    protected void dealRequest(ImageRequest request)
    {
        Bitmap bitmap = null;
        String imageUrl = request.getUrl();
        Scheme scheme = Scheme.ofUri(imageUrl);
        // 如果schema属于unknown，检测是否属于文件
        if (scheme == Scheme.UNKNOWN)
        {
            bitmap = tryToGetBitmapFromSDCard(request);
        } else
        {
            try
            {
                bitmap = mImageDecorder.decode(buildDecodeParams(request));
            } catch (IOException e)
            {
                e.printStackTrace();
                L.w("local dispatcher :" + imageUrl + " can not decode to a bitmap.");
            }
        }
        request.setBitmap(bitmap);
        if (bitmap == null)
        {
            sendErrorMsg(request);
        }
        sendSuccessMsg(request);

    }

    private Bitmap tryToGetBitmapFromSDCard(ImageRequest request)
    {
        Bitmap bitmap = null;
        String imageUrl = request.getUrl();
        File f = new File(imageUrl);
        if (f.exists() && f.length() > 0)
        {
            // 尝试以文件形式读取
            try
            {
                bitmap = mImageDecorder
                        .decode(buildFileDecodeParams(request));
            } catch (IOException e)
            {
                e.printStackTrace();
                L.w("local dispatcher :" + imageUrl + " is a right path on sdcard , but maybe not a picture.");
            }
        }
        return bitmap;
    }


}
