package com.core.bottomnav;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

/**
 * BottomNavConstraintView
 */

@SuppressWarnings("unused")
public class BottomNavConstraintView extends ConstraintLayout implements IBottomNavigation {

    enum DisplayMode {
        SPREAD,
        INSIDE,
        PACKED
    }

    //constants
    private static final String TAG = "BNLView";
    private static final int MIN_ITEMS = 2;
    private static final int MAX_ITEMS = 5;

    private ArrayList<BottomNavToggleView> bottomNavItems;
    private BottomNavChangeListener navigationChangeListener;

    private int currentActiveItemPosition = 0;
    private boolean loadPreviousState;

    //default display mode
    private DisplayMode displayMode = DisplayMode.SPREAD;

    private Typeface currentTypeface;

    private SparseArray<String> pendingBadgeUpdate;

    /**
     * Constructors
     */
    public BottomNavConstraintView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public BottomNavConstraintView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BottomNavConstraintView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putInt("current_item", currentActiveItemPosition);
        bundle.putBoolean("load_prev_state", true);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            currentActiveItemPosition = bundle.getInt("current_item");
            loadPreviousState = bundle.getBoolean("load_prev_state");
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    /////////////////////////////////////////
    // CURRENT ITEM SETTER
    /////////////////////////////////////////
    public Boolean setCurrentItem(int position) {
        if (position < 0 || position == currentActiveItemPosition) {
            return false;
        }
        BottomNavToggleView currentActiveToggleView = bottomNavItems.get(currentActiveItemPosition);
        BottomNavToggleView newActiveToggleView = bottomNavItems.get(position);
        if (currentActiveToggleView != null)
            currentActiveToggleView.toggle();
        if (newActiveToggleView != null)
            newActiveToggleView.toggle();

        //changed the current active position
        currentActiveItemPosition = position;
        return true;
    }

    /////////////////////////////////////////
    // CLICK LISTENER
    /////////////////////////////////////////
    OneClickNavigationListener clickListener = new OneClickNavigationListener() {
        @Override
        public void onOneClick(View v) {

            int changedPosition = getItemPositionById(v.getId());

            if (setCurrentItem(changedPosition) && navigationChangeListener != null)
                navigationChangeListener.onNavigationChanged(v, currentActiveItemPosition);
            else
                Log.w(TAG, "Error! Cannot toggle");
        }
    };

    /////////////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////////////

    /**
     * Initialize
     *
     * @param context current context
     * @param attrs   custom attributes
     */
    private void init(Context context, AttributeSet attrs) {
        int mode = 0;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, com.core.bottomnav.R.styleable.BottomNavConstraintView, 0, 0);
            try {
                mode = ta.getInteger(com.core.bottomnav.R.styleable.BottomNavConstraintView_bnc_mode, mode);
            } finally {
                ta.recycle();
            }
        }

        //sets appropriate display node
        if (mode >= 0 && mode < DisplayMode.values().length)
            displayMode = DisplayMode.values()[mode];

        post(new Runnable() {
            @Override
            public void run() {
                updateChildNavItems();
            }
        });
    }

    /**
     * Get the chain type from the display mode
     *
     * @param mode display mode
     * @return the constraint chain mode
     */
    private int getChainTypeFromMode(DisplayMode mode) {
        switch (mode) {
            case SPREAD:
                return ConstraintSet.CHAIN_SPREAD;
            case INSIDE:
                return ConstraintSet.CHAIN_SPREAD_INSIDE;
            case PACKED:
                return ConstraintSet.CHAIN_PACKED;
        }

        return ConstraintSet.CHAIN_SPREAD;
    }

    /**
     * Finds Child Elements of type {@link BottomNavToggleView} and adds them to {@link #bottomNavItems}
     */
    private void updateChildNavItems() {
        bottomNavItems = new ArrayList<>();
        for (int index = 0; index < getChildCount(); ++index) {
            View view = getChildAt(index);
            if (view instanceof BottomNavToggleView)
                bottomNavItems.add((BottomNavToggleView) view);
            else {
                Log.w(TAG, "Cannot have child bottomNavItems other than BottomNavToggleView");
                return;
            }
        }

        if (bottomNavItems.size() < MIN_ITEMS) {
            Log.w(TAG, "The bottomNavItems list should have at least 2 bottomNavItems of BottomNavToggleView");
        } else if (bottomNavItems.size() > MAX_ITEMS) {
            Log.w(TAG, "The bottomNavItems list should not have more than 5 bottomNavItems of BottomNavToggleView");
        }

        setClickListenerForItems();
        setInitialActiveState();
        updateMeasurementForItems();
        createChains();

        if (currentTypeface != null)
            setTypeface(currentTypeface);

        //update the badge count
        if (pendingBadgeUpdate != null && bottomNavItems != null) {
            for (int i = 0; i < pendingBadgeUpdate.size(); i++)
                setBadgeValue(pendingBadgeUpdate.keyAt(i), pendingBadgeUpdate.valueAt(i));
            pendingBadgeUpdate.clear();
        }
    }

    /**
     * Creates the chains to spread the {@link #bottomNavItems} based on the {@link #displayMode}
     */
    private void createChains() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);

        int[] chainIdsList = new int[bottomNavItems.size()];
        float[] chainWeightList = new float[bottomNavItems.size()];

        for (int i = 0; i < bottomNavItems.size(); i++) {
            int id = bottomNavItems.get(i).getId();
            chainIdsList[i] = id;
            chainWeightList[i] = 0.0f;
            //set the top and bottom constraint for each items
            constraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
            constraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        }

        //create an horizontal chain
        constraintSet.createHorizontalChain(getId(), ConstraintSet.LEFT,
                getId(), ConstraintSet.RIGHT,
                chainIdsList, chainWeightList,
                getChainTypeFromMode(displayMode));

        //apply the constraint
        constraintSet.applyTo(this);
    }

    /**
     * Makes sure that ONLY ONE child {@link #bottomNavItems} is active
     */
    private void setInitialActiveState() {

        if (bottomNavItems == null) return;

        boolean foundActiveElement = false;

        // find the initial state
        if (!loadPreviousState) {
            for (int i = 0; i < bottomNavItems.size(); i++) {
                if (bottomNavItems.get(i).isActive() && !foundActiveElement) {
                    foundActiveElement = true;
                    currentActiveItemPosition = i;
                } else {
                    bottomNavItems.get(i).setInitialState(false);
                }
            }
        } else {
            for (int i = 0; i < bottomNavItems.size(); i++) {
                bottomNavItems.get(i).setInitialState(false);
            }
        }
        //set the active element
        if (!foundActiveElement)
            bottomNavItems.get(currentActiveItemPosition).setInitialState(true);
    }

    /**
     * Update the measurements of the child components {@link #bottomNavItems}
     */
    private void updateMeasurementForItems() {
        int numChildElements = bottomNavItems.size();
        if (numChildElements > 0) {
            int calculatedEachItemWidth = (getMeasuredWidth() - (getPaddingRight() + getPaddingLeft())) / numChildElements;
            for (BottomNavToggleView btv : bottomNavItems)
                btv.updateMeasurements(calculatedEachItemWidth);
        }
    }

    /**
     * Sets {@link View.OnClickListener} for the child views
     */
    private void setClickListenerForItems() {
        for (BottomNavToggleView btv : bottomNavItems)
            btv.setOnClickListener(clickListener);
    }

    /**
     * Gets the Position of the Child from {@link #bottomNavItems} from its id
     *
     * @param id of view to be searched
     * @return position of the Item
     */
    private int getItemPositionById(int id) {
        for (int i = 0; i < bottomNavItems.size(); i++)
            if (id == bottomNavItems.get(i).getId())
                return i;
        return -1;
    }

    ///////////////////////////////////////////
    // PUBLIC METHODS
    ///////////////////////////////////////////

    /**
     * Set the navigation change listener {@link BottomNavChangeListener}
     *
     * @param navigationChangeListener sets the passed parameters as listener
     */
    @Override
    public void setNavigationChangeListener(BottomNavChangeListener navigationChangeListener) {
        this.navigationChangeListener = navigationChangeListener;
    }

    /**
     * Set the {@link Typeface} for the Text Elements of the View
     *
     * @param typeface to be used
     */
    @Override
    public void setTypeface(Typeface typeface) {
        if (bottomNavItems != null) {
            for (BottomNavToggleView btv : bottomNavItems)
                btv.setTitleTypeface(typeface);
        } else {
            currentTypeface = typeface;
        }
    }

    /**
     * Gets the current active position
     *
     * @return active item position
     */
    @Override
    public int getCurrentActiveItemPosition() {
        return currentActiveItemPosition;
    }

    /**
     * Sets the current active item
     *
     * @param position current position change
     */
    @Override
    public void setCurrentActiveItem(int position) {

        if (bottomNavItems == null) {
            currentActiveItemPosition = position;
            return;
        }

        if (currentActiveItemPosition == position) return;

        if (position < 0 || position >= bottomNavItems.size())
            return;

        BottomNavToggleView btv = bottomNavItems.get(position);
        btv.performClick();
    }

    /**
     * Sets the badge value
     *
     * @param position current position change
     * @param value    value to be set in the badge
     */
    @Override
    public void setBadgeValue(int position, String value) {
        if (bottomNavItems != null) {
            BottomNavToggleView btv = bottomNavItems.get(position);
            if (btv != null)
                btv.setBadgeText(value);
        } else {
            if (pendingBadgeUpdate == null)
                pendingBadgeUpdate = new SparseArray<>();
            pendingBadgeUpdate.put(position, value);
        }
    }

}
