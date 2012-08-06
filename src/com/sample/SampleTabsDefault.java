package com.sample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import com.kovalenych.R;
import com.viewpagerindicator.TabPageIndicator;

public class SampleTabsDefault extends BaseSampleActivity {
    private static final String[] CONTENT = new String[] { "Recent", "Artists", "Albums", "Songs", "Playlists", "Genres" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_tabs);

        mAdapter = new GoogleMusicAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (TabPageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }

    class GoogleMusicAdapter extends TestFragmentAdapter {
        public GoogleMusicAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return TestFragment.newInstance(SampleTabsDefault.CONTENT[position % SampleTabsDefault.CONTENT.length]);
        }

        @Override
        public int getCount() {
            return SampleTabsDefault.CONTENT.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return SampleTabsDefault.CONTENT[position % SampleTabsDefault.CONTENT.length].toUpperCase();
        }
    }
}
