package com.core.bottomnav;

import android.graphics.Typeface;

@SuppressWarnings("unused")
public interface IBottomNavigation {
    void setNavigationChangeListener(BottomNavChangeListener navigationChangeListener);

    void setTypeface(Typeface typeface);

    int getCurrentActiveItemPosition();

    void setCurrentActiveItem(int position);

    void setBadgeValue(int position, String value);
}
