
package com.android.contacts.detail;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.activities.ContactDetailActivity;
import com.android.contacts.detail.HistoryViewAdapter.BaseViewEntry;
import com.android.contacts.detail.HistoryViewAdapter.MsViewEntry;

/*Begin: Modified by xiepengfei for delete contact history info activity 2012/05/19*/

public class ContactDetailDeleteHistoryAcitivity extends Activity implements
        OnClickListener {
    private static final String TAG = "ContactDetailDeleteHistoryAcitivity";
    private static final boolean DBG = ContactDetailActivity.DBG;

    public static final String MODE_DELETE_HISTORY = "delete";
    public static final String MODE_VIEW_MODE = "view";

    private String mDisplayMode;
    private int mViewMode = 0;

    private ListView mListView;
    private HistoryViewAdapter mAdapter;
    private SimpleMultipleAdapter mAdapter2;
    private ArrayList<BaseViewEntry> mAllViewEntriesDelete;
    private ArrayList<BaseViewEntry2> mAllViewEntriesView;
    private CheckBox mCheckBox;
    private View mHeadView;
    private LayoutInflater mInflater;

    private final static int DELETE_CALL_LOG_TOKEN = 0;

    private DeleteQueryHandler mDeleteQueryHandler;

    private SharedPreferences mSettings;
    private SharedPreferences.Editor mSettingEditor;
    public final static String SETTING_FILE_NAME = "contact_detail_view_mode_settings";
    public final static String SETTING_ITEM_VIEW_MODE = "view_mode";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            mDisplayMode = intent.getDataString();
            log("mDisplayMode :" + mDisplayMode);
        }
        if (intent == null || TextUtils.isEmpty(mDisplayMode)) {
            finish();
        }
        mInflater = LayoutInflater.from(this);

        setContentView(R.layout.contact_detail_history_delete_actvity);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                    | ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_SHOW_TITLE);
            if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
                actionBar.setTitle(R.string.delete_history_info);
            } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
                actionBar.setTitle(R.string.delete_history_view_mode);
            }
        }

        mDeleteQueryHandler = new DeleteQueryHandler(getContentResolver());

        mListView = (ListView) findViewById(R.id.contact_detail_profile_or_log_list);
        mCheckBox = (CheckBox) findViewById(R.id.contact_detail_history_check_all_item);
        mHeadView = (RelativeLayout) findViewById(R.id.contact_detail_history_headview);
        mHeadView.setClickable(true);
        mCheckBox.setClickable(false);
        mHeadView.setOnClickListener(this);

        if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
            mAllViewEntriesDelete = ContactDetailHistoryFragment
                    .getAllViewEntries();
            mAdapter = new HistoryViewAdapter(this, mAllViewEntriesDelete,
                    mCheckBox);
            mAdapter.setMultipleChoiceMode(true);
            mListView.setAdapter(mAdapter);

            /*Begin: Modified by xiepengfei for debug test 2012/05/30*/
            mAdapter.notifyDataSetChanged();
            /*End: Modified by xiepengfei for debug test 2012/05/30*/
        } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
            mSettings = this.getSharedPreferences(SETTING_FILE_NAME, Activity.MODE_PRIVATE);
            mViewMode = mSettings.getInt(SETTING_ITEM_VIEW_MODE, 7);

            log("onCreate mViewMode:" + mViewMode);

            buildEntry();
            mAdapter2 = new SimpleMultipleAdapter();
            mListView.setAdapter(mAdapter2);

        }

        /*Begin: Modified by xiepengfei for add bottom toolbar 2012/06/13*/
        Button ok = (Button) findViewById(R.id.btn_ok);
        ok.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
                    if (mAdapter.getListChoiceItemCount() > 0)
                        confirmMultiMessageDelete();
                } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
                    mSettingEditor = mSettings.edit();
                    mSettingEditor.putInt(SETTING_ITEM_VIEW_MODE, mViewMode);
                    mSettingEditor.commit();

                    exitTheViewByActivity();

                }
            }
        });
        Button cancel = (Button)findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
                    exitTheDeleteActivity();
                } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
                    exitTheViewByActivity();
                }
            }
        });
        /*End: Modified by xiepengfei for add bottom toolbar 2012/06/13*/

    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        if (mDeleteQueryHandler.progress()) {
            mDeleteQueryHandler.dismissProgressDialog();
        }
    }

//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.contact_detail_delete, menu);
//        return true;
//    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.menu_contact_detail_delete_complete:
//                if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
//                    if (mAdapter.getListChoiceItemCount() > 0)
//                        confirmMultiMessageDelete();
//                } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
//                    mSettingEditor = mSettings.edit();
//                    mSettingEditor.putInt(SETTING_ITEM_VIEW_MODE, mViewMode);
//                    mSettingEditor.commit();
//                    /*Begin: Modified by xiepengfei for finish the activity 2012/05/23*/
//                    exitTheViewByActivity();
//                    /*End: Modified by xiepengfei for finish the activity 2012/05/23*/
//                }
//
//                return true;
//            case R.id.menu_contact_detail_delete_cancel:
//                if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
//                    exitTheDeleteActivity();
//                } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
//                    exitTheViewByActivity();
//                }
//                return true;
            case android.R.id.home:
                if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
                    exitTheDeleteActivity();
                } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
                    exitTheViewByActivity();
                }
                break;
        }
        return false;
    }

    private void exitTheViewByActivity() {
        this.finish();
    }

    private void exitTheDeleteActivity() {
        mAdapter.getHashMapListChoiceItem().clear();
        mAdapter.setListChoiceItemCount(0);
        this.finish();
    }

    private void buildEntry() {
        mAllViewEntriesView = new ArrayList<BaseViewEntry2>();
        mAllViewEntriesView.add(new SeparatorViewEntry());

        SimpleMultipleEntry entry1 = new SimpleMultipleEntry();
        entry1.content = getString(R.string.contact_detail_view_mode1);
        mAllViewEntriesView.add(entry1);

        mAllViewEntriesView.add(new SeparatorViewEntry());

        SimpleMultipleEntry entry2 = new SimpleMultipleEntry();
        entry2.content = getString(R.string.contact_detail_view_mode2);
        mAllViewEntriesView.add(entry2);

        mAllViewEntriesView.add(new SeparatorViewEntry());

        SimpleMultipleEntry entry3 = new SimpleMultipleEntry();
        entry3.content = getString(R.string.contact_detail_view_mode3);
        mAllViewEntriesView.add(entry3);

        mAllViewEntriesView.add(new SeparatorViewEntry());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.contact_detail_history_headview:

                if (mDisplayMode.equals(MODE_DELETE_HISTORY)) {
                    if (mCheckBox.isChecked()) {
                        mCheckBox.setChecked(false);
                        log("no check all");
                        mAdapter.setChoiceAllItem(false);
                        mAdapter.notifyDataSetChanged();
                    } else {
                        mCheckBox.setChecked(true);
                        log("check all");
                        mAdapter.setChoiceAllItem(true);
                        mAdapter.notifyDataSetChanged();
                    }
                } else if (mDisplayMode.equals(MODE_VIEW_MODE)) {
                    if (mCheckBox.isChecked()) {
                        mCheckBox.setChecked(false);
                        mAdapter2.setmChoiceAllItem(false);
                        mAdapter2.notifyDataSetChanged();
                    } else {
                        mCheckBox.setChecked(true);
                        mAdapter2.setmChoiceAllItem(true);
                        mAdapter2.notifyDataSetChanged();
                    }
                }

                break;
        }

    }

    /* Begin: Modified by xiepengfei for delete history info 2012/04/18 */
    private final class DeleteQueryHandler extends BaseProgressQueryHandler {

        public DeleteQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token) {
                case DELETE_CALL_LOG_TOKEN:
                    log("delete token:" + DELETE_CALL_LOG_TOKEN);
                    if (progress()) {
                        log("delete  dismiss dialog token:" + DELETE_CALL_LOG_TOKEN);
                        dismissProgressDialog();
                        exitTheDeleteActivity();
                    } else {
                        mDeleteQueryHandler.updateProgress();
                    }
                    break;
            }
        }

    }

    private void confirmMultiMessageDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_history_info);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        mDeleteQueryHandler.setProgressDialog(ContactDetailDisplayUtils
                                .getProgressDialog(ContactDetailDeleteHistoryAcitivity.this));
                        mDeleteQueryHandler.showProgressDialog();

                        new Thread(new Runnable() {
                            public void run() {
                                log("delete start mListChoiceItemCount:"+ mAdapter.getListChoiceItemCount());
                                mDeleteQueryHandler.setMax(mAdapter.getListChoiceItemCount());

                                long count = mListView.getCount();
                                log("delete mListView.getCount():" + count);
                                if (mAdapter.getListChoiceItemCount() > 0) {
                                    for (int i = 0; i < count; i++) {
                                        log("delete choice item position "+ i + "ischoice:" + mAdapter.getHashMapListChoiceItem().get(i));
                                        if (mAdapter.getHashMapListChoiceItem().get(i) != null && mAdapter.getHashMapListChoiceItem().get(i)) {

                                            /*Begin: Modified by xiepengfei for add delete ms info 2012/06/04*/
                                            if(mAllViewEntriesDelete.get(i).getViewType() == HistoryViewAdapter.VIEW_TYPE_MS){
                                                ContactDetailDisplayUtils.startDeleteMS(mDeleteQueryHandler,DELETE_CALL_LOG_TOKEN,
                                                        ((MsViewEntry)mAllViewEntriesDelete.get(i)).isMmsOrSms,
                                                        mAllViewEntriesDelete.get(i).getId());
                                            }else{
                                                ContactDetailDisplayUtils.startDelete(mDeleteQueryHandler,DELETE_CALL_LOG_TOKEN,
                                                        mAllViewEntriesDelete.get(i).getViewType(),
                                                        mAllViewEntriesDelete.get(i).getId());
                                            }
                                            /*End: Modified by xiepengfei for add delete ms info 2012/06/04*/

                                        }
                                    }
                                } else {
                                    mDeleteQueryHandler.dismissProgressDialog();
                                }
                            }
                        }).start();
                    }
                });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    /* End: Modified by xiepengfei for delete history info 2012/04/18 */

    private class SimpleMultipleAdapter extends BaseAdapter {
        public final static int VIEW_SIMPLE_MULTIPLE_ENTRY = 0;
        public final static int VIEW_TYPE_SEPARATOR_ENTRY = 1;

        IdsOfList mIdsOfList;

        public SimpleMultipleAdapter() {
            mIdsOfList = new IdsOfList(mViewMode);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mAllViewEntriesView.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return mAllViewEntriesView.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            return mAllViewEntriesView.get(position).getViewType();
        }

        public void setmChoiceAllItem(boolean mChoiceAllItem) {
            if (mChoiceAllItem) {
                mViewMode = mIdsOfList.getMaxCount();
            } else {
                mViewMode = 0;
            }
            mIdsOfList.setCount(mViewMode);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_SEPARATOR_ENTRY:
                    return getSeparatorView(position, convertView, parent);
                case VIEW_SIMPLE_MULTIPLE_ENTRY:
                    return getSimpleMultiple(position, convertView, parent);
                default:
                    throw new IllegalStateException("Invalid view type ID "
                            + getItemViewType(position));
            }
        }

        private View getSeparatorView(int position, View convertView,
                ViewGroup parent) {
            final View result = (convertView != null) ? convertView
                    : mInflater.inflate(R.layout.contact_detail_history_list_item_separator,
                            parent, false);
            return result;
        }

        private View getSimpleMultiple(final int position, View convertView,
                ViewGroup parent) {
            final SimpleMultipleEntry entry = (SimpleMultipleEntry) getItem(position);
            final int layout = R.layout.contact_detail_simple_list_item_multiple_choice;
            View result = null;

            result = mInflater.inflate(layout, null);
            TextView content = (TextView) result.findViewById(R.id.textview);
            content.setText(entry.content);
            final CheckBox box = (CheckBox) result.findViewById(R.id.checkbox);
            box.setClickable(false);
            box.setChecked(mIdsOfList.isChoice((position - 1) / 2));

            result.setClickable(true);
            result.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mCheckBox.isChecked()) {
                        mCheckBox.setChecked(false);
                    }
                    if (box.isChecked()) {
                        box.setChecked(false);
                        mViewMode -= 1 << ((position - 1) / 2);
                    } else {
                        box.setChecked(true);
                        mViewMode += 1 << ((position - 1) / 2);
                    }
                    log("getSimpleMultiple mViewMode:" + mViewMode);
                }
            });
            return result;

        }

    }

    public class BaseViewEntry2 {
        private final int viewTypeForAdapter;
        protected long id = -1;
        /** Whether or not the entry can be focused on or not. */
        protected boolean isEnabled = false;

        BaseViewEntry2(int viewType) {
            viewTypeForAdapter = viewType;
        }

        int getViewType() {
            return viewTypeForAdapter;
        }

        long getId() {
            return id;
        }

        boolean isEnabled() {
            return isEnabled;
        }
    }

    public class SimpleMultipleEntry extends BaseViewEntry2 {
        public String content;
        public boolean isEnabled = true;
        public int viewType;

        public SimpleMultipleEntry() {
            super(SimpleMultipleAdapter.VIEW_SIMPLE_MULTIPLE_ENTRY);
            isEnabled = true;
        }
    }

    public class SeparatorViewEntry extends BaseViewEntry2 {

        /**
         * Whether or not the entry is in a subsection (if true then the
         * contents will be indented to the right)
         */
        private boolean mIsInSubSection = false;

        SeparatorViewEntry() {
            super(SimpleMultipleAdapter.VIEW_TYPE_SEPARATOR_ENTRY);
        }

        public void setIsInSubSection(boolean isInSubSection) {
            mIsInSubSection = isInSubSection;
        }

        public boolean isInSubSection() {
            return mIsInSubSection;
        }
    }



    private void log(String s) {
        if (DBG)
            Log.v(TAG, s);
    }
}
    /*
     * ids of list code as a single int. 0x00: no choic 0x01: 1<<0 1 0x02: 1<<1
     * 2 0x04: 1<<2 4 0x08: 1<<3 8 0x10: 1<<4 16 0x20: 1<<5 32 0x40: 1<<6 64
     * ......
     */
class IdsOfList {

    private int mListCount = 3;
    private int mCount;

    public IdsOfList(int count) {
        this.mCount = count;
    }

    public void setCount(int count) {
        this.mCount = count;
    }

    public void setListItemCount(int count) {
        this.mListCount = count;
    }

    public boolean isChoice(int id) {
        return ((mCount & (1 << id)) > 0);
    }

    public boolean[] getBooleanArray() {
        boolean[] ret = new boolean[mListCount];
        for (int i = 0; i < mListCount; i++) {
            ret[i] = isChoice(i);
        }
        return ret;
    }

    public int getMaxCount() {
        int result = 0;
        for (int i = 0; i < mListCount; i++) {
            result += 1 << i;
        }
        return result;
    }
}
/*End: Modified by xiepengfei for delete contact history info activity 2012/05/19*/
