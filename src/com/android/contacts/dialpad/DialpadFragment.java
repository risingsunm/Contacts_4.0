/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (c) 2011-2012, Code Aurora Forum. All rights reserved.
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

package com.android.contacts.dialpad;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.SpecialCharSequenceMgr;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.activities.DialtactsActivity.ViewPagerVisibilityListener;
import com.android.contacts.activities.JoinContactActivity;
import com.android.contacts.util.PhoneNumberFormatter;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyMSim;
import com.android.phone.CallLogAsync;
import com.android.phone.HapticFeedback;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.provider.Contacts.Intents.Insert;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.MSimTelephonyManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.android.contacts.SpeedDialUtils;
import com.android.contacts.SpeedDialListActivity;

import com.qrd.plugin.feature_query.FeatureQuery;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.SpecialCharSequenceMgr;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.activities.DialtactsActivity.ViewPagerVisibilityListener;
import com.android.contacts.util.PhoneNumberFormatter;
import com.android.internal.telephony.ITelephony;
import com.android.phone.CallLogAsync;
import com.android.phone.HapticFeedback;
import com.android.contacts.util.UriUtils;
import java.util.Arrays;
import android.content.DialogInterface.OnDismissListener;

/**
 * Fragment that displays a twelve-key phone dialpad.
 */
public class DialpadFragment extends Fragment
        implements View.OnClickListener,
        View.OnLongClickListener, View.OnKeyListener,
        AdapterView.OnItemClickListener, TextWatcher,
        PopupMenu.OnMenuItemClickListener,
        ViewPagerVisibilityListener {
    private static final String TAG = DialpadFragment.class.getSimpleName();

    private static final boolean DEBUG = false;
    /*Start of siliangqi on 2012-3-15 17:40 dial_bg*/
    private View mAdditionalButtonsRow;
    /*End of siliangqi on 2012-3-15 17:40 dial_bg*/
    private static final String EMPTY_NUMBER = "";

    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150;

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

    /** Stream type used to play the DTMF tones off call, and mapped to the volume control keys */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_MUSIC;

    public interface Listener {
        public void onSearchButtonPressed();
    }

    /**
     * View (usually FrameLayout) containing mDigits field. This can be null, in which mDigits
     * isn't enclosed by the container.
     */
    private View mDigitsContainer;
    private EditText mDigits;
    /*Begin: Modified by siliangqi for associate_dial 2012-5-29*/
    private LinearLayout addNew;
    private LinearLayout searchResult;
    private TextView searchResult_count;
    private ImageView searchResult_photo;
    private TextView searchResult_displayname;
    private TextView searchResult_number;
    private ContactPhotoManager mContactPhotoManager;
    private LinearLayout searchResult_setNumber;
    private Cursor myCursor;
    private LinearLayout searchResult_displayall;
    /*End: Modified by siliangqi for associate_dial 2012-5-29*/

    private View mDelete;
    private ToneGenerator mToneGenerator;
    private Object mToneGeneratorLock = new Object();
    private View mDialpad;

    private View mSearchButton;
    private View mMenuButton;
    private Listener mListener;

    private View mDialButtonContainer;
    /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
    //private View mDialButton;
    private View cDialButton,gDialButton;
    /*End: Modified by siliangqi for dial_directory 2012-5-23*/
    /*Begin: Modified by siliangqi for IP_Dial 2012-5-23*/
    private View ipDialButton;
    /*End: Modified by siliangqi for IP_Dial 2012-5-23*/
    /*Start of siliangqi on 2012-3-15 17:40 dial_bg*/
    private View msgButton;
    /*End of siliangqi on 2012-3-15 17:40 dial_bg*/
    private ListView mDialpadChooser;
    private DialpadChooserAdapter mDialpadChooserAdapter;

    /**
     * Regular expression prohibiting manual phone call. Can be empty, which means "no rule".
     */
    private String mProhibitedPhoneNumberRegexp;

    private boolean mShowOptionsMenu;
    private int mSubscription = 0;

    // Last number dialed, retrieved asynchronously from the call DB
    // in onCreate. This number is displayed when the user hits the
    // send key and cleared in onPause.
    CallLogAsync mCallLog = new CallLogAsync();
    private String mLastNumberDialed = EMPTY_NUMBER;

    // determines if we want to playback local DTMF tones.
    private boolean mDTMFToneEnabled;

    // Vibration (haptic feedback) for dialer key presses.
    private HapticFeedback mHaptic = new HapticFeedback();

    /** Identifier for the "Add Call" intent extra. */
    static final String ADD_CALL_MODE_KEY = "add_call_mode";

    /**
     * Identifier for intent extra for sending an empty Flash message for
     * CDMA networks. This message is used by the network to simulate a
     * press/depress of the "hookswitch" of a landline phone. Aka "empty flash".
     *
     * TODO: Using an intent extra to tell the phone to send this flash is a
     * temporary measure. To be replaced with an ITelephony call in the future.
     * TODO: Keep in sync with the string defined in OutgoingCallBroadcaster.java
     * in Phone app until this is replaced with the ITelephony API.
     */
    static final String EXTRA_SEND_EMPTY_FLASH
            = "com.android.phone.extra.SEND_EMPTY_FLASH";

    private String mCurrentCountryIso;

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /**
         * Listen for phone state changes so that we can take down the
         * "dialpad chooser" if the phone becomes idle while the
         * chooser UI is visible.
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // Log.i(TAG, "PhoneStateListener.onCallStateChanged: "
            //       + state + ", '" + incomingNumber + "'");
            if ((state == TelephonyManager.CALL_STATE_IDLE) && dialpadChooserVisible()) {
                // Log.i(TAG, "Call ended with dialpad chooser visible!  Taking it down...");
                // Note there's a race condition in the UI here: the
                // dialpad chooser could conceivably disappear (on its
                // own) at the exact moment the user was trying to select
                // one of the choices, which would be confusing.  (But at
                // least that's better than leaving the dialpad chooser
                // onscreen, but useless...)
                showDialpadChooser(false);
            }
        }
    };

    private PhoneStateListener getPhoneStateListener(final int subscription) {
        PhoneStateListener mPhoneStateListener = new PhoneStateListener(subscription) {
            /**
             * Listen for phone state changes so that we can take down the
             * "dialpad chooser" if the phone becomes idle while the
             * chooser UI is visible.
             */
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                // Log.i(TAG, "PhoneStateListener.onCallStateChanged: "
                //       + state + ", '" + incomingNumber + "'");
                if ((state == TelephonyManager.CALL_STATE_IDLE) && dialpadChooserVisible()) {
                    // Log.i(TAG, "Call ended with dialpad chooser visible!  Taking it down...");
                    // Note there's a race condition in the UI here: the
                    // dialpad chooser could conceivably disappear (on its
                    // own) at the exact moment the user was trying to select
                    // one of the choices, which would be confusing.  (But at
                    // least that's better than leaving the dialpad chooser
                    // onscreen, but useless...)
                    showDialpadChooser(phoneIsInUse());
                }
            }
        };
        return mPhoneStateListener;
    }

    private boolean mWasEmptyBeforeTextChange;

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mWasEmptyBeforeTextChange = TextUtils.isEmpty(s);
    }

    public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
        if (mWasEmptyBeforeTextChange != TextUtils.isEmpty(input)) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }

        // DTMF Tones do not need to be played here any longer -
        // the DTMF dialer handles that functionality now.
    }

    public void afterTextChanged(Editable input) {
        if (SpecialCharSequenceMgr.handleChars(getActivity(), input.toString(), mDigits)) {
            // A special sequence was entered, clear the digits
            mDigits.getText().clear();
        }

        if (isDigitsEmpty()) {
            mDigits.setCursorVisible(false);
        }
        /*Begin: Modified by siliangqi for associate_dial 2012-5-29*/
        String inputFilter = filterSpace(mDigits.getText().toString());
        if(isDigitsEmpty()){
            addNew.setVisibility(View.GONE);
            searchResult.setVisibility(View.GONE);
        }else{
            if(mDigits.length()<=1){
                addNew.setVisibility(View.VISIBLE);
                searchResult.setVisibility(View.GONE);
            }else{
            /*myCursor = getActivity().getContentResolver().query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,"number/"+inputFilter),
                    null,null, null, null);*/
            if(PhoneNumberUtils.isEmergencyNumber(inputFilter)&&TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("ABSENT")&&TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("ABSENT")){
                gDialButton.setEnabled(true);
                gDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg1);
                cDialButton.setEnabled(true);
                cDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg1);
            }else if(TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("ABSENT")&&TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("ABSENT")){
                if(gDialButton.isEnabled()){
                    gDialButton.setEnabled(false);
                    gDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg);
                }
                if(cDialButton.isEnabled()){
                    cDialButton.setEnabled(false);
                    cDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg);
                }
            }
            if(myCursor!=null&&!myCursor.isClosed())
                myCursor.close();
            myCursor = getActivity().getContentResolver().query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,"name/"+inputFilter),
                    null,null, null, null);
            if(myCursor.getCount()==0){
                addNew.setVisibility(View.VISIBLE);
                searchResult.setVisibility(View.GONE);
            }else{
                myCursor.moveToFirst();
                searchResult_count.setText(""+myCursor.getCount());
                mContactPhotoManager.loadPhoto(searchResult_photo, UriUtils.parseUriOrNull(myCursor.getString(myCursor.getColumnIndex("photo_uri"))), false, true);
                //number match
                String s1 = myCursor.getString(myCursor.getColumnIndex("normalized_number"));
                String s2 = myCursor.getString(myCursor.getColumnIndex("contacts_name"));
                SpannableStringBuilder ssb1 = null;
                SpannableStringBuilder ssb2 = null;
                if(s2==null){
                    try {
                    int pos6 = s1.indexOf(inputFilter);
                    ssb1 = new SpannableStringBuilder(s1);
                    ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos6, pos6+inputFilter.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb2 = new SpannableStringBuilder(getString(R.string.unknown));
                    }catch (IndexOutOfBoundsException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        searchResult_number.setText(s1);
                        searchResult_displayname.setText(s2);
                    }
                }else{
                try {
                    String s3 = myNormalize(s2);
                    int pos1 = s1.indexOf(inputFilter);
                    int pos2 = s3.indexOf(inputFilter);
                    if(s3.length()!=inputFilter.length())
                        pos2 = -1;
                    ssb1 = new SpannableStringBuilder(s1);
                    ssb2 = new SpannableStringBuilder(s2);
                        //prefix match name with English or number
                        String s4 = myCursor.getString(myCursor.getColumnIndex("short_name"));
                        if(s4==null){
                            pos2 = s2.indexOf(inputFilter);
                            ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos2, pos2+inputFilter.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }else if(s4.length()==s2.length()){
                            pos2 = s4.indexOf(inputFilter);
                            if(pos2>=0)
                                ssb2.setSpan(new ForegroundColorSpan(0xff31b6e7), pos2, pos2+inputFilter.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            else{
                                String s5 = myCursor.getString(myCursor.getColumnIndex("full_name"));
                                String[] s6 = s5.split("\\s+");
                                if(s6.length==s2.length()){
                                    String s7 = filterSpace(s5);
                                    int pos3 = s7.indexOf(inputFilter);
                                    if(pos3>=0){
                                        int count=0,start=0,end=0;
                                        for(int i=0;i<s6.length;i++){
                                            count += s6[i].length();
                                            if(pos3>=(count-s6[i].length())&&pos3<count){
                                                start = i;
                                            }
                                            if((pos3+inputFilter.length())>(count-s6[i].length())&&(pos3+inputFilter.length())<=count){
                                                end = i;
                                                ssb2.setSpan(new ForegroundColorSpan(0xff31b6e7), start, end+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                break;
                                            }
                                        }
                                    }else if(pos1>-1){
                                        //number match
                                        ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos1, pos1+inputFilter.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                }
                            }
                        }else{
                            int pos4 = s4.indexOf(inputFilter);
                            //pref fix name with chinese
                            if(pos4>=0){
                                String short_name_normal = myCursor.getString(myCursor.getColumnIndex("short_name_normal"));
                                String s8 = myCursor.getString(myCursor.getColumnIndex("hanzi_name"));
                                String s9[] = s8.split("\\s+");
                                int start = pos4, end = pos4+inputFilter.length();
                                int symbolCount = containOtherSymbol(short_name_normal,0,pos4);
                                int temp = 0;
                                if(symbolCount!=0){
                                    do{
                                        temp = containOtherSymbol(short_name_normal,start,start+symbolCount);
                                        start += symbolCount;
                                        symbolCount = temp;
                                    }while(symbolCount!=0);
                                }
                                int finalStart = 0;
                                for(int i=0;i<start;i++){
                                    finalStart += s9[i].length();
                                }
                                end = finalStart + inputFilter.length();
                                symbolCount = containOtherSymbol(short_name_normal,finalStart,finalStart+inputFilter.length());
                                temp = 0;
                                if(symbolCount!=0){
                                    do{
                                        temp = containOtherSymbol(short_name_normal,end,end+symbolCount);
                                        end += symbolCount;
                                        symbolCount = temp;
                                    }while(symbolCount!=0);
                                }
                                int finalEnd = 0;
                                for(int i=0;i<end;i++){
                                    finalEnd += s9[i].length();
                                }
                                ssb2.setSpan(new ForegroundColorSpan(0xff31b6e7), finalStart, finalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }else{
                                //full name match
                                String s10 = myCursor.getString(myCursor.getColumnIndex("pinyin_name"));
                                String s11[] = s10.split("\\s+");
                                String s12 = filterSpace(s10);
                                String s13 = myCursor.getString(myCursor.getColumnIndex("hanzi_name"));
                                String s14[] = s13.split("\\s+");
                                int pos5 = s12.indexOf(inputFilter);
                                if(pos5>-1){
                                    int count1=0,count2=0,start=0,end=0;
                                    for(int i=0;i<s11.length;i++){
                                        count1 += s11[i].length();
                                        count2 += s14[i].length();
                                        if(pos5>=(count1-s11[i].length())&&pos5<count1){
                                            start = count2-s14[i].length()+pos5-(count1-s11[i].length());
                                        }
                                        if((pos5+inputFilter.length())>(count1-s11[i].length())&&(pos5+inputFilter.length())<=count1){
                                            if(s14[i].length()!=1)
                                                end = count2-s14[i].length()+pos5+inputFilter.length()-(count1-s11[i].length())-1;
                                            else
                                                end = count2-s14[i].length();
                                            ssb2.setSpan(new ForegroundColorSpan(0xff31b6e7), start, end+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            break;
                                        }
                                    }
                                }else if(pos1>-1){
                                    //number match
                                    ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos1, pos1+inputFilter.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                            }
                        }
                } catch (IndexOutOfBoundsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    ssb1 = new SpannableStringBuilder(s1);
                    ssb2 = new SpannableStringBuilder(s2);
                }catch (NullPointerException e){
                    e.printStackTrace();
                    ssb1 = new SpannableStringBuilder(s1);
                    ssb2 = new SpannableStringBuilder(s2);
                }
                }
                searchResult_number.setText(ssb1);
                searchResult_displayname.setText(ssb2);
                addNew.setVisibility(View.GONE);
                searchResult.setVisibility(View.VISIBLE);
            }
            }
        }
        /*End: Modified by siliangqi for associate_dial 2012-5-29*/
        updateDialAndDeleteButtonEnabledState();
    }
    /*Begin: Modified by siliangqi for associate_dial 2012-6-12*/
    public static String myNormalize(String name) {
        StringBuffer sb = new StringBuffer("");
        for(int i=0;i<name.length();i++){
            switch(name.charAt(i)){
                case 'a':
                case 'A':
                case 'b':
                case 'B':
                case 'c':
                case 'C':
                    sb.append('2');
                    break;
                case 'd':
                case 'D':
                case 'e':
                case 'E':
                case 'f':
                case 'F':
                    sb.append('3');
                    break;
                case 'g':
                case 'G':
                case 'h':
                case 'H':
                case 'i':
                case 'I':
                    sb.append('4');
                    break;
                case 'j':
                case 'J':
                case 'k':
                case 'K':
                case 'l':
                case 'L':
                    sb.append('5');
                    break;
                case 'm':
                case 'M':
                case 'n':
                case 'N':
                case 'o':
                case 'O':
                    sb.append('6');
                    break;
                case 'p':
                case 'P':
                case 'q':
                case 'Q':
                case 'r':
                case 'R':
                case 's':
                case 'S':
                    sb.append('7');
                    break;
                case 't':
                case 'T':
                case 'u':
                case 'U':
                case 'v':
                case 'V':
                    sb.append('8');
                    break;
                case 'w':
                case 'W':
                case 'x':
                case 'X':
                case 'y':
                case 'Y':
                case 'z':
                case 'Z':
                    sb.append('9');
                    break;
                case ' ':
                    sb.append(' ');
                    break;
            }
        }
        return sb.toString();
    }
    public static int containOtherSymbol(String s, int start, int end){
        int count = 0;
        for(int i=start;i<end;i++){
            if(!(s.charAt(i)>='0'&&s.charAt(i)<='9'||s.charAt(i)>='A'&&s.charAt(i)<='Z')){
                count++;
            }
        }
        return count;
    }
    /*End: Modified by siliangqi for associate_dial 2012-6-12*/
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        /*Begin: Modified by siliangqi for associate_dial 2012-5-30*/
        mContactPhotoManager = ContactPhotoManager.getInstance(getActivity());
        /*End: Modified by siliangqi for associate_dial 2012-5-30*/
        mCurrentCountryIso = ContactsUtils.getCurrentCountryIso(getActivity());

        try {
            mHaptic.init(getActivity(),
                         getResources().getBoolean(R.bool.config_enable_dialer_key_vibration));
        } catch (Resources.NotFoundException nfe) {
             Log.e(TAG, "Vibrate control bool missing.", nfe);
        }

        setHasOptionsMenu(true);

        mProhibitedPhoneNumberRegexp = getResources().getString(
                R.string.config_prohibited_phone_number_regexp);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View fragmentView = inflater.inflate(R.layout.dialpad_fragment, container, false);

        // Load up the resources for the text field.
        Resources r = getResources();

        mDigitsContainer = fragmentView.findViewById(R.id.digits_container);
        mDigits = (EditText) fragmentView.findViewById(R.id.digits);
        /*Begin: Modified by wqiang for Dialpad_MAX_Input 2012/08/10*/
        mDigits.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        /*End: Modified by wqiang for Dialpad_MAX_Input 2012/08/10*/
        mDigits.setKeyListener(DialerKeyListener.getInstance());
        mDigits.setOnClickListener(this);
        mDigits.setOnKeyListener(this);
        mDigits.setOnLongClickListener(this);
        mDigits.addTextChangedListener(this);
        /*Begin: Modified by siliangqi for associate_dial 2012-5-29*/
        addNew = (LinearLayout)fragmentView.findViewById(R.id.addNew);
        addNew.setOnClickListener(this);
        searchResult = (LinearLayout)fragmentView.findViewById(R.id.searchResult);
        searchResult_count = (TextView)fragmentView.findViewById(R.id.searchResult_count);
        searchResult_photo = (ImageView)fragmentView.findViewById(R.id.searchResult_photo);
        searchResult_photo.setClickable(false);
        searchResult_displayname = (TextView)fragmentView.findViewById(R.id.searchResult_displayname);
        searchResult_number = (TextView)fragmentView.findViewById(R.id.searchResult_number);
        searchResult_setNumber = (LinearLayout)fragmentView.findViewById(R.id.searchResult_setNumber);
        searchResult_setNumber.setOnClickListener(this);
        searchResult_displayall = (LinearLayout)fragmentView.findViewById(R.id.searchResult_displayall);
        searchResult_displayall.setOnClickListener(this);
        /*End: Modified by siliangqi for associate_dial 2012-5-29*/
        //PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(getActivity(), mDigits);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int minCellSize = (int) (56 * dm.density); // 56dip == minimum size of menu buttons
        int cellCount = dm.widthPixels / minCellSize;
        int fakeMenuItemWidth = dm.widthPixels / cellCount;
        if (DEBUG) Log.d(TAG, "The size of fake menu buttons (in pixel): " + fakeMenuItemWidth);

        // Soft menu button should appear only when there's no hardware menu button.
        mMenuButton = fragmentView.findViewById(R.id.overflow_menu);
        if (mMenuButton != null) {
            mMenuButton.setMinimumWidth(fakeMenuItemWidth);
            if (ViewConfiguration.get(getActivity()).hasPermanentMenuKey()) {
                // This is required for dialpad button's layout, so must not use GONE here.
                /* Start of siliangqi on 2012-3-15 17:40 dial_bg */
                // mMenuButton.setVisibility(View.INVISIBLE);
                mMenuButton.setVisibility(View.GONE);
                /* End of siliangqi on 2012-3-15 17:40 dial_bg */
            } else {
                mMenuButton.setOnClickListener(this);
            }
        }
        mSearchButton = fragmentView.findViewById(R.id.searchButton);
        if (mSearchButton != null) {
            mSearchButton.setMinimumWidth(fakeMenuItemWidth);
            mSearchButton.setOnClickListener(this);
        }

        // Check for the presence of the keypad
        View oneButton = fragmentView.findViewById(R.id.one);
        if (oneButton != null) {
            setupKeypad(fragmentView);
        }
        /* Start of siliangqi on 2012-3-15 17:40 dial_bg */
        mAdditionalButtonsRow = fragmentView
                .findViewById(R.id.dialpadAdditionalButtons);
        // mDialButtonContainer =
        // fragmentView.findViewById(R.id.dialButtonContainer);
        /* End of siliangqi on 2012-3-15 17:40 dial_bg */
        /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
        //mDialButton = fragmentView.findViewById(R.id.dialButton);
        cDialButton = fragmentView.findViewById(R.id.cDialButton);
        if(TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("ABSENT")){
            cDialButton.setEnabled(false);
            cDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg);
        }
        gDialButton = fragmentView.findViewById(R.id.gDialButton);
        if(TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("ABSENT")){
            gDialButton.setEnabled(false);
            gDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg);
        }
        /*if(TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("ABSENT")&&TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("ABSENT")){
            cDialButton.setEnabled(true);
            cDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg1);
        }*/
        /*End: Modified by siliangqi for dial_directory 2012-5-23*/
        if (r.getBoolean(R.bool.config_show_onscreen_dial_button)) {
            /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
            //mDialButton.setOnClickListener(this);
            cDialButton.setOnClickListener(this);
            gDialButton.setOnClickListener(this);
            /*End: Modified by siliangqi for dial_directory 2012-5-23*/
        } else {
            /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
            //mDialButton.setVisibility(View.GONE); // It's VISIBLE by default
            //mDialButton = null;
            cDialButton.setVisibility(View.GONE); // It's VISIBLE by default
            cDialButton = null;
            gDialButton.setVisibility(View.GONE); // It's VISIBLE by default
            gDialButton = null;
            /*End: Modified by siliangqi for dial_directory 2012-5-23*/
        }
        /*Begin: Modified by siliangqi for IP_Dial 2012-5-23*/
        ipDialButton = fragmentView.findViewById(R.id.ipDialButton);
        if(TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("ABSENT")&&TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("ABSENT")){
            ipDialButton.setEnabled(false);
            ipDialButton.setBackgroundResource(R.drawable.call_dial_btn_bg);
        }
        ipDialButton.setOnClickListener(this);
        /*End: Modified by siliangqi for IP_Dial 2012-5-23*/
        /* Start of siliangqi on 2012-3-15 17:40 dial_bg */
        msgButton = mAdditionalButtonsRow.findViewById(R.id.msgButton);
        msgButton.setOnClickListener(this);
        // mDelete = mAdditionalButtonsRow.findViewById(R.id.deleteButton);
        mDelete = fragmentView.findViewById(R.id.deleteButton);
        /* End of siliangqi on 2012-3-15 17:40 dial_bg */
        if (mDelete != null) {
            mDelete.setOnClickListener(this);
            mDelete.setOnLongClickListener(this);
        }

        mDialpad = fragmentView.findViewById(R.id.dialpad);  // This is null in landscape mode.

        mDigits.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        // Set up the "dialpad chooser" UI; see showDialpadChooser().
        mDialpadChooser = (ListView) fragmentView.findViewById(R.id.dialpadChooser);
        mDialpadChooser.setOnItemClickListener(this);

        configureScreenFromIntent(getActivity().getIntent());

        updateFakeMenuButtonsVisibility(mShowOptionsMenu);

        return fragmentView;
    }

    private boolean isLayoutReady() {
        return mDigits != null;
    }

    public EditText getDigitsWidget() {
        return mDigits;
    }

    /**
     * @return true when {@link #mDigits} is actually filled by the Intent.
     */
    private boolean fillDigitsIfNecessary(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                if ("tel".equals(uri.getScheme())) {
                    // Put the requested number into the input area
                    String data = uri.getSchemeSpecificPart();
                    setFormattedDigits(data, null);
                    return true;
                } else {
                    String type = intent.getType();
                    if (People.CONTENT_ITEM_TYPE.equals(type)
                            || Phones.CONTENT_ITEM_TYPE.equals(type)) {
                        // Query the phone number
                        Cursor c = getActivity().getContentResolver().query(intent.getData(),
                                new String[] {PhonesColumns.NUMBER, PhonesColumns.NUMBER_KEY},
                                null, null, null);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    // Put the number into the input area
                                    setFormattedDigits(c.getString(0), c.getString(1));
                                    return true;
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * @see #showDialpadChooser(boolean)
     */
    private static boolean needToShowDialpadChooser(Intent intent, boolean isAddCallMode) {
        final String action = intent.getAction();

        boolean needToShowDialpadChooser = false;

        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri == null) {
                // ACTION_DIAL or ACTION_VIEW with no data.
                // This behaves basically like ACTION_MAIN: If there's
                // already an active call, bring up an intermediate UI to
                // make the user confirm what they really want to do.
                // Be sure *not* to show the dialpad chooser if this is an
                // explicit "Add call" action, though.
                if (!isAddCallMode && phoneIsInUse()) {
                    needToShowDialpadChooser = true;
                }
            }
        } else if (Intent.ACTION_MAIN.equals(action)) {
            // The MAIN action means we're bringing up a blank dialer
            // (e.g. by selecting the Home shortcut, or tabbing over from
            // Contacts or Call log.)
            //
            // At this point, IF there's already an active call, there's a
            // good chance that the user got here accidentally (but really
            // wanted the in-call dialpad instead).  So we bring up an
            // intermediate UI to make the user confirm what they really
            // want to do.
            if (phoneIsInUse()) {
                // Log.i(TAG, "resolveIntent(): phone is in use; showing dialpad chooser!");
                needToShowDialpadChooser = true;
            }
        }

        return needToShowDialpadChooser;
    }

    private static boolean isAddCallMode(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            // see if we are "adding a call" from the InCallScreen; false by default.
            return intent.getBooleanExtra(ADD_CALL_MODE_KEY, false);
        } else {
            return false;
        }
    }

    /**
     * Checks the given Intent and changes dialpad's UI state. For example, if the Intent requires
     * the screen to enter "Add Call" mode, this method will show correct UI for the mode.
     */
    public void configureScreenFromIntent(Intent intent) {
        if (!isLayoutReady()) {
            // This happens typically when parent's Activity#onNewIntent() is called while
            // Fragment#onCreateView() isn't called yet, and thus we cannot configure Views at
            // this point. onViewCreate() should call this method after preparing layouts, so
            // just ignore this call now.
            Log.i(TAG,
                    "Screen configuration is requested before onCreateView() is called. Ignored");
            return;
        }

        boolean needToShowDialpadChooser = false;

        final boolean isAddCallMode = isAddCallMode(intent);
        if (!isAddCallMode) {
            final boolean digitsFilled = fillDigitsIfNecessary(intent);
            if (!digitsFilled) {
                needToShowDialpadChooser = needToShowDialpadChooser(intent, isAddCallMode);
            }
        }
        showDialpadChooser(needToShowDialpadChooser);
    }

    private void setFormattedDigits(String data, String normalizedNumber) {
        // strip the non-dialable numbers out of the data string.
        String dialString = PhoneNumberUtils.extractNetworkPortion(data);
        dialString =
                PhoneNumberUtils.formatNumber(dialString, normalizedNumber, mCurrentCountryIso);
        if (!TextUtils.isEmpty(dialString)) {
            Editable digits = mDigits.getText();
            digits.replace(0, digits.length(), dialString);
            // for some reason this isn't getting called in the digits.replace call above..
            // but in any case, this will make sure the background drawable looks right
            afterTextChanged(digits);
        }
    }

    private void setupKeypad(View fragmentView) {
        // Setup the listeners for the buttons
        View view = fragmentView.findViewById(R.id.one);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);

        fragmentView.findViewById(R.id.two).setOnClickListener(this);
        fragmentView.findViewById(R.id.three).setOnClickListener(this);
        fragmentView.findViewById(R.id.four).setOnClickListener(this);
        fragmentView.findViewById(R.id.five).setOnClickListener(this);
        fragmentView.findViewById(R.id.six).setOnClickListener(this);
        fragmentView.findViewById(R.id.seven).setOnClickListener(this);
        fragmentView.findViewById(R.id.eight).setOnClickListener(this);
        fragmentView.findViewById(R.id.nine).setOnClickListener(this);
        fragmentView.findViewById(R.id.star).setOnClickListener(this);
        if (FeatureQuery.FEATURE_CONTACTS_SPEED_DIAL) {
            fragmentView.findViewById(R.id.two).setOnLongClickListener(this);
            fragmentView.findViewById(R.id.three).setOnLongClickListener(this);
            fragmentView.findViewById(R.id.four).setOnLongClickListener(this);
            fragmentView.findViewById(R.id.five).setOnLongClickListener(this);
            fragmentView.findViewById(R.id.six).setOnLongClickListener(this);
            fragmentView.findViewById(R.id.seven).setOnLongClickListener(this);
            fragmentView.findViewById(R.id.eight).setOnLongClickListener(this);
            fragmentView.findViewById(R.id.nine).setOnLongClickListener(this);
        }

        view = fragmentView.findViewById(R.id.zero);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);

        fragmentView.findViewById(R.id.pound).setOnClickListener(this);
    }

    private void listen(int events) {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            MSimTelephonyManager telephonyManager = (MSimTelephonyManager) getActivity()
                    .getSystemService(Context.MSIM_TELEPHONY_SERVICE);
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            for (int i = 0; i < phoneCount; i++) {
                telephonyManager.listen(getPhoneStateListener(i), events);
            }
        } else {
            TelephonyManager telephonyManager =
                    (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(mPhoneStateListener, events);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Query the last dialed number. Do it first because hitting
        // the DB is 'slow'. This call is asynchronous.
        queryLastOutgoingCall();

        // retrieve the DTMF tone play back setting.
        mDTMFToneEnabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

        // Retrieve the haptic feedback setting.
        mHaptic.checkSystemSetting();

        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    // we want the user to be able to control the volume of the dial tones
                    // outside of a call, so we use the stream type that is also mapped to the
                    // volume control keys for this activity
                    mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
                    getActivity().setVolumeControlStream(DIAL_TONE_STREAM_TYPE);
                } catch (RuntimeException e) {
                    Log.w(TAG, "Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                }
            }
        }

        Activity parent = getActivity();
        if (parent instanceof DialtactsActivity) {
            // See if we were invoked with a DIAL intent. If we were, fill in the appropriate
            // digits in the dialer field.
            fillDigitsIfNecessary(parent.getIntent());
        }

        // While we're in the foreground, listen for phone state changes,
        // purely so that we can take down the "dialpad chooser" if the
        // phone becomes idle while the chooser UI is visible.
        listen(PhoneStateListener.LISTEN_CALL_STATE);

        // Potentially show hint text in the mDigits field when the user
        // hasn't typed any digits yet.  (If there's already an active call,
        // this hint text will remind the user that he's about to add a new
        // call.)
        //
        // TODO: consider adding better UI for the case where *both* lines
        // are currently in use.  (Right now we let the user try to add
        // another call, but that call is guaranteed to fail.  Perhaps the
        // entire dialer UI should be disabled instead.)
        if (phoneIsInUse()) {
            mDigits.setHint(R.string.dialerDialpadHintText);
        } else {
            // Common case; no hint necessary.
            mDigits.setHint(null);

            // Also, a sanity-check: the "dialpad chooser" UI should NEVER
            // be visible if the phone is idle!
            showDialpadChooser(false);
        }

        updateDialAndDeleteButtonEnabledState();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop listening for phone state changes.
        listen(PhoneStateListener.LISTEN_NONE);

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
        // TODO: I wonder if we should not check if the AsyncTask that
        // lookup the last dialed number has completed.
        mLastNumberDialed = EMPTY_NUMBER;  // Since we are going to query again, free stale number.
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mShowOptionsMenu && ViewConfiguration.get(getActivity()).hasPermanentMenuKey() &&
                isLayoutReady() && mDialpadChooser != null) {
            inflater.inflate(R.menu.dialpad_options, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hardware menu key should be available and Views should already be ready.
        if (mShowOptionsMenu && ViewConfiguration.get(getActivity()).hasPermanentMenuKey() &&
                isLayoutReady() && mDialpadChooser != null) {
             setupMenuItems(menu);
        }
    }

    private void setupMenuItems(Menu menu) {
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings_dialpad);
        final MenuItem addToContactMenuItem = menu.findItem(R.id.menu_add_contacts);
        final MenuItem twoSecPauseMenuItem = menu.findItem(R.id.menu_2s_pause);
        final MenuItem waitMenuItem = menu.findItem(R.id.menu_add_wait);

        // Check if all the menu items are inflated correctly. As a shortcut, we assume all menu
        // items are ready if the first item is non-null.
        if (callSettingsMenuItem == null) {
            return;
        }

        final Activity activity = getActivity();
        if (activity != null && ViewConfiguration.get(activity).hasPermanentMenuKey()) {
            // Call settings should be available via its parent Activity.
            callSettingsMenuItem.setVisible(false);
        } else {
            callSettingsMenuItem.setVisible(true);
            callSettingsMenuItem.setIntent(DialtactsActivity.getCallSettingsIntent());
        }

        // We show "add to contacts", "2sec pause", and "add wait" menus only when the user is
        // seeing usual dialpads and has typed at least one digit.
        // We never show a menu if the "choose dialpad" UI is up.
        if (dialpadChooserVisible() || isDigitsEmpty()) {
            addToContactMenuItem.setVisible(false);
            twoSecPauseMenuItem.setVisible(false);
            waitMenuItem.setVisible(false);
        } else {
            final CharSequence digits = mDigits.getText();

            // Put the current digits string into an intent
            addToContactMenuItem.setIntent(getAddToContactIntent(digits));
            addToContactMenuItem.setVisible(true);

            // Check out whether to show Pause & Wait option menu items
            int selectionStart;
            int selectionEnd;
            String strDigits = digits.toString();

            selectionStart = mDigits.getSelectionStart();
            selectionEnd = mDigits.getSelectionEnd();

            if (selectionStart != -1) {
                if (selectionStart > selectionEnd) {
                    // swap it as we want start to be less then end
                    int tmp = selectionStart;
                    selectionStart = selectionEnd;
                    selectionEnd = tmp;
                }

                if (selectionStart != 0) {
                    // Pause can be visible if cursor is not in the begining
                    twoSecPauseMenuItem.setVisible(true);

                    // For Wait to be visible set of condition to meet
                    waitMenuItem.setVisible(showWait(selectionStart, selectionEnd, strDigits));
                } else {
                    // cursor in the beginning both pause and wait to be invisible
                    twoSecPauseMenuItem.setVisible(false);
                    waitMenuItem.setVisible(false);
                }
            } else {
                twoSecPauseMenuItem.setVisible(true);

                // cursor is not selected so assume new digit is added to the end
                int strLength = strDigits.length();
                waitMenuItem.setVisible(showWait(strLength, strLength, strDigits));
            }
        }

        /* Begin: Modified by sunrise for CallMenuSet 2012/08/06 */
        final MenuItem SpeedDialSetMenuItem = menu.findItem(R.id.menu_speed_dial_setting);
        waitMenuItem.setVisible(false);
        twoSecPauseMenuItem.setVisible(false);
        SpeedDialSetMenuItem.setVisible(true);
        Intent i = new Intent();
        i.setClass(activity, SpeedDialListActivity.class);
        SpeedDialSetMenuItem.setIntent(i);
        /* End: Modified by sunrise for CallMenuSet 2012/08/06 */

    }

    private static Intent getAddToContactIntent(CharSequence digits) {
        final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Insert.PHONE, digits);
        intent.setType(People.CONTENT_ITEM_TYPE);
        return intent;
    }

    private void keyPressed(int keyCode) {
        mHaptic.vibrate();
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        mDigits.onKeyDown(keyCode, event);

        // If the cursor is at the end of the text we hide it.
        final int length = mDigits.length();
        if (length == mDigits.getSelectionStart() && length == mDigits.getSelectionEnd()) {
            mDigits.setCursorVisible(false);
        }
    }

    public boolean onKey(View view, int keyCode, KeyEvent event) {
        switch (view.getId()) {
            case R.id.digits:
               if (keyCode == KeyEvent.KEYCODE_ENTER | keyCode == KeyEvent.KEYCODE_CALL) {
                   if (event.getAction() == KeyEvent.ACTION_UP)
                       /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
                       //dialButtonPressed();
                       /*End: Modified by siliangqi for dial_directory 2012-5-23*/
                   return true;
               }
               if (keyCode == KeyEvent.KEYCODE_1) {
                   long timeDiff = SystemClock.uptimeMillis() - event.getDownTime();
                   if (timeDiff >= ViewConfiguration.getLongPressTimeout()) {
                       // Long press detected, call voice mail
                       Log.d(TAG,"call to Voicemail number");
                       callVoicemail();
                       return true;
                   }
               }
                break;
        }
        return false;
    }
        /*Begin: Modified by siliangqi for associate_dial 2012-6-1*/
        public String filterSpace(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ')
                continue;
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }
        /*End: Modified by siliangqi for associate_dial 2012-6-1*/
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.one: {
                playTone(ToneGenerator.TONE_DTMF_1);
                keyPressed(KeyEvent.KEYCODE_1);
                return;
            }
            case R.id.two: {
                playTone(ToneGenerator.TONE_DTMF_2);
                keyPressed(KeyEvent.KEYCODE_2);
                return;
            }
            case R.id.three: {
                playTone(ToneGenerator.TONE_DTMF_3);
                keyPressed(KeyEvent.KEYCODE_3);
                return;
            }
            case R.id.four: {
                playTone(ToneGenerator.TONE_DTMF_4);
                keyPressed(KeyEvent.KEYCODE_4);
                return;
            }
            case R.id.five: {
                playTone(ToneGenerator.TONE_DTMF_5);
                keyPressed(KeyEvent.KEYCODE_5);
                return;
            }
            case R.id.six: {
                playTone(ToneGenerator.TONE_DTMF_6);
                keyPressed(KeyEvent.KEYCODE_6);
                return;
            }
            case R.id.seven: {
                playTone(ToneGenerator.TONE_DTMF_7);
                keyPressed(KeyEvent.KEYCODE_7);
                return;
            }
            case R.id.eight: {
                playTone(ToneGenerator.TONE_DTMF_8);
                keyPressed(KeyEvent.KEYCODE_8);
                return;
            }
            case R.id.nine: {
                playTone(ToneGenerator.TONE_DTMF_9);
                keyPressed(KeyEvent.KEYCODE_9);
                return;
            }
            case R.id.zero: {
                playTone(ToneGenerator.TONE_DTMF_0);
                keyPressed(KeyEvent.KEYCODE_0);
                return;
            }
            case R.id.pound: {
                playTone(ToneGenerator.TONE_DTMF_P);
                keyPressed(KeyEvent.KEYCODE_POUND);
                return;
            }
            case R.id.star: {
                playTone(ToneGenerator.TONE_DTMF_S);
                keyPressed(KeyEvent.KEYCODE_STAR);
                return;
            }
            case R.id.deleteButton: {
                keyPressed(KeyEvent.KEYCODE_DEL);
                return;
            }
            /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
            //case R.id.dialButton: {
            case R.id.cDialButton: {
            /*End: Modified by siliangqi for dial_directory 2012-5-23*/
                mHaptic.vibrate();  // Vibrate here too, just like we do for the regular keys
                /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
                //dialButtonPressed();
                dialButtonPressed(0);
                /*End: Modified by siliangqi for dial_directory 2012-5-23*/
                return;
            }
            /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
            case R.id.gDialButton: {
                mHaptic.vibrate();  // Vibrate here too, just like we do for the regular keys
                dialButtonPressed(1);
                return;
            }
            /*End: Modified by siliangqi for dial_directory 2012-5-23*/
        /* Start of siliangqi on 2012-3-15 17:47 dial_bg */
        case R.id.msgButton: {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "sms", mDigits.getText().toString(), null));
            startActivity(intent);
            return;
        }
        /* End of siliangqi on 2012-3-15 17:47 dial_bg */
            /*Begin: Modified by siliangqi for IP_Dial 2012-5-23*/
            case R.id.ipDialButton:{
                 /*Begin: Modified by siliangqi for ipdial_number_null 2012-8-8*/
                 if (isDigitsEmpty())
                    return;
                 /*End: Modified by siliangqi for ipdial_number_null 2012-8-8*/
                Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,Uri.fromParts("tel", mDigits.getText().toString(),null));
                intent .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("ipcall", true);
                startActivity(intent);
            }
            /*End: Modified by siliangqi for IP_Dial 2012-5-23*/
        /* Start of siliangqi on 2012-3-16 9:12 dial_bg */
        /*
         * case R.id.searchButton: { mHaptic.vibrate(); if (mListener != null) {
         * mListener.onSearchButtonPressed(); } return; }
         */
        /* End of siliangqi on 2012-3-16 9:12 dial_bg */
            case R.id.digits: {
                if (!isDigitsEmpty()) {
                    mDigits.setCursorVisible(true);
                }
                return;
            }
            /*Begin: Modified by siliangqi for associate_dial 2012-5-29*/
            case R.id.addNew:{
                CreateImageDialog();
                return;
            }
            case R.id.searchResult_setNumber:{
                if(myCursor!=null){
                    mDigits.setText(myCursor.getString(myCursor.getColumnIndex("normalized_number")));
                    mDigits.setSelection(mDigits.getText().length());
                }
                return;
            }
            case R.id.searchResult_displayall:{
                final MyDialog md = new MyDialog(getActivity(),R.style.MyDialogStyle,myCursor,filterSpace(mDigits.getText().toString()));
                md.setTitle(R.string.search_result);
                md.show();
                md.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        if(md.getMyPosition()!=-1){
                            myCursor.moveToPosition(md.getMyPosition());
                            mDigits.setText(myCursor.getString(myCursor.getColumnIndex("normalized_number")));
                            mDigits.setSelection(mDigits.getText().length());
                        }else{
                            myCursor.moveToFirst();
                        }
                    }
                });
                return;
            }
            /*End: Modified by siliangqi for associate_dial 2012-5-29*/
            case R.id.overflow_menu: {
                PopupMenu popup = constructPopupMenu(view);
                if (popup != null) {
                    popup.show();
                }
            }
        }
    }

    /* Begin: Modified by siliangqi for associate_dial 2012-5-29 */
    private void CreateImageDialog() {
        CharSequence[] items = {
                getString(R.string.bottom_button_addcontact),
                getString(R.string.bottom_button_refreshcontact)
        };
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
                            mainActionIntent = new Intent(getActivity(),
                                    ContactEditorActivity.class);
                            mainActionIntent.setAction(Intent.ACTION_INSERT);
                            mainActionIntent.setType(Contacts.CONTENT_ITEM_TYPE);
                            mainActionIntent.putExtra(Insert.PHONE, mDigits.getText().toString());
                            startActivity(mainActionIntent);
                        } else if (which == 1) {
                            /*String[] contactId = {
                                    Contacts._ID
                            };
                            long value = 0;
                            Cursor cs = getActivity().getContentResolver().query(Contacts.CONTENT_URI, contactId,
                                    null, null, null);
                            if (cs != null && cs.moveToLast())
                            {
                                value = Long
                                        .parseLong(cs.getString(cs.getColumnIndex(Contacts._ID)));
                            }
                            value = value + 1;
                            Intent it = new Intent(JoinContactActivity.JOIN_CONTACT);
                            it.putExtra("com.android.contacts.action.CONTACT_ID", value);
                            it.putExtra("number", mDigits.getText().toString());
                            startActivity(it);*/
                            startActivity(getAddToContactIntent(mDigits.getText().toString()));
                        } else {
                            Log.d(TAG, "***other****");
                        }
                    }

                };
        new AlertDialog.Builder(getActivity())
                /* Begin: Modified by sunrise for DialPadDialogTitle 2012-7-24 */
                //.setTitle(R.string.bottom_button_addcontact)
                .setTitle(R.string.activity_title_confirm_add_detail)
                /* End: Modified by sunrise for DialPadDialogTitle 2012-7-24 */
                .setNegativeButton(android.R.string.cancel, null)
                .setItems(items, clickListener)
                .show();
    }

    /* End: Modified by siliangqi for associate_dial 2012-5-29 */

    private PopupMenu constructPopupMenu(View anchorView) {
        final Context context = getActivity();
        if (context == null) {
            return null;
        }
        final PopupMenu popupMenu = new PopupMenu(context, anchorView);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.dialpad_options);
        popupMenu.setOnMenuItemClickListener(this);
        setupMenuItems(menu);
        return popupMenu;
    }

    public boolean onLongClick(View view) {
        final Editable digits = mDigits.getText();
        int id = view.getId();
        switch (id) {
            case R.id.deleteButton: {
                digits.clear();
                // TODO: The framework forgets to clear the pressed
                // status of disabled button. Until this is fixed,
                // clear manually the pressed status. b/2133127
                mDelete.setPressed(false);
                return true;
            }
            case R.id.one: {
                if (isDigitsEmpty()) {
                    if (isVoicemailAvailable()) {
                        callVoicemail();
                    } else if (getActivity() != null) {
                        DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                                R.string.dialog_voicemail_not_ready_title,
                                R.string.dialog_voicemail_not_ready_message);
                        dialogFragment.show(getFragmentManager(), "voicemail_not_ready");
                    }
                    return true;
                }
                return false;
            }
            case R.id.zero: {
                keyPressed(KeyEvent.KEYCODE_PLUS);
                return true;
            }
            case R.id.two:
            case R.id.three:
            case R.id.four:
            case R.id.five:
            case R.id.six:
            case R.id.seven:
            case R.id.eight:
            case R.id.nine:
                if (FeatureQuery.FEATURE_CONTACTS_SPEED_DIAL && isDigitsEmpty()) {
                    callSpeedNumber(id);
                    return true;
                }
                return false;
            case R.id.digits: {
                // Right now EditText does not show the "paste" option when cursor is not visible.
                // To show that, make the cursor visible, and return false, letting the EditText
                // show the option by itself.
                mDigits.setCursorVisible(true);
                return false;
            }
        }
        return false;
    }

    public void callVoicemail() {
        startActivity(newVoicemailIntent());
        mDigits.getText().clear(); // TODO: Fix bug 1745781
        getActivity().finish();
    }

    public static class ErrorDialogFragment extends DialogFragment {
        private int mTitleResId;
        private Integer mMessageResId;  // can be null

        private static final String ARG_TITLE_RES_ID = "argTitleResId";
        private static final String ARG_MESSAGE_RES_ID = "argMessageResId";

        public static ErrorDialogFragment newInstance(int titleResId) {
            return newInstanceInter(titleResId, null);
        }

        public static ErrorDialogFragment newInstance(int titleResId, int messageResId) {
            return newInstanceInter(titleResId, messageResId);
        }

        private static ErrorDialogFragment newInstanceInter(
                int titleResId, Integer messageResId) {
            final ErrorDialogFragment fragment = new ErrorDialogFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_TITLE_RES_ID, titleResId);
            if (messageResId != null) {
                args.putInt(ARG_MESSAGE_RES_ID, messageResId);
            }
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mTitleResId = getArguments().getInt(ARG_TITLE_RES_ID);
            if (getArguments().containsKey(ARG_MESSAGE_RES_ID)) {
                mMessageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(mTitleResId)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dismiss();
                                }
                            });
            if (mMessageResId != null) {
                builder.setMessage(mMessageResId);
            }
            return builder.create();
        }
    }

    /**
     * In most cases, when the dial button is pressed, there is a
     * number in digits area. Pack it in the intent, start the
     * outgoing call broadcast as a separate task and finish this
     * activity.
     *
     * When there is no digit and the phone is CDMA and off hook,
     * we're sending a blank flash for CDMA. CDMA networks use Flash
     * messages when special processing needs to be done, mainly for
     * 3-way or call waiting scenarios. Presumably, here we're in a
     * special 3-way scenario where the network needs a blank flash
     * before being able to add the new participant.  (This is not the
     * case with all 3-way calls, just certain CDMA infrastructures.)
     *
     * Otherwise, there is no digit, display the last dialed
     * number. Don't finish since the user may want to edit it. The
     * user needs to press the dial button again, to dial it (general
     * case described above).
     */
    /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
    //public void dialButtonPressed() {
    public void dialButtonPressed(int slot) {
    /*End: Modified by siliangqi for dial_directory 2012-5-23*/
        if (isDigitsEmpty()) { // No number entered.
            if (phoneIsCdma() && phoneIsOffhook()) {
                // This is really CDMA specific. On GSM is it possible
                // to be off hook and wanted to add a 3rd party using
                // the redial feature.
                startActivity(newFlashIntent());
            } else {
                if (!TextUtils.isEmpty(mLastNumberDialed)) {
                    // Recall the last number dialed.
                    mDigits.setText(mLastNumberDialed);

                    // ...and move the cursor to the end of the digits string,
                    // so you'll be able to delete digits using the Delete
                    // button (just as if you had typed the number manually.)
                    //
                    // Note we use mDigits.getText().length() here, not
                    // mLastNumberDialed.length(), since the EditText widget now
                    // contains a *formatted* version of mLastNumberDialed (due to
                    // mTextWatcher) and its length may have changed.
                    mDigits.setSelection(mDigits.getText().length());
                } else {
                    // There's no "last number dialed" or the
                    // background query is still running. There's
                    // nothing useful for the Dial button to do in
                    // this case.  Note: with a soft dial button, this
                    // can never happens since the dial button is
                    // disabled under these conditons.
                    playTone(ToneGenerator.TONE_PROP_NACK);
                }
            }
        } else {
            final String number = mDigits.getText().toString();

            // "persist.radio.otaspdial" is a temporary hack needed for one carrier's automated
            // test equipment.
            // TODO: clean it up.
            if (number != null
                    && !TextUtils.isEmpty(mProhibitedPhoneNumberRegexp)
                    && number.matches(mProhibitedPhoneNumberRegexp)
                    && (SystemProperties.getInt("persist.radio.otaspdial", 0) != 1)) {
                Log.i(TAG, "The phone number is prohibited explicitly by a rule.");
                if (getActivity() != null) {
                    DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                                    R.string.dialog_phone_call_prohibited_title);
                    dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
                }

                // Clear the digits just in case.
                mDigits.getText().clear();
            } else {
                final Intent intent = newDialNumberIntent(number);
                if (getActivity() instanceof DialtactsActivity) {
                    intent.putExtra(DialtactsActivity.EXTRA_CALL_ORIGIN,
                            DialtactsActivity.CALL_ORIGIN_DIALTACTS);
                }
                /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
                intent.putExtra("subscription", slot);
                intent.putExtra("directDial", true);
                /*End: Modified by siliangqi for dial_directory 2012-5-23*/
                startActivity(intent);
                mDigits.getText().clear();  // TODO: Fix bug 1745781
                getActivity().finish();
            }
        }
    }

    /**
     * Plays the specified tone for TONE_LENGTH_MS milliseconds.
     *
     * The tone is played locally, using the audio stream for phone calls.
     * Tones are played only if the "Audible touch tones" user preference
     * is checked, and are NOT played if the device is in silent mode.
     *
     * @param tone a tone code from {@link ToneGenerator}
     */
    void playTone(int tone) {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager =
                (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
            || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
                return;
            }

            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, TONE_LENGTH_MS);
        }
    }

    /**
     * Brings up the "dialpad chooser" UI in place of the usual Dialer
     * elements (the textfield/button and the dialpad underneath).
     *
     * We show this UI if the user brings up the Dialer while a call is
     * already in progress, since there's a good chance we got here
     * accidentally (and the user really wanted the in-call dialpad instead).
     * So in this situation we display an intermediate UI that lets the user
     * explicitly choose between the in-call dialpad ("Use touch tone
     * keypad") and the regular Dialer ("Add call").  (Or, the option "Return
     * to call in progress" just goes back to the in-call UI with no dialpad
     * at all.)
     *
     * @param enabled If true, show the "dialpad chooser" instead
     *                of the regular Dialer UI
     */
    private void showDialpadChooser(boolean enabled) {
        // Check if onCreateView() is already called by checking one of View objects.
        if (!isLayoutReady()) {
            return;
        }

        if (enabled) {
            // Log.i(TAG, "Showing dialpad chooser!");
            if (mDigitsContainer != null) {
                mDigitsContainer.setVisibility(View.GONE);
            } else {
                // mDigits is not enclosed by the container. Make the digits field itself gone.
                mDigits.setVisibility(View.GONE);
            }
            if (mDialpad != null) mDialpad.setVisibility(View.GONE);
            /*Begin: Modified by siliangqi for dialpad_bug 2012-6-13*/
            if (mAdditionalButtonsRow != null) mAdditionalButtonsRow.setVisibility(View.GONE);
            /*End: Modified by siliangqi for dialpad_bug 2012-6-13*/
            /* Start of siliangqi on 2012-3-15 17:40 dial_bg */
            // if (mDialButtonContainer != null)
            // mDialButtonContainer.setVisibility(View.GONE);
            /* End of siliangqi on 2012-3-15 17:40 dial_bg */
            mDialpadChooser.setVisibility(View.VISIBLE);

            // Instantiate the DialpadChooserAdapter and hook it up to the
            // ListView.  We do this only once.
            if (mDialpadChooserAdapter == null) {
                mDialpadChooserAdapter = new DialpadChooserAdapter(getActivity());
            }
            mDialpadChooser.setAdapter(mDialpadChooserAdapter);
        } else {
            // Log.i(TAG, "Displaying normal Dialer UI.");
            if (mDigitsContainer != null) {
                mDigitsContainer.setVisibility(View.VISIBLE);
            } else {
                mDigits.setVisibility(View.VISIBLE);
            }
            if (mDialpad != null) mDialpad.setVisibility(View.VISIBLE);
            /*Begin: Modified by siliangqi for dialpad_bug 2012-6-13*/
            if (mAdditionalButtonsRow != null) mAdditionalButtonsRow.setVisibility(View.VISIBLE);
            /*End: Modified by siliangqi for dialpad_bug 2012-6-13*/
            /* Start of siliangqi on 2012-3-15 17:40 dial_bg */
            // if (mDialButtonContainer != null)
            // mDialButtonContainer.setVisibility(View.VISIBLE);
            /* End of siliangqi on 2012-3-15 17:40 dial_bg */
            mDialpadChooser.setVisibility(View.GONE);
        }
    }

    /**
     * @return true if we're currently showing the "dialpad chooser" UI.
     */
    private boolean dialpadChooserVisible() {
        return mDialpadChooser.getVisibility() == View.VISIBLE;
    }

    /**
     * Simple list adapter, binding to an icon + text label
     * for each item in the "dialpad chooser" list.
     */
    private static class DialpadChooserAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        // Simple struct for a single "choice" item.
        static class ChoiceItem {
            String text;
            Bitmap icon;
            int id;

            public ChoiceItem(String s, Bitmap b, int i) {
                text = s;
                icon = b;
                id = i;
            }
        }

        // IDs for the possible "choices":
        static final int DIALPAD_CHOICE_USE_DTMF_DIALPAD = 101;
        static final int DIALPAD_CHOICE_RETURN_TO_CALL = 102;
        static final int DIALPAD_CHOICE_ADD_NEW_CALL = 103;

        private static final int NUM_ITEMS = 3;
        private ChoiceItem mChoiceItems[] = new ChoiceItem[NUM_ITEMS];

        public DialpadChooserAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);

            // Initialize the possible choices.
            // TODO: could this be specified entirely in XML?

            // - "Use touch tone keypad"
            mChoiceItems[0] = new ChoiceItem(
                    context.getString(R.string.dialer_useDtmfDialpad),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_tt_keypad),
                    DIALPAD_CHOICE_USE_DTMF_DIALPAD);

            // - "Return to call in progress"
            mChoiceItems[1] = new ChoiceItem(
                    context.getString(R.string.dialer_returnToInCallScreen),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_current_call),
                    DIALPAD_CHOICE_RETURN_TO_CALL);

            // - "Add call"
            mChoiceItems[2] = new ChoiceItem(
                    context.getString(R.string.dialer_addAnotherCall),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_add_call),
                    DIALPAD_CHOICE_ADD_NEW_CALL);
        }

        public int getCount() {
            return NUM_ITEMS;
        }

        /**
         * Return the ChoiceItem for a given position.
         */
        public Object getItem(int position) {
            return mChoiceItems[position];
        }

        /**
         * Return a unique ID for each possible choice.
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a view for each row.
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            // When convertView is non-null, we can reuse it (there's no need
            // to reinflate it.)
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.dialpad_chooser_list_item, null);
            }

            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText(mChoiceItems[position].text);

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageBitmap(mChoiceItems[position].icon);

            return convertView;
        }
    }

    /**
     * Handle clicks from the dialpad chooser.
     */
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        DialpadChooserAdapter.ChoiceItem item =
                (DialpadChooserAdapter.ChoiceItem) parent.getItemAtPosition(position);
        int itemId = item.id;
        switch (itemId) {
            case DialpadChooserAdapter.DIALPAD_CHOICE_USE_DTMF_DIALPAD:
                // Log.i(TAG, "DIALPAD_CHOICE_USE_DTMF_DIALPAD");
                // Fire off an intent to go back to the in-call UI
                // with the dialpad visible.
                returnToInCallScreen(true);
                break;

            case DialpadChooserAdapter.DIALPAD_CHOICE_RETURN_TO_CALL:
                // Log.i(TAG, "DIALPAD_CHOICE_RETURN_TO_CALL");
                // Fire off an intent to go back to the in-call UI
                // (with the dialpad hidden).
                returnToInCallScreen(false);
                break;

            case DialpadChooserAdapter.DIALPAD_CHOICE_ADD_NEW_CALL:
                // Log.i(TAG, "DIALPAD_CHOICE_ADD_NEW_CALL");
                // Ok, guess the user really did want to be here (in the
                // regular Dialer) after all.  Bring back the normal Dialer UI.
                showDialpadChooser(false);
                break;

            default:
                Log.w(TAG, "onItemClick: unexpected itemId: " + itemId);
                break;
        }
    }

    /**
     * Returns to the in-call UI (where there's presumably a call in
     * progress) in response to the user selecting "use touch tone keypad"
     * or "return to call" from the dialpad chooser.
     */
    private void returnToInCallScreen(boolean showDialpad) {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            try {
                ITelephonyMSim phone = ITelephonyMSim.Stub.asInterface(ServiceManager
                        .checkService(Context.MSIM_TELEPHONY_SERVICE));
                if (phone != null)
                    phone.showCallScreenWithDialpad(showDialpad);
            } catch (RemoteException e) {
                Log.w(TAG, "phone.showCallScreenWithDialpad() failed", e);
            }
        } else {
            try {
                ITelephony phone = ITelephony.Stub
                        .asInterface(ServiceManager.checkService("phone"));
                if (phone != null)
                    phone.showCallScreenWithDialpad(showDialpad);
            } catch (RemoteException e) {
                Log.w(TAG, "phone.showCallScreenWithDialpad() failed", e);
            }
        }

        // Finally, finish() ourselves so that we don't stay on the
        // activity stack.
        // Note that we do this whether or not the showCallScreenWithDialpad()
        // call above had any effect or not!  (That call is a no-op if the
        // phone is idle, which can happen if the current call ends while
        // the dialpad chooser is up.  In this case we can't show the
        // InCallScreen, and there's no point staying here in the Dialer,
        // so we just take the user back where he came from...)
        getActivity().finish();
    }

    /**
     * @return true if the phone is "in use", meaning that at least one line
     *              is active (ie. off hook or ringing or dialing).
     */
    public static boolean phoneIsInUse() {
        boolean phoneInUse = false;
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            try {
                ITelephonyMSim phone = ITelephonyMSim.Stub.asInterface(ServiceManager
                        .checkService(Context.MSIM_TELEPHONY_SERVICE));
                if (phone != null)
                    for (int i = 0; i < phoneCount; i++) {
                        phoneInUse = !phone.isIdle(i);
                        if (phoneInUse)
                            break;
                    }
            } catch (RemoteException e) {
                Log.w(TAG, "phone.isIdle() failed", e);
            }
        } else {
            try {
                ITelephony phone = ITelephony.Stub
                        .asInterface(ServiceManager.checkService("phone"));
                if (phone != null)
                    phoneInUse = !phone.isIdle();
            } catch (RemoteException e) {
                Log.w(TAG, "phone.isIdle() failed", e);
            }
        }
        return phoneInUse;
    }

    /**
     * @return true if the phone is a CDMA phone type
     */
    private boolean phoneIsCdma() {
        boolean isCdma = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) {
                isCdma = (phone.getActivePhoneType() == TelephonyManager.PHONE_TYPE_CDMA);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "phone.getActivePhoneType() failed", e);
        }
        return isCdma;
    }

    /**
     * @return true if the phone state is OFFHOOK
     */
    private boolean phoneIsOffhook() {
        boolean phoneOffhook = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) phoneOffhook = phone.isOffhook();
        } catch (RemoteException e) {
            Log.w(TAG, "phone.isOffhook() failed", e);
        }
        return phoneOffhook;
    }

    /**
     * Returns true whenever any one of the options from the menu is selected.
     * Code changes to support dialpad options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_2s_pause:
                updateDialString(",");
                return true;
            case R.id.menu_add_wait:
                updateDialString(";");
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    /**
     * Updates the dial string (mDigits) after inserting a Pause character (,)
     * or Wait character (;).
     */
    private void updateDialString(String newDigits) {
        int selectionStart;
        int selectionEnd;

        // SpannableStringBuilder editable_text = new SpannableStringBuilder(mDigits.getText());
        int anchor = mDigits.getSelectionStart();
        int point = mDigits.getSelectionEnd();

        selectionStart = Math.min(anchor, point);
        selectionEnd = Math.max(anchor, point);

        Editable digits = mDigits.getText();
        if (selectionStart != -1) {
            if (selectionStart == selectionEnd) {
                // then there is no selection. So insert the pause at this
                // position and update the mDigits.
                digits.replace(selectionStart, selectionStart, newDigits);
            } else {
                digits.replace(selectionStart, selectionEnd, newDigits);
                // Unselect: back to a regular cursor, just pass the character inserted.
                mDigits.setSelection(selectionStart + 1);
            }
        } else {
            int len = mDigits.length();
            digits.replace(len, len, newDigits);
        }
    }

    /**
     * Update the enabledness of the "Dial" and "Backspace" buttons if applicable.
     */
    private void updateDialAndDeleteButtonEnabledState() {
        final boolean digitsNotEmpty = !isDigitsEmpty();

        /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
        //if (mDialButton != null) {
        if (cDialButton != null&&cDialButton != null) {
        /*End: Modified by siliangqi for dial_directory 2012-5-23*/
            // On CDMA phones, if we're already on a call, we *always*
            // enable the Dial button (since you can press it without
            // entering any digits to send an empty flash.)
            if (phoneIsCdma() && phoneIsOffhook()) {
                /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
                //mDialButton.setEnabled(true);
                if(!TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("ABSENT"))
                    cDialButton.setEnabled(true);
                if(!TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("ABSENT"))
                    gDialButton.setEnabled(true);
                /*End: Modified by siliangqi for dial_directory 2012-5-23*/
            } else {
                // Common case: GSM, or CDMA but not on a call.
                // Enable the Dial button if some digits have
                // been entered, or if there is a last dialed number
                // that could be redialed.
                /*Begin: Modified by siliangqi for dial_directory 2012-5-23*/
                /*mDialButton.setEnabled(digitsNotEmpty ||
                        !TextUtils.isEmpty(mLastNumberDialed));*/
                if(!TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("ABSENT"))
                    cDialButton.setEnabled(digitsNotEmpty ||
                            !TextUtils.isEmpty(mLastNumberDialed));
                if(!TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("ABSENT"))
                    gDialButton.setEnabled(digitsNotEmpty ||
                            !TextUtils.isEmpty(mLastNumberDialed));
                /*End: Modified by siliangqi for dial_directory 2012-5-23*/
            }
        }
        /* Start of siliangqi on 2012-3-17 11:2 dial_bg */
        // mDelete.setEnabled(digitsNotEmpty);
        if (digitsNotEmpty) {
            mDelete.setVisibility(View.VISIBLE);
        } else {
            mDelete.setVisibility(View.GONE);
        }
        /* End of siliangqi on 2012-3-17 11:2 dial_bg */
    }

    /**
     * Check if voicemail is enabled/accessible.
     *
     * @return true if voicemail is enabled and accessibly. Note that this can be false
     * "temporarily" after the app boot.
     * @see MSimTelephonyManager#getVoiceMailNumber()
     */
    private boolean isVoicemailAvailable() {
        boolean promptEnabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MULTI_SIM_VOICE_PROMPT, 0) == 1;
        Log.d(TAG, "prompt enabled :  "+ promptEnabled);
        if (promptEnabled) {
            return hasVMNumber();
        } else {
            try {
                mSubscription = MSimTelephonyManager.getDefault().getPreferredVoiceSubscription();
                return (MSimTelephonyManager.getDefault().getVoiceMailNumber(mSubscription) != null);
            } catch (SecurityException se) {
                // Possibly no READ_PHONE_STATE privilege.
                Log.w(TAG, "SecurityException is thrown. Maybe privilege isn't sufficient.");
            }
        }
        return false;
    }

    private boolean hasVMNumber() {
        boolean hasVMNum = false;
        int phoneCount = MSimTelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < phoneCount; i++) {
            try {
                hasVMNum = MSimTelephonyManager.getDefault().getVoiceMailNumber(i) != null;
            } catch (SecurityException se) {
                // Possibly no READ_PHONE_STATE privilege.
            }
            if (hasVMNum) {
                break;
            }
        }
        return hasVMNum;
    }

    /**
     * This function return true if Wait menu item can be shown
     * otherwise returns false. Assumes the passed string is non-empty
     * and the 0th index check is not required.
     */
    private static boolean showWait(int start, int end, String digits) {
        if (start == end) {
            // visible false in this case
            if (start > digits.length()) return false;

            // preceding char is ';', so visible should be false
            if (digits.charAt(start - 1) == ';') return false;

            // next char is ';', so visible should be false
            if ((digits.length() > start) && (digits.charAt(start) == ';')) return false;
        } else {
            // visible false in this case
            if (start > digits.length() || end > digits.length()) return false;

            // In this case we need to just check for ';' preceding to start
            // or next to end
            if (digits.charAt(start - 1) == ';') return false;
        }
        return true;
    }

    /**
     * @return true if the widget with the phone number digits is empty.
     */
    private boolean isDigitsEmpty() {
        return mDigits.length() == 0;
    }

    /**
     * Starts the asyn query to get the last dialed/outgoing
     * number. When the background query finishes, mLastNumberDialed
     * is set to the last dialed number or an empty string if none
     * exists yet.
     */
    private void queryLastOutgoingCall() {
        mLastNumberDialed = EMPTY_NUMBER;
        CallLogAsync.GetLastOutgoingCallArgs lastCallArgs =
                new CallLogAsync.GetLastOutgoingCallArgs(
                    getActivity(),
                    new CallLogAsync.OnLastOutgoingCallComplete() {
                        public void lastOutgoingCall(String number) {
                            // TODO: Filter out emergency numbers if
                            // the carrier does not want redial for
                            // these.
                            mLastNumberDialed = number;
                            updateDialAndDeleteButtonEnabledState();
                        }
                    });
        mCallLog.getLastOutgoingCall(lastCallArgs);
    }

    // Helpers for the call intents.
    private Intent newVoicemailIntent() {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                         Uri.fromParts("voicemail", EMPTY_NUMBER, null));
        intent.putExtra("phone_subscription", mSubscription);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private Intent newFlashIntent() {
        final Intent intent = newDialNumberIntent(EMPTY_NUMBER);
        intent.putExtra(EXTRA_SEND_EMPTY_FLASH, true);
        return intent;
    }

    private Intent newDialNumberIntent(String number) {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                         Uri.fromParts("tel", number, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onVisibilityChanged(boolean fragmentVisible) {
        mShowOptionsMenu = fragmentVisible;
        updateFakeMenuButtonsVisibility(fragmentVisible);
    }

    /**
     * Update visibility of the search button and menu button at the bottom of dialer screen, which
     * should be invisible when bottom ActionBar's real items are available and be visible
     * otherwise.
     *
     * @param visible True when visible.
     */
    public void updateFakeMenuButtonsVisibility(boolean visible) {
        if (DEBUG) Log.d(TAG, "updateFakeMenuButtonVisibility(" + visible + ")");
        /* Start of siliangqi on 2012-3-16 8:38 dial_bg */
        /*
         * if (mSearchButton != null) { if (visible) {
         * mSearchButton.setVisibility(View.VISIBLE); } else {
         * mSearchButton.setVisibility(View.INVISIBLE); } }
         */
        /* End of siliangqi on 2012-3-16 8:38 dial_bg */
        if (mMenuButton != null) {
            if (visible && !ViewConfiguration.get(getActivity()).hasPermanentMenuKey()) {
                mMenuButton.setVisibility(View.VISIBLE);
            } else {
                /* Start of siliangqi on 2012-3-15 17:40 dial_bg */
                // mMenuButton.setVisibility(View.INVISIBLE);
                mMenuButton.setVisibility(View.GONE);
                /* End of siliangqi on 2012-3-15 17:40 dial_bg */
            }
        }
    }

    private void callSpeedNumber(int id) {
        SpeedDialUtils speedDialUtils = new SpeedDialUtils(getActivity());
        int numId = 0;
        String speedNumber;
        Log.d("loky","call speed number");
        switch(id)
        {
        case R.id.two:
            numId = speedDialUtils.NUM_TWO;
            break;
        case R.id.three:
            numId = speedDialUtils.NUM_THREE;
            break;
        case R.id.four:
            numId = speedDialUtils.NUM_FOUR;
            break;
        case R.id.five:
            numId = speedDialUtils.NUM_FIVE;
            break;
        case R.id.six:
            numId = speedDialUtils.NUM_SIX;
            break;
        case R.id.seven:
            numId = speedDialUtils.NUM_SEVEN;
            break;
        case R.id.eight:
            numId = speedDialUtils.NUM_EIGHT;
            break;
        case R.id.nine:
            numId = speedDialUtils.NUM_NINE;
            break;
        }
        int rawContactId = speedDialUtils.getRawContactId(numId);
        Log.d("loky","raw contact id="+rawContactId);
        speedNumber = speedDialUtils.getSpeedDialInfo(rawContactId, speedDialUtils.INFO_NUMBER);
        Log.d("loky","speed number="+speedNumber);
        if(speedNumber == null) {
            showNoSpeedNumberDialog(numId);
        }else {
            Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED);
            intent.setData(Uri.fromParts("tel", speedNumber, null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void showNoSpeedNumberDialog(int numId) {
        //because numId start from 0, but numKey from 2 that represent num 2-9, so here add two
        int numKey = numId + 2;
        String dialogTxt = getString(R.string.is_set_speed, String.valueOf(numKey));
        final Activity thisActivity = getActivity();
        new AlertDialog.Builder(thisActivity).setTitle(R.string.dialog_title).setMessage(dialogTxt)
          .setPositiveButton(android.R.string.ok,
                 new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                         // TODO Auto-generated method stub
                         //go to speed dial setting screen, in this screen, the user can set speed dial number.
                         Intent intent = new Intent(thisActivity, SpeedDialListActivity.class);
                         startActivity(intent);
                     }
                }).setNegativeButton(android.R.string.cancel,null)
                .show();
    }

}
