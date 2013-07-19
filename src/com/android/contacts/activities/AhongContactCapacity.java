
/*Begin: Modified by wqiang for MAX_CONTACT_CAPABILITY 2012/08/11*//*End: Modified by wqiang for MAX_CONTACT_CAPABILITY 2012/08/11*/
package com.android.contacts.activities;

import java.util.List;

import com.android.contacts.R;

import com.android.contacts.SimContactsConstants;
import com.android.internal.telephony.MSimIccPhoneBookInterfaceManagerProxy;
import android.telephony.MSimTelephonyManager;
import android.app.ActionBar;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IIccPhoneBookMSim;
import com.android.internal.telephony.IccProvider;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.AdnRecord;
import com.android.internal.telephony.IIccPhoneBook;
import android.os.ServiceManager;

public class AhongContactCapacity extends Activity {

    public static final int MAX_CONTACT_TOTAL_COUNT = 5000;

    private TextView mTextView01,mTextView02,mTextView03,
    mTextView04,mTextView05,mTextView06,mTextView07,mTextView08;

    private ProgressBar mProgressBar01,mProgressBar02,mProgressBar03;
    private ImageView mImageView01,mImageView02,mImageView03;
    private String cCardState;
    private String gCardState;
    private LinearLayout mLinearLayout01,mLinearLayout02,mLinearLayout03;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_capacity);
        cCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",
                0, "");
        gCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",
                1, "");
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_actionbar_bg));
        }
        mTextView01 = (TextView) findViewById(R.id.Textview01);
        mTextView02 = (TextView) findViewById(R.id.Textview02);
        mTextView03 = (TextView) findViewById(R.id.Textview03);
        mTextView04 = (TextView) findViewById(R.id.Textview04);
        mTextView05 = (TextView) findViewById(R.id.Textview05);
        mTextView06 = (TextView) findViewById(R.id.Textview06);
        mTextView07 = (TextView) findViewById(R.id.Textview07);
        mTextView08 = (TextView) findViewById(R.id.Textview08);
        mProgressBar01 = (ProgressBar) findViewById(R.id.ProgressBar01);
        mProgressBar02 = (ProgressBar) findViewById(R.id.ProgressBar02);
        mProgressBar03 = (ProgressBar) findViewById(R.id.ProgressBar03);
        mImageView01= (ImageView) findViewById(R.id.ImageView01);
        mImageView02= (ImageView) findViewById(R.id.ImageView02);
        mImageView03= (ImageView) findViewById(R.id.ImageView03);

        mTextView01.setText(R.string.phone_contact_capacity);
        mTextView01.setTextColor(Color.WHITE);
        mLinearLayout01= (LinearLayout)findViewById(R.id.LinearLayout01);
        mLinearLayout02= (LinearLayout)findViewById(R.id.LinearLayout02);
        mLinearLayout03= (LinearLayout)findViewById(R.id.LinearLayout03);

        mTextView02.setText(getPhoneContactsTotalCount() + "/"
                + MAX_CONTACT_TOTAL_COUNT);
        mTextView02.setTextColor(Color.WHITE);
        mImageView01.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_phone_holo_light));
        mImageView01.setVisibility(View.VISIBLE);
        mProgressBar01.setIndeterminate(false);
        mProgressBar01.setVisibility(View.VISIBLE);
        mProgressBar01.setMax(MAX_CONTACT_TOTAL_COUNT);
        mProgressBar01.setProgress(getPhoneContactsTotalCount());
        if (!cCardState.equals("READY")) {
            mLinearLayout02.setVisibility(View.GONE);
        } else {
            Log.e("wangqqiang", "wangqiang1");
            mLinearLayout02.setVisibility(View.VISIBLE);
            mTextView03.setBackgroundColor(Color.BLACK);
            mTextView04.setText(R.string.sim1_contact_capacity);
            mTextView04.setTextColor(Color.WHITE);
            mTextView05.setText(getCDMASimContactTotalCount() + "/"
                    + getCDMASimCapacity());
            mTextView05.setTextColor(Color.WHITE);
            mImageView02.setImageDrawable(getResources().getDrawable(R.drawable.ic_contact_list_sim1));
            mImageView02.setVisibility(View.VISIBLE);
            mProgressBar02.setIndeterminate(false);
            mProgressBar02.setVisibility(View.VISIBLE);
            mProgressBar02.setMax(getCDMASimCapacity());
            mProgressBar02.setProgress(getCDMASimContactTotalCount());
        }
        if (!gCardState.equals("READY")) {
            mLinearLayout03.setVisibility(View.GONE);
        } else {
            mLinearLayout03.setVisibility(View.VISIBLE);
            mTextView06.setBackgroundColor(Color.BLACK);
            mTextView07.setText(R.string.sim2_contact_capacity);
            mTextView07.setTextColor(Color.WHITE);
            mTextView08.setText(getGSMSimContactTotalCount() + "/"
                    + getGSMSimCapacity());
            mTextView08.setTextColor(Color.WHITE);
            mImageView03.setImageDrawable(getResources().getDrawable(R.drawable.ic_contact_list_sim2));
            mImageView03.setVisibility(View.VISIBLE);
            mProgressBar03.setIndeterminate(false);
            mProgressBar03.setVisibility(View.VISIBLE);
            mProgressBar03.setMax(getGSMSimCapacity());
            mProgressBar03.setProgress(getGSMSimContactTotalCount());
        }
    }

    public int getCDMASimContactTotalCount() {
        int TotalCount = 0;
        Uri uri = Uri.parse("content://iccmsim/adn");
        Cursor cursor = this.getContentResolver().query(uri, null, null, null,
                null);
        while (cursor.moveToNext()) {
            TotalCount++;
        }
        return TotalCount;
    }

    public int getGSMSimContactTotalCount() {
        int TotalCount = 0;
        Uri uri = Uri.parse("content://iccmsim/adn_sub2");
        Cursor cursor = this.getContentResolver().query(uri, null, null, null,
                null);
        while (cursor.moveToNext()) {
            TotalCount++;
        }
        return TotalCount;
    }

    public int getPhoneContactsTotalCount() {
        int TotalCount = 0;
        String[] projection = { RawContacts._ID, RawContacts.ACCOUNT_TYPE };
        String[] selectionArgs = { SimContactsConstants.ACCOUNT_TYPE_PHONE };
        Cursor cursor = getContentResolver().query(Contacts.CONTENT_URI,
                projection, RawContacts.ACCOUNT_TYPE + "=?", selectionArgs,
                null);
        while (cursor.moveToNext()) {
            TotalCount++;
        }
        return TotalCount;
    }

    public int getGSMSimCapacity() {
        List<AdnRecord> adnRecords = null;
        try {
            IIccPhoneBookMSim iccIpb = IIccPhoneBookMSim.Stub
                    .asInterface(ServiceManager.getService("simphonebook_msim"));
            if (iccIpb != null) {
                adnRecords = iccIpb.getAdnRecordsInEfOnSubscription(
                        IccConstants.EF_ADN, 1);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (false)
                Log.e("wangqiang", ex.toString());
        }
        return adnRecords.size();
    }

    public int getCDMASimCapacity() {
        List<AdnRecord> adnRecords = null;
        try {
            IIccPhoneBookMSim iccIpb = IIccPhoneBookMSim.Stub
                    .asInterface(ServiceManager.getService("simphonebook_msim"));
            if (iccIpb != null) {
                adnRecords = iccIpb.getAdnRecordsInEfOnSubscription(
                        IccConstants.EF_ADN, 0);
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            if (false)
                Log.e("wangqiang", ex.toString());
        }
        return adnRecords.size();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // We have two logical "up" Activities: People and Phone.
                // Instead of having one static "up" direction, behave like back as an
                // exceptional case.
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
/*End: Modified by wqiang for MAX_CONTACT_CAPABILITY 2012/08/11*/