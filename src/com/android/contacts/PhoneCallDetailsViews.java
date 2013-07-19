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

import com.android.contacts.calllog.CallTypeIconsView;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

/**
 * Encapsulates the views that are used to display the details of a phone call in the call log.
 */
public final class PhoneCallDetailsViews {
    public final TextView nameView;
    public final View callTypeView;
    public final CallTypeIconsView callTypeIcons;
    public final TextView callTypeAndDate;
    /*Begin: Modified by siliangqi for list_height 2012-4-18*/
    public final TextView callCount;
    /*End: Modified by siliangqi for list_height 2012-4-18*/
    /*Begin: Modified by siliangqi for location_search 2012-4-24*/
    public final TextView location_search;
    /*End: Modified by siliangqi for location_search 2012-4-24*/
    public final TextView numberView;
    public final TextView subscription;

    private PhoneCallDetailsViews(TextView nameView, View callTypeView,
        /*Begin: Modified by siliangqi for list_height 2012-4-18*/
        //CallTypeIconsView callTypeIcons, TextView callTypeAndDate, TextView numberView, TextView subscription) {
        CallTypeIconsView callTypeIcons, TextView callTypeAndDate, TextView callCount,TextView location_search,TextView numberView, TextView subscription) {
        /*End: Modified by siliangqi for list_height 2012-4-18*/
        this.nameView = nameView;
        this.callTypeView = callTypeView;
        this.callTypeIcons = callTypeIcons;
        this.callTypeAndDate = callTypeAndDate;
        /*Begin: Modified by siliangqi for multi_delete 2012-4-18*/
        this.callCount = callCount;
        /*End: Modified by siliangqi for multi_delete 2012-4-18*/
        /* Begin: Modified by siliangqi for location_search 2012-4-24 */
        this.location_search = location_search;
        /* End: Modified by siliangqi for location_search 2012-4-24 */
        this.numberView = numberView;
        this.subscription = subscription;
    }

    /**
     * Create a new instance by extracting the elements from the given view.
     * <p>
     * The view should contain three text views with identifiers {@code R.id.name},
     * {@code R.id.date}, and {@code R.id.number}, and a linear layout with identifier
     * {@code R.id.call_types}.
     */
    public static PhoneCallDetailsViews fromView(View view) {
        return new PhoneCallDetailsViews((TextView) view.findViewById(R.id.name),
                view.findViewById(R.id.call_type),
                (CallTypeIconsView) view.findViewById(R.id.call_type_icons),
                (TextView) view.findViewById(R.id.call_count_and_date),
                /*Begin: Modified by siliangqi for list_height 2012-4-18*/
                (TextView) view.findViewById(R.id.callCount),
                /*End: Modified by siliangqi for list_height 2012-4-18*/
                /* Begin: Modified by siliangqi for location_search 2012-4-24 */
                (TextView) view.findViewById(R.id.location_search),
                /* End: Modified by siliangqi for location_search 2012-4-24 */
                (TextView) view.findViewById(R.id.number),
                (TextView) view.findViewById(R.id.subscription));
    }

    public static PhoneCallDetailsViews createForTest(Context context) {
        return new PhoneCallDetailsViews(
                new TextView(context),
                new View(context),
                new CallTypeIconsView(context),
                new TextView(context),
                /*Begin: Modified by siliangqi for list_height 2012-4-18*/
                new TextView(context),
                /*End: Modified by siliangqi for list_height 2012-4-18*/
                /* Begin: Modified by siliangqi for location_search 2012-4-24 */
                new TextView(context),
                /* End: Modified by siliangqi for location_search 2012-4-24 */
                new TextView(context),
                new TextView(context));
    }
}
