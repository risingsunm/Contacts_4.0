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

import com.android.common.io.MoreCloseables;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.activities.DialtactsActivity.ViewPagerVisibilityListener;
import com.android.contacts.util.EmptyLoader;
import com.android.contacts.voicemail.VoicemailStatusHelper;
import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.ITelephony;
import com.google.common.annotations.VisibleForTesting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

/*Start of wangqiang on 2012-3-26 10:16 CallLog_longClick*/
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import com.android.contacts.list.ContactListAdapter;
import com.android.contacts.list.OnContactBrowserActionListener;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.widget.ContextMenuAdapter;
import com.android.contacts.widget.TouchListView;
import com.android.contacts.widget.TouchListView.TriggerListener;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.Settings.SettingNotFoundException;
import android.view.ContextMenu;
import android.view.View.OnCreateContextMenuListener;
import android.provider.Settings;
import com.android.internal.telephony.SubscriptionManager;
/*End of wangqiang on 2012-3-26 10:16 CallLog_longClick*/
/*Begin: Modified by siliangqi for multi_delete 2012-3-23*/
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.app.ActionBar;
import android.view.LayoutInflater;
import android.widget.Button;
import android.view.View.OnClickListener;
import com.android.internal.view.menu.ActionMenuItem;
/*End: Modified by siliangqi for multi_delete 2012-3-23*/
/*Begin: Modified by siliangqi for location_search 2012-4-28*/
import com.android.contacts.LocSearchUtil;
import java.util.HashMap;
/*End: Modified by siliangqi for location_search 2012-4-28*/
import java.util.List;

/*Begin: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15*/
import android.content.ContentValues;
import android.widget.Toast;
/*End: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15*/

/**
 * Displays a list of call log entries.
 */
public class CallLogFragment extends ListFragment implements ViewPagerVisibilityListener,
        CallLogQueryHandler.Listener, CallLogAdapter.CallFetcher {
    private static final String TAG = "CallLogFragment";

    /**
     * ID of the empty loader to defer other fragments.
     */
    private static final int EMPTY_LOADER_ID = 0;

    private CallLogAdapter mAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    private boolean mScrollToTop;

    private boolean mShowOptionsMenu;
    /** Whether there is at least one voicemail source installed. */
    private boolean mVoicemailSourcesAvailable = false;
    /** Whether we are currently filtering over voicemail. */
    private boolean mShowingVoicemailOnly = false;

    private VoicemailStatusHelper mVoicemailStatusHelper;
    private View mStatusMessageView;
    private TextView mStatusMessageText;
    private TextView mStatusMessageAction;
    private KeyguardManager mKeyguardManager;
    /* Begin: Modified by siliangqi for multi_delete 2012-3-23 */
    private LinearLayout linear_delete_all;
    private LinearLayout linear_del_conf;
    private Button btn_ok, btn_cancel;
    public static boolean delete_flag = false;
    private static OnMenuDeleteClickListener mOnMenuDeleteClickListener;
    private boolean enterDeleteUi = false;
    private CheckBox myCb;
    private LinearLayout actionbar_layout;
    private LinearLayout actionbar_button;
    /* End: Modified by siliangqi for multi_delete 2012-3-23 */

    private boolean mEmptyLoaderRunning;
    private boolean mCallLogFetched;
    private boolean mVoicemailStatusFetched;

    private RadioButton allCallTypeBut;
    private RadioButton inCallTypeBut;
    private RadioButton outCallTypeBut;
    private RadioButton missCallTypeBut;

    private ImageView slotList;
    private ImageView slotSelect;
    /*Start of wangqiang on 2012-3-26 10:16 CallLog_longClick*/
    private String mVoiceMailNumber;
    private static final int MENU_ITEM_CALL = 1;
    private static final int MENU_ITEM_EDIT_BEFORE_CALL=2;
    private static final int MENU_ITEM_SEND_SMS = 3;
    private static final int MENU_ITEM_TOGGLE_CONTACTS=4;
    private static final int MENU_ITEM_DELETE = 5;
    /*End of wangqiang on 2012-3-26 10:16 CallLog_longClick*/
    /*Begin: Modified by siliangqi for IP_Dial 2012-4-23*/
    private static final int MENU_ITEM_IP_CALL = 6;
    /*End: Modified by siliangqi for IP_Dial 2012-4-23*/

    /* Begin: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15 */
    private static final int MENU_ITEM_ADD_BLACK = 7;
    private static final int MENU_ITEM_DEL_BLACK = 8;
    /* End: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15 */

    private static final int TYPE_INDEX_ALL = 0;

    private OnClickListener callTypeListener = new OnClickListener(){
        @Override
        public void onClick(View v) {
            int callType = TYPE_INDEX_ALL;
            switch(v.getId()){
                case R.id.call_in:
                    callType = Calls.INCOMING_TYPE;
                    break;
                case R.id.call_out:
                    callType = Calls.OUTGOING_TYPE;
                    break;
                case R.id.call_miss:
                    callType = Calls.MISSED_TYPE;
                    break;
                default:
                    callType = TYPE_INDEX_ALL;
                    break;
            }
            mCallLogQueryHandler.setCallType(callType);
            refreshData();
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        /*Start of wangqiang on 2012-3-26 10:18 CallLog_longClick*/
        mVoiceMailNumber = ((TelephonyManager) getActivity().getSystemService(
                Context.TELEPHONY_SERVICE)).getVoiceMailNumber();
        /*End of wangqiang on 2012-3-26 10:18 CallLog_longClick*/
        mCallLogQueryHandler = new CallLogQueryHandler(getActivity().getContentResolver(), this);
        mKeyguardManager =
                (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        setHasOptionsMenu(true);
    }

    /** Called by the CallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public void onCallsFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        mAdapter.setLoading(false);
        mAdapter.changeCursor(cursor);
        /*Begin: Modified by sunrise for CalllogGroupByDate 2012/05/19*/
        mAdapter.resetCallLogDispGroupByDate();
        /*End: Modified by sunrise for CalllogGroupByDate 2012/05/19*/

        // This will update the state of the "Clear call log" menu item.
        getActivity().invalidateOptionsMenu();
        if (mScrollToTop) {
            final ListView listView = getListView();
            if (listView.getFirstVisiblePosition() > 5) {
                listView.setSelection(5);
            }
            listView.smoothScrollToPosition(0);
            mScrollToTop = false;
        }
        mCallLogFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
    }

    /**
     * Called by {@link CallLogQueryHandler} after a successful query to voicemail status provider.
     */
    @Override
    public void onVoicemailStatusFetched(Cursor statusCursor) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        updateVoicemailStatusMessage(statusCursor);

        int activeSources = mVoicemailStatusHelper.getNumberActivityVoicemailSources(statusCursor);
        setVoicemailSourcesAvailable(activeSources != 0);
        MoreCloseables.closeQuietly(statusCursor);
        mVoicemailStatusFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
    }

    private void destroyEmptyLoaderIfAllDataFetched() {
        if (mCallLogFetched && mVoicemailStatusFetched && mEmptyLoaderRunning) {
            mEmptyLoaderRunning = false;
            getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
        }
    }

    /** Sets whether there are any voicemail sources available in the platform. */
    private void setVoicemailSourcesAvailable(boolean voicemailSourcesAvailable) {
        if (mVoicemailSourcesAvailable == voicemailSourcesAvailable) return;
        mVoicemailSourcesAvailable = voicemailSourcesAvailable;

        Activity activity = getActivity();
        if (activity != null) {
            // This is so that the options menu content is updated.
            activity.invalidateOptionsMenu();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.call_log_fragment, container, false);
        mVoicemailStatusHelper = new VoicemailStatusHelperImpl();
        mStatusMessageView = view.findViewById(R.id.voicemail_status);
        mStatusMessageText = (TextView) view.findViewById(R.id.voicemail_status_message);
        mStatusMessageAction = (TextView) view.findViewById(R.id.voicemail_status_action);

        allCallTypeBut = (RadioButton) view.findViewById(R.id.call_all);
        inCallTypeBut = (RadioButton) view.findViewById(R.id.call_in);
        outCallTypeBut = (RadioButton) view.findViewById(R.id.call_out);
        missCallTypeBut = (RadioButton) view.findViewById(R.id.call_miss);
        allCallTypeBut.setOnClickListener(callTypeListener);
        inCallTypeBut.setOnClickListener(callTypeListener);
        outCallTypeBut.setOnClickListener(callTypeListener);
        missCallTypeBut.setOnClickListener(callTypeListener);

        slotList = (ImageView) view.findViewById(R.id.slot_list);
        slotSelect = (ImageView) view.findViewById(R.id.slot_select);
        if (!TelephonyManager.getDefault().isMultiSimEnabled()) {
            view.findViewById(R.id.slot_select_container).setVisibility(View.GONE);
        }
        updateSubImage();

        slotList.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                showSlotChangeDialog();
            }
        });
        slotList.setOnTouchListener(new OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        slotSelect.setImageResource(R.drawable.ic_tab_sim_select_touch);
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        slotSelect.setImageResource(R.drawable.ic_tab_sim_select);
                        break;
                }
                return false;
            }});

        return view;
    }

    /*Begin: Added by bxinchun, add touch view style listview 2012/07/31*/
    private TriggerListener mTriggerListener = new TriggerListener() {
        public void onTrigger(int position, int actionType) {
            String number = null;

            try {
                number = ((Cursor) mAdapter.getItem(position)).getString(CallLogQuery.NUMBER);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            if (TextUtils.isEmpty(number)) return;
            System.out.println("number is:"+number);

            switch (actionType) {
                case LEFT: //sms
                    Intent smsIntent = new Intent(
                            Intent.ACTION_SENDTO, Uri.fromParts("sms", number, null));
                    startActivity(smsIntent);
                    break;
                case RIGHT: //call
                    Intent phoneIntent = new Intent(
                            Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel", number, null));
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
                    phoneIntent.putExtra("subscription", subscription);
                    phoneIntent.putExtra("directDial", true);
                    /*End: Modified by sliangqi for main_call 2012-8-25*/
                    startActivity(phoneIntent);
                    break;

                default:
                        break;
            }
        }
    };
    /*End: Added by bxinchun, add touch view style listview 2012/07/31*/
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
    protected void showSlotChangeDialog() {
        new AlertDialog.Builder(this.getActivity()).setSingleChoiceItems(
                new MultiSlotAdapter(this.getActivity()), 0, slotListener).setTitle(
                R.string.title_slot_change).create().show();
    }

    private DialogInterface.OnClickListener slotListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            int sub = -1;
            if (which >= TelephonyManager.getDefault().getPhoneCount())
                sub = -1;
            else
                sub = which;
            saveSlot(sub);
            updateSubImage();
            mCallLogQueryHandler.setSubscription(sub);
            refreshData();
        }
    };

    private void updateSubImage(){
        int sub = getSlot();
        switch(sub){
            case -1:
                slotList.setImageResource(R.drawable.ic_tab_sim12);
                break;
            case 0:
                slotList.setImageResource(R.drawable.ic_tab_sim1);
                break;
            case 1:
                slotList.setImageResource(R.drawable.ic_tab_sim2);
                break;
        }

    }

    private void saveSlot(int slot) {
        PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit().putInt("Subscription", slot).commit();
    }

    private int getSlot() {
        return PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getInt("Subscription", -1);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String currentCountryIso = ContactsUtils.getCurrentCountryIso(getActivity());
        mAdapter = new CallLogAdapter(getActivity(), this,
                new ContactInfoHelper(getActivity(), currentCountryIso));
        /* Begin: Modified by siliangqi for multi_delete 2012-3-23 */
        actionbar_layout = (LinearLayout) view.findViewById(R.id.actionbar_layout);
        actionbar_button = (LinearLayout) view.findViewById(R.id.actionbar_button);
        actionbar_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mOnMenuDeleteClickListener.onClick(false);
            }
        });
        final CheckBox cb = (CheckBox) view.findViewById(R.id.checkbox_delete_all);
        myCb = cb;
        cb.setClickable(false);
        linear_del_conf = (LinearLayout) view.findViewById(R.id.linear_del_conf);
        btn_ok = (Button) view.findViewById(R.id.btn_ok);
        final ActionMenuItem am = new ActionMenuItem(getActivity(), 0, android.R.id.home, 0, 0,
                "CallLog");
        btn_ok.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < getListView().getCount(); i++) {
                    Cursor cursor = (Cursor) mAdapter.getItem(i);
                    if (mAdapter.getdeleteCount().get((long) cursor.getInt(CallLogQuery.ID)) != null
                            && mAdapter.getdeleteCount().get((long) cursor.getInt(CallLogQuery.ID)) == true) {
                        int groupSize = 1;
                        if (mAdapter.isGroupHeader(i)) {
                            groupSize = mAdapter.getGroupSize(i);
                        }
                        for (int j = 0; j < groupSize; j++) {
                            if (!sb.toString().equals(""))
                                sb.append(",");
                            if (j != 0) {
                                cursor.moveToNext();
                            }
                            long id = cursor.getLong(CallLogQuery.ID);
                            sb.append(id);
                        }
                    }
                }
                getActivity().getContentResolver().delete(Calls.CONTENT_URI,
                        Calls._ID + " IN (" + sb + ")", null);
                /* Begin: Modified by siliangqi for location_search 2012-4-28 */
                getActivity().getContentResolver().delete(
                        Uri.parse("content://com.khong.provider.phonemanager/location/"
                                + sb.toString()), null, null);
                /* End: Modified by siliangqi for location_search 2012-4-28 */
                onOptionsItemSelected(am);
            }
        });
        btn_cancel = (Button) view.findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onOptionsItemSelected(am);
            }
        });
        linear_delete_all = (LinearLayout) view.findViewById(R.id.linear_delete_all);
        linear_delete_all.setClickable(true);
        linear_delete_all.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (cb.isChecked()) {
                    cb.setChecked(false);
                    mAdapter.setNonChecked();
                    setListAdapter(mAdapter);
                }
                else {
                    cb.setChecked(true);
                    mAdapter.setAllChecked();
                    setListAdapter(mAdapter);
                }
            }
        });
        /* End: Modified by siliangqi for multi_delete 2012-3-23 */
        setListAdapter(mAdapter);
        getListView().setItemsCanFocus(true);
        /* Begin: Modified by siliangqi for multi_delete 2012-3-28 */
        mAdapter.setOnDeleteListener(new CallLogAdapter.OnDeleteListener() {
            public void onClick() {
                if (cb.isChecked()) {
                    cb.setChecked(false);
                } else {
                    cb.setChecked(true);
                }
            }
        });
        if (enterDeleteUi)
            enterDeleteUi();
        if (delete_flag)
            updateSubImage();
        /* End: Modified by siliangqi for multi_delete 2012-3-28 */
        /*Start of wangqiang on 2012-3-26 10:17 CallLog_longClick*/
        getListView().setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        /*End of wangqiang on 2012-3-26 10:17 CallLog_longClick*/

        /*Begin: Modified by bxinchun, add touch view style listview 2012/07/31*/
        ListView mListView = getListView();
        if (mListView instanceof TouchListView) {
            TouchListView touchList = (TouchListView) mListView;
            touchList.setTouchMode(true);
            touchList.setTriggerListener(mTriggerListener);
        }
        /*End: Modified by bxinchun, add touch view style listview 2012/07/31*/
    }

    @Override
    public void onStart() {
        mScrollToTop = true;

        // Start the empty loader now to defer other fragments.  We destroy it when both calllog
        // and the voicemail status are fetched.
        getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
                new EmptyLoader.Callback(getActivity()));
        mEmptyLoaderRunning = true;
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallLogQueryHandler.setSubscription(getSlot());
        refreshData();
    }

    private void updateVoicemailStatusMessage(Cursor statusCursor) {
        List<StatusMessage> messages = mVoicemailStatusHelper.getStatusMessages(statusCursor);
        if (messages.size() == 0) {
            mStatusMessageView.setVisibility(View.GONE);
        } else {
            mStatusMessageView.setVisibility(View.VISIBLE);
            // TODO: Change the code to show all messages. For now just pick the first message.
            final StatusMessage message = messages.get(0);
            if (message.showInCallLog()) {
                mStatusMessageText.setText(message.callLogMessageId);
            }
            if (message.actionMessageId != -1) {
                mStatusMessageAction.setText(message.actionMessageId);
            }
            if (message.actionUri != null) {
                mStatusMessageAction.setVisibility(View.VISIBLE);
                mStatusMessageAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().startActivity(
                                new Intent(Intent.ACTION_VIEW, message.actionUri));
                    }
                });
            } else {
                mStatusMessageAction.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Kill the requests thread
        mAdapter.stopRequestProcessing();
    }

    @Override
    public void onStop() {
        super.onStop();
        updateOnExit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.stopRequestProcessing();
        mAdapter.changeCursor(null);
    }

    @Override
    public void fetchCalls() {
        if (mShowingVoicemailOnly) {
            mCallLogQueryHandler.fetchVoicemailOnly();
        } else {
            mCallLogQueryHandler.fetchAllCalls();
        }
    }

    /* Start of wangqiang on 2012-3-26 10:18 CallLog_longClick */
    private final OnCreateContextMenuListener mOnCreateContextMenuListener = new OnCreateContextMenuListener() {

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            // TODO Auto-generated method stub
            /* Begin: Modified by siliangqi for multi_delete 2012-3-30 */
            if (delete_flag)
                return;
            /* End: Modified by siliangqi for multi_delete 2012-3-30 */
            AdapterView.AdapterContextMenuInfo info;
            try {
                info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException e) {
                Log.wtf(TAG, "Bad menuInfo", e);
                return;
            }

            mAdapter = getAdapter();
            Cursor cursor = (Cursor) mAdapter.getItem(info.position);

            String number = cursor.getString(CallLogQuery.NUMBER);

            Log.v(TAG, number);
            String countryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
            ContactInfo mContactInfo = mAdapter.getContactInfo(mAdapter
                    .getNumberWithCountryIso(number, countryIso));

            boolean contactInfoPresent = (mContactInfo.name != null && mContactInfo != ContactInfo.EMPTY);
            // Setup the menu header

            if (contactInfoPresent) {
                menu.setHeaderTitle(mContactInfo.name);
                // Calling CallLog
                menu.add(
                        0,
                        MENU_ITEM_CALL,
                        0,
                        getResources().getString(
                                R.string.recentCalls_callNumber,
                                mContactInfo.name));
            } else {
                menu.setHeaderTitle(number);
                // Calling CallLog
                menu.add(
                        0,
                        MENU_ITEM_CALL,
                        0,
                        getResources().getString(
                                R.string.recentCalls_callNumber, number));

            }
            /*Begin: Modified by siliangqi for IP_Dial 2012-4-23*/
            menu.add(0,MENU_ITEM_IP_CALL,0,R.string.ipcall);
            /*End: Modified by siliangqi for IP_Dial 2012-4-23*/
            // Edit before call
            menu.add(0, MENU_ITEM_EDIT_BEFORE_CALL, 0,
                    R.string.recentCalls_editNumberBeforeCall);

            // Send SMS item
            menu.add(0, MENU_ITEM_SEND_SMS, 0, R.string.menu_sendTextMessage);

            if (contactInfoPresent) {
                // if the CallLog is called from the contacts View the contact
                menu.add(0, MENU_ITEM_TOGGLE_CONTACTS, 0,
                        R.string.menu_viewContact);
            } else {
                // if the CallLog is not called from the contacts add to this to
                // contacts
                menu.add(0, MENU_ITEM_TOGGLE_CONTACTS, 0,
                        R.string.recentCalls_addToContact);
            }

            /* Begin: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15 */
            System.out.println("sunrise add to black number:" + number);
            if (number != null)
            {
                Uri uriBlack = Uri
                        .parse("content://com.ahong.blackcall.AhongBlackCallProvider/black_number/"
                                + number);
                Cursor curBlack = getActivity().getContentResolver()
                        .query(uriBlack, null, null, null, null);

                if (curBlack != null && curBlack.getCount() > 0)
                {
                    menu.add(0, MENU_ITEM_DEL_BLACK, 0,
                            R.string.menu_delFromBlackList);
                }
                else
                {
                    menu.add(0, MENU_ITEM_ADD_BLACK, 0,
                            R.string.menu_addToBlackList);
                }
                curBlack.close();
            }
            /* End: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15 */

            // Delete CallLog
            menu.add(0, MENU_ITEM_DELETE, 0,
                    R.string.recentCalls_removeFromRecentList);
        }

    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.wtf(TAG, "Bad menuInfo", e);
            return false;
        }
        mAdapter = getAdapter();
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        String number = cursor.getString(CallLogQuery.NUMBER);

        Uri numberUri = null;
        boolean isVoicemail = false;
        boolean isSipNumber = false;
        if (number.equals(CallerInfo.UNKNOWN_NUMBER)) {
            number = getString(R.string.unknown);
        } else if (number.equals(CallerInfo.PRIVATE_NUMBER)) {
            number = getString(R.string.private_num);
        } else if (number.equals(CallerInfo.PAYPHONE_NUMBER)) {
            number = getString(R.string.payphone);
        } else if (PhoneNumberUtils.extractNetworkPortion(number).equals(
                mVoiceMailNumber)) {
            number = getString(R.string.voicemail);
            numberUri = Uri.parse("voicemail:x");
            isVoicemail = true;
        } else if (PhoneNumberUtils.isUriNumber(number)) {
            numberUri = Uri.fromParts("sip", number, null);
            isSipNumber = true;
        } else {
            numberUri = Uri.fromParts("tel", number, null);
        }

        String countryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
        ContactInfo mContactInfo = mAdapter.getContactInfo(mAdapter
                .getNumberWithCountryIso(number, countryIso));
        boolean contactInfoPresent = (mContactInfo.name != null && mContactInfo != ContactInfo.EMPTY);
        Uri lookupUri = mContactInfo.lookupUri;

        switch (item.getItemId()) {
        case MENU_ITEM_CALL: {
            if (numberUri != null) {
                Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                        numberUri);
                startActivity(intent);
                return true;
            }

        }

        case MENU_ITEM_TOGGLE_CONTACTS: {
            if (!contactInfoPresent && numberUri != null && !isVoicemail
                    && !isSipNumber) {
                Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                intent.setType(Contacts.CONTENT_ITEM_TYPE);
                intent.putExtra(Insert.PHONE, number);
                startActivity(intent);
                return true;
            } else if (contactInfoPresent && numberUri != null && !isVoicemail
                    && !isSipNumber) {
                Intent intent = new Intent(Intent.ACTION_VIEW, lookupUri);
                startActivity(intent);
                return true;
            }
        }

        case MENU_ITEM_EDIT_BEFORE_CALL: {
            if (numberUri != null && !isVoicemail && !isSipNumber) {
                Intent intent = new Intent(Intent.ACTION_DIAL, numberUri);
                startActivity(intent);
                return true;
            }

        }

        case MENU_ITEM_SEND_SMS: {
            if (numberUri != null && !isVoicemail && !isSipNumber) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "sms", number, null));
                Log.v(TAG, "wangqiang intent ****01");
                startActivity(intent);
                return true;
            }
        }

        case MENU_ITEM_DELETE: {

            int groupSize = 1;
            if (mAdapter.isGroupHeader(info.position)) {
                groupSize = mAdapter.getGroupSize(info.position);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < groupSize; i++) {
                if (i != 0) {
                    sb.append(",");
                    cursor.moveToNext();
                }
                long id = cursor.getLong(CallLogQuery.ID);
                sb.append(id);
            }
                /* Begin: Modified by siliangqi for location_search 2012/04/28 */
                final String loc_id = sb.toString();
                new Thread() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Cursor myCursor = getActivity().getContentResolver().query(
                                Calls.CONTENT_URI, null,
                                "_id=?", new String[] {
                                    loc_id
                                }, null);
                        Uri uri = Uri.parse("content://com.khong.provider.phonemanager/location/"
                                + loc_id);
                        getActivity().getContentResolver().delete(uri, null, null);
                    }

                }.start();
                /* End: Modified by siliangqi for location_search 2012/04/28 */

            getActivity().getContentResolver().delete(Calls.CONTENT_URI,
                    Calls._ID + " IN (" + sb + ")", null);
            return true;
        }
            /* Begin: Modified by siliangqi for IP_Dial 2012-4-23 */
            case MENU_ITEM_IP_CALL: {
                Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,Uri.fromParts("tel", number,null));
                intent .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("ipcall", true);
                startActivity(intent);
                return true;
            }
            /* End: Modified by siliangqi for IP_Dial 2012-4-23 */

        /* Begin: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15 */
        case MENU_ITEM_ADD_BLACK:
        {
            if (number != null)
            {
                ContentValues value = new ContentValues();
                if (number.length() > 48)
                {
                    value.put("number", number.substring(0, 47));
                }
                else
                {
                    value.put("number", number);
                }

                //at here,the better way is combine interface function AddBlackNumber(number)
                Uri uriInsert = Uri
                        .parse("content://com.ahong.blackcall.AhongBlackCallProvider/black_number");
                Uri uAdd = getActivity().getContentResolver()
                        .insert(uriInsert, value);
                Log.i(TAG, uAdd.toString());

                if (uAdd != null)
                {
                    toastToBlack(R.string.add_to_black_success);
                    return true;
                }
                else
                {
                    toastToBlack(R.string.add_to_black_fail);
                    return false;
                }
            }
            else
            {
                return false;
            }
        }

        case MENU_ITEM_DEL_BLACK:
        {
            if (number != null)
            {
                Uri uriDelete = Uri
                        .parse("content://com.ahong.blackcall.AhongBlackCallProvider/black_number");
                int result = getActivity()
                        .getContentResolver()
                        .delete(uriDelete, "number = ?",
                                new String[] { number });
                Log.i(TAG, "delete line: " + result);

                if (result > 0)
                {
                    toastToBlack(R.string.del_from_black_success);
                    return true;
                }
                else
                {
                    toastToBlack(R.string.del_from_black_fail);
                    return false;
                }
            }
            else
            {
                return false;
            }
        }

        /* End: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15 */

        }
        return false;
    }

    /*Begin: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15*/
    private void toastToBlack(int resId)
    {
        Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }
    /*End: Modified by sunrise for AddCallLogToBlackNumber 2012/08/15*/

    /* End of wangqiang on 2012-3-26 10:18 CallLog_longClick */
    public void startCallsQuery() {
        mAdapter.setLoading(true);
        mCallLogQueryHandler.fetchAllCalls();
        if (mShowingVoicemailOnly) {
            mShowingVoicemailOnly = false;
            getActivity().invalidateOptionsMenu();
        }
    }

    private void startVoicemailStatusQuery() {
        mCallLogQueryHandler.fetchVoicemailStatus();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        /* Begin: Modified by siliangqi for multi_delete 2012-3-30 */
        // if (mShowOptionsMenu) {
        if (mShowOptionsMenu && (!delete_flag)) {
            /* End: Modified by siliangqi for multi_delete 2012-3-30 */
            inflater.inflate(R.menu.call_log_options, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        /* Begin: Modified by siliangqi for multi_delete 2012-3-30 */
        // if (mShowOptionsMenu) {
        if (mShowOptionsMenu && (!delete_flag)) {
        /* End: Modified by siliangqi for multi_delete 2012-3-30 */
            /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
            // menu.findItem(R.id.delete_all).setEnabled(mAdapter != null &&
            // !mAdapter.isEmpty());
            //menu.findItem(R.id.delete).setEnabled(mAdapter != null && !mAdapter.isEmpty());
            menu.findItem(R.id.delete).setVisible(mAdapter != null && !mAdapter.isEmpty());
            /* End: Modified by siliangqi for multi_delete 2012-3-22 */
                menu.findItem(R.id.show_voicemails_only).setVisible(
                        mVoicemailSourcesAvailable && !mShowingVoicemailOnly);
                menu.findItem(R.id.show_all_calls).setVisible(
                        mVoicemailSourcesAvailable && mShowingVoicemailOnly);
            }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /* Begin: Modified by siliangqi for multi_delete 2012-3-22 */
            // case R.id.delete_all:
            case R.id.delete:
                // ClearCallLogDialog.show(getFragmentManager());
                // onDelCallLog();
                mOnMenuDeleteClickListener.onClick(true);
            /* End: Modified by siliangqi for multi_delete 2012-3-22 */
                return true;

            case R.id.show_voicemails_only:
                mCallLogQueryHandler.fetchVoicemailOnly();
                mShowingVoicemailOnly = true;
                return true;

            case R.id.show_all_calls:
                mCallLogQueryHandler.fetchAllCalls();
                mShowingVoicemailOnly = false;
                return true;
            /* Begin: Modified by siliangqi for multi_delete 2012-3-29 */
            case android.R.id.home: {
                mOnMenuDeleteClickListener.onClick(false);
                return true;
            }
            /* End: Modified by siliangqi for multi_delete 2012-3-29 */

            /*Begin: Modified by xiepengfei for add calls view by 2012/05/26*/
            case R.id.calls_viewby:{
                onCreateDialog(DIALOG_VIEW_BY);
            }
            /*End: Modified by xiepengfei for add calls view by 2012/05/26*/

            default:
                return false;
        }
    }

    private void onDelCallLog(){
        Intent intent = new Intent("com.android.contacts.action.MULTI_PICK_CALL");
        startActivity(intent);
    }
    /* Begin: Modified by siliangqi for multi_delete 2012-3-28 */
    public void setDeleteRecover() {
        /*getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActivity().getActionBar().setDisplayOptions(
                getActivity().getActionBar().getDisplayOptions()
                        ^ (ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP));*/
        mAdapter.setDelete(false);
        myCb.setChecked(false);
        delete_flag = false;
    }

    /* End: Modified by siliangqi for multi_delete 2012-3-28 */

    public void callSelectedEntry() {
        int position = getListView().getSelectedItemPosition();
        if (position < 0) {
            // In touch mode you may often not have something selected, so
            // just call the first entry to make sure that [send] [send] calls the
            // most recent entry.
            position = 0;
        }
        final Cursor cursor = (Cursor)mAdapter.getItem(position);
        if (cursor != null) {
            String number = cursor.getString(CallLogQuery.NUMBER);
            if (TextUtils.isEmpty(number)
                    || number.equals(CallerInfo.UNKNOWN_NUMBER)
                    || number.equals(CallerInfo.PRIVATE_NUMBER)
                    || number.equals(CallerInfo.PAYPHONE_NUMBER)) {
                // This number can't be called, do nothing
                return;
            }
            Intent intent;
            // If "number" is really a SIP address, construct a sip: URI.
            if (PhoneNumberUtils.isUriNumber(number)) {
                intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                    Uri.fromParts("sip", number, null));
            } else {
                // We're calling a regular PSTN phone number.
                // Construct a tel: URI, but do some other possible cleanup first.
                int callType = cursor.getInt(CallLogQuery.CALL_TYPE);
                if (!number.startsWith("+") &&
                       (callType == Calls.INCOMING_TYPE
                                || callType == Calls.MISSED_TYPE)) {
                    // If the caller-id matches a contact with a better qualified number, use it
                    String countryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
                    number = mAdapter.getBetterNumberFromContacts(number, countryIso);
                }
                intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                    Uri.fromParts("tel", number, null));
            }
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }
    }

    @VisibleForTesting
    CallLogAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (mShowOptionsMenu != visible) {
            mShowOptionsMenu = visible;
            // Invalidate the options menu since we are changing the list of options shown in it.
            Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }

        if (visible && isResumed()) {
            refreshData();
        }

        if (!visible) {
            updateOnExit();
        }
    }

    /** Requests updates to the data to be shown. */
    private void refreshData() {
        // Mark all entries in the contact info cache as out of date, so they will be looked up
        // again once being shown.
        mAdapter.invalidateCache();
        startCallsQuery();
        startVoicemailStatusQuery();
        updateOnEntry();
    }

    /** Removes the missed call notifications. */
    private void removeMissedCallNotifications() {
        try {
            ITelephony telephony =
                    ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (telephony != null) {
                telephony.cancelMissedCallsNotification();
            } else {
                Log.w(TAG, "Telephony service is null, can't call " +
                        "cancelMissedCallsNotification");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to clear missed calls notification due to remote exception");
        }
    }

    /** Updates call data and notification state while leaving the call log tab. */
    private void updateOnExit() {
        updateOnTransition(false);
    }

    /** Updates call data and notification state while entering the call log tab. */
    private void updateOnEntry() {
        updateOnTransition(true);
    }

    private void updateOnTransition(boolean onEntry) {
        // We don't want to update any call data when keyguard is on because the user has likely not
        // seen the new calls yet.
        if (!mKeyguardManager.inKeyguardRestrictedInputMode()) {
            // On either of the transitions we reset the new flag and update the notifications.
            // While exiting we additionally consume all missed calls (by marking them as read).
            // This will ensure that they no more appear in the "new" section when we return back.
            mCallLogQueryHandler.markNewCallsAsOld();
            if (!onEntry) {
                mCallLogQueryHandler.markMissedCallsAsRead();
            }
            removeMissedCallNotifications();
            updateVoicemailNotifications();
        }
    }

    private void updateVoicemailNotifications() {
        Intent serviceIntent = new Intent(getActivity(), CallLogNotificationsService.class);
        serviceIntent.setAction(CallLogNotificationsService.ACTION_UPDATE_NOTIFICATIONS);
        getActivity().startService(serviceIntent);
    }
    /* Begin: Modified by siliangqi for multi_delete 2012-3-30 */
    public static void setOnMenuDeleteClickListener(
            OnMenuDeleteClickListener mmOnMenuDeleteClickListener) {
        mOnMenuDeleteClickListener = mmOnMenuDeleteClickListener;
    }

    public interface OnMenuDeleteClickListener {
        public void onClick(boolean flag);
    }

    public void enterDeleteUi() {
        actionbar_layout.setVisibility(View.VISIBLE);
        linear_delete_all.setVisibility(View.VISIBLE);
        linear_del_conf.setVisibility(View.VISIBLE);
        mAdapter.setDelete(true);
        myCb.setChecked(false);
        delete_flag = true;
    }

    public void initDeleteUi() {
        mAdapter.setDelete(true);
        myCb.setChecked(false);
        delete_flag = true;
        setListAdapter(mAdapter);
    }

    public void setEnterDeleteUi(boolean enterDeleteUi) {
        this.enterDeleteUi = enterDeleteUi;
    }

    public RadioButton getAllCallTypeBut() {
        return allCallTypeBut;
    }

    public OnClickListener getCallTypeListener() {
        return callTypeListener;
    }

    public DialogInterface.OnClickListener getSlotListener() {
        return slotListener;
    }
    /* End: Modified by siliangqi for multi_delete 2012-3-30 */

    /*Begin: Modified by xiepengfei for add calls view by 2012/05/26*/
    private final static int DIALOG_VIEW_BY = 1000;
    private int mTheViewByMode = -1;

    private void onCreateDialog(int id) {
        if(mTheViewByMode == -1){
            switch(getSlot()){
                case 0: mTheViewByMode = 4;break;
                case 1: mTheViewByMode = 8;break;
                default: mTheViewByMode = 0;break;
            }
        }

        switch(id){
            case DIALOG_VIEW_BY:
                new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_history_view_mode)
                .setSingleChoiceItems(R.array.calls_view_by, mTheViewByMode, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        mTheViewByMode = whichButton;
                        switch(whichButton){
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                saveSlot(-1);
                                mCallLogQueryHandler.setSubscription(-1);
                                setCallType(whichButton-0);
                                updateSubImage();
                                refreshData();
                                break;
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                                saveSlot(0);
                                mCallLogQueryHandler.setSubscription(0);
                                setCallType(whichButton-4);
                                updateSubImage();
                                refreshData();
                                break;
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                                saveSlot(1);
                                mCallLogQueryHandler.setSubscription(1);
                                setCallType(whichButton-8);
                                updateSubImage();
                                refreshData();
                                break;
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
               .show();
        }
    }

    private void setCallType(int which){
        int callType = TYPE_INDEX_ALL;
        switch(which){
            case 1:
                callType = Calls.INCOMING_TYPE;
                break;
            case 2:
                callType = Calls.OUTGOING_TYPE;
                break;
            case 3:
                callType = Calls.MISSED_TYPE;
                break;
            default:
                callType = TYPE_INDEX_ALL;
                break;
        }
        mCallLogQueryHandler.setCallType(callType);
    }
    /*End: Modified by xiepengfei for add calls view by 2012/05/26*/
}
