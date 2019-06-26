package com.core.bottomnav;

import android.os.Handler;
import android.view.View;


public abstract class OneClickNavigationListener implements View.OnClickListener {

    private static final int DEFAULT_ANIM_DURATION = 400;

    private boolean clickable = true;

    /**
     * Override onOneClick() instead.
     */
    @Override
    public final void onClick(View v) {
        if (clickable) {
            clickable = false;
            onOneClick(v);

            // allow clicks after animation finishes
            new Handler().postDelayed(this::reset, DEFAULT_ANIM_DURATION);
        }
    }

    /**
     * Override this function to handle clicks.
     * reset() must be called after each click for this function to be called
     * again.
     */
    public abstract void onOneClick(View v);

    /**
     * Allows another click.
     */
    private void reset() {
        clickable = true;
    }
}