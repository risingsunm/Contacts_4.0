
package com.android.contacts.detail;

import java.util.ArrayList;
import java.util.HashSet;
import android.app.Fragment;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.activities.ContactDetailActivity;
import com.android.contacts.calllog.CallLogQuery;
import com.android.contacts.calllog.CallLogQueryHandler;
import com.android.contacts.calllog.IntentProvider;
import com.android.contacts.detail.HistoryViewAdapter.BaseViewEntry;
import com.android.contacts.detail.HistoryViewAdapter.CallLogViewEntry;
import com.android.contacts.detail.HistoryViewAdapter.EmailViewEntry;
import com.android.contacts.detail.HistoryViewAdapter.TitleViewEntry;
import com.google.common.base.Objects;

/*Begin: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/
import android.provider.ContactsContract.Data;
import android.provider.Telephony.Sms;
import java.util.Comparator;
import com.android.contacts.detail.HistoryViewAdapter.MsViewEntry;
import java.util.Collections;
import com.android.contacts.ContactLoader;

import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony.Mms;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Entity;
import android.content.Entity.NamedContentValues;
import android.text.TextUtils;
import android.os.Message;
import android.content.SharedPreferences;
/*End: Modified by xiepengfei for ContentObserver to local group 2012/05/21*/

/*Begin: Modified by xiepengfei for ContactDetailHistoryFragment 2012/05/19*/
public class ContactDetailHistoryFragment extends Fragment implements CallLogQueryHandler.Listener,
        OnItemClickListener {

    private final static String TAG = "ContactDetailHistoryFragment";
    private final static boolean DBG = ContactDetailActivity.DBG;

    private Uri mLookupUri;
    private CallLogQueryHandler mCallLogQueryHandler;

    private ListView mListView;
    private ScrollView mScrollView;
    private TextView mEmpty;

    private HistoryViewAdapter mAdapter;

    private HashSet<String> labelHashSet = new HashSet<String>();

/*Begin: Modified by xiepengfei for for add phone query 2012/05/30*/
    private static ArrayList<BaseViewEntry> mAllCallLogViewEntries = new ArrayList<BaseViewEntry>();
    private static ArrayList<BaseViewEntry> mAllMSViewEntries = new ArrayList<BaseViewEntry>();
    private static ArrayList<BaseViewEntry> mAllViews = new ArrayList<BaseViewEntry>();
/*End: Modified by xiepengfei for for add phone query 2012/05/30*/
    private static ArrayList<BaseViewEntry> mAllEmailViewEntries = new ArrayList<BaseViewEntry>(); //0806 bxinchun add.
    private static ArrayList<BaseViewEntry> mAllViewEntries = new ArrayList<BaseViewEntry>();

    /* Begin: Modified by xiepengfei for save list state 2012/03/31 */
    private Parcelable mListState;
    private static final String KEY_LIST_STATE = "liststate";
    private float mVisiblePosition = 0f;
    /* End: Modified by xiepengfei for save list state 2012/03/31 */

    /* Begin: Modified by xiepengfei for Multiple Choice Mode 2012/04/16 */
    private MenuItem mMenudelete;
    private MenuItem mMenuViewMode;
    /* End: Modified by xiepengfei for Multiple Choice Mode 2012/04/16 */

    /* Begin: Modified by xiepengfei for delete history info 2012/04/18 */
    public static String strToday;
    public static String strYesterday;

    private CallLogContent mCallLogContent;
    private MMSContent mMmsContent;
    private SMSContent mSmsContent;
    private EmailObserver emailObserver;

    private boolean isNeedRefreshCalls = true;
    private boolean isNeedRefreshMS = true;
    private boolean isNeedRefreshEmails = true;

    /* End: Modified by xiepengfei for delete history info 2012/04/18 */

    /*Begin: Modified by xiepengfei for list item view mode 2012/05/30*/
    private SharedPreferences mSettings;
    private int mListViewMode = -1;
    private IdsOfList mIdsOfList;
    /*End: Modified by xiepengfei for list item view mode 2012/05/30*/

    /*Begin: Modified by xiepengfei for add mms query 2012/05/28*/
    private ContactLoader.Result mContactData;

    private ArrayList<String> mAllPhoneNumber = new ArrayList<String>();
    private ContactsMMsInfoQueryHandler mMMsInfoQueryHandler;
    public final String[] ALL_THREADS_PROJECTION1 ={"date","body","sub","type","msg_box","sub_id","_id"};

    // add by bxinchun 0808 begin.
    private EmailInfoQueryHandler emailInfoQueryHandler;
    private ArrayList<String> mAllEmails = new ArrayList<String>();
    // add by bxinchun 0808 end.

    private MyComparator myComparator = new MyComparator();
    public final static int MESSAGE_QUERY_MS_REALLY_COMPLETE = 100;
    public final static int MESSAGE_QUERY_CALL_LOG_COMPLETE = 101;
    public final static int MESSAGE_QUERY_EMAIL_COMPLETE = 102;
    private Handler mHandler = new Handler(){

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_QUERY_MS_REALLY_COMPLETE:
                    refurbishListView();
                    break;
                case MESSAGE_QUERY_CALL_LOG_COMPLETE:
                    refurbishListView();
                    break;
                case MESSAGE_QUERY_EMAIL_COMPLETE:
                    refurbishListView();
                default:
                    break;
            }
        }

    };
    /*End: Modified by xiepengfei for add mms query 2012/05/28*/


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Begin: Modified by xiepengfei for save list state 2012/03/31 */
        if (savedInstanceState != null) {
            log("onCreate savedInstanceState is not null");
            mListState = savedInstanceState.getParcelable(KEY_LIST_STATE);
        }
        /* End: Modified by xiepengfei for save list state 2012/03/31 */
        mCallLogQueryHandler = new CallLogQueryHandler(this.getActivity().getContentResolver(), this);

        /* Begin: Modified by xiepengfei for delete history info 2012/04/18 */
        setHasOptionsMenu(true);

        strToday = getString(R.string.today);
        strYesterday = getString(R.string.yesterday);

        ContentResolver cResolver = getActivity().getContentResolver();

        mCallLogContent = new CallLogContent(new Handler());
        cResolver.registerContentObserver(Calls.CONTENT_URI, true, mCallLogContent);
        /* End: Modified by xiepengfei for delete history info 2012/04/18 */

        /*Begin: Modified by xiepengfei for add mms query 2012/05/30*/
        mMMsInfoQueryHandler = new ContactsMMsInfoQueryHandler(getActivity().getContentResolver());
        /*End: Modified by xiepengfei for add mms query 2012/05/30*/

        /*Begin: Modified by xiepengfei for add delete ms info 2012/06/04*/
        mMmsContent = new MMSContent(new Handler());
        cResolver.registerContentObserver(Mms.CONTENT_URI, true, mMmsContent);
        mSmsContent = new SMSContent(new Handler());
        cResolver.registerContentObserver(Sms.CONTENT_URI, true, mSmsContent);
        /*End: Modified by xiepengfei for add delete ms info 2012/06/04*/

        /*Begin: Modified by bxinchun to add email query 2012-08-08*/
        emailInfoQueryHandler = new EmailInfoQueryHandler(cResolver);

        emailObserver = new EmailObserver(new Handler());
        cResolver.registerContentObserver(EmailInfo.messageUri, true, emailObserver);
        /*End: Modified by bxinchun to add email query 2012-08-08*/

        /*Begin: Modified by xiepengfei for list item view mode 2012/05/30*/
        mSettings = getActivity().getSharedPreferences(ContactDetailDeleteHistoryAcitivity.SETTING_FILE_NAME, Activity.MODE_PRIVATE);
        /*End: Modified by xiepengfei for list item view mode 2012/05/30*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mListView != null) {
            outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
            log("onCreate onSaveInstanceState ");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        log("onCreateOptionsMenu");
        inflater.inflate(R.menu.contact_detail_history2, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mMenudelete = menu.findItem(R.id.menu_contact_detail_history_delete);
        mMenudelete.setVisible(true);
        mMenuViewMode = menu.findItem(R.id.menu_contact_detail_history_view_mode);
        mMenuViewMode.setVisible(true);

        /*Begin: Modified by xiepengfei for bug: delete menu show error 2012/06/04*/
        updataMenuState();
        /*End: Modified by xiepengfei for bug: delete menu show error 2012/06/04*/
    }

    /**
     * update menu state ,when list item size =0, menu set visible false,else
     * true
     */
    private void updataMenuState() {
        log("updataMenuState");
        /*Begin: Modified by xiepengfei for modify menu display bug 2012/05/23*/
        log("mAllViewEntyrs.size():"+mAllViewEntries.size());
        if (mMenudelete != null)
            mMenudelete.setVisible(mAllViewEntries.size() == 0 ? false : true);
        /*End: Modified by xiepengfei for modify menu display bug 2012/05/23*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contact_detail_history_delete:
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(getActivity(), ContactDetailDeleteHistoryAcitivity.class);
                intent.setData(Uri.parse(ContactDetailDeleteHistoryAcitivity.MODE_DELETE_HISTORY));
                startActivity(intent);
                // displayToolsView();
                return true;
            case R.id.menu_contact_detail_history_view_mode:
                Intent intent2 = new Intent();
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent2.setClass(getActivity(), ContactDetailDeleteHistoryAcitivity.class);
                intent2.setData(Uri.parse(ContactDetailDeleteHistoryAcitivity.MODE_VIEW_MODE));
                startActivity(intent2);

                return true;
        }
        return false;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_detail_history_fragment, null);
        mListView = (ListView) view.findViewById(R.id.contact_detail_profile_or_log_list);
        mEmpty = (TextView) view.findViewById(R.id.contact_detail_profile_or_log_emptyText);
        mScrollView = (ScrollView) view.findViewById(R.id.contact_detail_profile_or_log_scrollview);

        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
        mListView.setItemsCanFocus(true);
        mListView.setOnScrollListener(listviewScrollListener);
        mListView.setOnItemClickListener(this);
        log("onCreateView");
        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        log("onViewCreated");
    }

    @Override
    public void onStart() {
        super.onStart();

        if (isNeedRefreshCalls) {
            startCallsQuery();
            isNeedRefreshCalls = false; // add by bxinchun 2012-08-08
        }
        if (isNeedRefreshMS) {
            /*Begin: Modified by xiepengfei for add mms query 2012/05/28*/
            startMMsQuery();
            /*End: Modified by xiepengfei for add mms query 2012/05/28*/
            isNeedRefreshMS = false; // add by bxinchun 2012-08-08
        }
        /*Begin: Added by bxinchun 2012-08-08*/
        if (isNeedRefreshEmails) {
            Log.d("Tp", "onStart execute...");
            startEmailQuery();
            isNeedRefreshEmails = false;
        }
        /*End: Added by bxinchun 2012-08-08*/

        if (mAdapter == null) {
            mAdapter = new HistoryViewAdapter(getActivity(), mAllViewEntries);
            mListView.setAdapter(mAdapter);
            mListView.setItemsCanFocus(true);
        } else {
            mListView.setAdapter(mAdapter);
            mListView.setSelectionFromTop(0, (int) mVisiblePosition);
        }
        // Restore {@link ListView} state if applicable because the adapter is
        // now populated.
        if (mListState != null) {
            mListView.onRestoreInstanceState(mListState);
            mListState = null;
        }
        mAdapter.notifyDataSetChanged();

        /*Begin: Modified by xiepengfei for modify the empty display 2012/05/25*/
        refurbishListView();
        /*End: Modified by xiepengfei for modify the empty display 2012/05/25*/

        updataMenuState();
        log("onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume");
    }

    public void onPause() {
        super.onPause();
        log("onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        log("onStop");
    }

    @Override
    public void onDestroy() {
        /*Begin: Modified by bxinchun 2012-08-08, unregister all observers.*/
        log("onDestroy");
        ContentResolver resolver = getActivity().getContentResolver();
        if (mCallLogContent != null) {
            resolver.unregisterContentObserver(mCallLogContent);
        }
        if (mMmsContent != null) {
            resolver.unregisterContentObserver(mMmsContent);
        }
        if (mSmsContent != null) {
            resolver.unregisterContentObserver(mSmsContent);
        }
        if (emailObserver != null) {
            resolver.unregisterContentObserver(emailObserver);
        }
        resolver = null;
        /*End: Modified by bxinchun 2012-08-08*/
        super.onDestroy();
    }

    public void onDetach() {
        super.onDetach();
        log("onDetach");
        labelHashSet.clear();
        /*Begin: Modified by xiepengfei for for add phone query 2012/05/30*/
        mAllCallLogViewEntries.clear();
        mAllMSViewEntries.clear();
        /*End: Modified by xiepengfei for for add phone query 2012/05/30*/
        mAllEmailViewEntries.clear();
        mAllViewEntries.clear();
    }

    /** Requests updates to the data to be shown. */


    public void startCallsQuery() {
        /*Begin: Modified by xiepengfei for debug test 2012/05/30*/
        if(mLookupUri != null){
            log("startCallsQuery");
            mCallLogQueryHandler.fetchContactByLookupUri(mLookupUri.toSafeString());
        }
        //mCallLogQueryHandler.fetchContactByLookupUri(mLookupUri.toSafeString());
        /*End: Modified by xiepengfei for debug test 2012/05/30*/
    }

    public void displayEmptyView() {
        mScrollView.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
    }

    public void displayHistoryView() {
        mAdapter.notifyDataSetInvalidated();

        mScrollView.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
    }

    public void loadLookupUri(Uri uri) {
        if (Objects.equal(uri, mLookupUri))
            return;
        mLookupUri = uri;
    }

    public void onVoicemailStatusFetched(Cursor statusCursor) {

    }

    public void onCallsFetched(Cursor cursor) {
        log("detail onCallsFetched detail cursor:" + cursor.getCount());
        mAllCallLogViewEntries.clear();
        log("onCallsFetched mAllCallLogViewEntyrs size:"+mAllCallLogViewEntries.size());
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                CallLogViewEntry entry = mAdapter.new CallLogViewEntry();

                entry.subscription = cursor.getInt(cursor.getColumnIndex(Calls.SUBSCRIPTION));
                entry.date = cursor.getLong(cursor.getColumnIndex(Calls.DATE));
                entry.id = cursor.getInt(cursor.getColumnIndex(Calls._ID));
                entry.callType = cursor.getInt(cursor.getColumnIndex(Calls.TYPE));
                entry.callTime = ContactDetailDisplayUtils.getDateTime(getActivity(), entry.date);
                entry.callNumber = cursor.getString(cursor.getColumnIndex(Calls.NUMBER));
                /* Begin: Modified by zxiaona for ContactsHistory 2012/08/24 */
                // String duration =
                // ContactDetailDisplayUtils.getDurationString(cursor.getLong(cursor
                // .getColumnIndex(Calls.DURATION)));
                String duration = ContactDetailDisplayUtils.getDurationString(
                        getResources(),
                        cursor.getLong(cursor.getColumnIndex(Calls.DURATION)));
                /* End: Modified by zxiaona for ContactsHistory 2012/08/24 */

                entry.callDuration = duration;
                entry.voicemailUri = cursor.getString(CallLogQuery.VOICEMAIL_URI); // add by bxinchun 2012-08-13

                mAllCallLogViewEntries.add(entry);
            }
            cursor.close();
        }
        mHandler.sendEmptyMessage(MESSAGE_QUERY_CALL_LOG_COMPLETE);
        isNeedRefreshCalls = false;
        log("onCallsFetched mAllCallLogViewEntyrs size:"+mAllCallLogViewEntries.size());
    }

    /* Begin: Modified by xiepengfei for debug 2012/03/30 */

    private void log(String s) {
        if (DBG)
            Log.v(TAG, s);
    }

    /* End: Modified by xiepengfei for debug 2012/03/30 */
    private OnScrollListener listviewScrollListener = new OnScrollListener() {

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                mVisiblePosition = mListView.getVerticalScrollbarPosition();
            }
        }

        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {

        }
    };

    /* Begin: Modified by xiepengfei for Multiple Choice Mode 2012/04/16 */

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        log("list item position:" + position);
        /*Begin: Added by bxinchun 2012-8-13, add the item click func.*/
        if (mAdapter == null || getActivity() == null) { return; }
        HistoryViewAdapter adapter = mAdapter;
        switch (adapter.getItemViewType(position)) {
            case HistoryViewAdapter.VIEW_TYPE_CALL_LOG:
                CallLogViewEntry callEntry = (CallLogViewEntry) mAdapter.getItem(position);
                if (callEntry != null) {
                    getActivity().startActivity(IntentProvider.getCallDetailIntentProvider(
                            callEntry.voicemailUri, new long[] { callEntry.id }, callEntry.subscription)
                            .getIntent(getActivity()));
                }
                return;
            case HistoryViewAdapter.VIEW_TYPE_MS:
                MsViewEntry msEntry = (MsViewEntry) mAdapter.getItem(position);
                if (msEntry != null) {
                    Uri uri = Uri.parse("smsto:"+ msEntry.number);
                    Log.d("Tp", " view ms, number :" + msEntry.number);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    getActivity().startActivity(intent);
                }
                return;
            case HistoryViewAdapter.VIEW_TYPE_EMAIL:
                EmailViewEntry emailEntry = (EmailViewEntry) mAdapter.getItem(position);
                if (emailEntry != null) {
                    Intent intent = createOpenMessageIntent(emailEntry.mAccountId
                            , emailEntry.mMailboxId, emailEntry.mMessageId);
                    if (intent != null) {
                        try {
                            getActivity().startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Log.w(TAG, "not found activity to handler email view!", e);
                        } catch (Exception e) {
                            Log.w(TAG, "view email detail meet exception !", e);
                        }
                    }
                }
                break;

            default:
                break;
        }
        /*End: Added by bxinchun 2012-8-13, addthe item click func.*/
    }

    /* End: Modified by xiepengfei for Multiple Choice Mode 2012/04/16 */

    /*Begin: Modified by bxinchun 2012-08-15, the intent of jumping to a message view.*/
    /**
     * Create an intent to launch and open a message.
     *
     * @param accountId must not be -1.
     * @param mailboxId must not be -1.
     * @param messageId must not be -1.
     */
    public static Intent createOpenMessageIntent(long accountId,
            long mailboxId, long messageId) {
        if (accountId == -1 || mailboxId == -1 || messageId == -1) {
            Log.w(TAG, "illegal email item arguments.", new IllegalArgumentException());
            return null;
        }
        Intent i = new Intent();
        i.setClassName("com.android.email", "com.android.email.activity.EmailActivity");
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        i.putExtra("ACCOUNT_ID", accountId);
        i.putExtra("MAILBOX_ID", mailboxId);
        i.putExtra("MESSAGE_ID", messageId);

        return i;
    }
    /*End: Modified by bxinchun 2012-08-15, the intent of jumping to a message view.*/

    /* Begin: Modified by xiepengfei for updata history info 2012/04/19 */
    /**
     * when the Calls table is change,isNeadRefushData = true;
     *
     * @author xiepengfei
     */
    private class CallLogContent extends ContentObserver {
        public CallLogContent(Handler handler) {
            super(handler);
        }
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            isNeedRefreshCalls = true;
        }
    }

    /* End: Modified by xiepengfei for updata history info 2012/04/19 */
    private class MMSContent extends ContentObserver{
        public MMSContent(Handler handler) {
            super(handler);
        }
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            isNeedRefreshMS = true;
        }
    }

    private class SMSContent extends ContentObserver{
        public SMSContent(Handler handler) {
            super(handler);
        }
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            isNeedRefreshMS = true;
        }
    }

    private class EmailObserver extends ContentObserver {
        public EmailObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            isNeedRefreshEmails = true;
        }
    }

    /* Begin: Modified by xiepengfei for delete history info 2012/04/23 */
    public static ArrayList<BaseViewEntry> getAllViewEntries() {
        return mAllViewEntries;
    }
    /* End: Modified by xiepengfei for delete history info 2012/04/23 */

    /*Begin: Modified by xiepengfei for add mms query 2012/05/28*/
    public void startMMsQuery(){
        getContactAllPhoneNumber();

        int size = mAllPhoneNumber.size();
        if (size > 0) {
            mAllMSViewEntries.clear();
            mMMsInfoQueryHandler.setQueryMax(size);
            for(int i=0;i<size;i++){
                log("alll Phone Number:"+mAllPhoneNumber.get(i));
                Uri uri = Uri.parse("content://mms-sms/messages/byphone/"+mAllPhoneNumber.get(i));
                mMMsInfoQueryHandler.startQuery(ContactsMMsInfoQueryHandler.QUERY_MMMS
                        , mAllPhoneNumber.get(i), uri, ALL_THREADS_PROJECTION1, null, null, " date DESC");
            }

        }
    }

    public void setContactData(ContactLoader.Result data){
        this.mContactData = data;
    }

    public void getContactAllPhoneNumber(){
        log("getContactAllPhoneNumber");

        if (mContactData != null) {
            for (Entity entity: mContactData.getEntities()) {
                final ContentValues entValues = entity.getEntityValues();
                for (NamedContentValues subValue : entity.getSubValues()) {
                    final ContentValues entryValues = subValue.values;
                    final String mimeType = entryValues.getAsString(Data.MIMETYPE);
                    if (mimeType == null) continue;
                    if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) { // Get phone string
                        String number = entryValues.getAsString(Phone.NUMBER);
                        if(!TextUtils.isEmpty(number) && !mAllPhoneNumber.contains(number)){
                            mAllPhoneNumber.add(number);
                        }
                    } // end if
                }
            } // end for
        } // end if
    }

    /**
     * use the contact's phone number lookup mms
     * @author xiepengfei
     *
     */
    public class ContactsMMsInfoQueryHandler extends AsyncQueryHandler{

        public final static int QUERY_MMMS = 0;
        public final static int DELETE_MMS = 1;
        public int queryMax = 1;
        public int queryCount = 0;

        //public final String[] ALL_THREADS_PROJECTION1 ={"date","body","sub","type","msg_box","sub_id","_id"};

        public void setQueryMax(int queryMax) {
            this.queryMax = queryMax;
            queryCount = 0;
        }

        public ContactsMMsInfoQueryHandler(ContentResolver c) {
            super(c);
        }

        @Override
        protected void onQueryComplete(int token, Object object, Cursor cursor) {
            if (mAdapter == null || getActivity() == null) { // add by bxinchun 20120809
                return;
            }
            if(token == QUERY_MMMS){
                queryCount++;
                if(cursor!=null){
                    try {
                        cursor.moveToPosition(-1);
                        while(cursor.moveToNext()){
                            int type = cursor.getInt(3);
                            if(type == 1 || type == 2){
                                MsViewEntry entry= mAdapter.new MsViewEntry();
                                entry.date = cursor.getLong(0);//date;
                                entry.subscription = cursor.getInt(5);
                                entry.msType = type;
                                entry.id = cursor.getLong(6);
                                entry.time = ContactDetailDisplayUtils.getDateTime(getActivity(), entry.date);
                                String content = cursor.getString(1);//body
                                entry.isMmsOrSms = 0;//sms
                                if(TextUtils.isEmpty(content)){
                                    content = cursor.getString(2);//this is mms;
                                    entry.isMmsOrSms = 1;//mms
                                    if(TextUtils.isEmpty(content)){
                                        content = "No Title";// this is mms;
                                    }
                                }
                                entry.content = content;
                                entry.number = object.toString();
                                mAllMSViewEntries.add(entry);
                            }

                            //Log.d("111111111", ""+cursor.getLong(0)+"  ,"+cursor.getString(1)+"  ,"+cursor.getString(2)
                            //        +"  ,"+cursor.getString(3)+"  ,"+cursor.getString(4));
                        }
                    } finally {
                        cursor.close();
                    }
                }

                if(queryCount == queryMax){
                    //really query complete
                    mHandler.sendEmptyMessage(MESSAGE_QUERY_MS_REALLY_COMPLETE);
                }
            }
        }

    }

    // bxinchun 2012-07-16 modified the method to resolve compile warnings.
    public static class MyComparator implements Comparator < BaseViewEntry > {
        public int compare(BaseViewEntry e1, BaseViewEntry e2) {
            double time1 = Double.valueOf(e1.date);
            double time2 = Double.valueOf(e2.date);
            return Double.compare(time2, time1);
        }
    }

    private void flattenList(ArrayList<BaseViewEntry> entries){
        mAllViewEntries.clear();
        labelHashSet.clear();
        final int count = entries.size();


        //add the viewdata to allviewEntry
        for(int i=0;i<count;i++){
            if( i!=0 ){
                mAllViewEntries.add(mAdapter.new SeparatorViewEntry());
            }

          //build label title
          String dateTime = ContactDetailDisplayUtils.getDateString(getActivity(), entries.get(i).getDate());
          String[] labelTime = dateTime.split("=");
          if(!labelHashSet.contains(labelTime[0])){
              labelHashSet.add(labelTime[0]);
              TitleViewEntry title = mAdapter.new TitleViewEntry();
              title.title = labelTime[0];
              mAllViewEntries.add(title);
          }

          mAllViewEntries.add(entries.get(i));
        }
        entries.clear();
    }

    private void sortList(ArrayList<BaseViewEntry> entry){
        Collections.sort(entry, myComparator) ;
    }

    private void refurbishListView(){
        mListViewMode = mSettings.getInt(ContactDetailDeleteHistoryAcitivity.SETTING_ITEM_VIEW_MODE, 7);
        log("mode: "+mListViewMode);
        mIdsOfList = new IdsOfList(mListViewMode);
        if(mIdsOfList.isChoice(0)){
            mAllViews.addAll(mAllCallLogViewEntries);
        }
        if(mIdsOfList.isChoice(1)){
            mAllViews.addAll(mAllMSViewEntries);
        }
        if(mIdsOfList.isChoice(2)){
            mAllViews.addAll(mAllEmailViewEntries);
            log("email");
        }
        sortList(mAllViews);
        flattenList(mAllViews);
        log("mAllViewEntries.size():" + mAllViewEntries.size());
        if (mAllViewEntries.size() > 0) {
            displayHistoryView();
            mAdapter.notifyDataSetChanged();
        } else {
            displayEmptyView();
        }

        /*Begin: Modified by xiepengfei for bug: delete menu show error 2012/06/04*/
        updataMenuState();
        /*End: Modified by xiepengfei for bug: delete menu show error 2012/06/04*/

    }
    /*End: Modified by xiepengfei for add mms query 2012/05/28*/

    /*Begin:  Added by bxinchun to add email query 2012-08-08*/
    public void startEmailQuery() {
        //emailInfoQueryHandler.cancelOperation(EmailInfoQueryHandler.TOKEN_QUERY_EMAIL);

        getContactAllEmailAddresses();

        final int size = mAllEmails.size();
        //Log.d("Tp", "start email query, email size :" + size);
        if (size > 0) {
            mAllEmailViewEntries.clear();
            StringBuilder selection = new StringBuilder("");
            selection.append("(");
            for (int i = 0; i < size; i++) {
                //log(" emai " + i +" :"+mAllPhoneNumber.get(i));
                String emailAddress = mAllEmails.get(i);
                selection.append(" fromList like '%");
                selection.append(emailAddress);
                selection.append("%' OR toList like '%");
                selection.append(emailAddress);
                selection.append("%' ");
                if (i != size -1) {
                    selection.append(" OR ");
                }
            }
            selection.append(") ");
            //selection.append(" AND ");
            //selection.append(" (mailboxKey=? OR mailboxKey=? )");

            //selection.append(" GROUP BY accountKey, mailboxKey ");
            //Log.i("Tp", "query selection is :" + selection);

            /*String [] whereArgs = new String[] { String.valueOf(mailInBoxId)
                    , String.valueOf(mailOutBoxId)};*/
            emailInfoQueryHandler.startQuery(EmailInfoQueryHandler.TOKEN_QUERY_EMAIL
                    , null, EmailInfo.messageUri, EmailInfo.EMAIL_PROJECTION
                    , selection.toString(), /*whereArgs*/null, " timeStamp DESC");
            // end 08-15

        }
    }

    public void getContactAllEmailAddresses() {
        log("getContactAllEmailAddresses");

        mAllEmails.clear();
        if (mContactData != null) {
            for (Entity entity: mContactData.getEntities()) {
                //final ContentValues entValues = entity.getEntityValues();
                for (NamedContentValues subValue : entity.getSubValues()) {
                    final ContentValues entryValues = subValue.values;
                    final String mimeType = entryValues.getAsString(Data.MIMETYPE);
                    if (mimeType == null) continue;
                    if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) { // Get email address
                        String address = entryValues.getAsString(Email.ADDRESS);
                        if (TextUtils.isEmpty(address) || mAllEmails.contains(address)) {
                            continue;
                        }
                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                            mAllEmails.add(address);
                        }
                    } // end if
                }
            } // end for
        } // end if
    }

    public static class EmailInfo {
        public static final Uri messageUri = Uri.parse("content://com.ahong.email.provider/message");
        public static final String mailboxIdUriPrefix = "content://com.ahong.email.provider/mailboxIdFromAccountAndType";
        public static final String [] EMAIL_PROJECTION = {
            "_id"
            , "mailboxKey", "accountKey"
            , "displayName", "subject", "timeStamp"
            , "flagRead", "flagFavorite", "flagAttachment"
            , "fromList", "toList", "ccList", "bccList", "replyToList"
        };

        public static final int COLUMN_INDEX_ID = 0;
        public static final int COLUMN_INDEX_MAILBOX_KEY = 1;
        public static final int COLUMN_INDEX_ACCOUNT_KEY = 2;
        public static final int COLUMN_INDEX_DISPLAYNAME = 3;
        public static final int COLUMN_INDEX_SUBJECT = 4;
        public static final int COLUMN_INDEX_TIMESTAMP = 5;
        public static final int COLUMN_INDEX_FLAG_READ = 6;
        public static final int COLUMN_INDEX_FLAG_FAVORITE = 7;
        public static final int COLUMN_INDEX_FLAG_ATTACHMENT = 8;
        public static final int COLUMN_INDEX_FROM_LIST = 9;
        public static final int COLUMN_INDEX_TO_LIST = 10;
        public static final int COLUMN_INDEX_CC_LIST = 11;
        public static final int COLUMN_INDEX_BCC_LIST = 12;
        public static final int COLUMN_INDEX_REPLYTO_LIST = 13;

        // constants as those are defined in Email app.
        public static final int MAILBOX_TYPE_INBOX = 0;
        public static final int MAILBOX_TYPE_OUTBOX = 4;
        public static final int MAILBOX_TYPE_DRAFTS = 3;
        public static final int MAILBOX_TYPE_SENT = 5;
        //public static final int MAILBOX_TYPE_TRASH = 6;
    }

    public class EmailInfoQueryHandler extends AsyncQueryHandler {
        public static final int TOKEN_QUERY_EMAIL = 1;

        public EmailInfoQueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            Log.d("Tp", "query email completed. cookie :" + cookie + ", adapter :" + mAdapter);
            if (mAdapter == null) {
                return;
            }
            //if (cookie == null) { return; }

            if (getActivity() == null) { return; }

            if (token == TOKEN_QUERY_EMAIL) {
                /*String[] cook = (String [])cookie;
                int currentNum = Integer.parseInt(cook[0]);
                final int totalNum = Integer.parseInt(cook[1]);
                Log.d("Tp", "cookie :" + cook[0] + ", " + cook[1] + ", " + cook[2] + ", cursor :" + cursor);*/
                if (cursor != null) {
                    Log.d("Tp", "cursor size :" + cursor.getCount());
                    try {
                        ContentResolver resolve = getActivity().getContentResolver();
                        HashSet<Long> inMailBoxIds = new HashSet<Long>();
                        HashSet<Long> sentMailBoxIds = new HashSet<Long>();
                        cursor.moveToPosition(-1);
                        while(cursor.moveToNext()) {
                            EmailViewEntry entry = mAdapter.new EmailViewEntry(cursor);
                            entry.setTimeStamp(ContactDetailDisplayUtils.getDateTime(getActivity()
                                    , entry.date));

                            if (inMailBoxIds.contains(Long.valueOf(entry.mMailboxId))) {
                                entry.type = EmailInfo.MAILBOX_TYPE_INBOX;
                                mAllEmailViewEntries.add(entry);
                                continue;
                            } else if (sentMailBoxIds.contains(Long.valueOf(entry.mMailboxId))) {
                                entry.type = EmailInfo.MAILBOX_TYPE_SENT;
                                mAllEmailViewEntries.add(entry);
                                continue;
                            }

                            Cursor cr = null;
                            long inboxId = -1;
                            try {
                                Uri mailboxUri = Uri.parse(EmailInfo.mailboxIdUriPrefix + "/" + entry.mAccountId + "/" + EmailInfo.MAILBOX_TYPE_INBOX);
                                cr = resolve.query(mailboxUri, new String[] { "_id" }, null
                                        , null, null);
                                if (cr != null && cr.moveToFirst()) {
                                    inboxId = cr.getInt(0);
                                }
                            } finally {
                                if (cr != null) {
                                    cr.close();
                                }
                            }

                            if (inboxId != -1 && inboxId == entry.mMailboxId) {
                                entry.type = EmailInfo.MAILBOX_TYPE_INBOX;
                                mAllEmailViewEntries.add(entry);

                                inMailBoxIds.add(Long.valueOf(entry.mMailboxId));
                            } else {
                                long outboxId = -1;
                                try {
                                    Uri mailboxUri = Uri.parse(EmailInfo.mailboxIdUriPrefix + "/" + entry.mAccountId + "/" + EmailInfo.MAILBOX_TYPE_SENT);
                                    cr = resolve.query(mailboxUri, new String[] { "_id" }, null
                                            , null, null);
                                    if (cr != null && cr.moveToFirst()) {
                                        outboxId = cr.getInt(0);
                                    }
                                } finally {
                                    if (cr != null) {
                                        cr.close();
                                    }
                                }

                                if (outboxId != -1 && outboxId == entry.mMailboxId) {
                                    entry.type = EmailInfo.MAILBOX_TYPE_SENT;
                                    mAllEmailViewEntries.add(entry);

                                    sentMailBoxIds.add(Long.valueOf(entry.mMailboxId));
                                }
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }

                //if (currentNum == totalNum - 1) {
                    mHandler.sendEmptyMessage(MESSAGE_QUERY_EMAIL_COMPLETE);
                //}
                    return;
            }

        }
    }
    /*End: Added by bxinchun 2012-08-08 */

}

/*End: Modified by xiepengfei for ContactDetailHistoryFragment 2012/05/19*/
