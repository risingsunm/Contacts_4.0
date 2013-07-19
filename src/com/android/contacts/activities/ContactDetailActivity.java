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
 * limitations under the License
 */

package com.android.contacts.activities;

import com.android.contacts.ContactLoader;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.detail.ContactDetailDisplayUtils;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ContactDetailLayoutController;
import com.android.contacts.detail.ContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.util.PhoneCapabilityTester;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Toast;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;

/*Begin: Modified by xiepengfei for import jar 2012/05/21*/
import com.android.contacts.detail.ContactDetailHistoryFragment;
import android.media.RingtoneManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import com.android.contacts.ContactLoader.Result;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.ContentObserver;
import android.provider.LocalGroups;
/*End: Modified by xiepengfei for import jar 2012/05/21*/

public class ContactDetailActivity extends ContactsActivity{
    private static final String TAG = "ContactDetailActivity";

    public static final boolean DBG = false; // Add by xiepengfei 2012/05/17

    /**
     * Boolean intent key that specifies whether pressing the "up" affordance in this activity
     * should cause it to finish itself or launch an intent to bring the user back to a specific
     * parent activity - the {@link PeopleActivity}.
     */
    public static final String INTENT_KEY_FINISH_ACTIVITY_ON_UP_SELECTED =
            "finishActivityOnUpSelected";

    private ContactLoader.Result mContactData;
    private Uri mLookupUri;
    private boolean mFinishActivityOnUpSelected;

    private ContactDetailLayoutController mContactDetailLayoutController;
    private ContactLoaderFragment mLoaderFragment;

    private Handler mHandler = new Handler();

    /*Begin: Modified by xiepengfei for set ringtone 2012/04/10*/
    private String mCustomRingtone;
    /** The launch code when picking a ringtone */
    private static final int REQUEST_CODE_PICK_RINGTONE = 1;
    private static final int REQUEST_CODE_PICK_RINGTONE_FROM_FILEEXPLOER = 2;
    /*End: Modified by xiepengfei for set ringtone 2012/04/10*/

    /*Start of xiepengfei on 2012-3-19 10:21 added two tab*/
//    @Override
//    public void onCreate(Bundle savedState) {
//        super.onCreate(savedState);
//        if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
//            // This activity must not be shown. We have to select the contact in the
//            // PeopleActivity instead ==> Create a forward intent and finish
//            final Intent originalIntent = getIntent();
//            Intent intent = new Intent();
//            intent.setAction(originalIntent.getAction());
//            intent.setDataAndType(originalIntent.getData(), originalIntent.getType());
//            intent.setFlags(
//                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_FORWARD_RESULT
//                            | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//            intent.setClass(this, PeopleActivity.class);
//            startActivity(intent);
//            finish();
//            return;
//        }
//
//        mIgnoreDefaultUpBehavior = getIntent().getBooleanExtra(
//                INTENT_KEY_IGNORE_DEFAULT_UP_BEHAVIOR, false);
//
//        setContentView(R.layout.contact_detail_activity);
//
//        mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
//                getFragmentManager(), findViewById(R.id.contact_detail_container),
//                mContactDetailFragmentListener);
//
//        // We want the UP affordance but no app icon.
//        // Setting HOME_AS_UP, SHOW_TITLE and clearing SHOW_HOME does the trick.
//        ActionBar actionBar = getActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE,
//                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
//                    | ActionBar.DISPLAY_SHOW_HOME);
//            actionBar.setTitle("");
//        }
//
//        Log.i(TAG, getIntent().getData().toString());
//    }
    //private CallLogFragment mCallLogFragment;
    private ContactDetailHistoryFragment mCallLogFragment;
    /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
    private LocalGroupContent mLocalGroupContent;
    private boolean mIsNeadLoadAgain = false;
    /*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
            // This activity must not be shown. We have to select the contact in the
            // PeopleActivity instead ==> Create a forward intent and finish
            final Intent originalIntent = getIntent();
            Intent intent = new Intent();
            intent.setAction(originalIntent.getAction());
            intent.setDataAndType(originalIntent.getData(), originalIntent.getType());
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_FORWARD_RESULT
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            intent.setClass(this, PeopleActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        mFinishActivityOnUpSelected = getIntent().getBooleanExtra(
                INTENT_KEY_FINISH_ACTIVITY_ON_UP_SELECTED, false);

        setContentView(R.layout.contact_detail_activity);

        mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
                getFragmentManager(), findViewById(R.id.contact_detail_container),
                mContactDetailFragmentListener);

        // We want the UP affordance but no app icon.
        // Setting HOME_AS_UP, SHOW_TITLE and clearing SHOW_HOME does the trick.

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_SHOW_TITLE );

            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.setTitle("");
        }

        mLoaderFragment = new ContactLoaderFragment();
        mCallLogFragment = new ContactDetailHistoryFragment();

        actionBar.addTab(actionBar.newTab()
                .setTag("profile")
                .setText(R.string.profile).setTabListener(
                        new LoaderFragmentListener(mLoaderFragment, this)));

        actionBar.addTab(actionBar.newTab()
                .setTag("call_log")
                .setText(R.string.call_log).setTabListener(new CallLogFragmentListener(mCallLogFragment, "call_log",this)));

        mCallLogFragment.loadLookupUri(getIntent().getData());
        /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
        mLocalGroupContent = new LocalGroupContent(new Handler());
        getContentResolver().registerContentObserver(LocalGroups.CONTENT_URI, true, mLocalGroupContent);
        Log.v(TAG,"Eden registerContentObserver   LocalGroups.CONTENT_URI:"+LocalGroups.CONTENT_URI);
        /*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
        Log.i(TAG, getIntent().getData().toString());
    }
    /*End of xiepengfei on 2012-3-19 15:21 added two tab*/
    @Override
    public void onAttachFragment(Fragment fragment) {
         if (fragment instanceof ContactLoaderFragment) {
            mLoaderFragment = (ContactLoaderFragment) fragment;
            mLoaderFragment.setListener(mLoaderFragmentListener);
            mLoaderFragment.loadUri(getIntent().getData());
        }
    }

/*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/22*/

    @Override
    public void onStart() {
        super.onStart();

        Log.v(TAG,"Eden onStart:"+mIsNeadLoadAgain);
        if(mIsNeadLoadAgain){

            Bundle args = new Bundle();
            args.putParcelable("contactUri", mLookupUri);
            getLoaderManager().restartLoader(1, args, mDetailCallBack);
            //mLoaderFragment.loadUriAgain(mLookupUri);
            mIsNeadLoadAgain = false;
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
        getContentResolver().unregisterContentObserver(mLocalGroupContent);
        /*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/

    }
/*End: Modified by xiepengfei for ContentObserver to local group 2012/05/22*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.star, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
        ViewGroup starredContainer = (ViewGroup) getLayoutInflater().inflate(
                R.layout.favorites_star, null, false);
        final CheckBox starredView = (CheckBox) starredContainer.findViewById(R.id.star);
        starredView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle "starred" state
                // Make sure there is a contact
                if (mLookupUri != null) {
                    Intent intent = ContactSaveService.createSetStarredIntent(
                            ContactDetailActivity.this, mLookupUri, starredView.isChecked());
                    ContactDetailActivity.this.startService(intent);
                }
            }
        });
        // If there is contact data, update the starred state
        if (mContactData != null) {
            ContactDetailDisplayUtils.setStarred(mContactData, starredView);
        }
        starredMenuItem.setActionView(starredContainer);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First check if the {@link ContactLoaderFragment} can handle the key
        if (mLoaderFragment != null && mLoaderFragment.handleKeyDown(keyCode)) return true;

        // Otherwise find the correct fragment to handle the event
        FragmentKeyListener mCurrentFragment = mContactDetailLayoutController.getCurrentPage();
        if (mCurrentFragment != null && mCurrentFragment.handleKeyDown(keyCode)) return true;

        // In the last case, give the key event to the superclass.
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mContactDetailLayoutController != null) {
            mContactDetailLayoutController.onSaveInstanceState(outState);
        }
    }

    private final ContactLoaderFragmentListener mLoaderFragmentListener =
            new ContactLoaderFragmentListener() {
        @Override
        public void onContactNotFound() {
            finish();
        }

        @Override
        public void onDetailsLoaded(final ContactLoader.Result result) {
            if (result == null) {
                return;
            }
            // Since {@link FragmentTransaction}s cannot be done in the onLoadFinished() of the
            // {@link LoaderCallbacks}, then post this {@link Runnable} to the {@link Handler}
            // on the main thread to execute later.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // If the activity is destroyed (or will be destroyed soon), don't update the UI
                    if (isFinishing()) {
                        return;
                    }
                    mContactData = result;
                    mLookupUri = result.getLookupUri();
                    invalidateOptionsMenu();
                    setupTitle();
                    mContactDetailLayoutController.setContactData(mContactData);

                    /*Begin: Modified by xiepengfei for add mms query 2012/05/28*/
                    mCallLogFragment.setContactData(mContactData);
                    /*End: Modified by xiepengfei for add mms query 2012/05/28*/

                    /*Begin: Modified by xiepengfei for set ringtone 2012/04/10*/
                    mCustomRingtone = mContactData.getCustomRingtone();
                    /*End: Modified by xiepengfei for set ringtone 2012/04/10*/
                }
            });
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            // Don't finish the detail activity after launching the editor because when the
            // editor is done, we will still want to show the updated contact details using
            // this activity.
            startActivity(intent);
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
            ContactDeletionInteraction.start(ContactDetailActivity.this, contactUri, true);
        }
    };


    /*Start of xiepengfei on 2012-3-9 15:11 add contact's photo to action bar*/
    /**
     * Setup the activity title and subtitle with contact name and company.
     */
//    private void setupTitle() {
//        CharSequence displayName = ContactDetailDisplayUtils.getDisplayName(this, mContactData);
//        String company =  ContactDetailDisplayUtils.getCompany(this, mContactData);
//
//        ActionBar actionBar = getActionBar();
//        actionBar.setTitle(displayName);
//        actionBar.setSubtitle(company);
//
//        if (!TextUtils.isEmpty(displayName) &&
//                AccessibilityManager.getInstance(this).isEnabled()) {
//            View decorView = getWindow().getDecorView();
//            decorView.setContentDescription(displayName);
//            decorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
//        }
//    }
    private void setupTitle() {
        CharSequence displayName = ContactDetailDisplayUtils.getDisplayName(this, mContactData);
        String company =  ContactDetailDisplayUtils.getCompany(this, mContactData);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(displayName);
        actionBar.setSubtitle(company);

        final byte[] mPhotoData = mContactData.getPhotoBinaryData();
        if(mPhotoData != null && mPhotoData.length != 0){
            final Bitmap mbp = BitmapFactory.decodeByteArray(mPhotoData, 0, mPhotoData.length);
            final Drawable icon = new BitmapDrawable(mbp);
            actionBar.setIcon(icon);
        }else{
            actionBar.setIcon(R.drawable.contacts_default_image);
        }

        if (!TextUtils.isEmpty(displayName) &&
                AccessibilityManager.getInstance(this).isEnabled()) {
            View decorView = getWindow().getDecorView();
            decorView.setContentDescription(displayName);
            decorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }
    /*End of xiepengfei on 2012-3-9 15:11 add contact's photo to action bar */

    private final ContactDetailFragment.Listener mContactDetailFragmentListener =
            new ContactDetailFragment.Listener() {
        @Override
        public void onItemClicked(Intent intent) {

            /*Begin: Modified by xiepengfei for add bottom toolbar 2012/06/13*/
            /*Begin: Modified by sliangqi for group_add 2012-8-21*/
            ContactDetailFragment fragment = null;
            /*End: Modified by sliangqi for group_add 2012-8-21*/
            if(mContactDetailLayoutController.getCurrentPage() instanceof ContactDetailFragment){
                fragment = (ContactDetailFragment)mContactDetailLayoutController.getCurrentPage();
                if(fragment.mdisableIntent){
                    System.out.println("mdisableIntent == true");
                    return;
                }
            }
            ;
            /*End: Modified by xiepengfei for add bottom toolbar 2012/06/13*/
            if (intent == null) {
                return;
            }
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                /*Begin: Modified by xiepengfei for set ringtone 2012/04/10*/
                if(intent.getExtra("typestring") == null){
                    Log.e(TAG, "No activity found for intent: " + intent);
                    return;
                }
                int type = Integer.valueOf(intent.getExtra("typestring").toString());
                switch(type){
                /*Begin: Modified by sliangqi for group_add 2012-8-21*/
                case R.string.label_groups:{
                    fragment.createChooseLocalGroupsDialog(intent.getBooleanExtra("inGroup", false));
                }
                /*End: Modified by sliangqi for group_add 2012-8-21*/
                case R.string.contact_detail_fragment_groups:
                    Log.v(TAG,"type == groups");

                    return;
                case R.string.contact_detail_fragment_ringtone:
                    Log.v(TAG,"type == ringtone");
                    //doPickRingtone();
                    showDialog(DIALOG_CHOOSE_RINGTONE_ID);
                    return;
                    default: Log.e(TAG,"Unknow type:"+type);break;
                }
                /*End: Modified by xiepengfei for set ringtone 2012/04/10*/

            }
        }

        @Override
        public void onCreateRawContactRequested(
                ArrayList<ContentValues> values, AccountWithDataSet account) {
            Toast.makeText(ContactDetailActivity.this, R.string.toast_making_personal_copy,
                    Toast.LENGTH_LONG).show();
            Intent serviceIntent = ContactSaveService.createNewRawContactIntent(
                    ContactDetailActivity.this, values, account,
                    ContactDetailActivity.class, Intent.ACTION_VIEW);
            startService(serviceIntent);

        }
    };

    /**
     * This interface should be implemented by {@link Fragment}s within this
     * activity so that the activity can determine whether the currently
     * displayed view is handling the key event or not.
     */
    public interface FragmentKeyListener {
        /**
         * Returns true if the key down event will be handled by the implementing class, or false
         * otherwise.
         */
        public boolean handleKeyDown(int keyCode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mFinishActivityOnUpSelected) {
                    finish();
                    return true;
                }

                /*Begin: Modified by xiepengfei for add back 2012/05/24*/
//                Intent intent = new Intent(this, PeopleActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
                /*End: Modified by xiepengfei for add back 2012/05/24*/

                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    /*Begin: Modified by xiepengfei for two tab TabListener 2012/03/19*/
    public static class LoaderFragmentListener implements ActionBar.TabListener{
        private  ContactLoaderFragment mFragment;
        private  String mTag;
        private  Uri lookupUri;
        private  ContactLoaderFragmentListener value;
        private  Activity mActivity;

        public LoaderFragmentListener(ContactLoaderFragment fragment,Activity a){
            this.mFragment = fragment;
            this.mActivity = a;
        }
        public LoaderFragmentListener(ContactLoaderFragment f,String tag,Uri uri,ContactLoaderFragmentListener value,
                Activity a){
            this.mFragment = f;
            this.mTag = tag;
            this.lookupUri = uri;
            this.value = value;
            this.mActivity = a;
        }
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            //ft.attach(mFragment);
        }
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
             //ft.detach(mFragment);
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }
    }
    public static class CallLogFragmentListener implements ActionBar.TabListener{
        //private final TabContentFragment mFragment;
        private final ContactDetailHistoryFragment mFragment;
        private final String mTag;
        private final Activity mActivity;
        private boolean frist = true;
        public CallLogFragmentListener(ContactDetailHistoryFragment f,String tag,Activity a){
            this.mFragment = f;
            this.mTag = tag;
            this.mActivity = a;
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.add(R.id.contact_detail_call_log_view, mFragment,"call_log");
                ft.detach(mFragment);
                ft.commit();
            }
        }
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            ft.attach(mFragment);
        }
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }
    }
    /*End: Modified by xiepengfei for two tab TabListener 2012/03/19*/

    private static final int DIALOG_CHOOSE_RINGTONE_ID = 0x1; // add by bxinchun 2012-07-27
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_CHOOSE_RINGTONE_ID:
                return createChooseRingtoneDialog();

            default:
                break;
        }
        return super.onCreateDialog(id, args);
    }

    /**
     * Create choose ringtone dialog.
     * @author bxinchun @date 2012-07-27
     */
    private Dialog createChooseRingtoneDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.ringtone)
            .setAdapter(new ArrayAdapter<String>(this
                    , R.layout.simple_list_item_1
                    , new String [] {
                        getString(R.string.contact_detail_fragment_default_ringtone)
                        , getString(R.string.choose_ringtone_from_file)
                        , getString(R.string.phone_ringtone)})
                    , new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: // default
                                    handleRingtonePicked(null);
                                    break;
                                case 1: // choose from file
                                    try {
                                        Intent intent = new Intent("android.intent.action.GET_CONTENT");
                                        intent.setPackage("com.android.qrdfileexplorer");
                                        intent.setType("audio/*");

                                        startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE_FROM_FILEEXPLOER);
                                    } catch (ActivityNotFoundException anf) {
                                        System.out.println(anf.getMessage());
                                    }
                                    break;
                                case 2: // pick from system
                                    doPickRingtone();
                                    break;

                                default:
                                    break;
                            }
                        }
                    })
            .create();

        return dialog;
    }

    /*Begin: Modified by xiepengfei for set ringtone 2012/04/10*/
    /**
     * Pick a ringtone from ringtone manager.
     */
    private void doPickRingtone() {

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        // Allow user to pick 'Default'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false); // true , changed by bxinchun 2012-07-27, has default already.
        // Show only ringtones
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        // Don't show 'Silent'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

        Uri ringtoneUri;
        if (mCustomRingtone != null) {
            ringtoneUri = Uri.parse(mCustomRingtone);
        } else {
            // Otherwise pick default ringtone Uri so that something is selected.
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        // Put checkmark next to the current ringtone for this contact
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);

        // Launch!
        startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_PICK_RINGTONE: {
                Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                handleRingtonePicked(pickedUri);
                break;
            }
            case REQUEST_CODE_PICK_RINGTONE_FROM_FILEEXPLOER: {
                if (data != null) {
                    handleRingtonePicked(data.getData());
                }
                break;
            }
        }
    }

    private void handleRingtonePicked(Uri pickedUri) {
        if (pickedUri == null || RingtoneManager.isDefault(pickedUri)) {
            mCustomRingtone = null;
        } else {
            mCustomRingtone = pickedUri.toString();
        }
        Intent intent = ContactSaveService.createSetRingtone(
                this, mLookupUri, mCustomRingtone);
        startService(intent);
    }
    /*End: Modified by xiepengfei for set ringtone 2012/04/10*/

    /*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
    private class LocalGroupContent extends ContentObserver {

        public LocalGroupContent(Handler arg0) {
            super(arg0);
        }

        @Override
        public void onChange(boolean arg0) {
            super.onChange(arg0);
            mIsNeadLoadAgain = true;
        }
    }

    public final LoaderManager.LoaderCallbacks<ContactLoader.Result> mDetailCallBack = new LoaderCallbacks<ContactLoader.Result>() {

        @Override
        public void onLoaderReset(Loader<Result> arg0) {
        }

        @Override
        public void onLoadFinished(Loader<ContactLoader.Result> loader, ContactLoader.Result data) {
            if (!mLookupUri.equals(data.getRequestedUri())) {
                return;
            }

            if (data.isError()) {
                // This shouldn't ever happen, so throw an exception. The {@link
                // ContactLoader}
                // should log the actual exception.
                throw new IllegalStateException("Failed to load contact", data.getException());
            } else if (data.isNotFound()) {
                Log.i(TAG, "No contact found: " + ((ContactLoader) loader).getLookupUri());
                mContactData = null;
            } else {
                mContactData = data;
            }

            if (mContactData == null) {
                finish();
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // If the activity is destroyed (or will be destroyed
                        // soon), don't update the UI
                        if (isFinishing()) {
                            return;
                        }

                        mLookupUri = mContactData.getLookupUri();
                        invalidateOptionsMenu();
                        setupTitle();
                        mContactDetailLayoutController.setContactData(mContactData);

                        mCustomRingtone = mContactData.getCustomRingtone();

                        /*Begin: Modified by xiepengfei for add mms query 2012/05/28*/
                        mCallLogFragment.setContactData(mContactData);
                        /*End: Modified by xiepengfei for add mms query 2012/05/28*/
                    }
                });

            }

        }

        @Override
        public Loader<Result> onCreateLoader(int id, Bundle arg1) {
            Uri lookupUri = arg1.getParcelable("contactUri");
            return new ContactLoader(getApplicationContext(), lookupUri,
                    true /* loadGroupMetaData */,
                    true /* loadStreamItems */, true /* load invitable account types*/);
        }
    };
/*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
}
