package com.zhy.base.imageloader;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.widget.ImageView;

import java.util.concurrent.BlockingQueue;

/**
 * Created by zhy on 15/7/31.
 */
public abstract class Dispatcher extends Thread
{
    protected Context mContext;
    protected BlockingQueue<ImageRequest> mQueue;
    protected volatile boolean mQuit = false;
    protected Handler mUiHandler;

    protected int mMsgSuccessWhat;
    protected int mMsgFailWhat;

    public Dispatcher(Context context, BlockingQueue<ImageRequest> queue, Handler uiHandler, int msgSuccessWhat, int msgFailWhat)
    {
        mContext = context;
        mQueue = queue;
        mUiHandler = uiHandler;

        mMsgSuccessWhat = msgSuccessWhat;
        mMsgFailWhat = msgFailWhat;

    }

    public void quit()
    {
        mQuit = true;
        interrupt();
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
                if (request.checkTaskNotActual())
                    continue;

                dealRequest(request);

            } catch (InterruptedException e)
            {
                // 如果要求退出则退出，否则遇到异常继续
                if (mQuit)
                    return;
                else
                    continue;
            }
        }
    }

    protected void sendErrorMsg(ImageRequest request)
    {
        sendErrorMsg(request, mMsgFailWhat);
    }

    protected void sendSuccessMsg(ImageRequest request)
    {
        sendSuccessMsg(request, mMsgSuccessWhat);
    }

    protected abstract void dealRequest(ImageRequest request);

    protected void sendSuccessMsg(ImageRequest request, int what)
    {
        Message msg = Message.obtain(null, what);
        msg.obj = request;
        mUiHandler.sendMessage(msg);
    }

    protected void sendErrorMsg(ImageRequest request, int what)
    {
        Message msg = Message.obtain(null, what);
        msg.obj = request;
        mUiHandler.sendMessage(msg);
    }


    /**
     * 根据ImageRequest构造decode需要的参数
     *
     * @param request
     * @return
     */
    protected ImageDecorder.ImageDecorderParams buildDecodeParams(
            ImageRequest request)
    {
        ImageDecorder.ImageDecorderParams params = new ImageDecorder.ImageDecorderParams();
        params.imageView = (ImageView) request.getTarget();
        params.expectSize = request.getExpectSize();
        params.url = request.getUrl();
        return params;
    }

    protected ImageDecorder.ImageDecorderParams buildFileDecodeParams(
            ImageRequest request)
    {
        ImageDecorder.ImageDecorderParams params = new ImageDecorder.ImageDecorderParams();
        params.imageView = (ImageView) request.getTarget();
        params.expectSize = request.getExpectSize();
        params.url = Scheme.FILE.wrap(request.getUrl());
        return params;
    }
}
