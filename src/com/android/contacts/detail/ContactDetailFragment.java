/*
 * Copyright (c) 2012, Code Aurora Forum. All rights reserved.
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
 * limitations under the License
 */

package com.android.contacts.detail;

import java.util.ArrayList;
import android.telephony.MSimTelephonyManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.Entity.NamedContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.LocalGroup;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.LocalGroups.Group;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.Collapser;
import com.android.contacts.Collapser.Collapsible;
import com.android.contacts.ContactLoader;
import com.android.contacts.ContactPresenceIconUtil;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsUtils;
import com.android.contacts.GroupMetaData;
import com.android.contacts.R;
import com.android.contacts.SimContactsConstants;
import com.android.contacts.TypePrecedence;
import com.android.contacts.activities.ContactDetailActivity.FragmentKeyListener;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountType.EditType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityDeltaList;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.util.Constants;
import com.android.contacts.util.DataStatus;
import com.android.contacts.util.DateUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.StructuredPostalUtils;
import com.android.contacts.widget.TransitionAnimationView;
import com.android.internal.telephony.ITelephony;
import com.google.common.annotations.VisibleForTesting;

/*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/22*/
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.ContactsContract.Intents.UI;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Adapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.provider.LocalGroups;
/*End: Modified by xiepengfei for ContentObserver to local group 2012/05/22*/

/*Begin: Modified by sunrise for RingTone 2012/08/21*/
import java.io.File;
import java.io.IOException;
/*End: Modified by sunrise for RingTone 2012/08/21*/

public class ContactDetailFragment extends Fragment implements FragmentKeyListener, ViewOverlay,
        SelectAccountDialogFragment.Listener, OnItemClickListener {

    private static final String TAG = "ContactDetailFragment";

    private interface ContextMenuIds {
        static final int COPY_TEXT = 0;
        static final int CLEAR_DEFAULT = 1;
        static final int SET_DEFAULT = 2;
        static final int IPCALL = 4;    // add for new feature: ip call prefix
        static final int EDIT_BEFORE_CALL = 5;    // add for new feature: edit before call
    }

    private static final String KEY_CONTACT_URI = "contactUri";
    private static final String KEY_LIST_STATE = "liststate";

    // TODO: Make maxLines a field in {@link DataKind}
    private static final int WEBSITE_MAX_LINES = 1;
    private static final int SIP_ADDRESS_MAX_LINES= 1;
    private static final int POSTAL_ADDRESS_MAX_LINES = 10;
    private static final int GROUP_MAX_LINES = 10;
    private static final int NOTE_MAX_LINES = 100;

    private Context mContext;
    private View mView;
    private OnScrollListener mVerticalScrollListener;
    private Uri mLookupUri;
    private Listener mListener;

    private ContactLoader.Result mContactData;
    private ImageView mStaticPhotoView;
    private ListView mListView;
    private ViewAdapter mAdapter;
    private Uri mPrimaryPhoneUri = null;
    private ViewEntryDimensions mViewEntryDimensions;

    private Button mQuickFixButton;
    private QuickFix mQuickFix;
    private int mNumPhoneNumbers = 0;
    private String mDefaultCountryIso;
    private boolean mContactHasSocialUpdates;
    private boolean mShowStaticPhoto = true;
    /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/18*/
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";
    public static final String CALL_ORIGIN_DIALTACTS =
            "com.android.contacts.activities.DialtactsActivity";
    public static final String cCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",
            0, "");
    public static final String gCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",
            1, "");
    /*End: Modified by wqiang for modified ContactDetail View 2012/08/18*/


    private final QuickFix[] mPotentialQuickFixes = new QuickFix[] {
            new MakeLocalCopyQuickFix(),
            new AddToMyContactsQuickFix() };

    /**
     * Device capability: Set during buildEntries and used in the long-press context menu
     */
    private boolean mHasPhone;

    /**
     * Device capability: Set during buildEntries and used in the long-press context menu
     */
    private boolean mHasSms;

    /**
     * Device capability: Set during buildEntries and used in the long-press context menu
     */
    private boolean mHasSip;

    /**
     * The view shown if the detail list is empty.
     * We set this to the list view when first bind the adapter, so that it won't be shown while
     * we're loading data.
     */
    private View mEmptyView;

    /**
     * Initial alpha value to set on the alpha layer.
     */
    private float mInitialAlphaValue;

    /**
     * This optional view adds an alpha layer over the entire fragment.
     */
    private View mAlphaLayer;

    /**
     * This optional view adds a layer over the entire fragment so that when visible, it intercepts
     * all touch events on the fragment.
     */
    private View mTouchInterceptLayer;

    /**
     * Saved state of the {@link ListView}. This must be saved and applied to the {@ListView} only
     * when the adapter has been populated again.
     */
    private Parcelable mListState;

    /**
     * A list of distinct contact IDs included in the current contact.
     */
    private ArrayList<Long> mRawContactIds = new ArrayList<Long>();
    private ArrayList<DetailViewEntry> mPhoneEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mSmsEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mEmailEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mPostalEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mImEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mNicknameEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mGroupEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mRelationEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mNoteEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mWebsiteEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mSipEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mEventEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mLocalGroupEntries = new ArrayList<DetailViewEntry>();

    /*Begin: Modified by xiepengfei for display local groups and ringtone 2012/03/27*/
    private ArrayList<DetailViewEntry> mOtherEntries = new ArrayList<DetailViewEntry>();
    private MenuItem copyMenu;
    private MenuItem shareContactInfo;
    /*End: Modified by xiepengfei for display local groups and ringtone 2012/03/27*/


    /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
    private HashMap<Integer, String> mMapContactInfo = new HashMap<Integer, String>();
    private boolean isMultipleChoiceMode = false;
    public final static String CONTACT_DETAIL_FRAGMENT_LIST_CHOICE_MODE = "choice_mode";
//    private MenuItem completeMenu;
//    private MenuItem cancelMenu;
    private View bottomToolbar;
    public boolean mdisableIntent = false;
    /*End: Modified by xiepengfei for share info by sms 2012/04/01*/

    private final Map<AccountType, List<DetailViewEntry>> mOtherEntriesMap =
            new HashMap<AccountType, List<DetailViewEntry>>();
    private ArrayList<ViewEntry> mAllEntries = new ArrayList<ViewEntry>();
    private LayoutInflater mInflater;

    private boolean mTransitionAnimationRequested;

    private boolean mIsUniqueNumber;
    private boolean mIsUniqueEmail;

    public ContactDetailFragment() {
        // Explicit constructor for inflation
    }

    /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
    private LocalGroupContent mLocalGroupContent;
    /*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/

/*Begin: Modified by xiepengfei for set default phone 2012/06/09*/
    private MenuItem setDefaultPhone;
    private MenuItem sendContactSMS;
/*End: Modified by xiepengfei for set default phone 2012/06/09*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLookupUri = savedInstanceState.getParcelable(KEY_CONTACT_URI);
            mListState = savedInstanceState.getParcelable(KEY_LIST_STATE);

            /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
            isMultipleChoiceMode = savedInstanceState.getBoolean(CONTACT_DETAIL_FRAGMENT_LIST_CHOICE_MODE);
            /*End: Modified by xiepengfei for share info by sms 2012/04/01*/
        }
        /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
        mLocalGroupContent = new LocalGroupContent(new Handler());
        getActivity().getContentResolver().registerContentObserver(LocalGroups.CONTENT_URI, true, mLocalGroupContent);
        /*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CONTACT_URI, mLookupUri);
        if (mListView != null) {
            outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /*Begin: Modified by xiepengfei for clear list  2012/03/28*/
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"Destroy ");
        clearAllArrayList();

        /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
        getActivity().getContentResolver().unregisterContentObserver(mLocalGroupContent);
        /*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/

    }
    /*End: Modified by xiepengfei for  clear list 2012/03/28*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mDefaultCountryIso = ContactsUtils.getCurrentCountryIso(mContext);
        mViewEntryDimensions = new ViewEntryDimensions(mContext.getResources());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        /*Begin: Modified by xiepengfei for  2012/03/28*/
        setHasOptionsMenu(true);
        /*End: Modified by xiepengfei for  2012/03/28*/
        mView = inflater.inflate(R.layout.contact_detail_fragment, container, false);

        mInflater = inflater;

        mStaticPhotoView = (ImageView) mView.findViewById(R.id.photo);

        mListView = (ListView) mView.findViewById(android.R.id.list);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
        mListView.setOnItemClickListener(this);
        mListView.setItemsCanFocus(true);
        mListView.setOnScrollListener(mVerticalScrollListener);

        // Don't set it to mListView yet.  We do so later when we bind the adapter.
        mEmptyView = mView.findViewById(android.R.id.empty);

        mTouchInterceptLayer = mView.findViewById(R.id.touch_intercept_overlay);
        mAlphaLayer = mView.findViewById(R.id.alpha_overlay);
        ContactDetailDisplayUtils.setAlphaOnViewBackground(mAlphaLayer, mInitialAlphaValue);

        mQuickFixButton = (Button) mView.findViewById(R.id.contact_quick_fix);
        mQuickFixButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQuickFix != null) {
                    mQuickFix.execute();
                }
            }
        });

        mView.setVisibility(View.INVISIBLE);

        if (mContactData != null) {
            bindData();
        }

/*Begin: Modified by xiepengfei for add bottom toolbar 2012/06/13*/
        bottomToolbar = mView.findViewById(R.id.bottom_toolbar);
        bottomToolbar.setVisibility(View.GONE);
        Button ok = (Button) bottomToolbar.findViewById(R.id.btn_ok);
        ok.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (isMultipleChoiceMode) {
                    isMultipleChoiceMode = false;
                    mAdapter.setMultipleChoiceMode(isMultipleChoiceMode);
                    mAdapter.notifyDataSetInvalidated();
                    updataDetailFragmentMenu();

                    // start to sms
                    sendContactInfoBySMS();
                    mMapContactInfo.clear();
                }
            }
        });
        Button cancel = (Button)bottomToolbar.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (isMultipleChoiceMode) {
                    isMultipleChoiceMode = false;
                    mAdapter.setMultipleChoiceMode(isMultipleChoiceMode);
                    mAdapter.notifyDataSetInvalidated();
                    updataDetailFragmentMenu();
                    mMapContactInfo.clear();
                }
            }
        });
/*End: Modified by xiepengfei for add bottom toolbar 2012/06/13*/
        return mView;
    }

    protected View inflate(int resource, ViewGroup root, boolean attachToRoot) {
        return mInflater.inflate(resource, root, attachToRoot);
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    @Override
    public void setAlphaLayerValue(float alpha) {
        // If the alpha layer is not ready yet, store it for later when the view is initialized
        if (mAlphaLayer == null) {
            mInitialAlphaValue = alpha;
        } else {
            // Otherwise set the value immediately
            ContactDetailDisplayUtils.setAlphaOnViewBackground(mAlphaLayer, alpha);
        }
    }

    @Override
    public void enableTouchInterceptor(OnClickListener clickListener) {
        if (mTouchInterceptLayer != null) {
            mTouchInterceptLayer.setVisibility(View.VISIBLE);
            mTouchInterceptLayer.setOnClickListener(clickListener);
        }
    }

    @Override
    public void disableTouchInterceptor() {
        if (mTouchInterceptLayer != null) {
            mTouchInterceptLayer.setVisibility(View.GONE);
        }
    }

    protected Context getContext() {
        return mContext;
    }

    protected Listener getListener() {
        return mListener;
    }

    protected ContactLoader.Result getContactData() {
        return mContactData;
    }

    public void setVerticalScrollListener(OnScrollListener listener) {
        mVerticalScrollListener = listener;
    }

    public Uri getUri() {
        return mLookupUri;
    }

    /**
     * Sets whether the static contact photo (that is not in a scrolling region), should be shown
     * or not.
     */
    public void setShowStaticPhoto(boolean showPhoto) {
        mShowStaticPhoto = showPhoto;
    }

    public void showEmptyState() {
        setData(null, null);
    }

    public void setData(Uri lookupUri, ContactLoader.Result result) {
        mLookupUri = lookupUri;
        mContactData = result;

        /*Begin: Modified by xiepengfei for clear all arrayList data 2012/05/17*/
        clearAllArrayList();
        /*End: Modified by xiepengfei for clear all arrayList data 2012/05/17*/

        bindData();
    }

    /**
     * Reset the list adapter in this {@link Fragment} to get rid of any saved scroll position
     * from a previous contact.
     */
    public void resetAdapter() {
        if (mListView != null) {
            mListView.setAdapter(mAdapter);
        }
    }

    /**
     * Returns the top coordinate of the first item in the {@link ListView}. If the first item
     * in the {@link ListView} is not visible or there are no children in the list, then return
     * Integer.MIN_VALUE. Note that the returned value will be <= 0 because the first item in the
     * list cannot have a positive offset.
     */
    public int getFirstListItemOffset() {
        return ContactDetailDisplayUtils.getFirstListItemOffset(mListView);
    }

    /**
     * Tries to scroll the first item to the given offset (this can be a no-op if the list is
     * already in the correct position).
     * @param offset which should be <= 0
     */
    public void requestToMoveToOffset(int offset) {
        ContactDetailDisplayUtils.requestToMoveToOffset(mListView, offset);
    }

    protected void bindData() {
        if (mView == null) {
            return;
        }

        if (isAdded()) {
            getActivity().invalidateOptionsMenu();
        }

        if (mTransitionAnimationRequested) {
            TransitionAnimationView.startAnimation(mView, mContactData == null);
            mTransitionAnimationRequested = false;
        }

        if (mContactData == null) {
            mView.setVisibility(View.INVISIBLE);
            mAllEntries.clear();
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
            return;
        }

        // Figure out if the contact has social updates or not
        mContactHasSocialUpdates = !mContactData.getStreamItems().isEmpty();

        // Setup the photo if applicable
        if (mStaticPhotoView != null) {
            // The presence of a static photo view is not sufficient to determine whether or not
            // we should show the photo. Check the mShowStaticPhoto flag which can be set by an
            // outside class depending on screen size, layout, and whether the contact has social
            // updates or not.
            if (mShowStaticPhoto) {
                mStaticPhotoView.setVisibility(View.VISIBLE);
                ContactDetailDisplayUtils.setPhoto(mContext, mContactData, mStaticPhotoView);
            } else {
                mStaticPhotoView.setVisibility(View.GONE);
            }
        }

        // Build up the contact entries
        buildEntries();

        // Collapse similar data items for select {@link DataKind}s.
//        Collapser.collapseList(mPhoneEntries);
//        Collapser.collapseList(mSmsEntries);
//        Collapser.collapseList(mEmailEntries);
//        Collapser.collapseList(mPostalEntries);
//        Collapser.collapseList(mImEntries);

        mIsUniqueNumber = mPhoneEntries.size() == 1;
        mIsUniqueEmail = mEmailEntries.size() == 1;

        // Make one aggregated list of all entries for display to the user.
        setupFlattenedList();

        if (mAdapter == null) {
            mAdapter = new ViewAdapter();
            mListView.setAdapter(mAdapter);
        }

        // Restore {@link ListView} state if applicable because the adapter is now populated.
        if (mListState != null) {
            mListView.onRestoreInstanceState(mListState);
            mListState = null;
        }

        mAdapter.notifyDataSetChanged();

        mListView.setEmptyView(mEmptyView);

        configureQuickFix();

        mView.setVisibility(View.VISIBLE);

        /*Begin: Modified by xiepengfei for update menu 2012/03/29*/
        updataDetailFragmentMenu();
        /*End: Modified by xiepengfei forupdate menu 2012/03/29*/
    }

    /*
     * Sets {@link #mQuickFix} to a useful action and configures the visibility of
     * {@link #mQuickFixButton}
     */
    private void configureQuickFix() {
        mQuickFix = null;

        for (QuickFix fix : mPotentialQuickFixes) {
            if (fix.isApplicable()) {
                mQuickFix = fix;
                break;
            }
        }

        // Configure the button
        if (mQuickFix == null) {
            mQuickFixButton.setVisibility(View.GONE);
        } else {
            mQuickFixButton.setVisibility(View.VISIBLE);
            mQuickFixButton.setText(mQuickFix.getTitle());
        }
    }

    /** @return default group id or -1 if no group or several groups are marked as default */
    private long getDefaultGroupId(List<GroupMetaData> groups) {
        long defaultGroupId = -1;
        for (GroupMetaData group : groups) {
            if (group.isDefaultGroup()) {
                // two default groups? return neither
                if (defaultGroupId != -1) return -1;
                defaultGroupId = group.getGroupId();
            }
        }
        return defaultGroupId;
    }

    /**
     * Build up the entries to display on the screen.
     */
    private final void buildEntries() {
        mHasPhone = PhoneCapabilityTester.isPhone(mContext);
        mHasSms = PhoneCapabilityTester.isSmsIntentRegistered(mContext);
        mHasSip = PhoneCapabilityTester.isSipPhone(mContext);

        // Clear out the old entries
        mAllEntries.clear();

        mRawContactIds.clear();

        mPrimaryPhoneUri = null;
        mNumPhoneNumbers = 0;

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);

        // Build up method entries
        if (mContactData == null) {
            return;
        }

        ArrayList<String> groups = new ArrayList<String>();
        for (Entity entity: mContactData.getEntities()) {
            final ContentValues entValues = entity.getEntityValues();
            final String accountType = entValues.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = entValues.getAsString(RawContacts.DATA_SET);
            final long rawContactId = entValues.getAsLong(RawContacts._ID);

            if (!mRawContactIds.contains(rawContactId)) {
                mRawContactIds.add(rawContactId);
            }

            AccountType type = accountTypes.getAccountType(accountType, dataSet);

            for (NamedContentValues subValue : entity.getSubValues()) {
                final ContentValues entryValues = subValue.values;
                entryValues.put(Data.RAW_CONTACT_ID, rawContactId);

                final long dataId = entryValues.getAsLong(Data._ID);
                final String mimeType = entryValues.getAsString(Data.MIMETYPE);
                if (mimeType == null) continue;

                if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    Long groupId = entryValues.getAsLong(GroupMembership.GROUP_ROW_ID);
                    if (groupId != null) {
                        handleGroupMembership(groups, mContactData.getGroupMetaData(), groupId);
                    }
                    continue;
                }

                final DataKind kind = accountTypes.getKindOrFallback(
                        accountType, dataSet, mimeType);
                if (kind == null) continue;

                final DetailViewEntry entry = DetailViewEntry.fromValues(mContext, mimeType, kind,
                        dataId, entryValues, mContactData.isDirectoryEntry(),
                        mContactData.getDirectoryId());

                final boolean hasData = !TextUtils.isEmpty(entry.data);
                Integer superPrimary = entryValues.getAsInteger(Data.IS_SUPER_PRIMARY);
                final boolean isSuperPrimary = superPrimary != null && superPrimary != 0;

                if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    // Always ignore the name. It is shown in the header if set
                } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build phone entries
                    mNumPhoneNumbers++;
                    String phoneNumberE164 =
                            entryValues.getAsString(PhoneLookup.NORMALIZED_NUMBER);
                    entry.data = PhoneNumberUtils.formatNumber(
                            entry.data, phoneNumberE164, mDefaultCountryIso);
                    final Intent phoneIntent = mHasPhone ? new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts(Constants.SCHEME_TEL, entry.data, null)) : null;
                    final Intent smsIntent = mHasSms ? new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(Constants.SCHEME_SMSTO, entry.data, null)) : null;
                    /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
                    final Intent C_PhoneIntent =  mHasPhone ? new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts(Constants.SCHEME_TEL, entry.data, null)) : null;
                    final Intent G_PhoneIntent =  mHasPhone ? new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts(Constants.SCHEME_TEL, entry.data, null)) : null;
                    /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/
                    // Configure Icons and Intents.
                    if (mHasPhone && mHasSms) {
                        entry.intent = phoneIntent;
                        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
                        entry.cCardIntent = C_PhoneIntent;
                        entry.gCardIntent = G_PhoneIntent;
                        entry.cCardActionIcon =R.drawable.calldetail_c;
                        entry.gCardActionIcon =R.drawable.calldetail_g;
                        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/

                        entry.secondaryIntent = smsIntent;
                        /*Begin: Modified by xiepengfei for UI modify 2012/04/17*/
//                        entry.secondaryActionIcon = kind.iconAltRes;
                        entry.secondaryActionIcon = R.drawable.contact_detail_view_message;
                        /*End: Modified by xiepengfei for UI modify 2012/04/17*/
                        entry.secondaryActionDescription = kind.iconAltDescriptionRes;
                    } else if (mHasPhone) {
                        entry.intent = phoneIntent;
                    } else if (mHasSms) {
                        entry.intent = smsIntent;
                    } else {
                        entry.intent = null;
                    }

                    // Remember super-primary phone
                    if (isSuperPrimary) mPrimaryPhoneUri = entry.uri;

                    entry.isPrimary = isSuperPrimary;
                    mPhoneEntries.add(entry);
                } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build email entries
                    entry.intent = new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(Constants.SCHEME_MAILTO, entry.data, null));
                    entry.isPrimary = isSuperPrimary;
                    mEmailEntries.add(entry);

                    // When Email rows have status, create additional Im row
                    final DataStatus status = mContactData.getStatuses().get(entry.id);
                    if (status != null) {
                        final String imMime = Im.CONTENT_ITEM_TYPE;
                        final DataKind imKind = accountTypes.getKindOrFallback(accountType, dataSet,
                                imMime);
                        final DetailViewEntry imEntry = DetailViewEntry.fromValues(mContext, imMime,
                                imKind, dataId, entryValues, mContactData.isDirectoryEntry(),
                                mContactData.getDirectoryId());
                        buildImActions(mContext, imEntry, entryValues);
                        imEntry.applyStatus(status, false);
                        mImEntries.add(imEntry);
                    }
                } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build postal entries
                    entry.maxLines = POSTAL_ADDRESS_MAX_LINES;
                    entry.intent = StructuredPostalUtils.getViewPostalAddressIntent(entry.data);
                    mPostalEntries.add(entry);
                } else if (Im.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build IM entries
                    buildImActions(mContext, entry, entryValues);

                    // Apply presence and status details when available
                    final DataStatus status = mContactData.getStatuses().get(entry.id);
                    if (status != null) {
                        entry.applyStatus(status, false);
                    }
                    mImEntries.add(entry);
                } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    // Organizations are not shown. The first one is shown in the header
                    // and subsequent ones are not supported anymore
                } else if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build nickname entries
                    final boolean isNameRawContact =
                        (mContactData.getNameRawContactId() == rawContactId);

                    final boolean duplicatesTitle =
                        isNameRawContact
                        && mContactData.getDisplayNameSource() == DisplayNameSources.NICKNAME;

                    if (!duplicatesTitle) {
                        entry.uri = null;
                        mNicknameEntries.add(entry);
                    }
                } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build note entries
                    entry.uri = null;
                    entry.maxLines = NOTE_MAX_LINES;
                    mNoteEntries.add(entry);
                } else if (Website.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build Website entries
                    entry.uri = null;
                    entry.maxLines = WEBSITE_MAX_LINES;
                    try {
                        WebAddress webAddress = new WebAddress(entry.data);
                        entry.intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(webAddress.toString()));
                    } catch (ParseException e) {
                        Log.e(TAG, "Couldn't parse website: " + entry.data);
                    }
                    mWebsiteEntries.add(entry);
                } else if (SipAddress.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    // Build SipAddress entries
                    entry.uri = null;
                    entry.maxLines = SIP_ADDRESS_MAX_LINES;
                    if (mHasSip) {
                        entry.intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                Uri.fromParts(Constants.SCHEME_SIP, entry.data, null));
                    } else {
                        entry.intent = null;
                    }
                    mSipEntries.add(entry);
                    // TODO: Now that SipAddress is in its own list of entries
                    // (instead of grouped in mOtherEntries), consider
                    // repositioning it right under the phone number.
                    // (Then, we'd also update FallbackAccountType.java to set
                    // secondary=false for this field, and tweak the weight
                    // of its DataKind.)
                } else if (Event.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    entry.data = DateUtils.formatDate(mContext, entry.data);
                    entry.uri = null;
                    mEventEntries.add(entry);
                } else if (Relation.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    entry.intent = new Intent(Intent.ACTION_SEARCH);
                    entry.intent.putExtra(SearchManager.QUERY, entry.data);
                    entry.intent.setType(Contacts.CONTENT_TYPE);
                    mRelationEntries.add(entry);
                } else if (LocalGroup.CONTENT_ITEM_TYPE.equals(mimeType) && hasData) {
                    Uri data = ContentUris.withAppendedId(LocalGroup.CONTENT_URI,
                            +Long.parseLong(entry.data));
                    Intent intent = new Intent(Intent.ACTION_EDIT, data);
                    /*Begin: Modified by sliangqi for group_add 2012-8-21*/
                    intent.putExtra("typestring", R.string.label_groups);
                    intent.putExtra("inGroup",true);
                    //intent.putExtra("data", data);
                    //intent.setType("vnd.android.cursor.item/local-groups");
                    /*End: Modified by sliangqi for group_add 2012-8-21*/
                    entry.intent = intent;

                    /*Begin: Modified by xiepengfei for local group 2012/05/17*/
                    /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/18*/
                    entry.typeString = " ";
                    /*End: Modified by wqiang for modified ContactDetail View 2012/08/18*/
                    entry.secondaryIntent = intent;
                    entry.secondaryActionIcon = R.drawable.right_arrow;
                    entry.secondaryActionDescription = R.string.contact_detail_fragment_groups;
                    /*End: Modified by xiepengfei for local group 2012/05/17*/
                    mLocalGroupEntries.add(entry);
                } else {
                    // Handle showing custom rows
                    entry.intent = new Intent(Intent.ACTION_VIEW);
                    entry.intent.setDataAndType(entry.uri, entry.mimetype);

                    if (kind.actionBody != null) {
                         CharSequence body = kind.actionBody.inflateUsing(mContext, entryValues);
                         entry.data = (body == null) ? null : body.toString();
                    }

                    if (!TextUtils.isEmpty(entry.data)) {
                        // If the account type exists in the hash map, add it as another entry for
                        // that account type
                        if (mOtherEntriesMap.containsKey(type)) {
                            List<DetailViewEntry> listEntries = mOtherEntriesMap.get(type);
                            listEntries.add(entry);
                        } else {
                            // Otherwise create a new list with the entry and add it to the hash map
                            List<DetailViewEntry> listEntries = new ArrayList<DetailViewEntry>();
                            listEntries.add(entry);
                            mOtherEntriesMap.put(type, listEntries);
                        }
                    }
                }
            }
        }

        if (!groups.isEmpty()) {
            DetailViewEntry entry = new DetailViewEntry();
            Collections.sort(groups);
            StringBuilder sb = new StringBuilder();
            int size = groups.size();
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(groups.get(i));
            }
            entry.mimetype = GroupMembership.MIMETYPE;
            entry.kind = mContext.getString(R.string.groupsLabel);
            entry.data = sb.toString();
            entry.maxLines = GROUP_MAX_LINES;
            mGroupEntries.add(entry);
        }
        /*Begin: Modified by sliangqi for group_add 2012-8-21*/
        ContentValues entValues = mContactData.getEntities().get(0).getEntityValues();
        String accountType = entValues.getAsString(RawContacts.ACCOUNT_TYPE);
        if(mLocalGroupEntries.isEmpty()&&accountType.equals("com.android.localphone")){
                    DetailViewEntry entry = new DetailViewEntry();
                    Uri data = ContentUris.withAppendedId(LocalGroup.CONTENT_URI,
                            +1);
                    Intent intent = new Intent();
                    intent.putExtra("typestring", R.string.label_groups);
                    intent.putExtra("inGroup",false);
                    entry.intent = intent;
                    entry.data = mContext.getString(R.string.contact_detail_fragment_not_assigned);
                    entry.mimetype = LocalGroup.MIMETYPE;
                    entry.kind = mContext.getString(R.string.label_groups);
                    entry.typeString = mContext.getString(R.string.label_groups);
                    entry.secondaryIntent = intent;
                    entry.secondaryActionIcon = R.drawable.right_arrow;
                    entry.secondaryActionDescription = R.string.contact_detail_fragment_groups;
                    mLocalGroupEntries.add(entry);
          }
        /*End: Modified by sliangqi for group_add 2012-8-21*/

        /*Begin: Modified by xiepengfei for test 2012/03/27*/
        mOtherEntries.clear();
//        DetailViewEntry localGroupEntry = new DetailViewEntry();
//        localGroupEntry.kind = getString(R.string.contact_detail_fragment_other);
//        localGroupEntry.data = getString(R.string.contact_detail_fragment_not_assigned);
//        localGroupEntry.typeString = getString(R.string.contact_detail_fragment_groups);
//        /*Begin: Modified by xiepengfei for share info by sms 2012/04/10*/
//        //localGroupEntry.secondaryIntent = new Intent();
//        localGroupEntry.secondaryIntent = new Intent().putExtra("typestring", R.string.contact_detail_fragment_groups);
//        /*End: Modified by xiepengfei for share info by sms 2012/04/10*/
//        localGroupEntry.secondaryActionIcon = R.drawable.right_arrow;
//        localGroupEntry.secondaryActionDescription = R.string.contact_detail_fragment_groups;
        //mOtherEntries.add(localGroupEntry);

        DetailViewEntry ringtoneEntry = new DetailViewEntry();
        ringtoneEntry.kind = getString(R.string.contact_detail_fragment_other);
        ringtoneEntry.typeString = getString(R.string.contact_detail_fragment_ringtone);
        /* Begin: Modified by xiepengfei for share info by sms 2012/04/10 */
        // ringtoneEntry.secondaryIntent = new Intent();
        ringtoneEntry.intent = new Intent().putExtra("typestring",
                R.string.contact_detail_fragment_ringtone);
        ringtoneEntry.secondaryIntent = new Intent().putExtra("typestring",
                R.string.contact_detail_fragment_ringtone);
        /* End: Modified by xiepengfei for share info by sms 2012/04/10 */
        ringtoneEntry.secondaryActionIcon = R.drawable.right_arrow;
        ringtoneEntry.secondaryActionDescription = R.string.contact_detail_fragment_ringtone;
        String customRing = mContactData.getCustomRingtone();

        /*Begin: Modified by sunrise for RingTone 2012/08/21*/
        /*
        if (TextUtils.isEmpty(customRing)) {
            ringtoneEntry.data = getString(R.string.contact_detail_fragment_default_ringtone);
        } else {
            ringtoneEntry.data = ContactDetailDisplayUtils.getRingtoneTitle(getActivity(),
                    mContactData.getCustomRingtone());
        }
        */
        if (TextUtils.isEmpty(customRing)) {
            ringtoneEntry.data = getString(R.string.contact_detail_fragment_default_ringtone);
        }
        else
        {
            String strPath = customRing.substring(7);
            System.out.println("sunrise:" + strPath);
            File file = new File(strPath);
            System.out.println(strPath + file.exists());
            if(file.exists())
            {
                ringtoneEntry.data = ContactDetailDisplayUtils.getRingtoneTitle(getActivity(),
                        mContactData.getCustomRingtone());
            }
            else
            {
                ringtoneEntry.data = getString(R.string.contact_detail_fragment_default_ringtone);
            }
        }
        /*End: Modified by sunrise for RingTone 2012/08/21*/

        mOtherEntries.add(ringtoneEntry);
        Log.v(TAG, "Eden CustomRingtone:" + mContactData.getCustomRingtone());
        /* End: Modified by xiepengfei for test2012/03/27 */
    }

    /**
     * Collapse all contact detail entries into one aggregated list with a {@link HeaderViewEntry}
     * at the top.
     */
    private void setupFlattenedList() {
        // All contacts should have a header view (even if there is no data for the contact).
        /*Begin: Modified by xiepengfei for  2012/03/28*/
        //mAllEntries.add(new HeaderViewEntry());
        /*End: Modified by xiepengfei for  2012/03/28*/


        addPhoneticName();

        flattenList(mPhoneEntries);
        flattenList(mSmsEntries);
        flattenList(mEmailEntries);
        flattenList(mImEntries);
        flattenList(mNicknameEntries);
        flattenList(mWebsiteEntries);

        addNetworks();

        flattenList(mSipEntries);
        flattenList(mPostalEntries);
        flattenList(mEventEntries);
        flattenList(mGroupEntries);
        flattenList(mRelationEntries);
        flattenList(mNoteEntries);
        flattenList(mLocalGroupEntries);

        /*Begin: Modified by xiepengfei for  2012/03/27*/
        flattenList(mOtherEntries);
        /*End: Modified by xiepengfei for  2012/03/27*/

    }

    /**
     * Add phonetic name (if applicable) to the aggregated list of contact details. This has to be
     * done manually because phonetic name doesn't have a mimetype or action intent.
     */
    private void addPhoneticName() {
        String phoneticName = ContactDetailDisplayUtils.getPhoneticName(mContext, mContactData);
        if (TextUtils.isEmpty(phoneticName)) {
            return;
        }

        // Add a title
        String phoneticNameKindTitle = mContext.getString(R.string.name_phonetic);
        mAllEntries.add(new KindTitleViewEntry(phoneticNameKindTitle.toUpperCase()));

        // Add the phonetic name
        final DetailViewEntry entry = new DetailViewEntry();
        entry.kind = phoneticNameKindTitle;
        entry.data = phoneticName;
        mAllEntries.add(entry);
    }

    /**
     * Add attribution and other third-party entries (if applicable) under the "networks" section
     * of the aggregated list of contact details. This has to be done manually because the
     * attribution does not have a mimetype and the third-party entries don't have actually belong
     * to the same {@link DataKind}.
     */
    private void addNetworks() {
        String attribution = ContactDetailDisplayUtils.getAttribution(mContext, mContactData);
        boolean hasAttribution = !TextUtils.isEmpty(attribution);
        int networksCount = mOtherEntriesMap.keySet().size();

        // Note: invitableCount will always be 0 for me profile.  (ContactLoader won't set
        // invitable types for me profile.)
        int invitableCount = mContactData.getInvitableAccountTypes().size();
        if (!hasAttribution && networksCount == 0 && invitableCount == 0) {
            return;
        }

        // Add a title
        String networkKindTitle = mContext.getString(R.string.connections);
        mAllEntries.add(new KindTitleViewEntry(networkKindTitle.toUpperCase()));

        // Add the attribution if applicable
        if (hasAttribution) {
            final DetailViewEntry entry = new DetailViewEntry();
            entry.kind = networkKindTitle;
            entry.data = attribution;
            mAllEntries.add(entry);

            // Add a divider below the attribution if there are network details that will follow
            if (networksCount > 0) {
                mAllEntries.add(new SeparatorViewEntry());
            }
        }

        // Add the other entries from third parties
        for (AccountType accountType : mOtherEntriesMap.keySet()) {

            // Add a title for each third party app
            mAllEntries.add(NetworkTitleViewEntry.fromAccountType(mContext, accountType));

            for (DetailViewEntry detailEntry : mOtherEntriesMap.get(accountType)) {
                // Add indented separator
                SeparatorViewEntry separatorEntry = new SeparatorViewEntry();
                separatorEntry.setIsInSubSection(true);
                mAllEntries.add(separatorEntry);

                // Add indented detail
                detailEntry.setIsInSubSection(true);
                mAllEntries.add(detailEntry);
            }
        }

        mOtherEntriesMap.clear();

        // Add the "More networks" button, which opens the invitable account type list popup.
        if (invitableCount > 0) {
            addMoreNetworks();
        }
    }

    /**
     * Add the "More networks" entry.  When clicked, show a popup containing a list of invitable
     * account types.
     */
    private void addMoreNetworks() {
        // First, prepare for the popup.

        // Adapter for the list popup.
        final InvitableAccountTypesAdapter popupAdapter = new InvitableAccountTypesAdapter(mContext,
                mContactData);

        // Listener called when a popup item is clicked.
        final AdapterView.OnItemClickListener popupItemListener
                = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                if (mListener != null && mContactData != null) {
                    mListener.onItemClicked(ContactsUtils.getInvitableIntent(
                            popupAdapter.getItem(position) /* account type */,
                            mContactData.getLookupUri()));
                }
            }
        };

        // Then create the click listener for the "More network" entry.  Open the popup.
        View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                showListPopup(v, popupAdapter, popupItemListener);
            }
        };

        // Finally create the entry.
        mAllEntries.add(NetworkTitleViewEntry.forMoreNetworks(mContext, onClickListener));
    }

    /**
     * Iterate through {@link DetailViewEntry} in the given list and add it to a list of all
     * entries. Add a {@link KindTitleViewEntry} at the start if the length of the list is not 0.
     * Add {@link SeparatorViewEntry}s as dividers as appropriate. Clear the original list.
     */
    private void flattenList(ArrayList<DetailViewEntry> entries) {
        int count = entries.size();

        // Add a title for this kind by extracting the kind from the first entry
        if (count > 0) {
            String kind = entries.get(0).kind;
            mAllEntries.add(new KindTitleViewEntry(kind.toUpperCase()));
        }

        // Add all the data entries for this kind
        for (int i = 0; i < count; i++) {
            // For all entries except the first one, add a divider above the entry
            if (i != 0) {
                mAllEntries.add(new SeparatorViewEntry());
            }
            mAllEntries.add(entries.get(i));
        }

        /* Begin: Modified by xiepengfei for 2012/03/28 */
        // Clear old list because it's not needed anymore.
        // entries.clear();
        /* End: Modified by xiepengfei for 2012/03/28 */
    }

    /**
     * Maps group ID to the corresponding group name, collapses all synonymous groups.
     * Ignores default groups (e.g. My Contacts) and favorites groups.
     */
    private void handleGroupMembership(
            ArrayList<String> groups, List<GroupMetaData> groupMetaData, long groupId) {
        if (groupMetaData == null) {
            return;
        }

        for (GroupMetaData group : groupMetaData) {
            if (group.getGroupId() == groupId) {
                if (!group.isDefaultGroup() && !group.isFavorites()) {
                    String title = group.getTitle();
                    if (!TextUtils.isEmpty(title) && !groups.contains(title)) {
                        groups.add(title);
                    }
                }
                break;
            }
        }
    }

    private static String buildDataString(DataKind kind, ContentValues values,
            Context context) {
        if (kind.actionBody == null) {
            return null;
        }
        CharSequence actionBody = kind.actionBody.inflateUsing(context, values);
        return actionBody == null ? null : actionBody.toString();
    }

    /**
     * Writes the Instant Messaging action into the given entry value.
     */
    @VisibleForTesting
    public static void buildImActions(Context context, DetailViewEntry entry,
            ContentValues values) {
        final boolean isEmail = Email.CONTENT_ITEM_TYPE.equals(values.getAsString(Data.MIMETYPE));

        if (!isEmail && !isProtocolValid(values)) {
            return;
        }

        final String data = values.getAsString(isEmail ? Email.DATA : Im.DATA);
        if (TextUtils.isEmpty(data)) {
            return;
        }

        final int protocol = isEmail ? Im.PROTOCOL_GOOGLE_TALK : values.getAsInteger(Im.PROTOCOL);

        if (protocol == Im.PROTOCOL_GOOGLE_TALK) {
            final Integer chatCapabilityObj = values.getAsInteger(Im.CHAT_CAPABILITY);
            final int chatCapability = chatCapabilityObj == null ? 0 : chatCapabilityObj;
            entry.chatCapability = chatCapability;
            entry.typeString = Im.getProtocolLabel(context.getResources(), Im.PROTOCOL_GOOGLE_TALK,
                    null).toString();
            if ((chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0) {
                entry.intent =
                        new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
                entry.secondaryIntent =
                        new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?call"));
            } else if ((chatCapability & Im.CAPABILITY_HAS_VOICE) != 0) {
                // Allow Talking and Texting
                entry.intent =
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
                entry.secondaryIntent =
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?call"));
            } else {
                entry.intent =
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
            }
        } else {
            // Build an IM Intent
            String host = values.getAsString(Im.CUSTOM_PROTOCOL);

            if (protocol != Im.PROTOCOL_CUSTOM) {
                // Try bringing in a well-known host for specific protocols
                host = ContactsUtils.lookupProviderNameFromId(protocol);
            }

            if (!TextUtils.isEmpty(host)) {
                final String authority = host.toLowerCase();
                final Uri imUri = new Uri.Builder().scheme(Constants.SCHEME_IMTO).authority(
                        authority).appendPath(data).build();
                entry.intent = new Intent(Intent.ACTION_SENDTO, imUri);
            }
        }
    }

    private static boolean isProtocolValid(ContentValues values) {
        String protocolString = values.getAsString(Im.PROTOCOL);
        if (protocolString == null) {
            return false;
        }
        try {
            Integer.valueOf(protocolString);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Show a list popup.  Used for "popup-able" entry, such as "More networks".
     */
    private void showListPopup(View anchorView, ListAdapter adapter,
            final AdapterView.OnItemClickListener onItemClickListener) {
        final ListPopupWindow popup = new ListPopupWindow(mContext, null);
        popup.setAnchorView(anchorView);
        popup.setWidth(anchorView.getWidth());
        popup.setAdapter(adapter);
        popup.setModal(true);

        // We need to wrap the passed onItemClickListener here, so that we can dismiss() the
        // popup afterwards.  Otherwise we could directly use the passed listener.
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                onItemClickListener.onItemClick(parent, view, position, id);
                popup.dismiss();
            }
        });
        popup.show();
    }

    /**
     * Base class for an item in the {@link ViewAdapter} list of data, which is
     * supplied to the {@link ListView}.
     */
    static class ViewEntry {
        private final int viewTypeForAdapter;
        protected long id = -1;
        /** Whether or not the entry can be focused on or not. */
        protected boolean isEnabled = false;

        ViewEntry(int viewType) {
            viewTypeForAdapter = viewType;
        }

        int getViewType() {
            return viewTypeForAdapter;
        }

        long getId() {
            return id;
        }

        boolean isEnabled(){
            return isEnabled;
        }

        /**
         * Called when the entry is clicked.  Only {@link #isEnabled} entries can get clicked.
         *
         * @param clickedView  {@link View} that was clicked  (Used, for example, as the anchor view
         *        for a popup.)
         * @param fragmentListener  {@link Listener} set to {@link ContactDetailFragment}
         */
        public void click(View clickedView, Listener fragmentListener) {
        }
    }

    /**
     * Header item in the {@link ViewAdapter} list of data.
     */
    private static class HeaderViewEntry extends ViewEntry {

        HeaderViewEntry() {
            super(ViewAdapter.VIEW_TYPE_HEADER_ENTRY);
        }

    }

    /**
     * Separator between items of the same {@link DataKind} in the
     * {@link ViewAdapter} list of data.
     */
    private static class SeparatorViewEntry extends ViewEntry {

        /**
         * Whether or not the entry is in a subsection (if true then the contents will be indented
         * to the right)
         */
        private boolean mIsInSubSection = false;

        SeparatorViewEntry() {
            super(ViewAdapter.VIEW_TYPE_SEPARATOR_ENTRY);
        }

        public void setIsInSubSection(boolean isInSubSection) {
            mIsInSubSection = isInSubSection;
        }

        public boolean isInSubSection() {
            return mIsInSubSection;
        }
    }

    /**
     * Title entry for items of the same {@link DataKind} in the
     * {@link ViewAdapter} list of data.
     */
    private static class KindTitleViewEntry extends ViewEntry {

        private final String mTitle;

        KindTitleViewEntry(String titleText) {
            super(ViewAdapter.VIEW_TYPE_KIND_TITLE_ENTRY);
            mTitle = titleText;
        }

        public String getTitle() {
            return mTitle;
        }
    }

    /**
     * A title for a section of contact details from a single 3rd party network.  It's also
     * used for the "More networks" entry, which has the same layout.
     */
    private static class NetworkTitleViewEntry extends ViewEntry {
        private final Drawable mIcon;
        private final CharSequence mLabel;
        private final View.OnClickListener mOnClickListener;

        private NetworkTitleViewEntry(Drawable icon, CharSequence label, View.OnClickListener
                onClickListener) {
            super(ViewAdapter.VIEW_TYPE_NETWORK_TITLE_ENTRY);
            this.mIcon = icon;
            this.mLabel = label;
            this.mOnClickListener = onClickListener;
            this.isEnabled = false;
        }

        public static NetworkTitleViewEntry fromAccountType(Context context, AccountType type) {
            return new NetworkTitleViewEntry(
                    type.getDisplayIcon(context), type.getDisplayLabel(context), null);
        }

        public static NetworkTitleViewEntry forMoreNetworks(Context context, View.OnClickListener
                onClickListener) {
            // TODO Icon is temporary.  Need proper one.
            return new NetworkTitleViewEntry(
                    context.getResources().getDrawable(R.drawable.ic_menu_add_field_holo_light),
                    context.getString(R.string.add_connection_button),
                    onClickListener);
        }

        @Override
        public void click(View clickedView, Listener fragmentListener) {
            if (mOnClickListener == null) return;
            mOnClickListener.onClick(clickedView);
        }

        public Drawable getIcon() {
            return mIcon;
        }

        public CharSequence getLabel() {
            return mLabel;
        }
    }

    /**
     * An item with a single detail for a contact in the {@link ViewAdapter}
     * list of data.
     */
    static class DetailViewEntry extends ViewEntry implements Collapsible<DetailViewEntry> {
        // TODO: Make getters/setters for these fields
        public int type = -1;
        public String kind;
        public String typeString;
        public String data;
        public Uri uri;
        public int maxLines = 1;
        public String mimetype;

        public Context context = null;
        public String resPackageName = null;
        public boolean isPrimary = false;
        public int secondaryActionIcon = -1;
        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public int cCardActionIcon = -1;
        public int gCardActionIcon = -1;
        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/

        public int secondaryActionDescription = -1;
        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public int cCardActionDescription = -1;
        public int gCardActionDescription = -1;
        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/

        public Intent intent;
        public Intent secondaryIntent = null;
        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public Intent cCardIntent = null;
        public Intent gCardIntent = null;
        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/

        public ArrayList<Long> ids = new ArrayList<Long>();
        public int collapseCount = 0;

        public int presence = -1;
        public int chatCapability = 0;

        public CharSequence footerLine = null;

        private boolean mIsInSubSection = false;

        DetailViewEntry() {
            super(ViewAdapter.VIEW_TYPE_DETAIL_ENTRY);
            isEnabled = true;
        }

        /**
         * Build new {@link DetailViewEntry} and populate from the given values.
         */
        public static DetailViewEntry fromValues(Context context, String mimeType, DataKind kind,
                long dataId, ContentValues values, boolean isDirectoryEntry, long directoryId) {
            final DetailViewEntry entry = new DetailViewEntry();
            entry.id = dataId;
            entry.context = context;
            entry.uri = ContentUris.withAppendedId(Data.CONTENT_URI, entry.id);
            if (isDirectoryEntry) {
                entry.uri = entry.uri.buildUpon().appendQueryParameter(
                        ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(directoryId)).build();
            }
            entry.mimetype = mimeType;
            entry.kind = (kind.titleRes == -1 || kind.titleRes == 0) ? ""
                    : context.getString(kind.titleRes);
            entry.data = buildDataString(kind, values, context);
            entry.resPackageName = kind.resPackageName;

            if (kind.typeColumn != null && values.containsKey(kind.typeColumn)) {
                entry.type = values.getAsInteger(kind.typeColumn);

                // get type string
                entry.typeString = "";
                for (EditType type : kind.typeList) {
                    if (type.rawValue == entry.type) {
                        if (type.customColumn == null) {
                            // Non-custom type. Get its description from the resource
                            entry.typeString = context.getString(type.labelRes);
                        } else {
                            // Custom type. Read it from the database
                            entry.typeString = values.getAsString(type.customColumn);
                        }
                        break;
                    }
                }
            } else {
                entry.typeString = "";
            }

            return entry;
        }

        /**
         * Apply given {@link DataStatus} values over this {@link DetailViewEntry}
         *
         * @param fillData When true, the given status replaces {@link #data}
         *            and {@link #footerLine}. Otherwise only {@link #presence}
         *            is updated.
         */
        public DetailViewEntry applyStatus(DataStatus status, boolean fillData) {
            presence = status.getPresence();
            if (fillData && status.isValid()) {
                this.data = status.getStatus().toString();
                this.footerLine = status.getTimestampLabel(context);
            }

            return this;
        }

        public void setIsInSubSection(boolean isInSubSection) {
            mIsInSubSection = isInSubSection;
        }

        public boolean isInSubSection() {
            return mIsInSubSection;
        }

        @Override
        public boolean collapseWith(DetailViewEntry entry) {
            // assert equal collapse keys
            if (!shouldCollapseWith(entry)) {
                return false;
            }

            // Choose the label associated with the highest type precedence.
            if (TypePrecedence.getTypePrecedence(mimetype, type)
                    > TypePrecedence.getTypePrecedence(entry.mimetype, entry.type)) {
                type = entry.type;
                kind = entry.kind;
                typeString = entry.typeString;
            }

            // Choose the max of the maxLines and maxLabelLines values.
            maxLines = Math.max(maxLines, entry.maxLines);

            // Choose the presence with the highest precedence.
            if (StatusUpdates.getPresencePrecedence(presence)
                    < StatusUpdates.getPresencePrecedence(entry.presence)) {
                presence = entry.presence;
            }

            // If any of the collapsed entries are primary make the whole thing primary.
            isPrimary = entry.isPrimary ? true : isPrimary;

            // uri, and contactdId, shouldn't make a difference. Just keep the original.

            // Keep track of all the ids that have been collapsed with this one.
            ids.add(entry.getId());
            collapseCount++;
            return true;
        }

        @Override
        public boolean shouldCollapseWith(DetailViewEntry entry) {
            if (entry == null) {
                return false;
            }

            if (!ContactsUtils.shouldCollapse(mimetype, data, entry.mimetype, entry.data)) {
                return false;
            }

            if (!TextUtils.equals(mimetype, entry.mimetype)
                    || !ContactsUtils.areIntentActionEqual(intent, entry.intent)
                    || !ContactsUtils.areIntentActionEqual(
                            secondaryIntent, entry.secondaryIntent)) {
                return false;
            }

            return true;
        }

        @Override
        public void click(View clickedView, Listener fragmentListener) {
            if (fragmentListener == null || intent == null) return;
            fragmentListener.onItemClicked(intent);
        }
    }

    /**
     * Cache of the children views for a view that displays a header view entry.
     */
    private static class HeaderViewCache {
        public final TextView displayNameView;
        public final TextView companyView;
        public final ImageView photoView;
        public final CheckBox starredView;
        public final int layoutResourceId;

        public HeaderViewCache(View view, int layoutResourceInflated) {
            displayNameView = (TextView) view.findViewById(R.id.name);
            companyView = (TextView) view.findViewById(R.id.company);
            photoView = (ImageView) view.findViewById(R.id.photo);
            starredView = (CheckBox) view.findViewById(R.id.star);
            layoutResourceId = layoutResourceInflated;
        }
    }

    /**
     * Cache of the children views for a view that displays a {@link NetworkTitleViewEntry}
     */
    private static class NetworkTitleViewCache {
        public final TextView name;
        public final ImageView icon;

        public NetworkTitleViewCache(View view) {
            name = (TextView) view.findViewById(R.id.network_title);
            icon = (ImageView) view.findViewById(R.id.network_icon);
        }
    }
    /**
     * Cache of the children views of a contact detail entry represented by a
     * {@link DetailViewEntry}
     */
    private static class DetailViewCache {
        public final TextView type;
        public final TextView data;
        public final TextView footer;
        public final ImageView presenceIcon;
        public final ImageView secondaryActionButton;
        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public final ImageView cCardActionButton,gCardActionButton;
        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public final View actionsViewContainer;
        public final View primaryActionView;
        public final View secondaryActionViewContainer;
        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public final View cCardActionViewContainer,gCardActionViewContainer;
        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public final View secondaryActionDivider;
        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public final View cCardActionDivider,gCardActionDivider;
        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        public final View primaryIndicator;
        /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
        public final CheckBox checkbox;
        /*End: Modified by xiepengfei for share info by sms 2012/04/01*/
        public DetailViewCache(View view,
                OnClickListener primaryActionClickListener,
                /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
                OnClickListener cCardActionClickListener,
                OnClickListener gCardActionClickListener,
                /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/
                OnClickListener secondaryActionClickListener) {
            type = (TextView) view.findViewById(R.id.type);
            data = (TextView) view.findViewById(R.id.data);
            footer = (TextView) view.findViewById(R.id.footer);
            primaryIndicator = view.findViewById(R.id.primary_indicator);
            presenceIcon = (ImageView) view.findViewById(R.id.presence_icon);

            actionsViewContainer = view.findViewById(R.id.actions_view_container);
            actionsViewContainer.setOnClickListener(primaryActionClickListener);
            primaryActionView = view.findViewById(R.id.primary_action_view);

            secondaryActionViewContainer = view.findViewById(
                    R.id.secondary_action_view_container);
            /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
            cCardActionViewContainer = view.findViewById(
                    R.id.cCard_action_view_container);
            gCardActionViewContainer = view.findViewById(
                    R.id.gCard_action_view_container);
            /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/

            secondaryActionViewContainer.setOnClickListener(
                    secondaryActionClickListener);
            /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
            cCardActionViewContainer.setOnClickListener(
                    cCardActionClickListener);
            gCardActionViewContainer.setOnClickListener(
                    gCardActionClickListener);
            /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/
            secondaryActionButton = (ImageView) view.findViewById(
                    R.id.secondary_action_button);
            /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
            cCardActionButton = (ImageView) view.findViewById(
                    R.id.cCard_action_button);
            gCardActionButton = (ImageView) view.findViewById(
                    R.id.gCard_action_button);
            /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/


            secondaryActionDivider = view.findViewById(R.id.vertical_divider);
            /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/

            cCardActionDivider = view.findViewById(R.id.vertical_divider01);
            gCardActionDivider = view.findViewById(R.id.vertical_divider02);
            /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/


            /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
            checkbox = (CheckBox)view.findViewById(R.id.checkbox_detail);
            /*End: Modified by xiepengfei for share info by sms 2012/04/01*/
        }
    }

    private final class ViewAdapter extends BaseAdapter {

        public static final int VIEW_TYPE_DETAIL_ENTRY = 0;
        public static final int VIEW_TYPE_HEADER_ENTRY = 1;
        public static final int VIEW_TYPE_KIND_TITLE_ENTRY = 2;
        public static final int VIEW_TYPE_NETWORK_TITLE_ENTRY = 3;
        public static final int VIEW_TYPE_SEPARATOR_ENTRY = 4;
        private static final int VIEW_TYPE_COUNT = 5;


        /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
        private boolean multipleChoiceMode = false;
        /*End: Modified by xiepengfei for share info by sms 2012/04/01*/

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_HEADER_ENTRY:
                    return getHeaderEntryView(convertView, parent);
                case VIEW_TYPE_SEPARATOR_ENTRY:
                    return getSeparatorEntryView(position, convertView, parent);
                case VIEW_TYPE_KIND_TITLE_ENTRY:
                    return getKindTitleEntryView(position, convertView, parent);
                case VIEW_TYPE_DETAIL_ENTRY:
                    return getDetailEntryView(position, convertView, parent);
                case VIEW_TYPE_NETWORK_TITLE_ENTRY:
                    return getNetworkTitleEntryView(position, convertView, parent);
                default:
                    throw new IllegalStateException("Invalid view type ID " +
                            getItemViewType(position));
            }
        }

        private View getHeaderEntryView(View convertView, ViewGroup parent) {
            final int desiredLayoutResourceId = mContactHasSocialUpdates ?
                    R.layout.detail_header_contact_with_updates :
                    R.layout.detail_header_contact_without_updates;
            View result = null;
            HeaderViewCache viewCache = null;

            // Only use convertView if it has the same layout resource ID as the one desired
            // (the two can be different on wide 2-pane screens where the detail fragment is reused
            // for many different contacts that do and do not have social updates).
            if (convertView != null) {
                viewCache = (HeaderViewCache) convertView.getTag();
                if (viewCache.layoutResourceId == desiredLayoutResourceId) {
                    result = convertView;
                }
            }

            // Otherwise inflate a new header view and create a new view cache.
            if (result == null) {
                result = mInflater.inflate(desiredLayoutResourceId, parent, false);
                viewCache = new HeaderViewCache(result, desiredLayoutResourceId);
                result.setTag(viewCache);
            }

            ContactDetailDisplayUtils.setDisplayName(mContext, mContactData,
                    viewCache.displayNameView);
            ContactDetailDisplayUtils.setCompanyName(mContext, mContactData, viewCache.companyView);

            // Set the photo if it should be displayed
            if (viewCache.photoView != null) {
                ContactDetailDisplayUtils.setPhoto(mContext, mContactData, viewCache.photoView);
            }

            // Set the starred state if it should be displayed
            final CheckBox favoritesStar = viewCache.starredView;
            if (favoritesStar != null) {
                ContactDetailDisplayUtils.setStarred(mContactData, favoritesStar);
                final Uri lookupUri = mContactData.getLookupUri();
                favoritesStar.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Toggle "starred" state
                        // Make sure there is a contact
                        if (lookupUri != null) {
                            Intent intent = ContactSaveService.createSetStarredIntent(
                                    getContext(), lookupUri, favoritesStar.isChecked());
                            getContext().startService(intent);
                        }
                    }
                });
            }

            return result;
        }

        private View getSeparatorEntryView(int position, View convertView, ViewGroup parent) {
            final SeparatorViewEntry entry = (SeparatorViewEntry) getItem(position);
            /*Begin: Modified by xiepengfei for modify the list separator view 2012/04/16*/
//            final View result = (convertView != null) ? convertView :
//                mInflater.inflate(R.layout.contact_detail_separator_entry_view, parent, false);
//            result.setPadding(entry.isInSubSection() ? mViewEntryDimensions.getWidePaddingLeft() :
//                mViewEntryDimensions.getPaddingLeft(), 0,
//                mViewEntryDimensions.getPaddingRight(), 0);
            final View result = (convertView != null) ? convertView :
                mInflater.inflate(R.layout.contact_detail_history_list_item_separator, parent, false);
            /*End: Modified by xiepengfei for modify the list separator view 2012/04/16*/
            return result;
        }

        private View getKindTitleEntryView(int position, View convertView, ViewGroup parent) {
            final KindTitleViewEntry entry = (KindTitleViewEntry) getItem(position);

            final View result = (convertView != null) ? convertView :
                    mInflater.inflate(R.layout.list_separator, parent, false);
            final TextView titleTextView = (TextView) result.findViewById(R.id.title);
            titleTextView.setText(entry.getTitle());

            return result;
        }

        private View getNetworkTitleEntryView(int position, View convertView, ViewGroup parent) {
            final NetworkTitleViewEntry entry = (NetworkTitleViewEntry) getItem(position);
            final View result;
            final NetworkTitleViewCache viewCache;

            if (convertView != null) {
                result = convertView;
                viewCache = (NetworkTitleViewCache) result.getTag();
            } else {
                result = mInflater.inflate(R.layout.contact_detail_network_title_entry_view,
                        parent, false);
                viewCache = new NetworkTitleViewCache(result);
                result.setTag(viewCache);
                result.findViewById(R.id.primary_action_view).setOnClickListener(
                        entry.mOnClickListener);
            }

            viewCache.name.setText(entry.getLabel());
            viewCache.icon.setImageDrawable(entry.getIcon());

            return result;
        }

        private View getDetailEntryView(int position, View convertView, ViewGroup parent) {
            final DetailViewEntry entry = (DetailViewEntry) getItem(position);
            final View v;
            final DetailViewCache viewCache;

            // Check to see if we can reuse convertView
            if (convertView != null) {
                v = convertView;
                viewCache = (DetailViewCache) v.getTag();
            } else {
                // Create a new view if needed
                v = mInflater.inflate(R.layout.contact_detail_list_item, parent, false);

                // Cache the children
                /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/18*/
                //viewCache = new DetailViewCache(v,
                //        mPrimaryActionClickListener, mSecondaryActionClickListener);
                viewCache = new DetailViewCache(v,
                        mPrimaryActionClickListener,mcCardActionClickListener,
                        mgCardActionClickListener, mSecondaryActionClickListener);
                /*End: Modified by wqiang for modified ContactDetail View 2012/08/18*/
                v.setTag(viewCache);
            }

            bindDetailView(position, v, entry);
            return v;
        }

/*Begin: Modified by xiepengfei for modify the input data 2012/05/23*/
        //private void bindDetailView(int position, View view, DetailViewEntry entry) {
        private void bindDetailView(final int position, View view, final DetailViewEntry entry) {
/*End: Modified by xiepengfei for modify the input data 2012/05/23*/

            final Resources resources = mContext.getResources();
            final DetailViewCache views = (DetailViewCache) view.getTag();
            /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/18*/
            //if (!TextUtils.isEmpty(entry.typeString)) {
            //    views.type.setText(entry.typeString.toUpperCase());
            //    views.type.setVisibility(View.VISIBLE);
            //} else {
            //    views.type.setVisibility(View.GONE);
            //}
            if (!TextUtils.isEmpty(entry.typeString)) {
                views.type.setHeight(30);
                views.type.setText(entry.typeString.toUpperCase());
                views.type.setVisibility(View.VISIBLE);
            } else {
                views.type.setHeight(10);
                views.type.setText(" ");
                views.type.setVisibility(View.VISIBLE);
            }
            /*End: Modified by wqiang for modified ContactDetail View 2012/08/18*/
            if (LocalGroup.CONTENT_ITEM_TYPE.equals(entry.mimetype))
                views.data.setText(Group.restoreGroupById(view.getContext().getContentResolver(),
                        Long.parseLong(entry.data)).getTitle());
            else
                views.data.setText(entry.data);
            setMaxLines(views.data, entry.maxLines);
            // Set the footer
            if (!TextUtils.isEmpty(entry.footerLine)) {
                views.footer.setText(entry.footerLine);
                views.footer.setVisibility(View.VISIBLE);
            } else {
                views.footer.setVisibility(View.GONE);
            }

            // Set the default contact method
            views.primaryIndicator.setVisibility(entry.isPrimary ? View.VISIBLE : View.GONE);

            // Set the presence icon
            final Drawable presenceIcon = ContactPresenceIconUtil.getPresenceIcon(
                    mContext, entry.presence);
            final ImageView presenceIconView = views.presenceIcon;
            if (presenceIcon != null) {
                presenceIconView.setImageDrawable(presenceIcon);
                presenceIconView.setVisibility(View.VISIBLE);
            } else {
                presenceIconView.setVisibility(View.GONE);
            }

            final ActionsViewContainer actionsButtonContainer =
                    (ActionsViewContainer) views.actionsViewContainer;
            actionsButtonContainer.setTag(entry);
            actionsButtonContainer.setPosition(position);
            registerForContextMenu(actionsButtonContainer);


            // Set the secondary action button
            final ImageView secondaryActionView = views.secondaryActionButton;
            Drawable secondaryActionIcon = null;
            String secondaryActionDescription = null;
            if (entry.secondaryActionIcon != -1) {
                /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/18*/
                //if (entry.resPackageName != null) {
                //    secondaryActionIcon = mContext.getPackageManager()
                //            .getDrawable(entry.resPackageName,
                //                    entry.secondaryActionIcon, null);
                //} else {
                //    secondaryActionIcon = resources
                //            .getDrawable(entry.secondaryActionIcon);
                //}
                    secondaryActionIcon = resources
                            .getDrawable(entry.secondaryActionIcon);
                /*End: Modified by wqiang for modified ContactDetail View 2012/08/18*/
                secondaryActionDescription = resources.getString(entry.secondaryActionDescription);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0) {
                secondaryActionIcon =
                        resources.getDrawable(R.drawable.sym_action_videochat_holo_light);
                secondaryActionDescription = resources.getString(R.string.video_chat);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_VOICE) != 0) {
                secondaryActionIcon =
                        resources.getDrawable(R.drawable.sym_action_audiochat_holo_light);
                secondaryActionDescription = resources.getString(R.string.audio_chat);
            }
            final View secondaryActionViewContainer = views.secondaryActionViewContainer;
            if (entry.secondaryIntent != null && secondaryActionIcon != null) {
                secondaryActionView.setImageDrawable(secondaryActionIcon);
                secondaryActionView.setContentDescription(secondaryActionDescription);
                secondaryActionViewContainer.setTag(entry);
                secondaryActionViewContainer.setVisibility(View.VISIBLE);
                views.secondaryActionDivider.setVisibility(View.VISIBLE);
            } else {
                secondaryActionViewContainer.setVisibility(View.GONE);
                views.secondaryActionDivider.setVisibility(View.GONE);
            }
            /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
            final ImageView cCardActionView = views.cCardActionButton;
            final ImageView gCardActionView = views.gCardActionButton;
            Drawable cCardActionIcon = null;
            Drawable gCardActionIcon = null;
            String cCardActionDescription = null;
            String gCardActionDescription = null;
            if (entry.cCardActionIcon != -1) {
                    cCardActionIcon = resources
                            .getDrawable(entry.cCardActionIcon);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0) {
                cCardActionIcon =
                        resources.getDrawable(R.drawable.sym_action_videochat_holo_light);
                cCardActionDescription = resources.getString(R.string.video_chat);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_VOICE) != 0) {
                cCardActionIcon =
                        resources.getDrawable(R.drawable.sym_action_audiochat_holo_light);
                cCardActionDescription = resources.getString(R.string.audio_chat);
            }
            final View cCardActionViewContainer = views.cCardActionViewContainer;
            if (entry.cCardIntent != null && cCardActionIcon != null&&cCardState.equals("READY")) {
                cCardActionView.setImageDrawable(cCardActionIcon);
                cCardActionView.setContentDescription(cCardActionDescription);
                cCardActionViewContainer.setTag(entry);
                cCardActionViewContainer.setVisibility(View.VISIBLE);
                views.cCardActionDivider.setVisibility(View.VISIBLE);
            } else {
                cCardActionViewContainer.setVisibility(View.GONE);
                views.cCardActionDivider.setVisibility(View.GONE);
            }
            if (entry.gCardActionIcon != -1) {
                    gCardActionIcon = resources
                            .getDrawable(entry.gCardActionIcon);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0) {
                gCardActionIcon =
                        resources.getDrawable(R.drawable.sym_action_videochat_holo_light);
                gCardActionDescription = resources.getString(R.string.video_chat);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_VOICE) != 0) {
                gCardActionIcon =
                        resources.getDrawable(R.drawable.sym_action_audiochat_holo_light);
                gCardActionDescription = resources.getString(R.string.audio_chat);
            }
            final View gCardActionViewContainer = views.gCardActionViewContainer;
            if (entry.gCardIntent != null && gCardActionIcon != null&&gCardState.equals("READY")) {
                gCardActionView.setImageDrawable(gCardActionIcon);
                gCardActionView.setContentDescription(gCardActionDescription);
                gCardActionViewContainer.setTag(entry);
                gCardActionViewContainer.setVisibility(View.VISIBLE);
                views.gCardActionDivider.setVisibility(View.VISIBLE);
            } else {
                gCardActionViewContainer.setVisibility(View.GONE);
                views.gCardActionDivider.setVisibility(View.GONE);
            }
            /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/

            /*Begin: Modified by xiepengfei for modify the secondaryActionDivider Visibility 2012/05/23*/
            if(TextUtils.isEmpty(entry.mimetype) || entry.mimetype.equals(LocalGroup.CONTENT_ITEM_TYPE)){
                secondaryActionViewContainer.setClickable(false);
                views.secondaryActionDivider.setVisibility(View.GONE);
            }
            /*End: Modified by xiepengfei for modify the secondaryActionDivider Visibility 2012/05/23*/
            /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/18*/
            /*End: Modified by wqiang for modified ContactDetail View 2012/08/18*/
            /*Begin: Modified by xiepengfei for set detail item layout 2012/04/16*/
            // Right and left padding should not have "pressed" effect.
//            view.setPadding(
//                    entry.isInSubSection()
//                            ? mViewEntryDimensions.getWidePaddingLeft()
//                            : mViewEntryDimensions.getPaddingLeft(),
//                    0, mViewEntryDimensions.getPaddingRight(), 0);
//            // Top and bottom padding should have "pressed" effect.
//            final View primaryActionView = views.primaryActionView;
//            primaryActionView.setPadding(
//                    primaryActionView.getPaddingLeft(),
//                    mViewEntryDimensions.getPaddingTop(),
//                    primaryActionView.getPaddingRight(),
//                    mViewEntryDimensions.getPaddingBottom());
//            secondaryActionViewContainer.setPadding(
//                    secondaryActionViewContainer.getPaddingLeft(),
//                    mViewEntryDimensions.getPaddingTop(),
//                    secondaryActionViewContainer.getPaddingRight(),
//                    mViewEntryDimensions.getPaddingBottom());
            /*End: Modified by xiepengfei for set detail item layout 2012/04/16*/




            /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
            if(getMultipleChoiceMode()){
                views.checkbox.setVisibility(View.VISIBLE);
                //view.setClickable(false);
                mdisableIntent = true;
                actionsButtonContainer.setClickable(false);
                secondaryActionViewContainer.setClickable(false);
                views.checkbox.setClickable(false);
                actionsButtonContainer.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        Log.v(TAG,"views.checkbox.isChecked() "+views.checkbox.isChecked());
                        if(views.checkbox.isChecked()){
                            views.checkbox.setChecked(false);
                            mMapContactInfo.remove(position);
                        }else{
                            views.checkbox.setChecked(true);
                            mMapContactInfo.put(position, entry.typeString+":"+entry.data);
                        }
                    }
                });
            }else{
                views.checkbox.setVisibility(View.GONE);
                views.checkbox.setChecked(false);
                mdisableIntent = false;
                actionsButtonContainer.setClickable(true);
                actionsButtonContainer.setOnClickListener(mPrimaryActionClickListener);
                secondaryActionViewContainer.setClickable(true);
                secondaryActionViewContainer.setOnClickListener(mSecondaryActionClickListener);
            }

//            views.checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    if(isChecked){
//                        mMapContactInfo.put(position, entry.typeString+":"+entry.data);
//                    }else{
//                        mMapContactInfo.remove(position);
//                    }
//                }
//            });
            /*End: Modified by xiepengfei for share info by sms 2012/04/01*/
        }

        private void setMaxLines(TextView textView, int maxLines) {
            if (maxLines == 1) {
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                textView.setSingleLine(false);
                textView.setMaxLines(maxLines);
                textView.setEllipsize(null);
            }
        }

        private final OnClickListener mPrimaryActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener == null) return;
                final ViewEntry entry = (ViewEntry) view.getTag();
                if (entry == null) return;
                entry.click(view, mListener);
            }
        };

        private final OnClickListener mSecondaryActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener == null) return;
                if (view == null) return;
                final ViewEntry entry = (ViewEntry) view.getTag();
                if (entry == null || !(entry instanceof DetailViewEntry)) return;
                final DetailViewEntry detailViewEntry = (DetailViewEntry) entry;
                final Intent intent = detailViewEntry.secondaryIntent;
                if (intent == null) return;
                mListener.onItemClicked(intent);
            }
        };

        /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/17*/
        private final OnClickListener mcCardActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = Phone.NUMBER;
                dialButtonPressed(number, 0);
            }
        };

        private final OnClickListener mgCardActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = Phone.NUMBER;
                dialButtonPressed(number, 1);
            }
        };
        /*End: Modified by wqiang for modified ContactDetail View 2012/08/17*/

        @Override
        public int getCount() {
            return mAllEntries.size();
        }

        @Override
        public ViewEntry getItem(int position) {
            return mAllEntries.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return mAllEntries.get(position).getViewType();
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public long getItemId(int position) {
            final ViewEntry entry = mAllEntries.get(position);
            if (entry != null) {
                return entry.getId();
            }
            return -1;
        }

        @Override
        public boolean areAllItemsEnabled() {
            // Header will always be an item that is not enabled.
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
        public void setMultipleChoiceMode(boolean mode){
            this.multipleChoiceMode = mode;
        }
        public boolean getMultipleChoiceMode(){
            return this.multipleChoiceMode;
        }
        /*End: Modified by xiepengfei for share info by sms 2012/04/01*/
    }

    @Override
    public void onAccountSelectorCancelled() {
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        createCopy(account);
    }

    private void createCopy(AccountWithDataSet account) {
        if (mListener != null) {
            mListener.onCreateRawContactRequested(mContactData.getContentValues(), account);
        }
    }

    /**
     * Default (fallback) list item click listener.  Note the click event for DetailViewEntry is
     * caught by individual views in the list item view to distinguish the primary action and the
     * secondary action, so this method won't be invoked for that.  (The listener is set in the
     * bindview in the adapter)
     * This listener is used for other kind of entries.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener == null) return;
        final ViewEntry entry = mAdapter.getItem(position);
        if (entry == null) return;
        entry.click(view, mListener);
    }

    /* Begin: Modified by xiepengfei for 2012/03/28 */
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.contact_detail, menu);
    }
    public void onPrepareOptionsMenu(Menu menu) {
        copyMenu = menu.findItem(R.id.menu_copy_to_dialling_screen);
        copyMenu.setVisible(true);
        shareContactInfo = menu.findItem(R.id.menu_share_info);
        shareContactInfo.setVisible(true);

        /*Begin: Modified by xiepengfei for set default phone 2012/06/09*/
        setDefaultPhone = menu.findItem(R.id.menu_contact_detail_default_number_set);
        setDefaultPhone.setVisible(true);
        sendContactSMS = menu.findItem(R.id.menu_contact_detail_send_info_sms);
        sendContactSMS.setVisible(true);
        /*End: Modified by xiepengfei for set default phone 2012/06/09*/

        /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
//        completeMenu = menu.findItem(R.id.menu_contact_detail_complete);
//        completeMenu.setVisible(false);
//        cancelMenu = menu.findItem(R.id.menu_contact_detail_cancel);
//        cancelMenu.setVisible(false);

        updataDetailFragmentMenu();
        /*End: Modified by xiepengfei for share info by sms 2012/04/01*/
    }
    public void updataDetailFragmentMenu(){
        boolean isVisible = mPhoneEntries.size()!=0;
        copyMenu.setVisible(isVisible);

        /*Begin: Modified by xiepengfei for share info by sms 2012/04/01*/
        if(isMultipleChoiceMode){
//            completeMenu.setVisible(true);
//            cancelMenu.setVisible(true);
            bottomToolbar.setVisibility(View.VISIBLE);
            copyMenu.setVisible(false);
            shareContactInfo.setVisible(false);
        }else{
//            completeMenu.setVisible(false);
//            cancelMenu.setVisible(false);
            bottomToolbar.setVisibility(View.GONE);
            copyMenu.setVisible(true);
            shareContactInfo.setVisible(true);
        }
        /*End: Modified by xiepengfei for share info by sms 2012/04/01*/

        /*Begin: Modified by xiepengfei for set default phone 2012/06/09*/
        setDefaultPhone.setVisible(isVisible);
        /*End: Modified by xiepengfei for set default phone 2012/06/09*/
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_copy_to_dialling_screen: {
                int size = mPhoneEntries.size();
                if (size == 0) {
                    return true;
                } else if (size == 1) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + mPhoneEntries.get(0).data));
                    dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialIntent);
                } else if (size > 1) {
                    final ListOptionsFragment optionFragment = new ListOptionsFragment(
                            R.string.menu_contact_detail_copy_to_dialling_screen,
                            mPhoneEntries, new ListOptionsFragmentListener() {
                                public void getItemSelectedPosition(int position,
                                        String number) {
                                    Log.v(TAG, "getItemSelectedPosition");
                                    Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                                            Uri.parse("tel:" + number));
                                    dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(dialIntent);
                                }
                            });

                    optionFragment.show(getFragmentManager(), null);
                }
                return true;
            }
            case R.id.menu_share_info: {
                /*Begin: Modified by xiepengfei for set default phone 2012/06/09*/
//              int[] content = {
//              R.string.contact_detail_send_by_bluetooth,
//              R.string.contact_detail_send_by_sms,
//              R.string.contact_detail_send_by_mms
//      };
//      final ListOptionsFragment shareOptionDialog =
//              new ListOptionsFragment(getActivity(), R.string.menu_share, content,
//                      shareMenuListener);
//      shareOptionDialog.show(getFragmentManager(), null);
                sendContactInfoByBluetooth();
                /*End: Modified by xiepengfei for set default phone 2012/06/09*/

                return true;
            }
            /* Begin: Modified by xiepengfei for share info by sms 2012/04/01 */
//            case R.id.menu_contact_detail_complete: {
//                if (isMultipleChoiceMode) {
//                    isMultipleChoiceMode = false;
//                    mAdapter.setMultipleChoiceMode(isMultipleChoiceMode);
//                    mAdapter.notifyDataSetInvalidated();
//                    updataDetailFragmentMenu();

//                    // start to sms
//                    sendContactInfoBySMS();
//                    mMapContactInfo.clear();
//                }
//                return true;
//            }
//            case R.id.menu_contact_detail_cancel: {
//                if (isMultipleChoiceMode) {
//                    isMultipleChoiceMode = false;
//                    mAdapter.setMultipleChoiceMode(isMultipleChoiceMode);
//                    mAdapter.notifyDataSetInvalidated();
//                    updataDetailFragmentMenu();
//                    mMapContactInfo.clear();
//                }
//                return true;
//            }
            /* End: Modified by xiepengfei for share info by sms 2012/04/01 */

            /*Begin: Modified by xiepengfei for set default phone 2012/06/09*/
            case R.id.menu_contact_detail_default_number_set:{
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(getActivity(), ContactDefaultNumberSetActivity.class);
                intent.setData(mLookupUri);
                getActivity().startActivity(intent);
            }
            return true;
            case R.id.menu_contact_detail_send_info_sms:{
                isMultipleChoiceMode = true;
                mAdapter.setMultipleChoiceMode(isMultipleChoiceMode);
                mAdapter.notifyDataSetChanged();
                updataDetailFragmentMenu();
                mMapContactInfo.clear();
            }
            return true;
            /*End: Modified by xiepengfei for set default phone 2012/06/09*/
        }

        return false;
    }

    /* End: Modified by xiepengfei for 2012/03/28 */

    /* Begin: Modified by xiepengfei for share menu listener 2012/03/29 */
    private ListOptionsFragmentListener shareMenuListener = new ListOptionsFragmentListener() {
        public void getItemSelectedPosition(int position, String content) {
            switch (position) {
                case 0: // send contact's info by bluetooth
                    Log.v(TAG, "bluetooth type");
                    sendContactInfoByBluetooth();
                    break;
                case 1:// send contact's info by sms
                    Log.v(TAG, "sms type");
                    isMultipleChoiceMode = true;
                    mAdapter.setMultipleChoiceMode(isMultipleChoiceMode);
                    mAdapter.notifyDataSetChanged();
                    updataDetailFragmentMenu();
                    mMapContactInfo.clear();
                    break;
                case 2:// send contact's info by mms
                    Log.v(TAG, "mms type");
                    sendContactInfoByMMS();
                    break;
                default:
                    Log.w(TAG, "no defined type");
                    break;
            }

        }
    };

    /* End: Modified by xiepengfei for share menu listener 2012/03/29 */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        DetailViewEntry selectedEntry = (DetailViewEntry) mAllEntries.get(info.position);

        // get the title instead of group id
        if (LocalGroup.CONTENT_ITEM_TYPE.equals(selectedEntry.mimetype)) {
            menu.setHeaderTitle(Group.restoreGroupById(this.getActivity().getContentResolver(),
                    Long.parseLong(selectedEntry.data)).getTitle());
        } else {
            menu.setHeaderTitle(selectedEntry.data);
        }
        menu.add(ContextMenu.NONE, ContextMenuIds.COPY_TEXT,
                ContextMenu.NONE, getString(R.string.copy_text));

        String selectedMimeType = selectedEntry.mimetype;

        // Defaults to true will only enable the detail to be copied to the clipboard.
        boolean isUniqueMimeType = true;

        // Only allow primary support for Phone and Email content types
        if (Phone.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
            isUniqueMimeType = mIsUniqueNumber;
        } else if (Email.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
            isUniqueMimeType = mIsUniqueEmail;
        }

        // Checking for previously set default
        if (selectedEntry.isPrimary) {
            menu.add(ContextMenu.NONE, ContextMenuIds.CLEAR_DEFAULT,
                    ContextMenu.NONE, getString(R.string.clear_default));
        } else if (!isUniqueMimeType) {
            menu.add(ContextMenu.NONE, ContextMenuIds.SET_DEFAULT,
                    ContextMenu.NONE, getString(R.string.set_default));
        }

        // add for new feature: IP Prefix
        if (true && Phone.CONTENT_ITEM_TYPE.equals(selectedMimeType)) { // TODO: need change to feature query
            menu.add(ContextMenu.NONE, ContextMenuIds.IPCALL, ContextMenu.NONE, getString(R.string.ipcall));
            menu.add(ContextMenu.NONE, ContextMenuIds.EDIT_BEFORE_CALL, ContextMenu.NONE, getString(R.string.edit_before_call));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
            menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case ContextMenuIds.COPY_TEXT:
                copyToClipboard(menuInfo.position);
                return true;
            case ContextMenuIds.SET_DEFAULT:
                setDefaultContactMethod(mListView.getItemIdAtPosition(menuInfo.position));
                return true;
            case ContextMenuIds.CLEAR_DEFAULT:
                clearDefaultContactMethod(mListView.getItemIdAtPosition(menuInfo.position));
                return true;
            case ContextMenuIds.IPCALL:
                callViaIP(menuInfo.position);
                return true;
            case ContextMenuIds.EDIT_BEFORE_CALL:
                callByEdit(menuInfo.position);
                return true;
            default:
                throw new IllegalArgumentException("Unknown menu option " + item.getItemId());
        }
    }

    private void setDefaultContactMethod(long id) {
        Intent setIntent = ContactSaveService.createSetSuperPrimaryIntent(mContext, id);
        mContext.startService(setIntent);
    }

    private void clearDefaultContactMethod(long id) {
        Intent clearIntent = ContactSaveService.createClearPrimaryIntent(mContext, id);
        mContext.startService(clearIntent);
    }

    private void copyToClipboard(int viewEntryPosition) {
        // Getting the text to copied
        DetailViewEntry detailViewEntry = (DetailViewEntry) mAllEntries.get(viewEntryPosition);
        CharSequence textToCopy = detailViewEntry.data;

        // Checking for empty string
        if (TextUtils.isEmpty(textToCopy)) return;

        // get the title instead of group id
        if (LocalGroup.CONTENT_ITEM_TYPE.equals(detailViewEntry.mimetype))
            textToCopy = Group.restoreGroupById(this.getActivity().getContentResolver(),
                    Long.parseLong((String) textToCopy)).getTitle();

        // Adding item to clipboard
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        String[] mimeTypes = new String[]{detailViewEntry.mimetype};
        ClipData.Item clipDataItem = new ClipData.Item(textToCopy);
        ClipData cd = new ClipData(detailViewEntry.typeString, mimeTypes, clipDataItem);
        clipboardManager.setPrimaryClip(cd);

        // Display Confirmation Toast
        String toastText = getString(R.string.toast_text_copied);
        Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
    }

    private void callViaIP(int viewEntryPosition) {
        DetailViewEntry detailViewEntry = (DetailViewEntry) mAllEntries.get(viewEntryPosition);
        Intent callIntent = new Intent(detailViewEntry.intent);
        callIntent.putExtra("ipcall", true);
        mContext.startActivity(callIntent);
    }

    private void callByEdit(int viewEntryPosition) {
        DetailViewEntry detailViewEntry = (DetailViewEntry) mAllEntries.get(viewEntryPosition);
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", detailViewEntry.data,
                null));
        mContext.startActivity(intent);
    }

    @Override
    public boolean handleKeyDown(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                try {
                    ITelephony phone = ITelephony.Stub.asInterface(
                            ServiceManager.checkService("phone"));
                    if (phone != null && !phone.isIdle()) {
                        // Skip out and let the key be handled at a higher level
                        break;
                    }
                } catch (RemoteException re) {
                    // Fall through and try to call the contact
                }

                int index = mListView.getSelectedItemPosition();
                if (index != -1) {
                    final DetailViewEntry entry = (DetailViewEntry) mAdapter.getItem(index);
                    if (entry != null && entry.intent != null &&
                            entry.intent.getAction() == Intent.ACTION_CALL_PRIVILEGED) {
                        mContext.startActivity(entry.intent);
                        return true;
                    }
                } else if (mPrimaryPhoneUri != null) {
                    // There isn't anything selected, call the default number
                    final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            mPrimaryPhoneUri);
                    mContext.startActivity(intent);
                    return true;
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Base class for QuickFixes. QuickFixes quickly fix issues with the Contact without
     * requiring the user to go to the editor. Example: Add to My Contacts.
     */
    private static abstract class QuickFix {
        public abstract boolean isApplicable();
        public abstract String getTitle();
        public abstract void execute();
    }

    private class AddToMyContactsQuickFix extends QuickFix {
        @Override
        public boolean isApplicable() {
            // Only local contacts
            if (mContactData == null || mContactData.isDirectoryEntry()) return false;

            // User profile cannot be added to contacts
            if (mContactData.isUserProfile()) return false;

            // Only if exactly one raw contact
            if (mContactData.getEntities().size() != 1) return false;

            // test if the default group is assigned
            final List<GroupMetaData> groups = mContactData.getGroupMetaData();

            // For accounts without group support, groups is null
            if (groups == null) return false;

            // remember the default group id. no default group? bail out early
            final long defaultGroupId = getDefaultGroupId(groups);
            if (defaultGroupId == -1) return false;

            final Entity rawContactEntity = mContactData.getEntities().get(0);
            ContentValues rawValues = rawContactEntity.getEntityValues();
            final String accountType = rawValues.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = rawValues.getAsString(RawContacts.DATA_SET);
            final AccountTypeManager accountTypes =
                    AccountTypeManager.getInstance(mContext);
            final AccountType type = accountTypes.getAccountType(accountType, dataSet);
            // Offline or non-writeable account? Nothing to fix
            if (type == null || !type.areContactsWritable()) return false;

            // Check whether the contact is in the default group
            boolean isInDefaultGroup = false;
            for (NamedContentValues subValue : rawContactEntity.getSubValues()) {
                final String mimeType = subValue.values.getAsString(Data.MIMETYPE);

                if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    final Long groupId =
                            subValue.values.getAsLong(GroupMembership.GROUP_ROW_ID);
                    if (groupId == defaultGroupId) {
                        isInDefaultGroup = true;
                        break;
                    }
                }
            }

            return !isInDefaultGroup;
        }

        @Override
        public String getTitle() {
            return getString(R.string.add_to_my_contacts);
        }

        @Override
        public void execute() {
            final long defaultGroupId = getDefaultGroupId(mContactData.getGroupMetaData());
            // there should always be a default group (otherwise the button would be invisible),
            // but let's be safe here
            if (defaultGroupId == -1) return;

            // add the group membership to the current state
            final EntityDeltaList contactDeltaList = EntityDeltaList.fromIterator(
                    mContactData.getEntities().iterator());
            final EntityDelta rawContactEntityDelta = contactDeltaList.get(0);

            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
            final ValuesDelta values = rawContactEntityDelta.getValues();
            final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = values.getAsString(RawContacts.DATA_SET);
            final AccountType type = accountTypes.getAccountType(accountType, dataSet);
            final DataKind groupMembershipKind = type.getKindForMimetype(
                    GroupMembership.CONTENT_ITEM_TYPE);
            final ValuesDelta entry = EntityModifier.insertChild(rawContactEntityDelta,
                    groupMembershipKind);
            entry.put(GroupMembership.GROUP_ROW_ID, defaultGroupId);

            // and fire off the intent. we don't need a callback, as the database listener
            // should update the ui
            final Intent intent = ContactSaveService.createSaveContactIntent(getActivity(),
                    contactDeltaList, "", 0, false, getActivity().getClass(),
                    Intent.ACTION_VIEW);
            getActivity().startService(intent);
        }
    }

    private class MakeLocalCopyQuickFix extends QuickFix {
        @Override
        public boolean isApplicable() {
            // Not a directory contact? Nothing to fix here
            if (mContactData == null || !mContactData.isDirectoryEntry()) return false;

            // No export support? Too bad
            if (mContactData.getDirectoryExportSupport() == Directory.EXPORT_SUPPORT_NONE) {
                return false;
            }

            return true;
        }

        @Override
        public String getTitle() {
            return getString(R.string.menu_copyContact);
        }

        @Override
        public void execute() {
            if (mListener == null) {
                return;
            }

            int exportSupport = mContactData.getDirectoryExportSupport();
            switch (exportSupport) {
                case Directory.EXPORT_SUPPORT_SAME_ACCOUNT_ONLY: {
                    createCopy(new AccountWithDataSet(mContactData.getDirectoryAccountName(),
                                    mContactData.getDirectoryAccountType(), null));
                    break;
                }
                case Directory.EXPORT_SUPPORT_ANY_ACCOUNT: {
                    final List<AccountWithDataSet> accounts =
                            AccountTypeManager.getInstance(mContext).getAccounts(true);
                    if (accounts.isEmpty()) {
                        createCopy(null);
                        return;  // Don't show a dialog.
                    }

                    // In the common case of a single writable account, auto-select
                    // it without showing a dialog.
                    if (accounts.size() == 1) {
                        createCopy(accounts.get(0));
                        return;  // Don't show a dialog.
                    }

                    SelectAccountDialogFragment.show(getFragmentManager(),
                            ContactDetailFragment.this, R.string.dialog_new_contact_account,
                            AccountListFilter.ACCOUNTS_CONTACT_WRITABLE, null);
                    break;
                }
            }
        }
    }

    /**
     * This class loads the correct padding values for a contact detail item so they can be applied
     * dynamically. For example, this supports the case where some detail items can be indented and
     * need extra padding.
     */
    private static class ViewEntryDimensions {

        private final int mWidePaddingLeft;
        private final int mPaddingLeft;
        private final int mPaddingRight;
        private final int mPaddingTop;
        private final int mPaddingBottom;

        public ViewEntryDimensions(Resources resources) {
            mPaddingLeft = resources.getDimensionPixelSize(
                    R.dimen.detail_item_side_margin);
            mPaddingTop = resources.getDimensionPixelSize(
                    R.dimen.detail_item_vertical_margin);
            mWidePaddingLeft = mPaddingLeft +
                    resources.getDimensionPixelSize(R.dimen.detail_item_icon_margin) +
                    resources.getDimensionPixelSize(R.dimen.detail_network_icon_size);
            mPaddingRight = mPaddingLeft;
            mPaddingBottom = mPaddingTop;
        }

        public int getWidePaddingLeft() {
            return mWidePaddingLeft;
        }

        public int getPaddingLeft() {
            return mPaddingLeft;
        }

        public int getPaddingRight() {
            return mPaddingRight;
        }

        public int getPaddingTop() {
            return mPaddingTop;
        }

        public int getPaddingBottom() {
            return mPaddingBottom;
        }
    }

    public static interface Listener {
        /**
         * User clicked a single item (e.g. mail). The intent passed in could be null.
         */
        public void onItemClicked(Intent intent);

        /**
         * User requested creation of a new contact with the specified values.
         *
         * @param values ContentValues containing data rows for the new contact.
         * @param account Account where the new contact should be created.
         */
        public void onCreateRawContactRequested(ArrayList<ContentValues> values,
                AccountWithDataSet account);
    }

    /**
     * Adapter for the invitable account types; used for the invitable account type list popup.
     */
    private final static class InvitableAccountTypesAdapter extends BaseAdapter {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private final ContactLoader.Result mContactData;
        private final ArrayList<AccountType> mAccountTypes;

        public InvitableAccountTypesAdapter(Context context, ContactLoader.Result contactData) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mContactData = contactData;
            final List<AccountType> types = contactData.getInvitableAccountTypes();
            mAccountTypes = new ArrayList<AccountType>(types.size());

            AccountTypeManager manager = AccountTypeManager.getInstance(context);
            for (int i = 0; i < types.size(); i++) {
                mAccountTypes.add(types.get(i));
            }

            Collections.sort(mAccountTypes, new AccountType.DisplayLabelComparator(mContext));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View resultView =
                    (convertView != null) ? convertView
                    : mInflater.inflate(R.layout.account_selector_list_item, parent, false);

            final TextView text1 = (TextView)resultView.findViewById(android.R.id.text1);
            final TextView text2 = (TextView)resultView.findViewById(android.R.id.text2);
            final ImageView icon = (ImageView)resultView.findViewById(android.R.id.icon);

            final AccountType accountType = mAccountTypes.get(position);

            CharSequence action = accountType.getInviteContactActionLabel(mContext);
            CharSequence label = accountType.getDisplayLabel(mContext);
            if (TextUtils.isEmpty(action)) {
                text1.setText(label);
                text2.setVisibility(View.GONE);
            } else {
                text1.setText(action);
                text2.setVisibility(View.VISIBLE);
                text2.setText(label);
            }
            icon.setImageDrawable(accountType.getDisplayIcon(mContext));

            return resultView;
        }

        @Override
        public int getCount() {
            return mAccountTypes.size();
        }

        @Override
        public AccountType getItem(int position) {
            return mAccountTypes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    /* Begin: Modified by xiepengfei for 2012/03/28 */
    public interface ListOptionsFragmentListener {
        public void getItemSelectedPosition(int position, String content);
    }

    public class ListOptionsFragment extends DialogFragment implements OnItemSelectedListener,
            OnItemClickListener {
        private final int title;

        private String[] content2;
        private ListView mListView;
        private Adapter mAdapter;
        private final ListOptionsFragmentListener mListOptionsFragmentListener;
        private ArrayList<DetailViewEntry> entry;
        private Context mContext;

        public ListOptionsFragment(int titleId, ArrayList<DetailViewEntry> viewList,
                ListOptionsFragmentListener listener) {
            this.title = titleId;
            this.entry = viewList;
            this.mListOptionsFragmentListener = listener;

            int size = entry.size();
            content2 = new String[size];
            for (int i = 0; i < size; i++) {
                content2[i] = entry.get(i).data;
            }
        }

        /* Begin: Modified by xiepengfei for add a new constructor 2012/03/29 */
        public ListOptionsFragment(Context mContext, int titleId, int[] content,
                ListOptionsFragmentListener listener) {
            this.mContext = mContext;
            this.mListOptionsFragmentListener = listener;
            this.title = titleId;
            int size = content.length;
            content2 = new String[size];
            for (int i = 0; i < size; i++) {
                content2[i] = mContext.getString(content[i]);
            }
        }

        /* End: Modified by xiepengfei for add a new constructor 2012/03/29 */

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(title).setView(createContentView())
                    .create();
        }

        private View createContentView() {
            View view = LayoutInflater.from(getActivity()).inflate(
                    R.layout.contact_detail_list_options_fragment, null);
            mListView = (ListView) view
                    .findViewById(R.id.contact_detail_list_options_fragment_listview);
            mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.my_simple_list_item_1,
                    content2);
            mListView.setAdapter((ListAdapter) mAdapter);
            mListView.setOnItemSelectedListener(this);
            mListView.setOnItemClickListener(this);
            return view;
        }

        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {

        }

        public void onNothingSelected(AdapterView<?> parent) {

        }

        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            this.dismiss();
            mListOptionsFragmentListener.getItemSelectedPosition(position, content2[position]);
        }
    }

    /* End: Modified by xiepengfei for 2012/03/28 */

    /* Begin: Modified by xiepengfei for share menu content 2012/03/29 */
    /**
     * Calls into the contacts provider to get a pre-authorized version of the
     * given URI.
     */
    private Uri getPreAuthorizedUri(Uri uri) {
        Bundle uriBundle = new Bundle();
        uriBundle.putParcelable(ContactsContract.Authorization.KEY_URI_TO_AUTHORIZE, uri);
        Bundle authResponse = mContext.getContentResolver().call(
                ContactsContract.AUTHORITY_URI,
                ContactsContract.Authorization.AUTHORIZATION_METHOD,
                null,
                uriBundle);
        if (authResponse != null) {
            return (Uri) authResponse.getParcelable(
                    ContactsContract.Authorization.KEY_AUTHORIZED_URI);
        } else {
            return uri;
        }
    }

    public void sendContactInfoByBluetooth() {

        if (mContactData == null)
            return;

        final String lookupKey = mContactData.getLookupKey();
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey);
        if (mContactData.isUserProfile()) {
            // User is sharing the profile. We don't want to force the receiver
            // to have
            // the highly-privileged READ_PROFILE permission, so we need to
            // request a
            // pre-authorized URI from the provider.
            shareUri = getPreAuthorizedUri(shareUri);
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);

        // Launch chooser to share contact via
        final CharSequence chooseTitle = mContext.getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            mContext.startActivity(chooseIntent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(mContext, R.string.share_error, Toast.LENGTH_SHORT).show();
        }

    }

    public void sendContactInfoBySMS() {
        int size = mMapContactInfo.size();
        if (size == 0)
            return;

        StringBuffer smsBody = new StringBuffer();
        smsBody.append(ContactDetailDisplayUtils.getDisplayName(getActivity(), mContactData))
                .append("\n");
        smsBody.append(mMapContactInfo.values().toString());

        final Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.fromParts(Constants.SCHEME_SMSTO, "", TAG));
        smsIntent.putExtra("sms_body", smsBody.toString());
        getActivity().startActivity(smsIntent);

    }

    public void sendContactInfoByMMS() {
        if (mContactData == null)
            return;

        final String lookupKey = mContactData.getLookupKey();
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey);
        if (mContactData.isUserProfile()) {
            // User is sharing the profile. We don't want to force the receiver
            // to have
            // the highly-privileged READ_PROFILE permission, so we need to
            // request a
            // pre-authorized URI from the provider.
            shareUri = getPreAuthorizedUri(shareUri);
        }

/*Begin: Modified by xiepengfei for send by mms 2012/06/07*/
//        final Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setType("vnd.android-dir/mms-sms");
//        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");

/*End: Modified by xiepengfei for send by mms 2012/06/07*/


        // Launch chooser to share contact via
        final CharSequence chooseTitle = mContext.getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            mContext.startActivity(chooseIntent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(mContext, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllArrayList() {
        mPhoneEntries.clear();
        mSmsEntries.clear();
        mEmailEntries.clear();
        mPostalEntries.clear();
        mImEntries.clear();
        mNicknameEntries.clear();
        mGroupEntries.clear();
        mRelationEntries.clear();
        mNoteEntries.clear();
        mWebsiteEntries.clear();
        mSipEntries.clear();
        mEventEntries.clear();
        mLocalGroupEntries.clear();

        mOtherEntries.clear();
    }
   /*Begin: Modified by sliangqi for group_add 2012-8-21*/
   public void createChooseLocalGroupsDialog(final boolean flag) {
        Cursor c = getActivity().getContentResolver().query(LocalGroups.CONTENT_URI, null, null,null, null);
        String s[] = new String[c.getCount()];
        c.moveToFirst();
        int i = 0;
        do{
            s[i] = c.getString(c.getColumnIndex("title"));
            i++;
        }while(c.moveToNext());
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.label_groups)
            .setAdapter(new ArrayAdapter<String>(getActivity()
                    , R.layout.simple_list_item_1
                    , s)
                    , new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                                EntityDeltaList contactDeltaList = EntityDeltaList.fromIterator(
                    mContactData.getEntities().iterator());
                                EntityDelta entity = contactDeltaList.get(0);
                                ValuesDelta entry = entity.getValues();
                                ContentValues cv = new ContentValues();
                                cv.put("data1", which+1);
                                cv.put("mimetype", "vnd.android.cursor.item/local-groups");
                                cv.put("raw_contact_id", entry.getAsLong(RawContacts._ID));
                                if(!flag)
                                    getActivity().getContentResolver().insert(Uri.parse("content://com.android.contacts/data"), cv);
                                else{
                                    String s[] = {""+entry.getAsLong(RawContacts._ID),"vnd.android.cursor.item/local-groups"};
                                    getActivity().getContentResolver().update(Uri.parse("content://com.android.contacts/data"), cv,"raw_contact_id=? and mimetype=?",s);
                                }
                            }
                    })
            .create();
        dialog.show();
    }
   /*End: Modified by sliangqi for group_add 2012-8-21*/
    /* End: Modified by xiepengfei for share menu content 2012/03/29 */

    /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
    private class LocalGroupContent extends ContentObserver {

        public LocalGroupContent(Handler arg0) {
            super(arg0);
        }

        @Override
        public void onChange(boolean arg0) {
            super.onChange(arg0);
            Log.v(TAG,"Eden  local group change ");
        }

    }
/*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
    /*Begin: Modified by wqiang for modified ContactDetail View 2012/08/18*/
    private Intent newDialNumberIntent(String number) {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                Uri.fromParts("tel", number, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    public void dialButtonPressed(String mPhonenumber, int slot) {
        final Intent intent = newDialNumberIntent(mPhonenumber);
        intent.putExtra(EXTRA_CALL_ORIGIN,
                CALL_ORIGIN_DIALTACTS);
        intent.putExtra("subscription", slot);
        intent.putExtra("directDial", true);
        startActivity(intent);
        getActivity().finish();
    }
    /*End: Modified by wqiang for modified ContactDetail View 2012/08/18*/

}
