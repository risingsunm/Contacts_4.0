/*
 * Copyright (c) 2012, Code Aurora Forum. All rights reserved.
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

package com.android.contacts;

import com.android.contacts.BackScrollManager.ScrollableHeader;
import com.android.contacts.activities.ContactDetailActivity;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.activities.JoinContactActivity;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.calllog.CallDetailHistoryAdapter;
import com.android.contacts.calllog.CallTypeHelper;
import com.android.contacts.calllog.ContactInfo;
import com.android.contacts.calllog.ContactInfoHelper;
import com.android.contacts.calllog.PhoneNumberHelper;
import com.android.contacts.dialpad.DialpadFragment.ErrorDialogFragment;
import com.android.contacts.editor.BaseRawContactEditorView;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.Editor;
import com.android.contacts.editor.RawContactEditorView;
import com.android.contacts.editor.RawContactReadOnlyEditorView;
import com.android.contacts.editor.TextFieldsEditorView;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
import com.android.contacts.editor.Editor.EditorListener;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDeltaList;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.util.AsyncTaskExecutor;
import com.android.contacts.util.AsyncTaskExecutors;
import com.android.contacts.voicemail.VoicemailPlaybackFragment;
import com.android.contacts.voicemail.VoicemailStatusHelper;
import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;
import com.android.internal.telephony.MSimConstants;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.ToneGenerator;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.CallLog.Calls;
import android.provider.Contacts.Intents.Insert;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.VoicemailContract.Voicemails;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.telephony.MSimTelephonyManager;

/*Begin: Modified by zxiaona for calldetail_home_location 2012/07/23*/
import java.util.HashMap;
import com.android.contacts.LocSearchUtil;

/*End: Modified by zxiaona for calldetail_home_location 2012/07/23*/

/**
 * Displays the details of a specific call log entry.
 * <p>
 * This activity can be either started with the URI of a single call log entry, or with the
 * {@link #EXTRA_CALL_LOG_IDS} extra to specify a group of call log entries.
 */
public class CallDetailActivity extends Activity implements ProximitySensorAware {
    private static final String TAG = "CallDetail";

    /** The time to wait before enabling the blank the screen due to the proximity sensor. */
    private static final long PROXIMITY_BLANK_DELAY_MILLIS = 100;
    /** The time to wait before disabling the blank the screen due to the proximity sensor. */
    private static final long PROXIMITY_UNBLANK_DELAY_MILLIS = 500;

    /** The enumeration of {@link AsyncTask} objects used in this class. */
    public enum Tasks {
        MARK_VOICEMAIL_READ,
        DELETE_VOICEMAIL_AND_FINISH,
        REMOVE_FROM_CALL_LOG_AND_FINISH,
        UPDATE_PHONE_CALL_DETAILS,
    }

    /** A long array extra containing ids of call log entries to display. */
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";
    /** If we are started with a voicemail, we'll find the uri to play with this extra. */
    public static final String EXTRA_VOICEMAIL_URI = "EXTRA_VOICEMAIL_URI";
    /** If we should immediately start playback of the voicemail, this extra will be set to true. */
    public static final String EXTRA_VOICEMAIL_START_PLAYBACK = "EXTRA_VOICEMAIL_START_PLAYBACK";

    private CallTypeHelper mCallTypeHelper;
    private PhoneNumberHelper mPhoneNumberHelper;
    private PhoneCallDetailsHelper mPhoneCallDetailsHelper;
    private TextView mHeaderTextView;
    private View mHeaderOverlayView;
    private ImageView mMainActionView;
    private ImageView mMainActionPushLayerView;
    private ImageView mContactBackgroundView;
    private AsyncTaskExecutor mAsyncTaskExecutor;
    private ContactInfoHelper mContactInfoHelper;

    private String mNumber = null;
    private int sub = -1;
    private String mDefaultCountryIso;

    /* package */ LayoutInflater mInflater;
    /* package */ Resources mResources;
    /** Helper to load contact photos. */
    private ContactPhotoManager mContactPhotoManager;
    /** Helper to make async queries to content resolver. */
    private CallDetailActivityQueryHandler mAsyncQueryHandler;
    /** Helper to get voicemail status messages. */
    private VoicemailStatusHelper mVoicemailStatusHelper;
    // Views related to voicemail status message.
    private View mStatusMessageView;
    private TextView mStatusMessageText;
    private TextView mStatusMessageAction;

    /* Begin: Modified by zxiaona for call_detail_screen 2012/03/28 */
    private TextView mContactsName;
    private TextView mPhoneNumber;
    private ImageView mAddContactsIcon;
    private ImageButton mCallingButton;
    private ImageButton mIPButton;
    private ImageButton mGButton;
    private ImageButton mMessage;
    private TextView mHomeLocation;
    private TextView mHomeLocation2;
    /* End: Modified by zxiaona for call_detail_screen 2012/03/28 */

    /*Begin: Modified by zxiaona for call_detail_secondrybutton 2012/04/06*/
    private Button mBottomAdd;
    private Button mBottonRefresh;
    private View mBottomLayout;
    private TextView mUnknownNumber;
    /* End: Modified by zxiaona for call_detail_secondrybutton 2012/04/06 */

    /* Begin: Modified by zxiaona for call_detail_joincontacts 2012/04/17 */
    public static final String EXTRA_TARGET_CONTACT_ID = "com.android.contacts.action.CONTACT_ID";
    public QuickContactBadge quickContactView;
    private static final int REQUEST_CODE_JOIN = 0;
    /* End: Modified by zxiaona for call_detail_joincontacts 2012/04/17 */

    /* Begin: Modified by zxiaona for CG dial 2012/06/13 */
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";
    public static final String CALL_ORIGIN_DIALTACTS =
            "com.android.contacts.activities.DialtactsActivity";
    /* End: Modified by zxiaona for CG dial 2012/06/13 */

    /** Whether we should show "edit number before call" in the options menu. */
    private boolean mHasEditNumberBeforeCallOption;
    /** Whether we should show "trash" in the options menu. */
    private boolean mHasTrashOption;
    /** Whether we should show "remove from call log" in the options menu. */
    private boolean mHasRemoveFromCallLogOption;

    private ProximitySensorManager mProximitySensorManager;
    private final ProximitySensorListener mProximitySensorListener = new ProximitySensorListener();

    /** Listener to changes in the proximity sensor state. */
    private class ProximitySensorListener implements ProximitySensorManager.Listener {
        /** Used to show a blank view and hide the action bar. */
        private final Runnable mBlankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = findViewById(R.id.blank);
                blankView.setVisibility(View.VISIBLE);
                getActionBar().hide();
            }
        };
        /** Used to remove the blank view and show the action bar. */
        private final Runnable mUnblankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = findViewById(R.id.blank);
                blankView.setVisibility(View.GONE);
                getActionBar().show();
            }
        };

        @Override
        public synchronized void onNear() {
            clearPendingRequests();
            postDelayed(mBlankRunnable, PROXIMITY_BLANK_DELAY_MILLIS);
        }

        @Override
        public synchronized void onFar() {
            clearPendingRequests();
            postDelayed(mUnblankRunnable, PROXIMITY_UNBLANK_DELAY_MILLIS);
        }

        /** Removed any delayed requests that may be pending. */
        public synchronized void clearPendingRequests() {
            View blankView = findViewById(R.id.blank);
            blankView.removeCallbacks(mBlankRunnable);
            blankView.removeCallbacks(mUnblankRunnable);
        }

        /** Post a {@link Runnable} with a delay on the main thread. */
        private synchronized void postDelayed(Runnable runnable, long delayMillis) {
            // Post these instead of executing immediately so that:
            // - They are guaranteed to be executed on the main thread.
            // - If the sensor values changes rapidly for some time, the UI will not be
            //   updated immediately.
            View blankView = findViewById(R.id.blank);
            blankView.postDelayed(runnable, delayMillis);
        }
    }

    static final String[] CALL_LOG_PROJECTION = new String[] {
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
        CallLog.Calls.COUNTRY_ISO,
        CallLog.Calls.GEOCODED_LOCATION,
        CallLog.Calls.SUBSCRIPTION
    };

    static final int DATE_COLUMN_INDEX = 0;
    static final int DURATION_COLUMN_INDEX = 1;
    static final int NUMBER_COLUMN_INDEX = 2;
    static final int CALL_TYPE_COLUMN_INDEX = 3;
    static final int COUNTRY_ISO_COLUMN_INDEX = 4;
    static final int GEOCODED_LOCATION_COLUMN_INDEX = 5;
    static final int SUBSCRIPTION = 6;

    private final View.OnClickListener mPrimaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(((ViewEntry) view.getTag()).primaryIntent);
        }
    };

    private final View.OnClickListener mSecondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(((ViewEntry) view.getTag()).secondaryIntent);
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.call_detail);

        mAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();

        mCallTypeHelper = new CallTypeHelper(getResources());
        mPhoneNumberHelper = new PhoneNumberHelper(mResources);
        mPhoneCallDetailsHelper = new PhoneCallDetailsHelper(mResources, mCallTypeHelper,
                mPhoneNumberHelper);
        mVoicemailStatusHelper = new VoicemailStatusHelperImpl();
        mAsyncQueryHandler = new CallDetailActivityQueryHandler(this);
        mHeaderTextView = (TextView) findViewById(R.id.header_text);
        mHeaderOverlayView = findViewById(R.id.photo_text_bar);
        mStatusMessageView = findViewById(R.id.voicemail_status);
        mStatusMessageText = (TextView) findViewById(R.id.voicemail_status_message);
        mStatusMessageAction = (TextView) findViewById(R.id.voicemail_status_action);
        mMainActionView = (ImageView) findViewById(R.id.main_action);
        mMainActionPushLayerView = (ImageView) findViewById(R.id.main_action_push_layer);
        mContactBackgroundView = (ImageView) findViewById(R.id.contact_background);
        mDefaultCountryIso = ContactsUtils.getCurrentCountryIso(this);
        mContactPhotoManager = ContactPhotoManager.getInstance(this);
        mProximitySensorManager = new ProximitySensorManager(this, mProximitySensorListener);
        mContactInfoHelper = new ContactInfoHelper(this, ContactsUtils.getCurrentCountryIso(this));

        /*Begin: Modified by zxiaona for call_detail 2012/04/10*/
        mHomeLocation2 = (TextView) findViewById(R.id.home_location2);
        mBottomLayout = (View) findViewById(R.id.bottomlayout);
        mBottomAdd = (Button) mBottomLayout.findViewById(R.id.bottom_addcontacts);
        mBottonRefresh = (Button) mBottomLayout.findViewById(R.id.bottom_refreshcontacts);
        /*End: Modified by zxiaona for call_detail 2012/04/10*/

        /*Begin: Modified by zxiaona for call_detail 2012/03/28*/
        mContactsName = (TextView) findViewById(R.id.contacts_name);
        mPhoneNumber = (TextView) findViewById(R.id.phone_number);
        mAddContactsIcon = (ImageView) findViewById(R.id.contacts_add_icon);
        mCallingButton = (ImageButton) findViewById(R.id.call_action_push_layer);
        mCallingButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                String number = mPhoneNumber.getText().toString();
                dialButtonPressed(number,0);

        }
       });
        mGButton = (ImageButton) findViewById(R.id.g_call_action_push_layer);
        mGButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String number = mPhoneNumber.getText().toString();
                dialButtonPressed(number, 1);
            }
        });
        mIPButton = (ImageButton) findViewById(R.id.ipcall_action_push_layer);
        /* Begin: Modified by zxiaona for call_detail_for_ipcall 2012/04/24 */
        mIPButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                String number = mPhoneNumber.getText().toString();
                Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel",
                        number, null));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("ipcall", true);
                startActivity(intent);
            }
        });
        /* End: Modified by zxiaona for call_detail_for_ipcall 2012/04/24 */

        mMessage = (ImageButton) findViewById(R.id.sms_icon);
        mMessage.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                String number = mPhoneNumber.getText().toString();
                // String phonenumber = splitAndTrimPhoneNumbers(number);
                Intent intent = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts("sms", number, null));
                startActivity(intent);
            }
        });
        /* End: Modified by zxiaona for call_detail 2012/03/28 */
        configureActionBar();
        optionallyHandleVoicemail();
        /* Begin: Modified by zxiaona for bug for C . G call 2012/07/07 */
        String cCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",0,"");
        String gCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",1,"");
        if(gCardState.equals("ABSENT")) {
            mGButton.setEnabled(false);
        }
        if(cCardState.equals("ABSENT")) {
            mCallingButton.setEnabled(false);
        }
        if(gCardState.equals("ABSENT") && cCardState.equals("ABSENT")) {
            mGButton.setEnabled(false);
            mCallingButton.setEnabled(true);
            mIPButton.setEnabled(false);
        }
        /* End: Modified by zxiaona for bug for C . G call 2012/07/07 */
    }
    /* Begin: Modified by zxiaona for call_detail 2012/03/28 */
    private Intent newDialNumberIntent(String number) {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                Uri.fromParts("tel", number, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    /* End: Modified by zxiaona for call_detail 2012/03/28 */
    @Override
    public void onResume() {
        super.onResume();
        updateData(getCallLogEntryUris());
    }

    /**
     * Handle voicemail playback or hide voicemail ui.
     * <p>
     * If the Intent used to start this Activity contains the suitable extras, then start voicemail
     * playback.  If it doesn't, then hide the voicemail ui.
     */
    private void optionallyHandleVoicemail() {
        View voicemailContainer = findViewById(R.id.voicemail_container);
        if (hasVoicemail()) {
            // Has voicemail: add the voicemail fragment.  Add suitable arguments to set the uri
            // to play and optionally start the playback.
            // Do a query to fetch the voicemail status messages.
            VoicemailPlaybackFragment playbackFragment = new VoicemailPlaybackFragment();
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putParcelable(EXTRA_VOICEMAIL_URI, getVoicemailUri());
            if (getIntent().getBooleanExtra(EXTRA_VOICEMAIL_START_PLAYBACK, false)) {
                fragmentArguments.putBoolean(EXTRA_VOICEMAIL_START_PLAYBACK, true);
            }
            playbackFragment.setArguments(fragmentArguments);
            voicemailContainer.setVisibility(View.VISIBLE);
            getFragmentManager().beginTransaction()
                    .add(R.id.voicemail_container, playbackFragment).commitAllowingStateLoss();
            mAsyncQueryHandler.startVoicemailStatusQuery(getVoicemailUri());
            markVoicemailAsRead(getVoicemailUri());
        } else {
            // No voicemail uri: hide the status view.
            mStatusMessageView.setVisibility(View.GONE);
            voicemailContainer.setVisibility(View.GONE);
        }
    }

    private boolean hasVoicemail() {
        return getVoicemailUri() != null;
    }

    private Uri getVoicemailUri() {
        return getIntent().getParcelableExtra(EXTRA_VOICEMAIL_URI);
    }

    private void markVoicemailAsRead(final Uri voicemailUri) {
        mAsyncTaskExecutor.submit(Tasks.MARK_VOICEMAIL_READ, new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                ContentValues values = new ContentValues();
                values.put(Voicemails.IS_READ, true);
                getContentResolver().update(voicemailUri, values,
                        Voicemails.IS_READ + " = 0", null);
                return null;
            }
        });
    }

    /**
     * Returns the list of URIs to show.
     * <p>
     * There are two ways the URIs can be provided to the activity: as the data on the intent, or as
     * a list of ids in the call log added as an extra on the URI.
     * <p>
     * If both are available, the data on the intent takes precedence.
     */
    private Uri[] getCallLogEntryUris() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            // If there is a data on the intent, it takes precedence over the extra.
            return new Uri[]{ uri };
        }
        long[] ids = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
        Uri[] uris = new Uri[ids.length];
        for (int index = 0; index < ids.length; ++index) {
            uris[index] = ContentUris.withAppendedId(Calls.CONTENT_URI_WITH_VOICEMAIL, ids[index]);
        }
        return uris;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                // Make sure phone isn't already busy before starting direct call
                TelephonyManager tm = (TelephonyManager)
                        getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                    if (mNumber == null) {
                        Log.e(TAG, "Details view is in progress so ignore CALL KEY");
                        return true;
                    }
                    Intent callIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts("tel", mNumber, null));
                    if (sub != -1)
                        callIntent.putExtra(MSimConstants.SUBSCRIPTION_KEY, sub);
                    Log.d(TAG, "call log sub is " + sub);
                    startActivity(callIntent);
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Update user interface with details of given call.
     *
     * @param callUris URIs into {@link CallLog.Calls} of the calls to be displayed
     */
    private void updateData(final Uri... callUris) {
        class UpdateContactDetailsTask extends AsyncTask<Void, Void, PhoneCallDetails[]> {
            @Override
            public PhoneCallDetails[] doInBackground(Void... params) {
                // TODO: All phone calls correspond to the same person, so we can make a single
                // lookup.
                final int numCalls = callUris.length;
                PhoneCallDetails[] details = new PhoneCallDetails[numCalls];
                try {
                    for (int index = 0; index < numCalls; ++index) {
                        details[index] = getPhoneCallDetailsForUri(callUris[index]);
                    }
                    return details;
                } catch (IllegalArgumentException e) {
                    // Something went wrong reading in our primary data.
                    Log.w(TAG, "invalid URI starting call details", e);
                    return null;
                }
            }

            @Override
            public void onPostExecute(PhoneCallDetails[] details) {
                if (details == null) {
                    // Somewhere went wrong: we're going to bail out and show error to users.
                    Toast.makeText(CallDetailActivity.this, R.string.toast_call_detail_error,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                // We know that all calls are from the same number and the same contact, so pick the
                // first.
                PhoneCallDetails firstDetails = details[0];
                mNumber = firstDetails.number.toString();
                sub = firstDetails.subscription;
                final Uri contactUri = firstDetails.contactUri;
                final Uri photoUri = firstDetails.photoUri;
                // Set the details header, based on the first phone call.
                /* Begin: Modified by zxiaona for call_detail 2012/03/28 */
                // mPhoneCallDetailsHelper.setCallDetailsHeader(mHeaderTextView,
                // firstDetails);
                mPhoneCallDetailsHelper.setCallDetailsHeader(mContactsName, firstDetails);
                mAddContactsIcon.setVisibility(View.INVISIBLE);
                /* End: Modified by zxiaona for call_detail 2012/03/28 */

                /* Begin: Modified by zxiaona for call_detail 2012/04/10 */
                String flag = mContactsName.getText().toString();
                final int length = flag.length();
                if (length > 0)
                {
                    mBottomLayout.setVisibility(View.GONE);
                    findViewById(R.id.unknown_number).setVisibility(View.GONE);
                    findViewById(R.id.home_location).setVisibility(View.GONE);
                    mPhoneNumber.setVisibility(View.VISIBLE);
                    if(LocSearchUtil.locSearch==null) {
                        LocSearchUtil.locSearch = new HashMap();
                    } else {
                         mHomeLocation2.setText(LocSearchUtil.locSearch.get(mNumber));
                     }
                    mMainActionPushLayerView.setVisibility(View.GONE);
                    mContactBackgroundView.setVisibility(View.GONE);
                    /*
                     * Begin: Modified by zxiaona for call_detail_joincontacts
                     * 2012/04/17
                     */
                    long photoId = 0;
                    final String photoIdColumn = Contacts.PHOTO_ID;
                    ContentResolver resolver = getContentResolver();
                    Cursor cur = resolver.query(contactUri, null, photoIdColumn, null, null);
                    if (cur != null && cur.moveToFirst())
                    {
                        photoId = Long.parseLong(cur.getString(cur.getColumnIndex(photoIdColumn)));
                    }
                    /*
                     * End: Modified by zxiaona for call_detail_joincontacts
                     * 2012/04/17
                     */
                    /* Begin: Modified by zxiaona for call_detail_star 2012/04/11 */
                    final String starred = Contacts.STARRED;
                    ContentResolver cr = getContentResolver();
                    Cursor c = cr.query(contactUri, null, starred, null, null);
                    if (c != null && c.moveToFirst())
                    {
                        int values = Integer.parseInt(c.getString(c.getColumnIndex(starred)));
                        if (values == 1)
                        {
                            findViewById(R.id.contacts_add_icon).setVisibility(View.VISIBLE);
                        }
                    }

                    findViewById(R.id.contacts_infromation).setOnClickListener(
                            new View.OnClickListener() {

                                @Override
                                public void onClick(View view) {
                                    // TODO Auto-generated method stub
                                    Intent intent = new Intent(CallDetailActivity.this,
                                            ContactDetailActivity.class);
                                    intent.setData(contactUri);
                                    startActivity(intent);
                                }
                            });

                    /* End: Modified by zxiaona for call_detail 2012/04/11 */

                    /* Begin: Modified by zxiaona for call_detail_image 2012/04/17 */
                    quickContactView = (QuickContactBadge) findViewById(R.id.quick_contact_photo);
                    quickContactView.setVisibility(View.VISIBLE);
                    mContactPhotoManager.loadPhoto(quickContactView, photoId, false, true);
                    quickContactView.assignContactUri(contactUri);
                    /* End: Modified by zxiaona for call_detail_image 2012/04/17 */
                }
                else
                {
                    mBottomLayout.setVisibility(View.VISIBLE);
                    mPhoneNumber.setVisibility(View.GONE);
                    mUnknownNumber = (TextView) findViewById(R.id.unknown_number);
                    mHomeLocation = (TextView) findViewById(R.id.home_location);
                    mUnknownNumber.setText(mNumber);
                    if(LocSearchUtil.locSearch==null)
                        LocSearchUtil.locSearch = new HashMap();
                       mHomeLocation.setText(LocSearchUtil.locSearch.get(mNumber));
                    mUnknownNumber.setVisibility(View.VISIBLE);
                    mHomeLocation.setVisibility(View.VISIBLE);
                    mBottomAdd.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // TODO Auto-generated method stub
                            final Intent mainActionIntent;
                            mainActionIntent = new Intent(CallDetailActivity.this,
                                    ContactEditorActivity.class);
                            mainActionIntent.setAction(Intent.ACTION_INSERT);
                            mainActionIntent.setType(Contacts.CONTENT_ITEM_TYPE);
                            mainActionIntent.putExtra(Insert.PHONE, mNumber);
                            startActivity(mainActionIntent);

                        }
                    });
                    mBottonRefresh.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // TODO Auto-generated method stub
                            /*
                             * Begin: Modified by zxiaona for
                             * call_detail_joincontacts 2012/04/16
                             */
                            String[] contactId = {
                                    Contacts._ID
                            };
                            long value = 0;
                            Cursor cs = getContentResolver().query(Contacts.CONTENT_URI, contactId,
                                    null, null, null);
                            if (cs != null && cs.moveToLast())
                            {
                                value = Long.parseLong(cs.getString(cs.getColumnIndex(Contacts._ID)));
                            }
                            value = value + 1;
                            Log.i("********", "values = " + value);
                            Intent it = new Intent(JoinContactActivity.JOIN_CONTACT);
                            it.putExtra(EXTRA_TARGET_CONTACT_ID, value);
                            it.putExtra("number", mNumber);
                            startActivity(it);
                            /*
                             * End: Modified by zxiaona for
                             * call_detail_joincontacts 2012/04/16
                             */
                        }

                    });
                    mContactBackgroundView.setVisibility(View.GONE);
                    mMainActionPushLayerView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CreateImageDialog();
                        }
                    });
                }
                /* End: Modified by zxiaona for call_detail 2012/04/10 */

                // Cache the details about the phone number.
                final Uri numberCallUri = mPhoneNumberHelper.getCallUri(mNumber);
                final boolean canPlaceCallsTo = mPhoneNumberHelper.canPlaceCallsTo(mNumber);
                final boolean isVoicemailNumber = mPhoneNumberHelper.isVoicemailNumber(mNumber);
                final boolean isSipNumber = mPhoneNumberHelper.isSipNumber(mNumber);

                // Let user view contact details if they exist, otherwise add option to create new
                // contact from this number.
                final Intent mainActionIntent;
                final int mainActionIcon;
                final String mainActionDescription;

                final CharSequence nameOrNumber;
                if (!TextUtils.isEmpty(firstDetails.name)) {
                    nameOrNumber = firstDetails.name;
                } else {
                    nameOrNumber = firstDetails.number;
                }

                if (contactUri != null) {
                    mainActionIntent = new Intent(Intent.ACTION_VIEW, contactUri);
                    /* Begin: Modified by zxiaona for call_detail 2012/03/28 */
                    // mainActionIcon = R.drawable.ic_contacts_holo_dark;
                    // mainActionDescription =
                    // getString(R.string.description_view_contact,
                    // nameOrNumber);
                    mainActionIcon = 0;
                    mainActionDescription =
                            getString(R.string.description_view_contact, nameOrNumber);
                    /* End: Modified by zxiaona for call_detail 2012/03/28 */

                } else if (isVoicemailNumber) {
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                } else if (isSipNumber) {
                    // TODO: This item is currently disabled for SIP addresses, because
                    // the Insert.PHONE extra only works correctly for PSTN numbers.
                    //
                    // To fix this for SIP addresses, we need to:
                    // - define ContactsContract.Intents.Insert.SIP_ADDRESS, and use it here if
                    //   the current number is a SIP address
                    // - update the contacts UI code to handle Insert.SIP_ADDRESS by
                    //   updating the SipAddress field
                    // and then we can remove the "!isSipNumber" check above.
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                } else if (canPlaceCallsTo) {
                    mainActionIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    mainActionIntent.setType(Contacts.CONTENT_ITEM_TYPE);
                    mainActionIntent.putExtra(Insert.PHONE, mNumber);
                    mainActionIcon = R.drawable.ic_add_contact_holo_dark;
                    mainActionDescription = getString(R.string.description_add_contact);
                } else {
                    // If we cannot call the number, when we probably cannot add it as a contact either.
                    // This is usually the case of private, unknown, or payphone numbers.
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                }

                if (mainActionIntent == null) {
                    mMainActionView.setVisibility(View.INVISIBLE);
                    mMainActionPushLayerView.setVisibility(View.GONE);
                    mHeaderTextView.setVisibility(View.INVISIBLE);
                    /* Begin: Modified by zxiaona for call_detail 2012/03/29 */
                    // mHeaderOverlayView.setVisibility(View.INVISIBLE);
                    mHeaderOverlayView.setVisibility(View.GONE);
                    /* End: Modified by zxiaona for call_detail 2012/03/29 */

                } else {
                    mMainActionView.setVisibility(View.VISIBLE);
                    mMainActionView.setImageResource(mainActionIcon);
                    mMainActionPushLayerView.setVisibility(View.VISIBLE);
                    mMainActionPushLayerView.setContentDescription(mainActionDescription);
                    mHeaderTextView.setVisibility(View.VISIBLE);
                    /* Begin: Modified by zxiaona for call_detail 2012/03/29 */
                    // mHeaderOverlayView.setVisibility(View.VISIBLE);
                    mHeaderOverlayView.setVisibility(View.GONE);
                    /* End: Modified by zxiaona for call_detail 2012/03/29 */
                }

                // This action allows to call the number that places the call.
                if (canPlaceCallsTo) {
                    final CharSequence displayNumber =
                            mPhoneNumberHelper.getDisplayNumber(
                                    firstDetails.number, firstDetails.formattedNumber);

                    Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberCallUri);
                    if (sub != -1)
                        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, sub);
                    Log.d(TAG, "call log sub is " + sub);
                    ViewEntry entry = new ViewEntry(
                            getString(R.string.menu_callNumber, displayNumber),
                            intent,
                            getString(R.string.description_call, nameOrNumber));

                    // Only show a label if the number is shown and it is not a SIP address.
                    if (!TextUtils.isEmpty(firstDetails.name)
                            && !TextUtils.isEmpty(firstDetails.number)
                            && !PhoneNumberUtils.isUriNumber(firstDetails.number.toString())) {
                        entry.label = Phone.getTypeLabel(mResources, firstDetails.numberType,
                                firstDetails.numberLabel);
                    }

                    // The secondary action allows to send an SMS to the number that placed the
                    // call.
                    if (mPhoneNumberHelper.canSendSmsTo(mNumber)) {
                        entry.setSecondaryAction(
                                R.drawable.ic_text_holo_dark,
                                new Intent(Intent.ACTION_SENDTO,
                                           Uri.fromParts("sms", mNumber, null)),
                                getString(R.string.description_send_text_message, nameOrNumber));
                    }

                    configureCallButton(entry);
                } else {
                    disableCallButton();
                }

                mHasEditNumberBeforeCallOption =
                        canPlaceCallsTo && !isSipNumber && !isVoicemailNumber;
                mHasTrashOption = hasVoicemail();
                mHasRemoveFromCallLogOption = !hasVoicemail();
                invalidateOptionsMenu();

                ListView historyList = (ListView) findViewById(R.id.history);
                historyList.setAdapter(
                        new CallDetailHistoryAdapter(CallDetailActivity.this, mInflater,
                                mCallTypeHelper, details, hasVoicemail(), canPlaceCallsTo,
                                findViewById(R.id.controls)));
                BackScrollManager.bind(
                        new ScrollableHeader() {
                            private View mControls = findViewById(R.id.controls);
                            /*
                             * Begin: Modified by zxiaona for call_detail
                             * 2012/04/09
                             */
                            // private View mPhoto =
                            // findViewById(R.id.contact_background_sizer);
                            /*
                             * End: Modified by zxiaona for call_detail
                             * 2012/04/09
                             */
                            private View mHeader = findViewById(R.id.photo_text_bar);
                            private View mSeparator = findViewById(R.id.blue_separator);

                            @Override
                            public void setOffset(int offset) {
                                mControls.setY(-offset);
                            }

                            @Override
                            public int getMaximumScrollableHeaderOffset() {
                                // We can scroll the photo out, but we should keep the header if
                                // present.
                                /*
                                 * Begin: Modified by zxiaona for call_detail
                                 * 2012/04/09
                                 */
                                // if (mHeader.getVisibility() == View.VISIBLE)
                                // {
                                // return mPhoto.getHeight() -
                                // mHeader.getHeight();
                                // } else {
                                // //If the header is not present, we should
                                // also scroll out the
                                // // separator line.
                                // return mPhoto.getHeight() +
                                // mSeparator.getHeight();
                                // }
                                return 0;
                            }
                            /*
                             * End: Modified by zxiaona for call_detail
                             * 2012/04/09
                             */
                        },
                        historyList);
                loadContactPhotos(photoUri);
                findViewById(R.id.call_detail).setVisibility(View.VISIBLE);
            }
        }
        mAsyncTaskExecutor.submit(Tasks.UPDATE_PHONE_CALL_DETAILS, new UpdateContactDetailsTask());
    }

    /** Return the phone call details for a given call log URI. */
    private PhoneCallDetails getPhoneCallDetailsForUri(Uri callUri) {
        ContentResolver resolver = getContentResolver();
        Cursor callCursor = resolver.query(callUri, CALL_LOG_PROJECTION, null, null, null);
        try {
            if (callCursor == null || !callCursor.moveToFirst()) {
                throw new IllegalArgumentException("Cannot find content: " + callUri);
            }

            // Read call log specifics.
            String number = callCursor.getString(NUMBER_COLUMN_INDEX);
            long date = callCursor.getLong(DATE_COLUMN_INDEX);
            long duration = callCursor.getLong(DURATION_COLUMN_INDEX);
            int callType = callCursor.getInt(CALL_TYPE_COLUMN_INDEX);
            String countryIso = callCursor.getString(COUNTRY_ISO_COLUMN_INDEX);
            final String geocode = callCursor.getString(GEOCODED_LOCATION_COLUMN_INDEX);
            final int subscription = callCursor.getInt(SUBSCRIPTION);

            if (TextUtils.isEmpty(countryIso)) {
                countryIso = mDefaultCountryIso;
            }

            // Formatted phone number.
            final CharSequence formattedNumber;
            // Read contact specifics.
            final CharSequence nameText;
            final int numberType;
            final CharSequence numberLabel;
            final Uri photoUri;
            final Uri lookupUri;
            // If this is not a regular number, there is no point in looking it up in the contacts.
            ContactInfo info =
                    mPhoneNumberHelper.canPlaceCallsTo(number)
                    && !mPhoneNumberHelper.isVoicemailNumber(number)
                            ? mContactInfoHelper.lookupNumber(number, countryIso)
                            : null;
            if (info == null) {
                formattedNumber = mPhoneNumberHelper.getDisplayNumber(number, null);
                nameText = "";
                numberType = 0;
                numberLabel = "";
                photoUri = null;
                lookupUri = null;
            } else {
                formattedNumber = info.formattedNumber;
                nameText = info.name;
                numberType = info.type;
                numberLabel = info.label;
                photoUri = info.photoUri;
                lookupUri = info.lookupUri;
            }
            return new PhoneCallDetails(number, formattedNumber, countryIso, geocode,
                    new int[]{ callType }, date, duration, subscription,
                    nameText, numberType, numberLabel, lookupUri, photoUri);
        } finally {
            if (callCursor != null) {
                callCursor.close();
            }
        }
    }

    /** Load the contact photos and places them in the corresponding views. */
    private void loadContactPhotos(Uri photoUri) {
        /*Begin: Modified by zxiaona for image_bug 2012/06/05*/
        //mContactPhotoManager.loadPhoto(mContactBackgroundView, photoUri, true, true);
        mContactPhotoManager.loadPhoto(mMainActionPushLayerView, photoUri, true, true);
        /*End: Modified by zxiaona for image_bug 2012/06/05*/

    }

    static final class ViewEntry {
        public final String text;
        public final Intent primaryIntent;
        /** The description for accessibility of the primary action. */
        public final String primaryDescription;

        public CharSequence label = null;
        /** Icon for the secondary action. */
        public int secondaryIcon = 0;
        /** Intent for the secondary action. If not null, an icon must be defined. */
        public Intent secondaryIntent = null;
        /** The description for accessibility of the secondary action. */
        public String secondaryDescription = null;

        public ViewEntry(String text, Intent intent, String description) {
            this.text = text;
            primaryIntent = intent;
            primaryDescription = description;
        }

        public void setSecondaryAction(int icon, Intent intent, String description) {
            secondaryIcon = icon;
            secondaryIntent = intent;
            secondaryDescription = description;
        }
    }

    /** Disables the call button area, e.g., for private numbers. */
    private void disableCallButton() {
        findViewById(R.id.call_and_sms).setVisibility(View.GONE);
    }

    /** Configures the call button area using the given entry. */
    private void configureCallButton(ViewEntry entry) {
        View convertView = findViewById(R.id.call_and_sms);
        convertView.setVisibility(View.VISIBLE);

        ImageView icon = (ImageView) convertView.findViewById(R.id.call_and_sms_icon);
        View divider = convertView.findViewById(R.id.call_and_sms_divider);
        TextView text = (TextView) convertView.findViewById(R.id.call_and_sms_text);

        View mainAction = convertView.findViewById(R.id.call_and_sms_main_action);
        mainAction.setOnClickListener(mPrimaryActionListener);
        mainAction.setTag(entry);
        mainAction.setContentDescription(entry.primaryDescription);

        if (entry.secondaryIntent != null) {
            icon.setOnClickListener(mSecondaryActionListener);
            icon.setImageResource(entry.secondaryIcon);
            icon.setVisibility(View.VISIBLE);
            icon.setTag(entry);
            icon.setContentDescription(entry.secondaryDescription);
            divider.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }
        text.setText(entry.text);

        /* Begin: Modified by zxiaona for call_detail 2012/03/28 */
        mPhoneNumber.setText(entry.text);
        /* End: Modified by zxiaona for call_detail 2012/03/28 */

        TextView label = (TextView) convertView.findViewById(R.id.call_and_sms_label);
        if (TextUtils.isEmpty(entry.label)) {
            label.setVisibility(View.GONE);
        } else {
            label.setText(entry.label);
            label.setVisibility(View.VISIBLE);
        }
    }

    protected void updateVoicemailStatusMessage(Cursor statusCursor) {
        if (statusCursor == null) {
            mStatusMessageView.setVisibility(View.GONE);
            return;
        }
        final StatusMessage message = getStatusMessage(statusCursor);
        if (message == null || !message.showInCallDetails()) {
            mStatusMessageView.setVisibility(View.GONE);
            return;
        }

        mStatusMessageView.setVisibility(View.VISIBLE);
        mStatusMessageText.setText(message.callDetailsMessageId);
        if (message.actionMessageId != -1) {
            mStatusMessageAction.setText(message.actionMessageId);
        }
        if (message.actionUri != null) {
            mStatusMessageAction.setClickable(true);
            mStatusMessageAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW, message.actionUri));
                }
            });
        } else {
            mStatusMessageAction.setClickable(false);
        }
    }

    private StatusMessage getStatusMessage(Cursor statusCursor) {
        List<StatusMessage> messages = mVoicemailStatusHelper.getStatusMessages(statusCursor);
        if (messages.size() == 0) {
            return null;
        }
        // There can only be a single status message per source package, so num of messages can
        // at most be 1.
        if (messages.size() > 1) {
            Log.w(TAG, String.format("Expected 1, found (%d) num of status messages." +
                    " Will use the first one.", messages.size()));
        }
        return messages.get(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.call_details_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // This action deletes all elements in the group from the call log.
        // We don't have this action for voicemails, because you can just use the trash button.
        menu.findItem(R.id.menu_remove_from_call_log).setVisible(mHasRemoveFromCallLogOption);
        menu.findItem(R.id.menu_edit_number_before_call).setVisible(mHasEditNumberBeforeCallOption);
        menu.findItem(R.id.menu_trash).setVisible(mHasTrashOption);
        /* Begin: Modified by sunrise for SendSms 2012/05/19 */
        menu.findItem(R.id.menu_sendsms_from_call_log).setVisible(mNumber == null ? false : true);
        /* End: Modified by sunrise for SendSms 2012/05/19 */

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onHomeSelected();
                return true;
            }

            // All the options menu items are handled by onMenu... methods.
            default:
                throw new IllegalArgumentException();
        }
    }

    /*Begin: Modified by sunrise for SendSms 2012/05/19*/
    public void onMenuSendSmsFromCallLog(MenuItem menuItem)
    {
        if (mPhoneNumberHelper.canSendSmsTo(mNumber))
        {
            Intent MenuToSms = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "sms", mNumber, null));
            CallDetailActivity.this.startActivity(MenuToSms);
        }
    }
    /*End: Modified by sunrise for SendSms 2012/05/19*/

    public void onMenuRemoveFromCallLog(MenuItem menuItem) {
        final StringBuilder callIds = new StringBuilder();
        for (Uri callUri : getCallLogEntryUris()) {
            if (callIds.length() != 0) {
                callIds.append(",");
            }
            callIds.append(ContentUris.parseId(callUri));
        }
        mAsyncTaskExecutor.submit(Tasks.REMOVE_FROM_CALL_LOG_AND_FINISH,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        getContentResolver().delete(Calls.CONTENT_URI_WITH_VOICEMAIL,
                                Calls._ID + " IN (" + callIds + ")", null);
                        return null;
                    }

                    @Override
                    public void onPostExecute(Void result) {
                        finish();
                    }
                });
    }

    public void onMenuEditNumberBeforeCall(MenuItem menuItem) {
        startActivity(new Intent(Intent.ACTION_DIAL, mPhoneNumberHelper.getCallUri(mNumber)));
    }

    public void onMenuTrashVoicemail(MenuItem menuItem) {
        final Uri voicemailUri = getVoicemailUri();
        mAsyncTaskExecutor.submit(Tasks.DELETE_VOICEMAIL_AND_FINISH,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        getContentResolver().delete(voicemailUri, null, null);
                        return null;
                    }
                    @Override
                    public void onPostExecute(Void result) {
                        finish();
                    }
                });
    }

    private void configureActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            /*Begin: Modified by zxiaona for actionBar text 2012/06/07*/
            //actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.callDetailTitle);
            /*End: Modified by zxiaona for actionBar text 2012/06/07*/
           /* Begin: Modified by zxiaona for actionBar image 2012/06/06 */
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_actionbar_bg));
           /* End: Modified by zxiaona for actionBar image 2012/06/06 */

        }
    }

    /** Invoked when the user presses the home button in the action bar. */
    private void onHomeSelected() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Calls.CONTENT_URI);
        // This will open the call log even if the detail view has been opened directly.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        // Immediately stop the proximity sensor.
        disableProximitySensor(false);
        mProximitySensorListener.clearPendingRequests();
        super.onPause();
    }

    @Override
    public void enableProximitySensor() {
        mProximitySensorManager.enable();
    }

    @Override
    public void disableProximitySensor(boolean waitForFarState) {
        mProximitySensorManager.disable(waitForFarState);
    }

    // /*Begin: Modified by zxiaona for call_detail 2012/03/29*/
    // private String splitAndTrimPhoneNumbers(final String phoneNumber) {
    // final List<String> phoneList = new ArrayList<String>();
    //
    // StringBuilder builder = new StringBuilder();
    // final int length = phoneNumber.length();
    // for (int i = 0; i < length; i++) {
    // final char ch = phoneNumber.charAt(i);
    // // TODO: add a test case for string with '+', and care the other possible
    // issues
    // // which may happen by ignoring non-digits other than '+'.
    // if (Character.isDigit(ch) || (ch == '*') || (ch == '#') || (ch == '+')||
    // (ch == ';') || (ch == ',')) {
    // builder.append(ch);
    // }else if ((ch == '\n') && builder.length() > 0) {
    // phoneList.add(builder.toString());
    // }
    // }
    // if (builder.length() > 0) {
    // phoneList.add(builder.toString());
    // }
    // String phonenumber = phoneList.toString();
    // String onlynumber = phonenumber.trim();
    // return onlynumber;
    // }
    // /*End: Modified by zxiaona for call_detail 2012/03/29*/
    /* Begin: Modified by zxiaona for imageButton 2012/04/05 */
    private void CreateImageDialog() {
        /* Begin: Modified by zxiaona for call_detail_for_ipcall 2012/04/24 */
        // final Context dialogContext = new ContextThemeWrapper(this,
        // android.R.style.Theme_Light);
        // final Resources res = dialogContext.getResources();
        // final LayoutInflater dialogInflater = (LayoutInflater)dialogContext
        // .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // final ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,
        // android.R.layout.simple_list_item_1) {
        // @Override
        // public View getView(int position, View convertView, ViewGroup parent)
        // {
        // if (convertView == null) {
        // convertView =
        // dialogInflater.inflate(android.R.layout.simple_list_item_1,
        // parent, false);
        // }
        //
        // final int resId = this.getItem(position);
        // ((TextView)convertView).setText(resId);
        // return convertView;
        // }
        // };
        // adapter.add(R.string.bottom_button_addcontact);
        // adapter.add(R.string.bottom_button_refreshcontact);

        CharSequence[] items = {
                getString(R.string.bottom_button_addcontact),
                getString(R.string.bottom_button_refreshcontact)
        };

        /* End: Modified by zxiaona for call_detail_for_ipcall 2012/04/24 */
        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        dialog.dismiss();
                        if (which == 0) {
                            final Intent mainActionIntent;
                            // mainActionIntent = new
                            // Intent(Intent.ACTION_INSERT_OR_EDIT);
                            mainActionIntent = new Intent(CallDetailActivity.this,
                                    ContactEditorActivity.class);
                            mainActionIntent.setAction(Intent.ACTION_INSERT);
                            mainActionIntent.setType(Contacts.CONTENT_ITEM_TYPE);
                            mainActionIntent.putExtra(Insert.PHONE, mNumber);
                            startActivity(mainActionIntent);
                        } else if (which == 1) {

                            /*
                             * Begin: Modified by zxiaona for
                             * call_detail_joincontacts 2012/04/21
                             */
                            // Intent it = new Intent(CallDetailActivity.this,
                            // PeopleActivity.class);
                            String[] contactId = {
                                    Contacts._ID
                            };
                            long value = 0;
                            Cursor cs = getContentResolver().query(Contacts.CONTENT_URI, contactId,
                                    null, null, null);
                            if (cs != null && cs.moveToLast())
                            {
                                value = Long
                                        .parseLong(cs.getString(cs.getColumnIndex(Contacts._ID)));
                            }
                            value = value + 1;
                            Log.i("********", "values = " + value);
                            Intent it = new Intent(JoinContactActivity.JOIN_CONTACT);
                            it.putExtra(EXTRA_TARGET_CONTACT_ID, value);
                            it.putExtra("number", mNumber);
                            startActivity(it);
                            /*
                             * End: Modified by zxiaona for
                             * call_detail_joincontacts 2012/04/21
                             */
                        } else {
                            Log.d(TAG, "***other****");
                        }
                    }

                };
        new AlertDialog.Builder(this)
                .setTitle(R.string.bottom_button_addcontact)
                .setNegativeButton(android.R.string.cancel, null)
                .setItems(items, clickListener)
                .show();
    }
    /* End: Modified by zxiaona for imageButton 2012/04/05 */

    /* Begin: Modified by zxiaona for CG dial 2012/06/13 */
    public void dialButtonPressed(String mPhonenumber, int slot) {
        final Intent intent = newDialNumberIntent(mPhonenumber);
        intent.putExtra(EXTRA_CALL_ORIGIN,
                CALL_ORIGIN_DIALTACTS);
        intent.putExtra("subscription", slot);
        intent.putExtra("directDial", true);
        startActivity(intent);
        finish();
    }
    /* End: Modified by zxiaona for CG dial 2012/06/13 */

}
