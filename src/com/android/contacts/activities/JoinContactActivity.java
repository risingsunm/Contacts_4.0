/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.activities;


import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.JoinContactListFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.widget.PinnedHeaderListView;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;

/**
 * An activity that shows a list of contacts that can be joined with the target contact.
 */
public class JoinContactActivity extends ContactsActivity {

    private static final String TAG = "JoinContactActivity";

    /**
     * The action for the join contact activity.
     * <p>
     * Input: extra field {@link #EXTRA_TARGET_CONTACT_ID} is the aggregate ID.
     * TODO: move to {@link ContactsContract}.
     */
    public static final String JOIN_CONTACT = "com.android.contacts.action.JOIN_CONTACT";

    /**
     * Used with {@link #JOIN_CONTACT} to give it the target for aggregation.
     * <p>
     * Type: LONG
     */
    public static final String EXTRA_TARGET_CONTACT_ID = "com.android.contacts.action.CONTACT_ID";

    private static final String KEY_TARGET_CONTACT_ID = "targetContactId";

    private long mTargetContactId;
    /* Begin: Modified by zxiaona for call_detail_joincontacts 2012/04/20 */
    private String mNumber;
    /* End: Modified by zxiaona for call_detail_joincontacts 2012/04/20 */

    private JoinContactListFragment mListFragment;

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof JoinContactListFragment) {
            mListFragment = (JoinContactListFragment) fragment;
            setupActionListener();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        /* Begin: Modified by zxiaona for call_detail_joincontacts 2012/04/20 */
        mNumber = intent.getStringExtra("number");
        /* End: Modified by zxiaona for call_detail_joincontacts 2012/04/20 */
        mTargetContactId = intent.getLongExtra(EXTRA_TARGET_CONTACT_ID, -1);
        /* Begin: Modified by zxiaona for call_detail_joincontacts 2012/04/20 */
        // if (mTargetContactId == -1) {
        // Log.e(TAG, "Intent " + intent.getAction() +
        // " is missing required extra: "
        // + EXTRA_TARGET_CONTACT_ID);
        // setResult(RESULT_CANCELED);
        // finish();
        // return;
        // }
        if (mNumber == null)
        {
            if (mTargetContactId == -1) {
                Log.e(TAG, "Intent " + intent.getAction() + " is missing required extra: "
                        + EXTRA_TARGET_CONTACT_ID);
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
        /* End: Modified by zxiaona for call_detail_joincontacts 2012/04/20 */

        setContentView(R.layout.join_contact_picker);
        setTitle(R.string.titleJoinContactDataWith);

        if (mListFragment == null) {
            mListFragment = new JoinContactListFragment();

            getFragmentManager().beginTransaction()
                    .replace(R.id.list_container, mListFragment)
                    .commitAllowingStateLoss();
        }

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            /*Begin: Modified by siliangqi for unit_contact 2011-6-7*/
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_actionbar_bg));
            /*End: Modified by siliangqi for unit_contact 2011-6-7*/
        }
    }

/*Begin: Modified by xiepengfei for bug 2012/06/14*/
    @Override
    protected void onStart() {
        super.onStart();
            super.onStart();
            /*Begin: Modified by xiepengfei for add email query 2012/06/12*/
            ListView listview = (ListView)mListFragment.getListView();
            if(listview == null){
            }else if(listview instanceof PinnedHeaderListView){
                PinnedHeaderListView list = (PinnedHeaderListView)listview;
                list.setTouchMode(false);
            }
            /*End: Modified by xiepengfei for add email query 2012/06/12*/

    }

/*End: Modified by xiepengfei for bug 2012/06/14*/

    private void setupActionListener() {
        /* Begin: Modified by zxiaona for call_detail_joincontacts 2012/04/21 */
        mListFragment.setJoinedNumber(mNumber);
        /* End: Modified by zxiaona for call_detail_joincontacts 2012/04/21 */
        mListFragment.setTargetContactId(mTargetContactId);
        mListFragment.setOnContactPickerActionListener(new OnContactPickerActionListener() {
            @Override
            public void onPickContactAction(Uri contactUri) {
                Intent intent = new Intent(null, contactUri);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onShortcutIntentCreated(Intent intent) {
            }

            @Override
            public void onCreateNewContactAction() {
            }

            /*
             * Begin: Modified by zxiaona for call_detail_joincontacts
             * 2012/04/21
             */
            // @Override
            // public void onEditContactAction(Uri contactLookupUri) {
            // }
            @Override
            public void onEditContactAction(Uri contactLookupUri) {
                Intent it = new Intent(Intent.ACTION_EDIT, contactLookupUri);
                it.putExtra(Insert.PHONE, mNumber);
                startActivity(it);
                finish();
            }
            /* End: Modified by zxiaona for call_detail_joincontacts 2012/04/21 */
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back to previous screen, intending "cancel"
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_TARGET_CONTACT_ID, mTargetContactId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTargetContactId = savedInstanceState.getLong(KEY_TARGET_CONTACT_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ContactEntryListFragment.ACTIVITY_REQUEST_CODE_PICKER
                && resultCode == RESULT_OK) {
            mListFragment.onPickerResult(data);
        }
    }
}