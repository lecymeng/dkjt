package com.sspai.dkjt.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.sspai.dkjt.R;
import com.sspai.dkjt.model.BooleanPreference;
import com.sspai.dkjt.model.DeviceProvider;
import com.sspai.dkjt.prefs.GlareEnabled;
import com.sspai.dkjt.prefs.ShadowEnabled;
import com.sspai.dkjt.ui.adapter.DeviceFragmentPagerAdapter;
import javax.inject.Inject;

/**
 * FIXME
 *
 * @author Makoshan
 * @date 2015-01-10 17:25
 */
public class HomeFragment extends BaseFragment {
  @Inject @GlareEnabled BooleanPreference glareEnabled;
  @Inject @ShadowEnabled BooleanPreference shadowEnabled;
  @Inject DeviceProvider deviceProvider;
  @Inject WindowManager windowManager;
  private ViewPager pager;
  //private PagerSlidingTabStrip tabStrip;
  private TabLayout mTabLayout;
  DeviceFragmentPagerAdapter pagerAdapter;
  private String mCategory;

  public static HomeFragment newInstance (String mCategory) {
    HomeFragment fragment = new HomeFragment();
    Bundle bundle = new Bundle();
    bundle.putString("Category", mCategory);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_home, container, false);
    pager = rootView.findViewById(R.id.pager);
    //tabStrip = rootView.findViewById(R.id.tabs);
    mTabLayout=rootView.findViewById(R.id.tab_layout);
    parseArgument();
    initView();
    return rootView;
  }

  private void initView () {
    pagerAdapter = new DeviceFragmentPagerAdapter(getFragmentManager(), deviceProvider.asList(mCategory));
    pager.setAdapter(pagerAdapter);
    pager.setCurrentItem(pagerAdapter.getDeviceIndex(deviceProvider.getDefaultDevice()));
    //tabStrip.setTextColor(Utils.getColor(getActivity(), R.color.title_text_color, Color.WHITE));
    //tabStrip.setViewPager(pager);
    mTabLayout.setupWithViewPager(pager);
  }

  //获得分类字段
  private void parseArgument () {
    Bundle bundle = getArguments();
    mCategory = bundle.getString("Category");
  }
}
