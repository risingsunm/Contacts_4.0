/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2012, Code Aurora Forum. All rights reserved.
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

import com.android.contacts.PhoneCallDetailsViews;
import com.android.contacts.R;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
/*Begin: Modified by siliangqi for multi_delete 2012-3-22*/
import android.widget.CheckBox;

/*End: Modified by siliangqi for multi_delete 2012-3-22*/

/**
 * Simple value object containing the various views within a call log entry.
 */
public final class CallLogListItemViews {
    /** The quick contact badge for the contact. */
    public final QuickContactBadge quickContactView;
    /** The primary action view of the entry. */
    public final View primaryActionView;
    /** The secondary action button on the entry. */
    public final ImageView secondaryActionView;
    /** The divider between the primary and secondary actions. */
    public final View dividerView;
    /** The details of the phone call. */
    public final PhoneCallDetailsViews phoneCallDetailsViews;
    /** The text of the header of a section. */
    public final TextView listHeaderTextView;
    /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
    public final CheckBox checkbox_delete;
    /* End: Modified by siliangqi for multi_delete 2012-3-22 */
    /** The divider to be shown below items. */
    public final View bottomDivider;
	/** The details of the sim card. */
	public TextView subscriptionView;

    private CallLogListItemViews(QuickContactBadge quickContactView, View primaryActionView,
            ImageView secondaryActionView, View dividerView,
            PhoneCallDetailsViews phoneCallDetailsViews,
            /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
            // TextView listHeaderTextView, View bottomDivider) {
            TextView listHeaderTextView, CheckBox checkbox_delete, View bottomDivider) {
            /* End: Modified by siliangqi for multi_delete 2012-3-22 */
        this.quickContactView = quickContactView;
        this.primaryActionView = primaryActionView;
        this.secondaryActionView = secondaryActionView;
        this.dividerView = dividerView;
        this.phoneCallDetailsViews = phoneCallDetailsViews;
        this.listHeaderTextView = listHeaderTextView;
        /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
        this.checkbox_delete = checkbox_delete;
        /* End: Modified by siliangqi for multi_delete 2012-3-22 */
        this.bottomDivider = bottomDivider;
    }

    public static CallLogListItemViews fromView(View view) {
        return new CallLogListItemViews(
                (QuickContactBadge) view.findViewById(R.id.quick_contact_photo),
                view.findViewById(R.id.primary_action_view),
                (ImageView) view.findViewById(R.id.secondary_action_icon),
                view.findViewById(R.id.divider),
                PhoneCallDetailsViews.fromView(view),
                (TextView) view.findViewById(R.id.call_log_header),
                /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
                (CheckBox) view.findViewById(R.id.checkBox_delete),
                /* End: Modified by siliangqi for multi_delete 2012-3-22 */
                view.findViewById(R.id.call_log_divider));
    }

    public static CallLogListItemViews createForTest(Context context) {
        return new CallLogListItemViews(
                new QuickContactBadge(context),
                new View(context),
                new ImageView(context),
                new View(context),
                PhoneCallDetailsViews.createForTest(context),
                new TextView(context),
                /*Begin: Modified by siliangqi for multi_delete 2012-3-22*/
                new CheckBox(context),
                /*End: Modified by siliangqi for multi_delete 2012-3-22*/
                new View(context));
    }
}
