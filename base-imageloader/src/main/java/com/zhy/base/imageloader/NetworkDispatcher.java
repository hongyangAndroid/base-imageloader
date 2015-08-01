package com.zhy.base.imageloader;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.zhy.base.http.HttpError;
import com.zhy.base.http.HttpHelper;
import com.zhy.base.http.NetworkResponse;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by zhy on 15/7/31.
 */
public class NetworkDispatcher extends Dispatcher
{
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台轮询线程handler
     */
    private Handler mPoolThreadHandler;
    /**
     * UI线程中的Handler
     */


    /**
     * 队列的调度方式
     */
    private ImageLoader.Type mType = ImageLoader.Type.LIFO;

    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEAFULT_THREAD_COUNT = 3;
    private Semaphore mSemaphoreThreadPool;


    private Handler mUiHandler;

    public NetworkDispatcher(Context context, Handler uiHandler, BlockingQueue<ImageRequest> nextworkQueue)
    {
        super(context, nextworkQueue);
        mUiHandler = uiHandler;

        // 创建线程池
        mThreadPool = Executors.newFixedThreadPool(DEAFULT_THREAD_COUNT);
        mTaskQueue = new LinkedList<Runnable>();
        mSemaphoreThreadPool = new Semaphore(DEAFULT_THREAD_COUNT);

        HandlerThread mBackThread = new HandlerThread("back-thread");
        mBackThread.start();
        //初始化后台线程
        mPoolThreadHandler = new Handler(mBackThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                // 线程池去取出一个任务进行执行
                mThreadPool.execute(getTask());
                try
                {
                    mSemaphoreThreadPool.acquire();
                } catch (InterruptedException e)
                {
                }
            }
        };
    }

    /**
     * 从任务队列取出一个方法
     *
     * @return
     */
    private Runnable getTask()
    {
        if (mType == ImageLoader.Type.FIFO)
        {
            return mTaskQueue.removeFirst();
        } else if (mType == ImageLoader.Type.LIFO)
        {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    @Override
    public void run()
    {
        L.e(ImageLoader.TAG, "network dispatcher start!!");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while (true)
        {
            try
            {
                final ImageRequest request = mQueue.take();

                if (request.getCancel())
                {
                    continue;
                }
                L.e(ImageLoader.TAG, "network dispatcher path :" + request.getUrl());


                mTaskQueue.add(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            String url = request.getUrl();
                            Log.e(ImageLoader.TAG, "network dispatcher :" + url);
                            NetworkResponse response = HttpHelper.doGet(url);
                            request.bytes2bitmap(response.data);
                            Message msg = Message.obtain(null, ImageLoader.MSG_HTTP_GET_SUCCESS);
                            msg.obj = request;
                            mUiHandler.sendMessage(msg);

                        } catch (HttpError httpError)
                        {
                            httpError.printStackTrace();
                            Message msg = Message.obtain(null, ImageLoader.MSG_HTTP_GET_ERROR);
                            msg.obj = request;
                            mUiHandler.sendMessage(msg);
                        } finally
                        {
                            mSemaphoreThreadPool.release();
                        }
                    }
                });

                mPoolThreadHandler.sendEmptyMessage(0x110);


            } catch (InterruptedException e)
            {
                //如果要求退出则退出，否则遇到异常继续
                if (mQuit) return;
                else continue;
            }
        }
    }
}
