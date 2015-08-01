package com.zhy.base.imageloader.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.zhy.base.imageloader.ImageLoader;
import com.zhy.base.imageloader.R;

/**
 * http://blog.csdn.net/lmj623565791/article/details/41874561
 *
 * @author zhy
 */
public class ListImgsFragment extends Fragment
{
    private GridView mGridView;
    private String[] mUrlStrs = Images.imageThumbUrls;
    private ImageLoader mImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_list_imgs, container,
                false);
        mGridView = (GridView) view.findViewById(R.id.id_gridview);
        setUpAdapter();
        return view;
    }

    private void setUpAdapter()
    {
        if (getActivity() == null || mGridView == null)
            return;

        if (mUrlStrs != null)
        {
            mGridView.setAdapter(new ListImgItemAdaper(getActivity(), 0,
                    mUrlStrs));
        } else
        {
            mGridView.setAdapter(null);
        }

    }

    private class ListImgItemAdaper extends ArrayAdapter<String>
    {

        public ListImgItemAdaper(Context context, int resource, String[] datas)
        {
            super(getActivity(), 0, datas);
            Log.e("TAG", "ListImgItemAdaper");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
                convertView = getActivity().getLayoutInflater().inflate(
                        R.layout.item_fragment_list_imgs, parent, false);
            }
            ImageView imageview = (ImageView) convertView
                    .findViewById(R.id.id_img);
            imageview.setImageResource(R.drawable.pictures_no);
            ImageLoader.with(getContext()).load(getItem(position), imageview);
            return convertView;
        }

    }

}
