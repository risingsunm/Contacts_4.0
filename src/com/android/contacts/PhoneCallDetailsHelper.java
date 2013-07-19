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

package com.android.contacts;

import com.android.contacts.calllog.CallTypeHelper;
import com.android.contacts.calllog.PhoneNumberHelper;
import com.android.contacts.format.FormatUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.provider.Settings;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

/*Begin: Modified by sunrise for CallTime 2012/05/19*/
import java.text.SimpleDateFormat;
import java.util.Date;
/*End: Modified by sunrise for CallTime 2012/05/19*/
/*Begin: Modified by siliangqi for location_search 2012-4-24*/
import android.net.Uri;
import java.util.HashMap;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
/*End: Modified by siliangqi for location_search 2012-4-24*/
/**
 * Helper class to fill in the views in {@link PhoneCallDetailsViews}.
 */
public class PhoneCallDetailsHelper {
    /** The maximum number of icons will be shown to represent the call types in a group. */
    /*Begin: Modified by siliangqi for list_height 2012-4-18*/
    //private static final int MAX_CALL_TYPE_ICONS = 3;
    private static final int MAX_CALL_TYPE_ICONS = 1;
    /*End: Modified by siliangqi for list_height 2012-4-18*/

    private final Resources mResources;
    /** The injected current time in milliseconds since the epoch. Used only by tests. */
    private Long mCurrentTimeMillisForTest;
    // Helper classes.
    private final CallTypeHelper mCallTypeHelper;
    private final PhoneNumberHelper mPhoneNumberHelper;

    /**
     * Creates a new instance of the helper.
     * <p>
     * Generally you should have a single instance of this helper in any context.
     *
     * @param resources used to look up strings
     */
    public PhoneCallDetailsHelper(Resources resources, CallTypeHelper callTypeHelper,
            PhoneNumberHelper phoneNumberHelper) {
        /*Begin: Modified by siliangqi for location_search 2012-4-28*/
        if(LocSearchUtil.locSearch==null)
            LocSearchUtil.locSearch = new HashMap < String, String >();
        /*End: Modified by siliangqi for location_search 2012-4-28*/
        mResources = resources;
        mCallTypeHelper = callTypeHelper;
        mPhoneNumberHelper = phoneNumberHelper;
    }

    /* Begin: Modified by siliangqi for location_search 2012-4-24 */
    public void setLocalSearch(PhoneCallDetailsViews views, PhoneCallDetails details,
            Context context) {
        CharSequence displayNumber =
                mPhoneNumberHelper.getDisplayNumber(details.number, details.formattedNumber);
        String myNumber = displayNumber.toString();

        if (LocSearchUtil.locSearch.containsKey(myNumber)) {
            views.location_search.setText(LocSearchUtil.locSearch.get(myNumber));
            return;
        }
        final Context mContext = context;
        final String myNumbers = myNumber;
        final LocationSearch ls = new LocationSearch();
        ls.views = views;
        new Thread() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                ls.myLocation = getAddress(myNumbers, mContext);
                LocSearchUtil.locSearch.put(myNumbers, ls.myLocation);
                Message msg = new Message();
                msg.obj = ls;
                mHandler.sendMessage(msg);
            }

        }.start();

    }

    class LocationSearch {
        public PhoneCallDetailsViews views;
        public String myLocation;
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            ((LocationSearch) msg.obj).views.location_search
                    .setText(((LocationSearch) msg.obj).myLocation);
        }

    };

    private String getAddress(String number, Context context) {
        String result = null;
        Uri uri = Uri.parse("content://com.khong.provider.phonemanager/location/" + number);
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String city = cursor.getString(2);
            result = city;
        } else {
            result = null;
        }
        if (cursor != null)
            cursor.close();
        return result;
    }

    /* End: Modified by siliangqi for location_search 2012-4-24 */
    private String getMultiSimName(Context context, int subscription) {
        if(context == null)
            return "sub" + subscription;
        return Settings.System.getString(context.getContentResolver(),
                Settings.System.MULTI_SIM_NAME[subscription]);
    }

    /** Fills the call details views with content. */
    public void setPhoneCallDetails(PhoneCallDetailsViews views, PhoneCallDetails details,
            boolean isHighlighted, Context context) {
        /*Begin: Modified by siliangqi for call_log_bg 2012-5-28*/
        //views.subscription.setText(getMultiSimName(context,details.subscription));
        if(details.subscription==0)
            views.subscription.setBackgroundResource(R.drawable.call_dial_g_call_icon);
        else
            views.subscription.setBackgroundResource(R.drawable.call_dial_c_call_icon);
        /*End: Modified by siliangqi for call_log_bg 2012-5-28*/
        // Display up to a given number of icons.
        views.callTypeIcons.clear();
        int count = details.callTypes.length;
        for (int index = 0; index < count && index < MAX_CALL_TYPE_ICONS; ++index) {
            views.callTypeIcons.add(details.callTypes[index]);
        }
        views.callTypeIcons.setVisibility(View.VISIBLE);

        // Show the total call count only if there are more than the maximum number of icons.
        final Integer callCount;
        if (count > MAX_CALL_TYPE_ICONS) {
            callCount = count;
        } else {
            callCount = null;
        }
        // The color to highlight the count and date in, if any. This is based on the first call.
        Integer highlightColor =
                isHighlighted ? mCallTypeHelper.getHighlightedColor(details.callTypes[0]) : null;

        // The date of this call, relative to the current time.
        /*Begin: Modified by sunrise for CallTime 2012/05/19*/
        // orignal code:
        /*
         * CharSequence dateText =
         * DateUtils.getRelativeTimeSpanString(details.date,
         * getCurrentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
         * DateUtils.FORMAT_ABBREV_RELATIVE);
         */

        // new code
        Date date = new Date(details.date);
        SimpleDateFormat dateformat = new SimpleDateFormat("HH:mm");
        /*
         * if (DateUtils.isToday(details.date)) { dateformat = new
         * SimpleDateFormat("HH:mm"); } else { dateformat = new
         * SimpleDateFormat("yyyy/MM/dd HH:mm"); }
         */

        CharSequence dateText = dateformat.format(date);
        /* End of modification by sunrise on 2012-4-10 9:58 */

        // Set the call count and date.
        setCallCountAndDate(views, callCount, dateText, highlightColor);

        CharSequence numberFormattedLabel = null;
        // Only show a label if the number is shown and it is not a SIP address.
        if (!TextUtils.isEmpty(details.number)
                && !PhoneNumberUtils.isUriNumber(details.number.toString())) {
            numberFormattedLabel = Phone.getTypeLabel(mResources, details.numberType,
                    details.numberLabel);
            /*Begin: Modified by siliangqi for call_log_bg 2012-5-28*/
            numberFormattedLabel = "";
            /*End: Modified by siliangqi for call_log_bg 2012-5-28*/
        }

        final CharSequence nameText;
        final CharSequence numberText;
        final CharSequence displayNumber =
            mPhoneNumberHelper.getDisplayNumber(details.number, details.formattedNumber);
        if (TextUtils.isEmpty(details.name)) {
            nameText = displayNumber;
            if (TextUtils.isEmpty(details.geocode)
                    || mPhoneNumberHelper.isVoicemailNumber(details.number)) {
                /*Begin: Modified by sunrise for NoContactsName 2012/07/26*/
                //numberText = mResources.getString(R.string.call_log_empty_gecode);
                numberText = mResources.getString(R.string.no_contacts_name);
                /*End: Modified by sunrise for NoContactsName 2012/07/26*/
            } else {
                /*Begin: Modified by sunrise for NoContactsName 2012/07/27*/
                //numberText = details.geocode;
                numberText = mResources.getString(R.string.no_contacts_name);
                /*End: Modified by sunrise for NoContactsName 2012/07/27*/
            }
        } else {
            nameText = details.name;
            if (numberFormattedLabel != null) {
                numberText = FormatUtils.applyStyleToSpan(Typeface.BOLD,
                        numberFormattedLabel + " " + displayNumber, 0,
                        numberFormattedLabel.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                numberText = displayNumber;
            }
        }

        views.nameView.setText(nameText);
        /*Begin: Modified by sunrise for NoContactsName 2012/07/27*/
        //views.numberView.setText(numberText);
        views.numberView.setText(numberText.toString().trim().replace(" ", "").replace("-", ""));
        /*End: Modified by sunrise for NoContactsName 2012/07/27*/

    }

    /** Sets the text of the header view for the details page of a phone call. */
    public void setCallDetailsHeader(TextView nameView, PhoneCallDetails details) {
        final CharSequence nameText;
        final CharSequence displayNumber =
                mPhoneNumberHelper.getDisplayNumber(details.number,
                        mResources.getString(R.string.recentCalls_addToContact));
        if (TextUtils.isEmpty(details.name)) {
            /* Begin: Modified by zxiaona for call_detail 2012/04/10 */
            // nameText = displayNumber;
            nameText = null;
            /* End: Modified by zxiaona for call_detail 2012/04/10 */

        } else {
            nameText = details.name;
        }

        nameView.setText(nameText);
    }

    public void setCurrentTimeForTest(long currentTimeMillis) {
        mCurrentTimeMillisForTest = currentTimeMillis;
    }

    /**
     * Returns the current time in milliseconds since the epoch.
     * <p>
     * It can be injected in tests using {@link #setCurrentTimeForTest(long)}.
     */
    private long getCurrentTimeMillis() {
        if (mCurrentTimeMillisForTest == null) {
            return System.currentTimeMillis();
        } else {
            return mCurrentTimeMillisForTest;
        }
    }

    /** Sets the call count and date. */
    private void setCallCountAndDate(PhoneCallDetailsViews views, Integer callCount,
            CharSequence dateText, Integer highlightColor) {
        // Combine the count (if present) and the date.
        final CharSequence text;
        /* Begin: Modified by siliangqi for list_height 2012-4-18 */
        /*
         * if (callCount != null) { text = mResources.getString(
         * R.string.call_log_item_count_and_date, callCount.intValue(),
         * dateText); } else { text = dateText; }
         */
        text = dateText;
        if (callCount != null)
            views.callCount.setText("(" + callCount + ")");
        /* End: Modified by siliangqi for list_height 2012-4-18 */

        // Apply the highlight color if present.
        final CharSequence formattedText;
        if (highlightColor != null) {
            formattedText = addBoldAndColor(text, highlightColor);
        } else {
            formattedText = text;
        }

        views.callTypeAndDate.setText(formattedText);
    }

    /** Creates a SpannableString for the given text which is bold and in the given color. */
    private CharSequence addBoldAndColor(CharSequence text, int color) {
        int flags = Spanned.SPAN_INCLUSIVE_INCLUSIVE;
        SpannableString result = new SpannableString(text);
        result.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), flags);
        result.setSpan(new ForegroundColorSpan(color), 0, text.length(), flags);
        return result;
    }
}
