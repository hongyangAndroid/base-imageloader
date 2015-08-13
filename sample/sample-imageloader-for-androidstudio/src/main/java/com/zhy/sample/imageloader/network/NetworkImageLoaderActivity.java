package com.zhy.sample.imageloader.network;

import android.support.v4.app.Fragment;

import com.zhy.sample.imageloader.R;


public class NetworkImageLoaderActivity extends AbsSingleFragmentActivity
{
    @Override
    protected Fragment createFragment()
    {
        return new ListImgsFragment();
    }

    @Override
    protected int getLayoutId()
    {
        return R.layout.activity_single_fragment;
    }

}
