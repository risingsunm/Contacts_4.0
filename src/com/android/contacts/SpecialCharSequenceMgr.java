/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.contacts;

import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.dialpad.DialpadFragment;
import com.android.internal.telephony.ITelephonyMSim;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.Intents;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneNumberUtils;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.StringBuilder;
/*zhanglu.hoperun 2012.7.4 add some import*/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import android.os.Build;
import android.telephony.cdma.CdmaCellLocation;
import android.content.ContentResolver;
import android.database.Cursor;
/*zhanglu.hoperun 2012.7.11 end*/

/**
 * Helper class to listen for some magic character sequences
 * that are handled specially by the dialer.
 *
 * Note the Phone app also handles these sequences too (in a couple of
 * relativly obscure places in the UI), so there's a separate version of
 * this class under apps/Phone.
 *
 * TODO: there's lots of duplicated code between this class and the
 * corresponding class under apps/Phone.  Let's figure out a way to
 * unify these two classes (in the framework? in a common shared library?)
 */
public class SpecialCharSequenceMgr {
    private static final String TAG = "SpecialCharSequenceMgr";
    private static final String MMI_IMEI_DISPLAY = "*#06#";
    /*zhanglu.hoperun 2012.7.4 add String TELECOM_DISPLAY and FILENAME_MSV*/
    private static final String TELECOM_DISPLAY = "*#0000#";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";
    private static ContentResolver contentResolver = null;
    private final static String CONTENT_URI_INFO="content://com.ahong.MyContentProvider/info";
    private static Uri uri = Uri.parse(CONTENT_URI_INFO);
    /*zhanglu.hoperun 2012.7.11 end*/
    private static final int SUB1 = 0;
    private static final int SUB2 = 1;

/*begin: mzikun add for ahong input handle*/
    private static final boolean DEBUG_CONST = true;
    private static final String AHONG_SECRET_CODE_ACTION="com.android.contacts.SPECIALCHARSEQUENCEMGR.ACTION";
    private static final String[] AHONGINPUTCODES={"*#0000#","*#2005808#"};
/*end:mzikun add for ahong input handle*/

    /** This class is never instantiated. */
    private SpecialCharSequenceMgr() {
    }

    public static boolean handleChars(Context context, String input, EditText textField) {
        return handleChars(context, input, false, textField);
    }

    static boolean handleChars(Context context, String input) {
        return handleChars(context, input, false, null);
    }

/*begin:mzikun add for ahong input handle*/
    static boolean ahongInputHandle(Context context, String input) {
        // Secret codes are in the form *#<code>#
        int len = input.length();

        if(DEBUG_CONST){
            Log.d(TAG, "ahongInputHandle");
        }

        for(int i=0;i<AHONGINPUTCODES.length;i++){
            if(DEBUG_CONST){
                Log.d(TAG, "ahongInputHandle->for(int i=0;i<AHONGINPUTCODES.length;i++)");
            }

            //AHONGINPUTCODES->Intent;
            if (input.equals(AHONGINPUTCODES[i]) && len < 11
                    && input.startsWith("*#") && input.endsWith("#")) {
                if(DEBUG_CONST){
                    Log.d(TAG, "ahongInputHandle->if (input.equals(AHONGINPUTCODES[i])");
                }

                Intent intent = new Intent(AHONG_SECRET_CODE_ACTION,
                Uri.parse("ahong_dialog_secret_code://" + input.substring(2, len - 1)));
                context.sendBroadcast(intent);

                return true;
            }
        }
        return false;
    }
/*end:mzikun add for ahong input handle*/

    static boolean handleChars(Context context, String input, boolean useSystemWindow,
            EditText textField) {

        //get rid of the separators so that the string gets parsed correctly
        String dialString = PhoneNumberUtils.stripSeparators(input);

/*begin:mzikun add for ahong input handle*/
        if(ahongInputHandle(context,dialString)){
            if(DEBUG_CONST){
                Log.d(TAG, "handleChars->ahongInputHandle");
            }
            return true;
        }
/*end:mzikun add for ahong input handle*/

        if (handleIMEIDisplay(context, dialString, useSystemWindow)
                /*zhanglu.hoperun 2012.7.4 add if condition handleTelecomDisplay
                || handleTelecomDisplay(context, dialString, useSystemWindow)*/
                || handlePinEntry(context, dialString)
                || handleAdnEntry(context, dialString, textField)
                || handleSecretCode(context, dialString)) {
            return true;
        }

        return false;
    }

    /**
     * Handles secret codes to launch arbitrary activities in the form of *#*#<code>#*#*.
     * If a secret code is encountered an Intent is started with the android_secret_code://<code>
     * URI.
     *
     * @param context the context to use
     * @param input the text to check for a secret code in
     * @return true if a secret code was encountered
     */
    static boolean handleSecretCode(Context context, String input) {
        // Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 8 && input.startsWith("*#*#") && input.endsWith("#*#*")) {
            Intent intent = new Intent(Intents.SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + input.substring(4, len - 4)));
            context.sendBroadcast(intent);
            return true;
        }

        return false;
    }

    /**
     * Handle ADN requests by filling in the SIM contact number into the requested
     * EditText.
     *
     * This code works alongside the Asynchronous query handler {@link QueryHandler}
     * and query cancel handler implemented in {@link SimContactQueryCookie}.
     */
    static boolean handleAdnEntry(Context context, String input, EditText textField) {
        /* ADN entries are of the form "N(N)(N)#" */

        // if the phone is keyguard-restricted, then just ignore this
        // input.  We want to make sure that sim card contacts are NOT
        // exposed unless the phone is unlocked, and this code can be
        // accessed from the emergency dialer.
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.inKeyguardRestrictedInputMode()) {
            return false;
        }

        int len = input.length();
        int subscription = 0;
        Uri uri = null;

        if ((len > 1) && (len < 5) && (input.endsWith("#"))) {
            try {
                // get the ordinal number of the sim contact
                int index = Integer.parseInt(input.substring(0, len-1));

                // The original code that navigated to a SIM Contacts list view did not
                // highlight the requested contact correctly, a requirement for PTCRB
                // certification.  This behaviour is consistent with the UI paradigm
                // for touch-enabled lists, so it does not make sense to try to work
                // around it.  Instead we fill in the the requested phone number into
                // the dialer text field.

                // create the async query handler
                QueryHandler handler = new QueryHandler (context.getContentResolver());

                // create the cookie object
                SimContactQueryCookie sc = new SimContactQueryCookie(index - 1, handler,
                        ADN_QUERY_TOKEN);

                // setup the cookie fields
                sc.contactNum = index - 1;
                sc.setTextField(textField);

                // create the progress dialog
                sc.progressDialog = new ProgressDialog(context);
                sc.progressDialog.setTitle(R.string.simContacts_title);
                sc.progressDialog.setMessage(context.getText(R.string.simContacts_emptyLoading));
                sc.progressDialog.setIndeterminate(true);
                sc.progressDialog.setCancelable(true);
                sc.progressDialog.setOnCancelListener(sc);
                sc.progressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

                // display the progress dialog
                sc.progressDialog.show();
                subscription = MSimTelephonyManager.getDefault().getPreferredVoiceSubscription();

                if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
                    if(subscription == SUB1) {
                        uri = Uri.parse("content://iccmsim/adn");
                    } else if (subscription == SUB2) {
                        uri = Uri.parse("content://iccmsim/adn_sub2");
                    } else {
                        Log.d(TAG, "handleAdnEntry:Invalid Subscription");
                    }
                } else {
                    uri = Uri.parse("content://icc/adn");
                }

                // run the query.
                handler.startQuery(ADN_QUERY_TOKEN, sc, uri,
                        new String[]{ADN_PHONE_NUMBER_COLUMN_NAME}, null, null, null);
                return true;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
        return false;
    }

    static boolean handlePinEntry(Context context, String input) {
        int subscription = 0;
        if ((input.startsWith("**04") || input.startsWith("**05")) && input.endsWith("#")) {
            try {
                // Use Voice Subscription for both change PIN & unblock PIN using PUK.
                subscription = MSimTelephonyManager.getDefault().getPreferredVoiceSubscription();
                Log.d(TAG, "Sending MMI on subscription :" + subscription);
                if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                    return ITelephonyMSim.Stub.asInterface(ServiceManager.getService("phone_msim"))
                            .handlePinMmi(input, subscription);
                } else {
                    return ITelephony.Stub.asInterface(ServiceManager.getService("phone"))
                            .handlePinMmi(input);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to handlePinMmi due to remote exception");
                return false;
            }
        }
        return false;
    }

    static boolean handleIMEIDisplay(Context context, String input, boolean useSystemWindow) {
        if (input.equals(MMI_IMEI_DISPLAY)) {
            showDeviceIdPanel(context);
            return true;
        }
        return false;
    }

    static private void showDeviceIdPanel(Context context) {
        Log.d(TAG, "showDeviceIdPanel()...");

        int labelId;
        StringBuilder deviceId = null;
        TelephonyManager tm = TelephonyManager.getDefault();

        if (tm.isMultiSimEnabled()) {
            MSimTelephonyManager mtm = (MSimTelephonyManager)tm;
            labelId = R.string.imei;

            int phonecount = mtm.getPhoneCount();
            int[] type = new int[phonecount];
            String[] ids = new String[phonecount];
            boolean multimode = false;
            for (int i=0; i<phonecount; i++) {
                type[i] = mtm.getCurrentPhoneType(i);
                ids[i] = mtm.getDeviceId(i);
                if (type[i] == TelephonyManager.PHONE_TYPE_CDMA) {
                    // C+G mode
                    multimode = true;
                    labelId = R.string.device_id;
                }
            }

            // 16 IMEI characters, or 7 MEID characters, maybe plus subscription name
            deviceId = new StringBuilder(50);
            for (int i=0; i<phonecount; i++) {
                if (multimode) {
                    String prefix =
                        (type[i] == TelephonyManager.PHONE_TYPE_GSM)
                        ? "IMEI " : "MEID ";
                    deviceId.append(prefix);
                }
                deviceId.append(ids[i] == null ? "" : ids[i]);
                if (i != mtm.getPhoneCount()-1) {
                    deviceId.append("\n");
                }
            }
        } else {
            int type = tm.getCurrentPhoneType();
            labelId = (type == TelephonyManager.PHONE_TYPE_GSM)
                ? R.string.imei : R.string.meid;
            deviceId = new StringBuilder();
            deviceId.append(tm.getDeviceId());
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle(labelId)
            .setMessage(deviceId.toString())
            .setPositiveButton(android.R.string.ok, null)
            .setCancelable(true)
            .show();
    }

    /*zhanglu.hoperun 2012.7.4 add codes to show some informations when dail *#0000# */
    static boolean handleTelecomDisplay(Context context, String input, boolean useSystemWindow) {
        if (input.equals(TELECOM_DISPLAY)) {
            showTelecomDevicePanel(context);
            return true;
        }
        return false;
    }

    static private void showTelecomDevicePanel(Context context) {
        Log.d(TAG, "showDeviceIdPanel()...");
        
        contentResolver = context.getContentResolver();
        int labelId = R.string.device_id;
        StringBuilder telecomInfo = new StringBuilder();
        //TelephonyManager tm = TelephonyManager.getDefault();
        //MSimTelephonyManager mtm = (MSimTelephonyManager)tm;
        MSimTelephonyManager mtm = (MSimTelephonyManager)context.getSystemService(Context.MSIM_TELEPHONY_SERVICE);
        int indexOfCDMA = 0;
        CdmaCellLocation location = null;
        String prlVersion = "";
        String uimId = "";

        telecomInfo.append("Model "+Build.MODEL + getMsvSuffix()+"\n");
        telecomInfo.append("Software Version "+Build.DISPLAY+"\n");

        Cursor cursor = contentResolver.query(uri, null, "id=?", new String[]{"1"}, null);
        if(cursor.moveToNext()) {
            prlVersion = cursor.getString(cursor.getColumnIndex("prl"));
            uimId = cursor.getString(cursor.getColumnIndex("esn"));
            }
        cursor.close();
        telecomInfo.append("UIM ID "+uimId+"\n");
        telecomInfo.append("PrlVersion "+prlVersion+"\n");
        telecomInfo.append("MEID "+mtm.getDeviceId(indexOfCDMA)+"\n");
        location = (CdmaCellLocation) mtm.getCdmaCellLocation(indexOfCDMA);
        telecomInfo.append("SID "+location.getSystemId()+"\n");
        telecomInfo.append("NID "+location.getNetworkId()+"\n");

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle(labelId)
            .setMessage(telecomInfo.toString())
            .setPositiveButton(android.R.string.ok, null)
            .setCancelable(true)
            .show();
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     * @return a string to append to the model number description.
     */
    private static String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }
    /*zhanglu.hoperun 2012.7.11 end*/

    /*******
     * This code is used to handle SIM Contact queries
     *******/
    private static final String ADN_PHONE_NUMBER_COLUMN_NAME = "number";
    private static final String ADN_NAME_COLUMN_NAME = "name";
    private static final int ADN_QUERY_TOKEN = -1;

    /**
     * Cookie object that contains everything we need to communicate to the
     * handler's onQuery Complete, as well as what we need in order to cancel
     * the query (if requested).
     *
     * Note, access to the textField field is going to be synchronized, because
     * the user can request a cancel at any time through the UI.
     */
    private static class SimContactQueryCookie implements DialogInterface.OnCancelListener{
        public ProgressDialog progressDialog;
        public int contactNum;

        // Used to identify the query request.
        private int mToken;
        private QueryHandler mHandler;

        // The text field we're going to update
        private EditText textField;

        public SimContactQueryCookie(int number, QueryHandler handler, int token) {
            contactNum = number;
            mHandler = handler;
            mToken = token;
        }

        /**
         * Synchronized getter for the EditText.
         */
        public synchronized EditText getTextField() {
            return textField;
        }

        /**
         * Synchronized setter for the EditText.
         */
        public synchronized void setTextField(EditText text) {
            textField = text;
        }

        /**
         * Cancel the ADN query by stopping the operation and signaling
         * the cookie that a cancel request is made.
         */
        public synchronized void onCancel(DialogInterface dialog) {
            // close the progress dialog
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            // setting the textfield to null ensures that the UI does NOT get
            // updated.
            textField = null;

            // Cancel the operation if possible.
            mHandler.cancelOperation(mToken);
        }
    }

    /**
     * Asynchronous query handler that services requests to look up ADNs
     *
     * Queries originate from {@link handleAdnEntry}.
     */
    private static class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * Override basic onQueryComplete to fill in the textfield when
         * we're handed the ADN cursor.
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            SimContactQueryCookie sc = (SimContactQueryCookie) cookie;

            // close the progress dialog.
            sc.progressDialog.dismiss();

            // get the EditText to update or see if the request was cancelled.
            EditText text = sc.getTextField();

            // if the textview is valid, and the cursor is valid and postionable
            // on the Nth number, then we update the text field and display a
            // toast indicating the caller name.
            if ((c != null) && (text != null) && (c.moveToPosition(sc.contactNum))) {
                String name = c.getString(c.getColumnIndexOrThrow(ADN_NAME_COLUMN_NAME));
                String number = c.getString(c.getColumnIndexOrThrow(ADN_PHONE_NUMBER_COLUMN_NAME));

                // fill the text in.
                text.getText().replace(0, 0, number);

                // display the name as a toast
                Context context = sc.progressDialog.getContext();
                name = context.getString(R.string.menu_callNumber, name);
                Toast.makeText(context, name, Toast.LENGTH_SHORT)
                    .show();
            }
        }
    }
}
