package com.zhy.base.imageloader.activity;

import android.support.v4.app.Fragment;

import com.zhy.base.imageloader.R;

public class MainActivity extends AbsSingleFragmentActivity
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
