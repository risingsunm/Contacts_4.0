/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.contacts.list;

import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.util.AccountFilterUtil;
/*Begin: Modified by siliangqi for word_index 2012-5-17*/
import com.android.contacts.widget.BladeView;
/*End: Modified by siliangqi for word_index 2012-5-17*/

import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.provider.Settings;
import com.android.internal.telephony.SubscriptionManager;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;

/*Begin: Modified by siliangqi for 1lev_search 2012-4-10*/
import android.widget.SearchView.OnQueryTextListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
/*End: Modified by siliangqi for 1lev_search 2012-4-10*/
/*Begin: Modified by siliangqi for word_index 2012-5-17*/
import android.graphics.Color;
/*End: Modified by siliangqi for word_index 2012-5-17*/

/*Begin: Modified by xiepengfei for add touch listview 2012/06/07*/
import com.android.contacts.widget.PinnedHeaderListView;
import com.android.contacts.widget.TouchListView.TriggerListener;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
/*End: Modified by xiepengfei for add touch listview 2012/06/07*/
/**
 * Fragment containing a contact list used for browsing (as compared to
 * picking a contact with one of the PICK intents).
 */
public class DefaultContactBrowseListFragment extends ContactBrowseListFragment {
    private static final String TAG = DefaultContactBrowseListFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    private TextView mCounterHeaderView;
    private View mSearchHeaderView;
    private View mAccountFilterHeader;
    private FrameLayout mProfileHeaderContainer;
    private View mProfileHeader;
    private Button mProfileMessage;
    private FrameLayout mMessageContainer;
    private TextView mProfileTitle;
    /*Begin: Modified by siliangqi for word_index 2012-5-17*/
    private BladeView mBladeView;
    /*End: Modified by siliangqi for word_index 2012-5-17*/
    /*Begin: Modified by siliangqi for 1lev_search 2012-4-10*/
    private EditText_Search mEditText_Search;
    private ImageButton imageButton_search;
    private OnQueryTextListener q;
    /*End: Modified by siliangqi for 1lev_search 2012-4-10*/

    private View mPaddingView;

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                        DefaultContactBrowseListFragment.this, REQUEST_CODE_ACCOUNT_FILTER);
        }
    }
    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    public DefaultContactBrowseListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
    }

    @Override
    public CursorLoader createCursorLoader() {
        return new ProfileAndContactsLoader(getActivity());
    }

    @Override
    protected void onItemClick(int position, long id) {
        viewContact(getAdapter().getContactUri(position));
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter adapter = new DefaultContactListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        /* Begin: Modified by siliangqi for word_index 2012-5-17 */
        mBladeView = (BladeView) getView().findViewById(R.id.category);
        /*if (mBladeView != null) {
            mBladeView.setEnableSectionColor(Color.argb(0xFF, 0x6C, 0x6C, 0x6C));
            mBladeView.setDisableSectionColor(Color.argb(0xFF, 0x6C, 0x6C, 0x6C));
        }*/
        /* End: Modified by siliangqi for word_index 2012-5-17 */
        /* Begin: Modified by siliangqi for 1lev_search 2012-4-10 */
        mEditText_Search = (EditText_Search) getView().findViewById(R.id.editText_search);
        mEditText_Search.setQueryTextListener(q);
        imageButton_search = (ImageButton) getView().findViewById(R.id.imageButton_search);
        imageButton_search.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                final Activity activity = getActivity();
                if (activity != null) {
                    final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                    activity.startActivity(intent);
                }
            }
        });
        /* End: Modified by siliangqi for 1lev_search 2012-4-10 */
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        mCounterHeaderView = (TextView) getView().findViewById(R.id.contacts_count);

        // Create an empty user profile header and hide it for now (it will be visible if the
        // contacts list will have no user profile).
        addEmptyUserProfileHeader(inflater);
        showEmptyUserProfile(false);

        // Putting the header view inside a container will allow us to make
        // it invisible later. See checkHeaderViewVisibility()
        FrameLayout headerContainer = new FrameLayout(inflater.getContext());
        mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
        headerContainer.addView(mSearchHeaderView);
        getListView().addHeaderView(headerContainer, null, false);

        /*Begin: Modified by xiepengfei for add touch listview 2012/06/06*/
        ((PinnedHeaderListView)getListView()).setTriggerListener(mTriggerListener);
        /*End: Modified by xiepengfei for add touch listview 2012/06/06*/
        /* Begin: Modified by siliangqi for word_index 2012-5-17 */
        mBladeView.setList(getListView());
        /* End: Modified by siliangqi for word_index 2012-5-17 */
        checkHeaderViewVisibility();
    }

    /* Begin: Modified by siliangqi for 1lev_search 2012-4-10 */
    public void setEditText_SearchQueryTextListener(OnQueryTextListener q) {
        this.q = q;
    }

    /* End: Modified by siliangqi for 1lev_search 2012-4-10 */
    @Override
    protected void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        checkHeaderViewVisibility();
    }

    private void checkHeaderViewVisibility() {
        if (mCounterHeaderView != null) {
            mCounterHeaderView.setVisibility(isSearchMode() ? View.GONE : View.VISIBLE);
        }
        updateFilterHeaderView();

        // Hide the search header by default. See showCount().
        if (mSearchHeaderView != null) {
            mSearchHeaderView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setFilter(ContactListFilter filter) {
        super.setFilter(filter);
        updateFilterHeaderView();
    }

    private void updateFilterHeaderView() {
        if (mAccountFilterHeader == null) {
            return; // Before onCreateView -- just ignore it.
        }
        final ContactListFilter filter = getFilter();
        if (filter != null && !isSearchMode()) {
            final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPeople(
                    mAccountFilterHeader, filter, false, false);
            mAccountFilterHeader.setVisibility(shouldShowHeader ? View.VISIBLE : View.GONE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    @Override
    protected void showCount(int partitionIndex, Cursor data) {
        if (!isSearchMode() && data != null) {
            int count = data.getCount();
            if (count != 0) {
                count -= (mUserProfileExists ? 1: 0);
                String format = getResources().getQuantityText(
                        R.plurals.listTotalAllContacts, count).toString();
                // Do not count the user profile in the contacts count
                if (mUserProfileExists) {
                    getAdapter().setContactsCount(String.format(format, count));
                } else {
                    mCounterHeaderView.setText(String.format(format, count));
                }
            } else {
                ContactListFilter filter = getFilter();
                int filterType = filter != null ? filter.filterType
                        : ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS;
                switch (filterType) {
                    case ContactListFilter.FILTER_TYPE_ACCOUNT:
                        mCounterHeaderView.setText(getString(
                                R.string.listTotalAllContactsZeroGroup, filter.accountName));
                        break;
                    case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                        mCounterHeaderView.setText(R.string.listTotalPhoneContactsZero);
                        break;
                    case ContactListFilter.FILTER_TYPE_STARRED:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZeroStarred);
                        break;
                    case ContactListFilter.FILTER_TYPE_CUSTOM:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZeroCustom);
                        break;
                    default:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZero);
                        break;
                }
            }
        } else {
            ContactListAdapter adapter = getAdapter();
            if (adapter == null) {
                return;
            }

            // In search mode we only display the header if there is nothing found
            if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
                mSearchHeaderView.setVisibility(View.GONE);
            } else {
                TextView textView = (TextView) mSearchHeaderView.findViewById(
                        R.id.totalContactsText);
                ProgressBar progress = (ProgressBar) mSearchHeaderView.findViewById(
                        R.id.progress);
                mSearchHeaderView.setVisibility(View.VISIBLE);
                if (adapter.isLoading()) {
                    textView.setText(R.string.search_results_searching);
                    progress.setVisibility(View.VISIBLE);
                } else {
                    textView.setText(R.string.listFoundAllContactsZero);
                    textView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
                    progress.setVisibility(View.GONE);
                }
            }
            showEmptyUserProfile(false);
        }
    }

    @Override
    protected void setProfileHeader() {
        mUserProfileExists = getAdapter().hasProfile();
        showEmptyUserProfile(!mUserProfileExists && !isSearchMode());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(
                        ContactListFilterController.getInstance(getActivity()), resultCode, data);
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    private void showEmptyUserProfile(boolean show) {
        // Changing visibility of just the mProfileHeader doesn't do anything unless
        // you change visibility of its children, hence the call to mCounterHeaderView
        // and mProfileTitle
        mProfileHeaderContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileHeader.setVisibility(show ? View.VISIBLE : View.GONE);
        mCounterHeaderView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        mMessageContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileMessage.setVisibility(show ? View.VISIBLE : View.GONE);

        mPaddingView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * This method creates a pseudo user profile contact. When the returned query doesn't have
     * a profile, this methods creates 2 views that are inserted as headers to the listview:
     * 1. A header view with the "ME" title and the contacts count.
     * 2. A button that prompts the user to create a local profile
     */
    private void addEmptyUserProfileHeader(LayoutInflater inflater) {

        ListView list = getListView();
        // Put a header with the "ME" name and a view for the number of contacts
        // The view is embedded in a frame view since you cannot change the visibility of a
        // view in a ListView without having a parent view.
        mProfileHeaderContainer = new FrameLayout(inflater.getContext());
        mProfileHeader = inflater.inflate(R.layout.user_profile_header, null, false);
        mCounterHeaderView = (TextView) mProfileHeader.findViewById(R.id.contacts_count);
        mProfileTitle = (TextView) mProfileHeader.findViewById(R.id.profile_title);
        mProfileTitle.setAllCaps(true);
        mProfileHeaderContainer.addView(mProfileHeader);
        list.addHeaderView(mProfileHeaderContainer, null, false);

        // Add a selectable view with a message inviting the user to create a local profile
        mMessageContainer = new FrameLayout(inflater.getContext());
        mProfileMessage = (Button)inflater.inflate(R.layout.user_profile_button, null, false);
        mMessageContainer.addView(mProfileMessage);
        list.addHeaderView(mMessageContainer, null, true);

        mProfileMessage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                intent.putExtra(ContactEditorFragment.INTENT_EXTRA_NEW_LOCAL_PROFILE, true);
                startActivity(intent);
            }
        });

        View paddingViewContainer =
                inflater.inflate(R.layout.contact_detail_list_padding, null, false);
        mPaddingView = paddingViewContainer.findViewById(R.id.contact_detail_list_padding);
        mPaddingView.setVisibility(View.GONE);
        getListView().addHeaderView(paddingViewContainer);
    }

    /*Begin: Modified by xiepengfei for add touch listview 2012/06/06*/
    private TriggerListener mTriggerListener = new TriggerListener(){

        public void onTrigger(int position, int actionType) {
            String number = null;
            String firstNumber = null;
            String primaryPhone = null;

            /*Begin: Modified by xiepengfei for bug nullPointerException 2012/06/09*/
            String contactId = null;
            try{
                contactId = String.valueOf(getAdapter().getContactId(position - getListView().getHeaderViewsCount()));
            }catch (Exception e) {
                Log.e(TAG,e.toString());
            } finally {
                if (TextUtils.isEmpty(contactId)) {
                    return;
                }
            }
            /*End: Modified by xiepengfei for bug nullPointerException 2012/06/09*/

            String [ ] proj = { Phone.NUMBER, Phone.DISPLAY_NAME, Phone._ID, Phone.IS_SUPER_PRIMARY };

            Cursor cursor = null;
            try {
                cursor = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        proj, Phone.CONTACT_ID + " = ?", new String [ ] { contactId }, null);

                if (cursor != null && cursor.moveToFirst()) {
                    firstNumber = cursor.getString(0);
                    //System.out.println("firstNumber is:"+firstNumber);
                    final int indexIsPrimary = cursor.getColumnIndex(Phone.IS_SUPER_PRIMARY);
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        if (cursor.getInt(indexIsPrimary) != 0) {
                            // Found super primary, call it.
                            primaryPhone = cursor.getString(0);
                            //System.out.println("primaryPhone:" + primaryPhone);
                            break;
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if(!TextUtils.isEmpty(firstNumber)){
                number = firstNumber;
            }
            if(!TextUtils.isEmpty(primaryPhone)){
                number = primaryPhone;
            }
            if(TextUtils.isEmpty(number)) return;
            System.out.println("number is:"+number);
            switch (actionType) {
                case LEFT://sms
                    Uri DstNum2 = Uri.parse("smsto:" + number);
                    Intent telIntent2 = new Intent( Intent.ACTION_SENDTO, DstNum2);
                    startActivity(telIntent2);
                    break;
                case RIGHT://call
                    Uri DstNum = Uri.parse("tel:" + number);
                    Intent telIntent = new Intent( Intent.ACTION_CALL_PRIVILEGED, DstNum);
                    /*Begin: Modified by sliangqi for main_call 2012-8-25*/
                    int subscription = 0;
                    if (isMultiSimAvailable()) {
                        try {
                            subscription = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.MULTI_SIM_VOICE_CALL_SUBSCRIPTION);
                        } catch (SettingNotFoundException snfe) {
                            Log.e(TAG, "Settings Exception Reading Dual Sim Voice Call Values");
                        }
                    } else {
                        subscription = getDefaultSubscription();
                    }
                    telIntent.putExtra("subscription", subscription);
                    telIntent.putExtra("directDial", true);
                    /*End: Modified by sliangqi for main_call 2012-8-25*/
                    startActivity(telIntent);
                    break;
                default:
                    break;
            }

        }

    };
    /*End: Modified by xiepengfei for add touch listview 2012/06/06*/
    /*Begin: Modified by sliangqi for main_call 2012-8-25*/
    public int getDefaultSubscription() {
        int subscription = 0;
        try {
            subscription = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.DEFAULT_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Default Subscription");
        }

        return subscription;
    }
    private boolean isMultiSimAvailable() {
        return TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("READY")&&TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("READY");
    }
    /*End: Modified by sliangqi for main_call 2012-8-25*/
}
