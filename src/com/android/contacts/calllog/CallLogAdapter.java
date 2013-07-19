/*
 * Copyright (c) 2012, Code Aurora Forum. All rights reserved.
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.calllog;

import com.android.common.widget.GroupingListAdapter;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.PhoneCallDetails;
import com.android.contacts.PhoneCallDetailsHelper;
import com.android.contacts.R;
import com.android.contacts.util.ExpirableCache;
import com.android.contacts.util.UriUtils;
import com.google.common.annotations.VisibleForTesting;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
/*Start of wangqiang on 2012-3-26 10:4 CallLog_longClick*/
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.view.View.OnCreateContextMenuListener;
import android.util.Log;
import android.view.ContextMenu;
import android.telephony.PhoneNumberUtils;
import com.android.contacts.list.ContactListAdapter;
import com.android.contacts.util.PhoneCapabilityTester;
import android.R.string;
/*End of wangqiang on 2012-3-26 10:4 CallLog_longClick*/

import java.util.LinkedList;

import libcore.util.Objects;
import android.provider.Settings;
/*Begin: Modified by siliangqi for multi_delete 2012-3-26*/
import android.widget.LinearLayout;
import android.widget.CheckBox;
import java.util.HashMap;
/*End: Modified by siliangqi for multi_delete 2012-3-26*/

/*Begin: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/
import android.text.format.DateUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;//add 2012-5-17 17:11
import java.util.Date;
/*End: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/

/**
 * Adapter class to fill in data for the Call Log.
 */
/*package*/ class CallLogAdapter extends GroupingListAdapter
        implements Runnable, ViewTreeObserver.OnPreDrawListener, CallLogGroupBuilder.GroupCreator {
    /** Interface used to initiate a refresh of the content. */
    public interface CallFetcher {
        public void fetchCalls();
    }

    /**
     * Stores a phone number of a call with the country code where it originally occurred.
     * <p>
     * Note the country does not necessarily specifies the country of the phone number itself, but
     * it is the country in which the user was in when the call was placed or received.
     */
    private static final class NumberWithCountryIso {
        public final String number;
        public final String countryIso;

        public NumberWithCountryIso(String number, String countryIso) {
            this.number = number;
            this.countryIso = countryIso;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof NumberWithCountryIso)) return false;
            NumberWithCountryIso other = (NumberWithCountryIso) o;
            return TextUtils.equals(number, other.number)
                    && TextUtils.equals(countryIso, other.countryIso);
        }

        @Override
        public int hashCode() {
            return (number == null ? 0 : number.hashCode())
                    ^ (countryIso == null ? 0 : countryIso.hashCode());
        }
    }

    /** The time in millis to delay starting the thread processing requests. */
    private static final int START_PROCESSING_REQUESTS_DELAY_MILLIS = 1000;

    /** The size of the cache of contact info. */
    private static final int CONTACT_INFO_CACHE_SIZE = 100;

    /*Begin: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/
    //private static String m_strSunrise = null;
    //private static boolean m_bCallLogHeadTodayTag = false;
    private static ArrayList<String> strDateTag = new ArrayList<String>();
    /*End: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/

    private final Context mContext;
    private final ContactInfoHelper mContactInfoHelper;
    private final CallFetcher mCallFetcher;

    /**
     * A cache of the contact details for the phone numbers in the call log.
     * <p>
     * The content of the cache is expired (but not purged) whenever the application comes to
     * the foreground.
     * <p>
     * The key is number with the country in which the call was placed or received.
     */
    private ExpirableCache<NumberWithCountryIso, ContactInfo> mContactInfoCache;

    /**
     * A request for contact details for the given number.
     */
    private static final class ContactInfoRequest {
        /** The number to look-up. */
        public final String number;
        /** The country in which a call to or from this number was placed or received. */
        public final String countryIso;
        /** The cached contact information stored in the call log. */
        public final ContactInfo callLogInfo;

        public ContactInfoRequest(String number, String countryIso, ContactInfo callLogInfo) {
            this.number = number;
            this.countryIso = countryIso;
            this.callLogInfo = callLogInfo;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof ContactInfoRequest)) return false;

            ContactInfoRequest other = (ContactInfoRequest) obj;

            if (!TextUtils.equals(number, other.number)) return false;
            if (!TextUtils.equals(countryIso, other.countryIso)) return false;
            if (!Objects.equal(callLogInfo, other.callLogInfo)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((callLogInfo == null) ? 0 : callLogInfo.hashCode());
            result = prime * result + ((countryIso == null) ? 0 : countryIso.hashCode());
            result = prime * result + ((number == null) ? 0 : number.hashCode());
            return result;
        }
    }

    /**
     * List of requests to update contact details.
     * <p>
     * Each request is made of a phone number to look up, and the contact info currently stored in
     * the call log for this number.
     * <p>
     * The requests are added when displaying the contacts and are processed by a background
     * thread.
     */
    private final LinkedList<ContactInfoRequest> mRequests;

    private volatile boolean mDone;
    private boolean mLoading = true;
    /*Begin: Modified by siliangqi for multi_delete 2012-3-22*/
    private boolean flag_delete = false;
    private OnDeleteListener mOnDeleteListener;
    private HashMap<Object,Boolean> deleteCount;
    private int allDeleteCount;
    /*End: Modified by siliangqi for multi_delete 2012-3-22*/
    private ViewTreeObserver.OnPreDrawListener mPreDrawListener;
    private static final int REDRAW = 1;
    private static final int START_THREAD = 2;

    private boolean mFirst;
    private Thread mCallerIdThread;

    /** Instance of helper class for managing views. */
    private final CallLogListItemHelper mCallLogViewsHelper;

    /** Helper to set up contact photos. */
    private final ContactPhotoManager mContactPhotoManager;
    /** Helper to parse and process phone numbers. */
    private PhoneNumberHelper mPhoneNumberHelper;
    /** Helper to group call log entries. */
    private final CallLogGroupBuilder mCallLogGroupBuilder;

    /** Can be set to true by tests to disable processing of requests. */
    private volatile boolean mRequestProcessingDisabled = false;

    /** Listener for the primary action in the list, opens the call details. */
    private final View.OnClickListener mPrimaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            IntentProvider intentProvider = (IntentProvider) view.getTag();
            /* Begin: Modified by siliangqi for multi_delete 2012-3-26 */
            if (flag_delete) {
                if (view.getClass().getName()
                        .equals("android.widget.LinearLayout")) {
                    LinearLayout ll = (LinearLayout) view;
                    for (int i = 0; i < ll.getChildCount(); i++) {
                        if (ll.getChildAt(i).getClass().getName()
                                .equals("android.widget.CheckBox")) {
                            CheckBox cb = (CheckBox) ll.getChildAt(i);
                            if (cb.isChecked()) {
                                cb.setChecked(false);
                                if (allDeleteCount == getCount()) {
                                    mOnDeleteListener.onClick();
                                }
                                deleteCount.put(cb.getTag(), false);
                                allDeleteCount--;
                            } else {
                                cb.setChecked(true);
                                allDeleteCount++;
                                deleteCount.put(cb.getTag(), true);
                                if (allDeleteCount == getCount()) {
                                    mOnDeleteListener.onClick();
                                }
                            }
                            return;
                        }
                    }
                }
            }
            /* End: Modified by siliangqi for multi_delete 2012-3-26 */
            if (intentProvider != null) {
                mContext.startActivity(intentProvider.getIntent(mContext));
            }
        }
    };
    /*Start of wangqiang on 2012-3-26 10:4 CallLog_longClick*/
    /** Listener for the primary action in the list ,LongClick for the ListView */
    private final OnCreateContextMenuListener mOnCreateContextMenuListener = new OnCreateContextMenuListener() {

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
        }
    };
    /*End of wangqiang on 2012-3-26 10:4 CallLog_longClick*/
    /** Listener for the secondary action in the list, either call or play. */
    private final View.OnClickListener mSecondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            IntentProvider intentProvider = (IntentProvider) view.getTag();
            if (intentProvider != null) {
                mContext.startActivity(intentProvider.getIntent(mContext));
            }
        }
    };

    @Override
    public boolean onPreDraw() {
        if (mFirst) {
            mHandler.sendEmptyMessageDelayed(START_THREAD,
                    START_PROCESSING_REQUESTS_DELAY_MILLIS);
            mFirst = false;
        }
        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REDRAW:
                    notifyDataSetChanged();
                    break;
                case START_THREAD:
                    startRequestProcessing();
                    break;
            }
        }
    };

    CallLogAdapter(Context context, CallFetcher callFetcher,
            ContactInfoHelper contactInfoHelper) {
        super(context);

        mContext = context;
        mCallFetcher = callFetcher;
        mContactInfoHelper = contactInfoHelper;

        mContactInfoCache = ExpirableCache.create(CONTACT_INFO_CACHE_SIZE);
        mRequests = new LinkedList<ContactInfoRequest>();
        mPreDrawListener = null;

        Resources resources = mContext.getResources();
        CallTypeHelper callTypeHelper = new CallTypeHelper(resources);

        mContactPhotoManager = ContactPhotoManager.getInstance(mContext);
        mPhoneNumberHelper = new PhoneNumberHelper(resources);
        PhoneCallDetailsHelper phoneCallDetailsHelper = new PhoneCallDetailsHelper(
                resources, callTypeHelper, mPhoneNumberHelper);
        mCallLogViewsHelper =
                new CallLogListItemHelper(
                        phoneCallDetailsHelper, mPhoneNumberHelper, resources);
        mCallLogGroupBuilder = new CallLogGroupBuilder(this);
        /* Begin: Modified by siliangqi for multi_delete 2012-3-28 */
        deleteCount = new HashMap<Object, Boolean>();
        /* End: Modified by siliangqi for multi_delete 2012-3-28 */
    }

    /**
     * Requery on background thread when {@link Cursor} changes.
     */
    @Override
    protected void onContentChanged() {
        mCallFetcher.fetchCalls();
    }

    void setLoading(boolean loading) {
        mLoading = loading;
    }

    /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
    void setDelete(boolean flag_delete) {
        this.flag_delete = flag_delete;
        deleteCount.clear();
        allDeleteCount = 0;
    }

    boolean getDelete() {
        return flag_delete;
    }

    void setAllChecked() {
        for (int i = 0; i < getCount(); i++)
            deleteCount.put(getItemId(i), true);
        allDeleteCount = getCount();
    }

    void setOnDeleteListener(OnDeleteListener mOnDeleteListener) {
        this.mOnDeleteListener = mOnDeleteListener;
    }

    void setNonChecked() {
        for (int i = 0; i < getCount(); i++) {
            deleteCount.put(getItemId(i), false);
        }
        allDeleteCount = 0;
    }

    /* End: Modified by siliangqi for multi_delete 2012-3-22 */
    @Override
    public boolean isEmpty() {
        if (mLoading) {
            // We don't want the empty state to show when loading.
            return false;
        } else {
            return super.isEmpty();
        }
    }

    /*Begin: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/
    public void resetCallLogDispGroupByDate()
    {
         //m_strSunrise="";
         //m_bCallLogHeadTodayTag = false;
         strDateTag.clear();
    }
    /*End: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/

        /*Start of wangqiang on 2012-3-26 10:18 CallLog_longClick*/
        public ContactInfo getContactInfo(NumberWithCountryIso numberCountryIso) {
            return mContactInfoCache.getPossiblyExpired(numberCountryIso);
        }
        public NumberWithCountryIso getNumberWithCountryIso(String number,String countryIso){
            return new NumberWithCountryIso(number, countryIso);
        }
        /*End of wangqiang on 2012-3-26 10:18 CallLog_longClick*/
    private void startRequestProcessing() {
        if (mRequestProcessingDisabled) {
            return;
        }

        mDone = false;
        mCallerIdThread = new Thread(this, "CallLogContactLookup");
        mCallerIdThread.setPriority(Thread.MIN_PRIORITY);
        mCallerIdThread.start();
    }

    /**
     * Stops the background thread that processes updates and cancels any pending requests to
     * start it.
     * <p>
     * Should be called from the main thread to prevent a race condition between the request to
     * start the thread being processed and stopping the thread.
     */
    public void stopRequestProcessing() {
        // Remove any pending requests to start the processing thread.
        mHandler.removeMessages(START_THREAD);
        mDone = true;
        if (mCallerIdThread != null) mCallerIdThread.interrupt();
    }

    public void invalidateCache() {
        mContactInfoCache.expireAll();
        // Let it restart the thread after next draw
        mPreDrawListener = null;
    }

    /**
     * Enqueues a request to look up the contact details for the given phone number.
     * <p>
     * It also provides the current contact info stored in the call log for this number.
     * <p>
     * If the {@code immediate} parameter is true, it will start immediately the thread that looks
     * up the contact information (if it has not been already started). Otherwise, it will be
     * started with a delay. See {@link #START_PROCESSING_REQUESTS_DELAY_MILLIS}.
     */
    @VisibleForTesting
    void enqueueRequest(String number, String countryIso, ContactInfo callLogInfo,
            boolean immediate) {
        ContactInfoRequest request = new ContactInfoRequest(number, countryIso, callLogInfo);
        synchronized (mRequests) {
            if (!mRequests.contains(request)) {
                mRequests.add(request);
                mRequests.notifyAll();
            }
        }
        if (mFirst && immediate) {
            startRequestProcessing();
            mFirst = false;
        }
    }

    /**
     * Queries the appropriate content provider for the contact associated with the number.
     * <p>
     * Upon completion it also updates the cache in the call log, if it is different from
     * {@code callLogInfo}.
     * <p>
     * The number might be either a SIP address or a phone number.
     * <p>
     * It returns true if it updated the content of the cache and we should therefore tell the
     * view to update its content.
     */
    private boolean queryContactInfo(String number, String countryIso, ContactInfo callLogInfo) {
        final ContactInfo info = mContactInfoHelper.lookupNumber(number, countryIso);

        if (info == null) {
            // The lookup failed, just return without requesting to update the view.
            return false;
        }

        // Check the existing entry in the cache: only if it has changed we should update the
        // view.
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        ContactInfo existingInfo = mContactInfoCache.getPossiblyExpired(numberCountryIso);
        boolean updated = !info.equals(existingInfo);
        // Store the data in the cache so that the UI thread can use to display it. Store it
        // even if it has not changed so that it is marked as not expired.
        mContactInfoCache.put(numberCountryIso, info);
        // Update the call log even if the cache it is up-to-date: it is possible that the cache
        // contains the value from a different call log entry.
        updateCallLogContactInfoCache(number, countryIso, info, callLogInfo);
        return updated;
    }
    /*
     * Handles requests for contact name and number type
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        boolean needNotify = false;
        while (!mDone) {
            ContactInfoRequest request = null;
            synchronized (mRequests) {
                if (!mRequests.isEmpty()) {
                    request = mRequests.removeFirst();
                } else {
                    if (needNotify) {
                        needNotify = false;
                        mHandler.sendEmptyMessage(REDRAW);
                    }
                    try {
                        mRequests.wait(1000);
                    } catch (InterruptedException ie) {
                        // Ignore and continue processing requests
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (!mDone && request != null
                    && queryContactInfo(request.number, request.countryIso, request.callLogInfo)) {
                needNotify = true;
            }
        }
    }

    @Override
    protected void addGroups(Cursor cursor) {
        mCallLogGroupBuilder.addGroups(cursor);
    }

    private String getMultiSimName(int subscription) {
        return Settings.System.getString(mContext.getContentResolver(),
            Settings.System.MULTI_SIM_NAME[subscription]);
    }

    @Override
    protected View newStandAloneView(Context context, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindStandAloneView(View view, Context context, Cursor cursor) {

        final CallLogListItemViews views = (CallLogListItemViews) view.getTag();

        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            int subscription = cursor.getInt(CallLogQuery.SUBSCRIPTION);
            views.subscriptionView.setTag(subscription);
            /*Begin: Modified by siliangqi for call_log_bg 2012-5-28*/
            //views.subscriptionView.setText(getMultiSimName(subscription));
            if(subscription==0)
                views.subscriptionView.setBackgroundResource(R.drawable.call_dial_g_call_icon);
            else
                views.subscriptionView.setBackgroundResource(R.drawable.call_dial_c_call_icon);
            /*End: Modified by siliangqi for call_log_bg 2012-5-28*/
            views.subscriptionView.setVisibility(View.VISIBLE);
        }

        bindView(view, cursor, 1);
    }

    @Override
    protected View newChildView(Context context, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor) {
        bindView(view, cursor, 1);
    }

    @Override
    protected View newGroupView(Context context, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, int groupSize,
            boolean expanded) {
        bindView(view, cursor, groupSize);
    }

    private void findAndCacheViews(View view) {
        // Get the views to bind to.
        CallLogListItemViews views = CallLogListItemViews.fromView(view);
        views.primaryActionView.setOnClickListener(mPrimaryActionListener);
        /*Start of wangqiang on 2012-3-26 10:7 CallLog_longClick*/
        views.primaryActionView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        /*End of wangqiang on 2012-3-26 10:7 CallLog_longClick*/
        views.secondaryActionView.setOnClickListener(mSecondaryActionListener);
        if(TelephonyManager.getDefault().isMultiSimEnabled()) {
            views.subscriptionView = (TextView) view.findViewById(R.id.subscription);
        }
        view.setTag(views);
    }

    /*Begin: Modified by sunrise for CalllogGroupByDate 2012-5-17 14:11*/
    public void initDateTag(Cursor c)
    {
        if (strDateTag.size() != c.getCount())
        {
            int position = c.getPosition();
            System.out.println("initi:position = " + position);
            strDateTag.clear();
            String strBefore = "";
            String strTag = "";
            c.moveToPosition(-1);
            while (c.moveToNext())
            {
                Date dateTag = new Date(c.getLong(CallLogQuery.DATE));
                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
                strTag = dateformat.format(dateTag).toString();
                System.out.println("c.data=" + strTag);
                if (!strTag.equals(strBefore))
                {
                    strDateTag.add(strTag);
                    strBefore = strTag;
                }
                else
                {
                    strDateTag.add("");
                }
            }
            System.out.println(strDateTag.toString());
            c.moveToPosition(position);
        }
    }
    /*End: Modified by sunrise for CalllogGroupByDate 2012-5-17 14:11*/


    /**
     * Binds the views in the entry to the data in the call log.
     *
     * @param view the view corresponding to this entry
     * @param c the cursor pointing to the entry in the call log
     * @param count the number of entries in the current item, greater than 1 if it is a group
     */
    private void bindView(View view, Cursor c, int count) {
        final CallLogListItemViews views = (CallLogListItemViews) view.getTag();
        final int section = c.getInt(CallLogQuery.SECTION);

        // This might be a header: check the value of the section column in the cursor.
        /*Begin: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/
        if (section == CallLogQuery.SECTION_NEW_HEADER
                || section == CallLogQuery.SECTION_OLD_HEADER) {
            views.primaryActionView.setVisibility(View.GONE);
            views.bottomDivider.setVisibility(View.GONE);
            views.listHeaderTextView.setVisibility(View.VISIBLE);
            views.listHeaderTextView.setText(
                    section == CallLogQuery.SECTION_NEW_HEADER
                            ? R.string.call_log_new_header
                            : R.string.call_log_old_header);
            // Nothing else to set up for a header.
            return;
        }

        /*Begin: Modified by sunrise for CalllogGroupByDate 2012-4-11 10:11*/
        /*if(c.getInt(CallLogQuery.CALL_TYPE) == 0)
        {
            return;
        }*/
        /*End: Modified by sunrise for CalllogGroupByDate 2012-5-24 10:11*/
        //initDateTag(c);

        /*final long date = c.getLong(CallLogQuery.DATE);
        Date dateTag = new Date(date);
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
        CharSequence csListHeaderText = dateformat.format(dateTag);
        String sTemp = csListHeaderText.toString();
        System.out.println("c.position:" + c.getPosition() + "  c.count:"
                + c.getCount() + "  c.date= " + sTemp);

        //expose tag if same they are.
        if(sTemp.equals(strDateTag.get(c.getPosition())))
        {
            if(DateUtils.isToday(date))
            {
                views.listHeaderTextView.setText(R.string.call_log_tag_today);
            }
            else
            {
                views.listHeaderTextView.setText(csListHeaderText);
            }
            views.listHeaderTextView.setVisibility(View.VISIBLE);
        }
        else
        {
            views.listHeaderTextView.setVisibility(View.GONE);
        }*/

//        if (DateUtils.isToday(date) && (!m_bCallLogHeadTodayTag))
//        {
//            m_bCallLogHeadTodayTag = true;
//            views.listHeaderTextView.setText(R.string.call_log_tag_today);
//            views.listHeaderTextView.setVisibility(View.VISIBLE);
//            m_strSunrise = sTemp;
//        }
//        else if (!sTemp.equals(m_strSunrise))
//        {
//            views.listHeaderTextView.setText(csListHeaderText);
//            views.listHeaderTextView.setVisibility(View.VISIBLE);
//             m_strSunrise = sTemp;
//        }
//        else
//        {
//            views.listHeaderTextView.setVisibility(View.GONE);
//        }
        /*End: Modified by sunrise for CalllogGroupByDate 2012-4-11 14:11*/

        // Default case: an item in the call log.
        /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
        views.checkbox_delete.setClickable(false);
        if (deleteCount.get((long) c.getInt(c.getColumnIndex("_id"))) != null
                && deleteCount.get((long) c.getInt(c.getColumnIndex("_id"))) == true) {
            views.checkbox_delete.setChecked(true);
            views.checkbox_delete.setTag((long) c.getInt(c.getColumnIndex("_id")));
        } else {
            views.checkbox_delete.setChecked(false);
            views.checkbox_delete.setTag((long) c.getInt(c.getColumnIndex("_id")));
        }
        if (flag_delete == false) {
            views.checkbox_delete.setVisibility(View.GONE);
        }
        else {
            views.checkbox_delete.setVisibility(View.VISIBLE);
            views.quickContactView.setClickable(false);
            views.secondaryActionView.setClickable(false);
        }
        /* End: Modified by siliangqi for multi_delete 2012-3-22 */
        views.primaryActionView.setVisibility(View.VISIBLE);
        views.bottomDivider.setVisibility(isLastOfSection(c) ? View.GONE : View.VISIBLE);
        /*Begin: Modified by sunrise for CalllogGroupByDate 2012/05/19*/
        views.listHeaderTextView.setVisibility(View.GONE);
        /*End: Modified by sunrise for CalllogGroupByDate 2012/05/19*/


        final String number = c.getString(CallLogQuery.NUMBER);
        /*Begin: Modified by sunrise for CalllogGroupByDate 2012/05/19*/
        final long date = c.getLong(CallLogQuery.DATE);
        /*End: Modified by sunrise for CalllogGroupByDate 2012/05/19*/

        final long duration = c.getLong(CallLogQuery.DURATION);
        final int callType = c.getInt(CallLogQuery.CALL_TYPE);
        final String countryIso = c.getString(CallLogQuery.COUNTRY_ISO);
        final int subscription = c.getInt(CallLogQuery.SUBSCRIPTION);

        final ContactInfo cachedContactInfo = getContactInfoFromCallLog(c);

        views.primaryActionView.setTag(
                IntentProvider.getCallDetailIntentProvider(
                        this, c.getPosition(), c.getLong(CallLogQuery.ID), count, subscription));
        // Store away the voicemail information so we can play it directly.
        if (callType == Calls.VOICEMAIL_TYPE) {
            String voicemailUri = c.getString(CallLogQuery.VOICEMAIL_URI);
            final long rowId = c.getLong(CallLogQuery.ID);
            views.secondaryActionView.setTag(
                    IntentProvider.getPlayVoicemailIntentProvider(rowId, voicemailUri, subscription));
        } else if (!TextUtils.isEmpty(number)) {
            // Store away the number so we can call it directly if you click on the call icon.
            views.secondaryActionView.setTag(
                    IntentProvider.getReturnCallIntentProvider(number, subscription));
        } else {
            // No action enabled.
            views.secondaryActionView.setTag(null);
        }

        // Lookup contacts with this number
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        ExpirableCache.CachedValue<ContactInfo> cachedInfo =
                mContactInfoCache.getCachedValue(numberCountryIso);
        ContactInfo info = cachedInfo == null ? null : cachedInfo.getValue();
        if (!mPhoneNumberHelper.canPlaceCallsTo(number)
                || mPhoneNumberHelper.isVoicemailNumber(number)) {
            // If this is a number that cannot be dialed, there is no point in looking up a contact
            // for it.
            info = ContactInfo.EMPTY;
        } else if (cachedInfo == null) {
            mContactInfoCache.put(numberCountryIso, ContactInfo.EMPTY);
            // Use the cached contact info from the call log.
            info = cachedContactInfo;
            // The db request should happen on a non-UI thread.
            // Request the contact details immediately since they are currently missing.
            enqueueRequest(number, countryIso, cachedContactInfo, true);
            // We will format the phone number when we make the background request.
        } else {
            if (cachedInfo.isExpired()) {
                // The contact info is no longer up to date, we should request it. However, we
                // do not need to request them immediately.
                enqueueRequest(number, countryIso, cachedContactInfo, false);
            } else  if (!callLogInfoMatches(cachedContactInfo, info)) {
                // The call log information does not match the one we have, look it up again.
                // We could simply update the call log directly, but that needs to be done in a
                // background thread, so it is easier to simply request a new lookup, which will, as
                // a side-effect, update the call log.
                enqueueRequest(number, countryIso, cachedContactInfo, false);
            }

            if (info == ContactInfo.EMPTY) {
                // Use the cached contact info from the call log.
                info = cachedContactInfo;
            }
        }

        final Uri lookupUri = info.lookupUri;
        /*Begin: Modified by siliangqi for not_in_contacts_unclickable 2012-8-14*/
        if(lookupUri==null){
            views.quickContactView.setClickable(false);
        }else{
            views.quickContactView.setClickable(true);
        }
        /*End: Modified by siliangqi for not_in_contacts_unclickable 2012-8-14*/
        final String name = info.name;
        final int ntype = info.type;
        final String label = info.label;
        final long photoId = info.photoId;
        CharSequence formattedNumber = info.formattedNumber;
        final int[] callTypes = getCallTypes(c, count);
        final String geocode = c.getString(CallLogQuery.GEOCODED_LOCATION);
        final PhoneCallDetails details;
        if (TextUtils.isEmpty(name)) {
            details = new PhoneCallDetails(number, formattedNumber, countryIso, geocode,
                    callTypes, date, duration,subscription);
        } else {
            // We do not pass a photo id since we do not need the high-res picture.
            details = new PhoneCallDetails(number, formattedNumber, countryIso, geocode,
                    callTypes, date, duration, subscription, name, ntype, label, lookupUri, null);
        }

        final boolean isNew = c.getInt(CallLogQuery.IS_READ) == 0;
        // New items also use the highlighted version of the text.
        final boolean isHighlighted = isNew;
        mCallLogViewsHelper.setPhoneCallDetails(views, details, isHighlighted, view.getContext());
        /* Begin: Modified by siliangqi for location_search 2012-4-24 */
        mCallLogViewsHelper.setLocalSearch(views, details, mContext);
        /* End: Modified by siliangqi for location_search 2012-4-24 */
        setPhoto(views, photoId, lookupUri);

        // Listen for the first draw
        if (mPreDrawListener == null) {
            mFirst = true;
            mPreDrawListener = this;
            view.getViewTreeObserver().addOnPreDrawListener(this);
        }
    }

    /** Returns true if this is the last item of a section. */
    private boolean isLastOfSection(Cursor c) {
        if (c.isLast()) return true;
        final int section = c.getInt(CallLogQuery.SECTION);
        if (!c.moveToNext()) return true;
        final int nextSection = c.getInt(CallLogQuery.SECTION);
        c.moveToPrevious();
        return section != nextSection;
    }

    /** Checks whether the contact info from the call log matches the one from the contacts db. */
    private boolean callLogInfoMatches(ContactInfo callLogInfo, ContactInfo info) {
        // The call log only contains a subset of the fields in the contacts db.
        // Only check those.
        return TextUtils.equals(callLogInfo.name, info.name)
                && callLogInfo.type == info.type
                && TextUtils.equals(callLogInfo.label, info.label);
    }

    /** Stores the updated contact info in the call log if it is different from the current one. */
    private void updateCallLogContactInfoCache(String number, String countryIso,
            ContactInfo updatedInfo, ContactInfo callLogInfo) {
        final ContentValues values = new ContentValues();
        boolean needsUpdate = false;

        if (callLogInfo != null) {
            if (!TextUtils.equals(updatedInfo.name, callLogInfo.name)) {
                values.put(Calls.CACHED_NAME, updatedInfo.name);
                needsUpdate = true;
            }

            if (updatedInfo.type != callLogInfo.type) {
                values.put(Calls.CACHED_NUMBER_TYPE, updatedInfo.type);
                needsUpdate = true;
            }

            if (!TextUtils.equals(updatedInfo.label, callLogInfo.label)) {
                values.put(Calls.CACHED_NUMBER_LABEL, updatedInfo.label);
                needsUpdate = true;
            }
            if (!UriUtils.areEqual(updatedInfo.lookupUri, callLogInfo.lookupUri)) {
                values.put(Calls.CACHED_LOOKUP_URI, UriUtils.uriToString(updatedInfo.lookupUri));
                needsUpdate = true;
            }
            if (!TextUtils.equals(updatedInfo.normalizedNumber, callLogInfo.normalizedNumber)) {
                values.put(Calls.CACHED_NORMALIZED_NUMBER, updatedInfo.normalizedNumber);
                needsUpdate = true;
            }
            if (!TextUtils.equals(updatedInfo.number, callLogInfo.number)) {
                values.put(Calls.CACHED_MATCHED_NUMBER, updatedInfo.number);
                needsUpdate = true;
            }
            if (updatedInfo.photoId != callLogInfo.photoId) {
                values.put(Calls.CACHED_PHOTO_ID, updatedInfo.photoId);
                needsUpdate = true;
            }
            if (!TextUtils.equals(updatedInfo.formattedNumber, callLogInfo.formattedNumber)) {
                values.put(Calls.CACHED_FORMATTED_NUMBER, updatedInfo.formattedNumber);
                needsUpdate = true;
            }
            /*Begin: Modified by siliangqi for associate_dial 2012-6-28*/
            if (updatedInfo.rawId!=callLogInfo.rawId) {
                values.put("raw_contact_id", updatedInfo.rawId);
                needsUpdate = true;
            }
            /*End: Modified by siliangqi for associate_dial 2012-6-28*/
        } else {
            // No previous values, store all of them.
            values.put(Calls.CACHED_NAME, updatedInfo.name);
            values.put(Calls.CACHED_NUMBER_TYPE, updatedInfo.type);
            values.put(Calls.CACHED_NUMBER_LABEL, updatedInfo.label);
            values.put(Calls.CACHED_LOOKUP_URI, UriUtils.uriToString(updatedInfo.lookupUri));
            values.put(Calls.CACHED_MATCHED_NUMBER, updatedInfo.number);
            values.put(Calls.CACHED_NORMALIZED_NUMBER, updatedInfo.normalizedNumber);
            values.put(Calls.CACHED_PHOTO_ID, updatedInfo.photoId);
            values.put(Calls.CACHED_FORMATTED_NUMBER, updatedInfo.formattedNumber);
            /*Begin: Modified by siliangqi for associate_dial 2012-6-28*/
            values.put("raw_contact_id", updatedInfo.rawId);
            /*End: Modified by siliangqi for associate_dial 2012-6-28*/
            needsUpdate = true;
        }

        if (!needsUpdate) {
            return;
        }

        if (countryIso == null) {
            mContext.getContentResolver().update(Calls.CONTENT_URI_WITH_VOICEMAIL, values,
                    Calls.NUMBER + " = ? AND " + Calls.COUNTRY_ISO + " IS NULL",
                    new String[]{ number });
        } else {
            mContext.getContentResolver().update(Calls.CONTENT_URI_WITH_VOICEMAIL, values,
                    Calls.NUMBER + " = ? AND " + Calls.COUNTRY_ISO + " = ?",
                    new String[]{ number, countryIso });
        }
    }

    /** Returns the contact information as stored in the call log. */
    private ContactInfo getContactInfoFromCallLog(Cursor c) {
        ContactInfo info = new ContactInfo();
        info.lookupUri = UriUtils.parseUriOrNull(c.getString(CallLogQuery.CACHED_LOOKUP_URI));
        info.name = c.getString(CallLogQuery.CACHED_NAME);
        info.type = c.getInt(CallLogQuery.CACHED_NUMBER_TYPE);
        info.label = c.getString(CallLogQuery.CACHED_NUMBER_LABEL);
        String matchedNumber = c.getString(CallLogQuery.CACHED_MATCHED_NUMBER);
        info.number = matchedNumber == null ? c.getString(CallLogQuery.NUMBER) : matchedNumber;
        info.normalizedNumber = c.getString(CallLogQuery.CACHED_NORMALIZED_NUMBER);
        info.photoId = c.getLong(CallLogQuery.CACHED_PHOTO_ID);
        info.photoUri = null;  // We do not cache the photo URI.
        info.formattedNumber = c.getString(CallLogQuery.CACHED_FORMATTED_NUMBER);
        return info;
    }

    /**
     * Returns the call types for the given number of items in the cursor.
     * <p>
     * It uses the next {@code count} rows in the cursor to extract the types.
     * <p>
     * It position in the cursor is unchanged by this function.
     */
    private int[] getCallTypes(Cursor cursor, int count) {
        int position = cursor.getPosition();
        int[] callTypes = new int[count];
        for (int index = 0; index < count; ++index) {
            callTypes[index] = cursor.getInt(CallLogQuery.CALL_TYPE);
            cursor.moveToNext();
        }
        cursor.moveToPosition(position);
        return callTypes;
    }

    private void setPhoto(CallLogListItemViews views, long photoId, Uri contactUri) {
        views.quickContactView.assignContactUri(contactUri);

        /*Begin: Modified code by bxinchun, set default photo 2012-07-25*/
        //mContactPhotoManager.loadPhoto(views.quickContactView, photoId, false, true);

        long contactId = 0;
        if (contactUri != null) {
            try {
                contactId = ContentUris.parseId(contactUri);
            } catch (Exception e) {
            }
        }
        mContactPhotoManager.loadPhoto(views.quickContactView, photoId, false, true, contactId);
        /*End: Modified code by bxinchun, set default photo 2012-07-25*/
    }

    /**
     * Sets whether processing of requests for contact details should be enabled.
     * <p>
     * This method should be called in tests to disable such processing of requests when not
     * needed.
     */
    @VisibleForTesting
    void disableRequestProcessingForTest() {
        mRequestProcessingDisabled = true;
    }

    @VisibleForTesting
    void injectContactInfoForTest(String number, String countryIso, ContactInfo contactInfo) {
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        mContactInfoCache.put(numberCountryIso, contactInfo);
    }

    @Override
    public void addGroup(int cursorPosition, int size, boolean expanded) {
        super.addGroup(cursorPosition, size, expanded);
    }

    /*
     * Get the number from the Contacts, if available, since sometimes
     * the number provided by caller id may not be formatted properly
     * depending on the carrier (roaming) in use at the time of the
     * incoming call.
     * Logic : If the caller-id number starts with a "+", use it
     *         Else if the number in the contacts starts with a "+", use that one
     *         Else if the number in the contacts is longer, use that one
     */
    public String getBetterNumberFromContacts(String number, String countryIso) {
        String matchingNumber = null;
        // Look in the cache first. If it's not found then query the Phones db
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        ContactInfo ci = mContactInfoCache.getPossiblyExpired(numberCountryIso);
        if (ci != null && ci != ContactInfo.EMPTY) {
            matchingNumber = ci.number;
        } else {
            try {
                Cursor phonesCursor = mContext.getContentResolver().query(
                        Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, number),
                        PhoneQuery._PROJECTION, null, null, null);
                if (phonesCursor != null) {
                    if (phonesCursor.moveToFirst()) {
                        matchingNumber = phonesCursor.getString(PhoneQuery.MATCHED_NUMBER);
                    }
                    phonesCursor.close();
                }
            } catch (Exception e) {
                // Use the number from the call log
            }
        }
        if (!TextUtils.isEmpty(matchingNumber) &&
                (matchingNumber.startsWith("+")
                        || matchingNumber.length() > number.length())) {
            number = matchingNumber;
        }
        return number;
    }
    /* Begin: Modified by siliangqi for multi_delete 2012-3-26 */
    public interface OnDeleteListener {
        public void onClick();
    }

    public HashMap<Object, Boolean> getdeleteCount() {
        return deleteCount;
    }
    /* End: Modified by siliangqi for multi_delete 2012-3-26 */
}
