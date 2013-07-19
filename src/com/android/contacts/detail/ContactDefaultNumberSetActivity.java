
package com.android.contacts.detail;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.interactions.PhoneNumberInteraction.PhoneItem;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;

/*Begin: Modified by xiepengfei for set default phone 2012/06/09*/
public class ContactDefaultNumberSetActivity extends Activity implements
        OnLoadCompleteListener<Cursor> {
    private Uri mLookUpKey;
    private CursorLoader mLoader;
    private PhoneItemAdapter ada;

    ArrayList<PhoneItem> phoneList = new ArrayList<PhoneItem>();
    private static String primaryPhone = "";

    private boolean isDisplay = false;

    private static final String PHONE_NUMBER_SELECTION = Data.MIMETYPE + "='"
            + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.NUMBER + " NOT NULL";

    private static final String[] PHONE_NUMBER_PROJECTION = new String[] {
            Phone._ID,
            Phone.NUMBER,
            Phone.IS_SUPER_PRIMARY,
            RawContacts.ACCOUNT_TYPE,
            RawContacts.DATA_SET,
            Phone.TYPE,
            Phone.LABEL
    };
    private ListView mListView;

    /* Begin: Modified by xiepengfei for set default phone 2012/06/09 */

    /* End: Modified by xiepengfei for set default phone 2012/06/09 */
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        mLookUpKey = getIntent().getData();
        setContentView(R.layout.contact_detail_set_default_phone);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_SHOW_TITLE);
        getActionBar().setTitle(R.string.contact_detail_menu_set_default_phone_number);
        mListView = (ListView) findViewById(R.id.listview);
        startInteraction(mLookUpKey);
        isDisplay = true;
        ada = new PhoneItemAdapter(this, phoneList);
        mListView.setAdapter(ada);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                System.out.println("onItemClick        " + arg2);
                ada.setItemChecked(arg2);

            }
        });

        /*Begin: Modified by xiepengfei for add bottom toolbar 2012/06/13*/
        Button ok = (Button) findViewById(R.id.btn_ok);
        ok.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
              if(ada!= null && ada.mCheckedPosition != -1){
                  final PhoneItem phoneItem = phoneList.get(ada.mCheckedPosition);
                  final Intent serviceIntent = ContactSaveService.createSetSuperPrimaryIntent(
                          getApplication(), phoneItem.id);
                  startService(serviceIntent);
                  finish();
              }
            }
        });
        Button cancel = (Button)findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                finish();
            }
        });
        /*End: Modified by xiepengfei for add bottom toolbar 2012/06/13*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        isDisplay = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isDisplay = false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            //case R.id.menu_contact_detail_cancel:
                finish();
                return true;
//            case R.id.menu_contact_detail_complete:
//                if(ada!= null && ada.mCheckedPosition != -1){
//                    final PhoneItem phoneItem = phoneList.get(ada.mCheckedPosition);
//                    final Intent serviceIntent = ContactSaveService.createSetSuperPrimaryIntent(
//                            this, phoneItem.id);
//                    startService(serviceIntent);
//                }
//                finish();
//                return true;
        }
        // TODO Auto-generated method stub
        return super.onOptionsItemSelected(item);
    }

    /**
     * Initiates the interaction. This may result in a phone call or sms message
     * started or a disambiguation dialog to determine which phone number should
     * be used.
     */
    @VisibleForTesting
    /* package */void startInteraction(Uri uri) {
        if (mLoader != null) {
            mLoader.reset();
        }

        final Uri queryUri;
        final String inputUriAsString = uri.toString();
        if (inputUriAsString.startsWith(Contacts.CONTENT_URI.toString())) {
            if (!inputUriAsString.endsWith(Contacts.Data.CONTENT_DIRECTORY)) {
                queryUri = Uri.withAppendedPath(uri, Contacts.Data.CONTENT_DIRECTORY);
            } else {
                queryUri = uri;
            }
        } else if (inputUriAsString.startsWith(Data.CONTENT_URI.toString())) {
            queryUri = uri;
        } else {
            throw new UnsupportedOperationException(
                    "Input Uri must be contact Uri or data Uri (input: \"" + uri + "\")");
        }

        mLoader = new CursorLoader(this,
                queryUri,
                PHONE_NUMBER_PROJECTION,
                PHONE_NUMBER_SELECTION,
                null,
                null);
        mLoader.registerListener(0, this);
        mLoader.startLoading();
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {

        if (cursor == null || !isDisplay) {
            return;
        }

        try {
            while (cursor.moveToNext()) {
                if (cursor.getInt(cursor.getColumnIndex(Phone.IS_SUPER_PRIMARY)) != 0) {
                    // Found super primary, call it.
                    primaryPhone = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                    System.out.println("primaryPhone:" + primaryPhone);
                }

                PhoneItem item = new PhoneItem();
                item.id = cursor.getLong(cursor.getColumnIndex(Data._ID));
                item.phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                item.accountType =
                        cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE));
                item.dataSet = cursor.getString(cursor.getColumnIndex(RawContacts.DATA_SET));
                item.type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));
                item.label = cursor.getString(cursor.getColumnIndex(Phone.LABEL));

                phoneList.add(item);
            }
        } finally {
            cursor.close();
        }

    }

    private static class PhoneItemAdapter extends BaseAdapter {
        private Context mContext;
        private List<PhoneItem> mlist;
        private int mCheckedPosition = -1;

        public PhoneItemAdapter(Context mContext, List<PhoneItem> list) {
            this.mContext = mContext;
            this.mlist = list;

        }

        public void setItemChecked(int position) {
            this.mCheckedPosition = position;
            notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            PhoneItem item = mlist.get(position);

            final View view = LayoutInflater.from(mContext).inflate(
                    R.layout.contact_detail_set_default_phone_list_item, null);
            TextView mType = (TextView) view.findViewById(R.id.textView1);
            TextView mNumber = (TextView) view.findViewById(R.id.textView2);
            final RadioButton mRadioButton = (RadioButton) view.findViewById(R.id.radioButton1);
            int t = ContactsContract.CommonDataKinds.Phone.getTypeLabelResource((int) item.type);

            mType.setText(mContext.getResources().getString(t));
            mNumber.setText(item.phoneNumber);
            if(mCheckedPosition == -1){
                if(item.phoneNumber.equals(primaryPhone)){
                    mCheckedPosition = position;
                }
            }
            if (mCheckedPosition == position) {
                mRadioButton.setChecked(true);
            } else {
                mRadioButton.setChecked(false);
            }
            return view;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mlist.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }
    }

}
/* End: Modified by xiepengfei for set default phone 2012/06/09 */
