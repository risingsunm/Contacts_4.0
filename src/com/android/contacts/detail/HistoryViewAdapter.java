
package com.android.contacts.detail;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.detail.ContactDetailHistoryFragment.EmailInfo;

/*Begin: Modified by xiepengfei for help display call log ,sms and other history info 2012/05/19*/

public class HistoryViewAdapter extends BaseAdapter {
    private final static String TAG = "HistoryViewAdapter";
    private final static boolean DBG = true;

    public final static int VIEW_TYPE_CALL_LOG = 0;
    public final static int VIEW_TYPE_TITLE = 1;
    public final static int VIEW_TYPE_MS = 2;
    public final static int VIEW_TYPE_EMAIL = 3;
    public final static int VIEW_TYPE_SEPARATOR_ENTRY = 4;

    public final static int VIEW_TYPT_COUNT = 5;

    public ArrayList<BaseViewEntry> mAllViewEntries;
    private LayoutInflater mInflater;

    private boolean isMultipleChoiceMode = false;
    private boolean isChoiceAllItem = false;
    private static HashMap<Integer, Boolean> mHashMapListChoiceItem = new HashMap<Integer, Boolean>();
    private CheckBox mCheckBox;
    private int mListChoiceItemCount = 0;

    /* Begin: Modified by xiepengfei for delete history info 2012/04/23 */

    public HistoryViewAdapter(Context context, ArrayList<BaseViewEntry> allViewEntries) {
        this.mInflater = LayoutInflater.from(context);
        this.mAllViewEntries = allViewEntries;
    }

    public HistoryViewAdapter(Context context, ArrayList<BaseViewEntry> allViewEntries,
            CheckBox checkBox) {
        this.mInflater = LayoutInflater.from(context);
        this.mAllViewEntries = allViewEntries;
        this.mCheckBox = checkBox;
    }

    /* End: Modified by xiepengfei for delete history info 2012/04/23 */
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_TITLE:
                return getTitleView(position, convertView, parent);
            case VIEW_TYPE_CALL_LOG:
                return getCallLogView(position, convertView, parent);
                /*Begin: Modified by xiepengfei for for add ms view 2012/05/30*/
            case VIEW_TYPE_MS:
                return getMsView(position, convertView, parent);
                /*End: Modified by xiepengfei for for add ms view 2012/05/30*/
            case VIEW_TYPE_EMAIL:
                return getEmailView(position, convertView, parent);
            case VIEW_TYPE_SEPARATOR_ENTRY:
                return getSeparatorView(position, convertView, parent);
            default:
                throw new IllegalStateException("Invalid view type ID "
                        + getItemViewType(position));
        }
    }

    private View getSeparatorView(int position, View convertView,
            ViewGroup parent) {
        final View result = (convertView != null) ? convertView : mInflater
                .inflate(R.layout.contact_detail_history_list_item_separator,
                        parent, false);
        return result;
    }

    private View getTitleView(int position, View convertView, ViewGroup parent) {
        final TitleViewEntry entry = (TitleViewEntry) getItem(position);
        final int layout = R.layout.list_separator;
        View result = null;
        TitleViewCache viewCache = null;

        if (convertView != null) {
            viewCache = (TitleViewCache) convertView.getTag();
            if (viewCache.layoutResourceId == layout) {
                result = convertView;
            }
        }

        // Otherwise inflate a new header view and create a new view cache.
        if (result == null) {
            result = mInflater.inflate(layout, null);
            viewCache = new TitleViewCache(result, layout);
            result.setTag(viewCache);
        }
        viewCache.mTitle.setText(entry.title);

        return result;
    }

/*Begin: Modified by xiepengfei for for add ms view 2012/05/30*/
    private View getMsView(final int position ,View convertView, ViewGroup parent){
        final MsViewEntry entry = (MsViewEntry) getItem(position);
        final int layout = R.layout.contact_detail_history_list_item;
        View result = null;
        CallLogViewCache viewCache = null;

        if (convertView != null) {
            viewCache = (CallLogViewCache) convertView.getTag();
            if (viewCache.layoutResourceId == layout) {
                result = convertView;
            }
        }
        if (result == null) {
            result = mInflater.inflate(layout, null);
            viewCache = new CallLogViewCache(result, layout);
            result.setTag(viewCache);
        }

        viewCache.mCallType.setImageResource(getCallTypeIconId(entry.msType));
        viewCache.mCallTime.setText(entry.time);
        viewCache.mCallNumber.setText(entry.number);
        viewCache.mCallDuration.setText(entry.content);

        /*Begin: Modified by xiepengfei for call log add subscription 2012/05/21*/
        //viewCache.mViewType.setImageResource(R.drawable.ic_list_item_view_call);
        viewCache.mViewType.setImageResource(entry.subscription == 0?R.drawable.ms_g:R.drawable.ms_c);
        /*End: Modified by xiepengfei for call log add subscription 2012/05/21*/


        if (isMultipleChoiceMode()) {
            System.out.println(" ms viewCache.mCheckBox.setVisibility(View.VISIBLE)");
            viewCache.mCheckBox.setVisibility(View.VISIBLE);
            viewCache.mCheckBox.setClickable(false);
            if (mHashMapListChoiceItem.get(position) == null
                    || mHashMapListChoiceItem.get(position) == false) {
                viewCache.mCheckBox.setChecked(false);
            } else {
                viewCache.mCheckBox.setChecked(true);
            }
            final CheckBox box = viewCache.mCheckBox;
            result.setClickable(true);
            result.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    if (isChoiceAllItem()) {
                        isChoiceAllItem = false;
                        mCheckBox.setChecked(false);
                    }
                    if (box.isChecked()) {
                        box.setChecked(false);
                        mHashMapListChoiceItem.put(position, false);
                        --mListChoiceItemCount;
                        if (DBG)
                            Log.v(TAG, "position:" + position + ", isChecked:true  "
                                    + mListChoiceItemCount);
                    } else {
                        box.setChecked(true);
                        mHashMapListChoiceItem.put(position, true);
                        ++mListChoiceItemCount;
                        if (DBG)
                            Log.v(TAG, "position:" + position + ", isChecked:true   "
                                    + mListChoiceItemCount);
                    }
                }
            });
        } else {
            viewCache.mCheckBox.setVisibility(View.GONE);
            result.setClickable(false);
        }
        return result;
    }
/*End: Modified by xiepengfei for for add ms view 2012/05/30*/

    private View getEmailView(final int position, View convertView,
            ViewGroup parent) {
        final EmailViewEntry entry = (EmailViewEntry) getItem(position);
        final int layout = R.layout.contact_detail_history_list_item;
        View result = null;
        EmailViewCache viewCache = null;

        if (convertView != null) {
            viewCache = (EmailViewCache) convertView.getTag();
            if (viewCache.layoutResourceId == layout) {
                result = convertView;
            }
        }

        if (result == null) {
            result = mInflater.inflate(layout, null);
            viewCache = new EmailViewCache(result, layout);
            result.setTag(viewCache);
        }

        // init the view.
        viewCache.ivEmailType.setImageResource(entry.getEmailTypeRes());
        viewCache.tvName.setText(entry.displayName);
        viewCache.tvSubject.setText(entry.subject);
        viewCache.tvTime.setText(entry.timeStamp);

        /*if (entry.hasAttachment()) {
            viewCache.ivAttachMark.setVisibility(View.VISIBLE);
            viewCache.ivAttachMark.setImageResource(R.drawable.account_spinner_icon);
        } else {
            viewCache.ivAttachMark.setVisibility(View.GONE);
        }*/
        viewCache.ivAttachMark.setVisibility(View.VISIBLE);
        viewCache.ivAttachMark.setImageResource(R.drawable.email);

        // set check box.
        if (isMultipleChoiceMode()) {
            System.out.println(" email viewCache.mCheckBox.setVisibility(View.VISIBLE)");
            viewCache.mCheckBox.setVisibility(View.VISIBLE);
            viewCache.mCheckBox.setClickable(false);
            if (mHashMapListChoiceItem.get(position) == null
                    || mHashMapListChoiceItem.get(position) == false) {
                viewCache.mCheckBox.setChecked(false);
            } else {
                viewCache.mCheckBox.setChecked(true);
            }
            final CheckBox box = viewCache.mCheckBox;
            result.setClickable(true);
            result.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    if (isChoiceAllItem()) {
                        isChoiceAllItem = false;
                        mCheckBox.setChecked(false);
                    }
                    if (box.isChecked()) {
                        box.setChecked(false);
                        mHashMapListChoiceItem.put(position, false);
                        --mListChoiceItemCount;
                        if (DBG)
                            Log.v(TAG, "position:" + position + ", isChecked:true  "
                                    + mListChoiceItemCount);
                    } else {
                        box.setChecked(true);
                        mHashMapListChoiceItem.put(position, true);
                        ++mListChoiceItemCount;
                        if (DBG)
                            Log.v(TAG, "position:" + position + ", isChecked:true   "
                                    + mListChoiceItemCount);
                    }
                }
            });
        } else {
            viewCache.mCheckBox.setVisibility(View.GONE);
            result.setClickable(false);
        }
        return result;
    }

    private View getCallLogView(final int position, View convertView,
            ViewGroup parent) {
        final CallLogViewEntry entry = (CallLogViewEntry) getItem(position);
        final int layout = R.layout.contact_detail_history_list_item;
        View result = null;
        CallLogViewCache viewCache = null;

        if (convertView != null) {
            viewCache = (CallLogViewCache) convertView.getTag();
            if (viewCache.layoutResourceId == layout) {
                result = convertView;
            }
        }
        if (result == null) {
            result = mInflater.inflate(layout, null);
            viewCache = new CallLogViewCache(result, layout);
            result.setTag(viewCache);
        }

        viewCache.mCallType.setImageResource(getCallTypeIconId(entry.callType));
        viewCache.mCallTime.setText(entry.callTime);
        viewCache.mCallNumber.setText(entry.callNumber);
        viewCache.mCallDuration.setText(entry.callDuration);

        /*Begin: Modified by xiepengfei for call log add subscription 2012/05/21*/
        //viewCache.mViewType.setImageResource(R.drawable.ic_list_item_view_call);
        viewCache.mViewType.setImageResource(entry.subscription == 0?R.drawable.call_dial_g_call_icon:R.drawable.call_dial_c_call_icon);
        /*End: Modified by xiepengfei for call log add subscription 2012/05/21*/


        if (isMultipleChoiceMode()) {
            viewCache.mCheckBox.setVisibility(View.VISIBLE);
            viewCache.mCheckBox.setClickable(false);
            if (mHashMapListChoiceItem.get(position) == null
                    || mHashMapListChoiceItem.get(position) == false) {
                viewCache.mCheckBox.setChecked(false);
            } else {
                viewCache.mCheckBox.setChecked(true);
            }
            final CheckBox box = viewCache.mCheckBox;
            result.setClickable(true);
            result.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    if (isChoiceAllItem()) {
                        isChoiceAllItem = false;
                        mCheckBox.setChecked(false);
                    }
                    if (box.isChecked()) {
                        box.setChecked(false);
                        mHashMapListChoiceItem.put(position, false);
                        --mListChoiceItemCount;
                        if (DBG)
                            Log.v(TAG, "position:" + position + ", isChecked:true  "
                                    + mListChoiceItemCount);
                    } else {
                        box.setChecked(true);
                        mHashMapListChoiceItem.put(position, true);
                        ++mListChoiceItemCount;
                        if (DBG)
                            Log.v(TAG, "position:" + position + ", isChecked:true   "
                                    + mListChoiceItemCount);
                    }
                }
            });
        } else {
            viewCache.mCheckBox.setVisibility(View.GONE);
            result.setClickable(false);
        }
        return result;
    }

    @Override
    public int getItemViewType(int position) {
        return mAllViewEntries.get(position).getViewType();
    }

    @Override
    public int getCount() {
        return mAllViewEntries.size();
    }

    public BaseViewEntry getItem(int position) {
        return mAllViewEntries.get(position);
    }

    public boolean isDetailView(int position) {
        if (getItemViewType(position) != VIEW_TYPE_TITLE
                && getItemViewType(position) != VIEW_TYPE_SEPARATOR_ENTRY) {
            return true;
        } else {
            return false;
        }
    }

    public long getItemId(int position) {
        final BaseViewEntry entry = mAllViewEntries.get(position);
        if (entry != null) {
            return entry.getId();
        }
        return -1;
    }

    @Override
    public boolean areAllItemsEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getViewTypeCount() {
        // TODO Auto-generated method stub
        return VIEW_TYPT_COUNT;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).isEnabled();
    }

    /**
     * @param callType
     * @return 1.incoming call;2.outgoing call;3.missed call;4.voice mail call
     */
    /*Begin: Modified by xiepengfei for modify the calltype icon 2012/05/28*/
    private int getCallTypeIconId(int callType) {
        int result = -1;
        switch (callType) {
            case 1:
                result = com.android.contacts.R.drawable.call_received_voicecall;
                break;
            case 2:
                result = com.android.contacts.R.drawable.call_dialled_voicecall;
                break;
            case 3:
                result = com.android.contacts.R.drawable.call_missed_voicecall;
                break;
            case 4:
                result = com.android.contacts.R.drawable.ic_call_type_voicemail;
                break;
            default:
                break;
        }
        return result;
    }
    /*End: Modified by xiepengfei for modify the calltype icon 2012/05/28*/
    public boolean isMultipleChoiceMode() {
        return isMultipleChoiceMode;
    }

    public void setMultipleChoiceMode(boolean isMultipleChoiceMode) {
        this.isMultipleChoiceMode = isMultipleChoiceMode;
    }

    public boolean isChoiceAllItem() {
        return isChoiceAllItem;
    }

    public void setChoiceAllItem(boolean isChoiceAllItem) {
        if (DBG)
            Log.v(TAG, "setChoiceAllItem:" + isChoiceAllItem);
        this.isChoiceAllItem = isChoiceAllItem;
        mListChoiceItemCount = 0;

        int count2 = mAllViewEntries.size();
        if (count2 > 0) {
            for (int i = 0; i < count2; i++) {
                if (isDetailView(i)) {
                    mHashMapListChoiceItem.put(i, isChoiceAllItem);
                    ++mListChoiceItemCount;
                } else {
                    mHashMapListChoiceItem.put(i, false);
                }
            }
        }

        if (!isChoiceAllItem)
            mListChoiceItemCount = 0;
    }

    public static HashMap<Integer, Boolean> getHashMapListChoiceItem() {
        return mHashMapListChoiceItem;
    }

    public int getListChoiceItemCount() {
        return mListChoiceItemCount;
    }

    public void setListChoiceItemCount(int mListChoiceItemCount) {
        this.mListChoiceItemCount = mListChoiceItemCount;
    }

    public static class TitleViewCache {
        public final TextView mTitle;
        public final int layoutResourceId;

        public TitleViewCache(View view, int layoutResourceId) {
            this.mTitle = (TextView) view.findViewById(R.id.title);
            this.layoutResourceId = layoutResourceId;
        }
    }

    public static class CallLogViewCache {
        public final ImageView mCallType;
        public final TextView mCallTime;
        public final TextView mCallNumber;
        public final TextView mCallDuration;
        public final ImageView mViewType;
        public final CheckBox mCheckBox;
        public final int layoutResourceId;

        public CallLogViewCache(View view, int layout) {
            mCallType = (ImageView) view
                    .findViewById(R.id.contact_detail_call_log_item_imageview_left);
            mCallTime = (TextView) view
                    .findViewById(R.id.contact_detail_call_log_item_textview_time);
            mCallNumber = (TextView) view
                    .findViewById(R.id.contact_detail_call_log_item_textview_number);
            mCallDuration = (TextView) view
                    .findViewById(R.id.contact_detail_call_log_item_textview_content);
            mViewType = (ImageView) view
                    .findViewById(R.id.contact_detail_call_log_item_imageview_right);
            mCheckBox = (CheckBox) view
                    .findViewById(R.id.contact_detail_call_log_item_checkbox);
            layoutResourceId = layout;
        }
    }

    public static class EmailViewCache {
        public final ImageView ivEmailType; // mark the email is in or out.
        public final ImageView ivAttachMark; // if there is attach file, then set it visible.
        public final TextView tvTime;
        public final TextView tvName;
        public final TextView tvSubject;
        public final CheckBox mCheckBox;

        public final int layoutResourceId;

        public EmailViewCache(View view, int layout) {
            ivEmailType = (ImageView) view
                    .findViewById(R.id.contact_detail_call_log_item_imageview_left);
            tvTime = (TextView) view
                    .findViewById(R.id.contact_detail_call_log_item_textview_time);
            tvName = (TextView) view
                    .findViewById(R.id.contact_detail_call_log_item_textview_number);
            tvSubject = (TextView) view
                    .findViewById(R.id.contact_detail_call_log_item_textview_content);
            ivAttachMark = (ImageView) view
                    .findViewById(R.id.contact_detail_call_log_item_imageview_right);
            mCheckBox = (CheckBox) view
                    .findViewById(R.id.contact_detail_call_log_item_checkbox);

            layoutResourceId = layout;
        }
    }

    /* Begin: Modified by xiepengfei for 2012/03/30 */
    /**
     * Base class for an item in the {@link HistoryViewAdapter} list of data,
     * which is supplied to the {@link ListView}.
     */
    public class BaseViewEntry {
        private final int viewTypeForAdapter;
        protected long id = -1;
        public long date;

        /** Whether or not the entry can be focused on or not. */
        protected boolean isEnabled = false;

        BaseViewEntry(int viewType) {
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
        public long getDate(){
            return date;
        }
        /**
         * Called when the entry is clicked. Only {@link #isEnabled} entries can
         * get clicked.
         *
         * @param clickedView {@link View} that was clicked (Used, for example,
         *            as the anchor view for a popup.)
         * @param fragmentListener {@link Listener} set to
         *            {@link ContactDetailFragment}
         */
        // public void click(View clickedView, Listener fragmentListener) {
        // }
    }

    public class TitleViewEntry extends BaseViewEntry {
        public String title;

        TitleViewEntry() {
            super(HistoryViewAdapter.VIEW_TYPE_TITLE);
        }
    }

    public class CallLogViewEntry extends BaseViewEntry {
        public int callType;// 1 incoming, 2 outgoing, 3missing, 4voice
        public String callTime;
        public String callNumber;
        public String callDuration;
        public String voicemailUri; // add by bxinchun 2012-08-13

        /*Begin: Modified by xiepengfei for call log add subscription 2012/05/21*/
        public int subscription;
        /*End: Modified by xiepengfei for call log add subscription 2012/05/21*/

        CallLogViewEntry() {
            super(HistoryViewAdapter.VIEW_TYPE_CALL_LOG);
            isEnabled = true;
        }
    }

    public class SeparatorViewEntry extends BaseViewEntry {

        /**
         * Whether or not the entry is in a subsection (if true then the
         * contents will be indented to the right)
         */
        private boolean mIsInSubSection = false;

        SeparatorViewEntry() {
            super(HistoryViewAdapter.VIEW_TYPE_SEPARATOR_ENTRY);
        }

        public void setIsInSubSection(boolean isInSubSection) {
            mIsInSubSection = isInSubSection;
        }

        public boolean isInSubSection() {
            return mIsInSubSection;
        }
    }
    /* End: Modified by xiepengfei for 2012/03/30 */

    /*Begin: Modified by xiepengfei for ms info cache 2012/06/01*/
    public class MsViewEntry extends BaseViewEntry{
        public int msType; //1.incoming 2,outgoing
        public String time;
        public String number;
        public String content;
        public int subscription;
        public int isMmsOrSms;//0,sms,1,mms

        MsViewEntry(){
            super(HistoryViewAdapter.VIEW_TYPE_MS);
            isEnabled = true;
        }
    }
    /*End: Modified by xiepengfei for ms info cache 2012/06/01*/

    /*Begin: Added Email View by bxinchun 2012-08 */
    public class EmailViewEntry extends BaseViewEntry {
        private long mailboxKey;
        private long accountKey;
        private String displayName;
        private String subject;
        private String timeStamp;
        private int flagRead;
        private int flagFavorite;
        private int flagAttachment;
        //private int flags;

        private String fromList;
        private String toList;
        private String ccList;
        private String bccList;
        private String replyToList;

        /* package */ long mMessageId = -1;
        /* package */ long mMailboxId = -1;
        /* package */ long mAccountId = -1;
        /* package */ int type = -1;

        private EmailViewEntry() {
            super(HistoryViewAdapter.VIEW_TYPE_EMAIL);
            isEnabled = true;
        }

        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }

        public EmailViewEntry(Cursor cr) {
            this();

            id = cr.getLong(EmailInfo.COLUMN_INDEX_ID);
            mMessageId = id;
            mailboxKey = cr.getLong(EmailInfo.COLUMN_INDEX_MAILBOX_KEY);
            mMailboxId = mailboxKey;
            accountKey = cr.getLong(EmailInfo.COLUMN_INDEX_ACCOUNT_KEY);
            mAccountId = accountKey;

            displayName = cr.getString(EmailInfo.COLUMN_INDEX_DISPLAYNAME);
            subject = cr.getString(EmailInfo.COLUMN_INDEX_SUBJECT);
            date = cr.getLong(EmailInfo.COLUMN_INDEX_TIMESTAMP);
            flagRead = cr.getInt(EmailInfo.COLUMN_INDEX_FLAG_READ);
            flagFavorite = cr.getInt(EmailInfo.COLUMN_INDEX_FLAG_FAVORITE);
            flagAttachment = cr.getInt(EmailInfo.COLUMN_INDEX_FLAG_ATTACHMENT);

            fromList = cr.getString(EmailInfo.COLUMN_INDEX_FROM_LIST);
            toList = cr.getString(EmailInfo.COLUMN_INDEX_TO_LIST);
            ccList = cr.getString(EmailInfo.COLUMN_INDEX_CC_LIST);
            bccList = cr.getString(EmailInfo.COLUMN_INDEX_BCC_LIST);
            replyToList = cr.getString(EmailInfo.COLUMN_INDEX_REPLYTO_LIST);
        }

        public boolean isInMail() {
            return type == EmailInfo.MAILBOX_TYPE_INBOX;
        }

        public boolean isDraftMail() {
            return type == EmailInfo.MAILBOX_TYPE_DRAFTS;
        }

        public boolean isSentMail() {
            return type == EmailInfo.MAILBOX_TYPE_SENT;
        }

        public boolean isOutEmail() {
            return type == EmailInfo.MAILBOX_TYPE_OUTBOX;
        }

        public boolean isReaded() {
            return flagRead > 0;
        }

        public boolean isFavorite() {
            return flagFavorite > 0;
        }

        public boolean hasAttachment() {
            return flagAttachment > 0;
        }

        public int getEmailTypeRes() {
            switch (type) {
                case EmailInfo.MAILBOX_TYPE_INBOX:
                    return R.drawable.email_mark_received;
                case EmailInfo.MAILBOX_TYPE_SENT:
                    return R.drawable.email_mark_sent;
                default:
                    return 0;
            }
        }
    }
    /*End: Added Email View by bxinchun 2012-08 */

}
/*End: Modified by xiepengfei for help display call log ,sms and other history info 2012/05/19*/