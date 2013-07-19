package com.android.contacts.dialpad;

import android.content.Context;
import android.database.Cursor;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.util.UriUtils;

/*Begin: Modified by siliangqi for associate_dial 2012-6-1*/
public class MyAdapter extends BaseAdapter {
    private ImageView searchResult_photo;
    private TextView searchResult_number;
    private TextView searchResult_displayname;
    private LayoutInflater mInflater;
    private Cursor mCursor;
    private Context mContext;
    private ContactPhotoManager mContactPhotoManager;
    private String input;
    private static final String TAG = "MyAdapter";
    public MyAdapter(Context context,Cursor cursor,String input){
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mCursor = cursor;
        mContactPhotoManager = ContactPhotoManager.getInstance(context);
        this.input = input;
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View v = mInflater.inflate(R.layout.mydialog, null);
        searchResult_photo = (ImageView)v.findViewById(R.id.searchResult_photo);
        searchResult_number = (TextView)v.findViewById(R.id.searchResult_number);
        searchResult_displayname = (TextView)v.findViewById(R.id.searchResult_displayname);
        mCursor.moveToPosition(position);
        mContactPhotoManager.loadPhoto(searchResult_photo, UriUtils.parseUriOrNull(mCursor.getString(mCursor.getColumnIndex("photo_uri"))), false, true);
      //number match
        String s1 = mCursor.getString(mCursor.getColumnIndex("normalized_number"));
        String s2 = mCursor.getString(mCursor.getColumnIndex("contacts_name"));
        SpannableStringBuilder ssb1 = null;
        SpannableStringBuilder ssb2 = null;
        if(s2==null){
            try {
            int pos6 = s1.indexOf(input);
            ssb1 = new SpannableStringBuilder(s1);
            ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos6, pos6+input.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb2 = new SpannableStringBuilder(mContext.getString(R.string.unknown));
            }catch (IndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                searchResult_number.setText(s1);
                searchResult_displayname.setText(s2);
                return v;
            }
        }else{
        try {
            String s3 = DialpadFragment.myNormalize(s2);
            int pos1 = s1.indexOf(input);
            int pos2 = s3.indexOf(input);
            if(s3.length()!=input.length())
                pos2 = -1;
            ssb1 = new SpannableStringBuilder(s1);
            ssb2 = new SpannableStringBuilder(s2);
                //prefix match
                String s4 = mCursor.getString(mCursor.getColumnIndex("short_name"));
                if(s4==null){
                    pos2 = s2.indexOf(input);
                    ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos2, pos2+input.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }else if(s4.length()==s2.length()){
                    pos2 = s4.indexOf(input);
                    if(pos2>=0)
                        ssb2.setSpan(new ForegroundColorSpan(0xff31b6e7), pos2, pos2+input.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    else{
                        String s5 = mCursor.getString(mCursor.getColumnIndex("full_name"));
                        String[] s6 = s5.split("\\s+");
                        if(s6.length==s2.length()){
                            String s7 = filterSpace(s5);
                            int pos3 = s7.indexOf(input);
                            if(pos3>=0){
                                int count=0,start=0,end=0;
                                for(int i=0;i<s6.length;i++){
                                    count += s6[i].length();
                                    if(pos3>=(count-s6[i].length())&&pos3<count){
                                        start = i;
                                    }
                                    if((pos3+input.length())>(count-s6[i].length())&&(pos3+input.length())<=count){
                                        end = i;
                                        ssb2.setSpan(new ForegroundColorSpan(0xff31b6e7), start, end+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        break;
                                    }
                                }
                            }else if(pos1>-1){
                                //number match
                                ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos1, pos1+input.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                    }
                }else{
                    int pos4 = s4.indexOf(input);
                    if(pos4>=0){
                        int count = 0;
                        String short_name_normal = mCursor.getString(mCursor.getColumnIndex("short_name_normal"));
                        String s8 = mCursor.getString(mCursor.getColumnIndex("hanzi_name"));
                        String s9[] = s8.split("\\s+");
                        int start = pos4, end = pos4+input.length();
                        int symbolCount = DialpadFragment.containOtherSymbol(short_name_normal,0,pos4);
                        int temp = 0;
                        if(symbolCount!=0){
                            do{
                                temp = DialpadFragment.containOtherSymbol(short_name_normal,start,start+symbolCount);
                                start += symbolCount;
                                symbolCount = temp;
                            }while(symbolCount!=0);
                        }
                        int finalStart = 0;
                        for(int i=0;i<start;i++){
                            finalStart += s9[i].length();
                        }
                        end = finalStart + input.length();
                        symbolCount = DialpadFragment.containOtherSymbol(short_name_normal,pos4,pos4+input.length());
                        temp = 0;
                        if(symbolCount!=0){
                            do{
                                temp = DialpadFragment.containOtherSymbol(short_name_normal,end,end+symbolCount);
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
                        String s10 = mCursor.getString(mCursor.getColumnIndex("pinyin_name"));
                        String s11[] = s10.split("\\s+");
                        String s12 = filterSpace(s10);
                        String s13 = mCursor.getString(mCursor.getColumnIndex("hanzi_name"));
                        String s14[] = s13.split("\\s+");
                        int pos5 = s12.indexOf(input);
                        if(pos5>-1){
                            int count1=0,count2=0,start=0,end=0;
                            for(int i=0;i<s11.length;i++){
                                count1 += s11[i].length();
                                count2 += s14[i].length();
                                if(pos5>=(count1-s11[i].length())&&pos5<count1){
                                    start = count2-s14[i].length()+pos5-(count1-s11[i].length());
                                }
                                if((pos5+input.length())>(count1-s11[i].length())&&(pos5+input.length())<=count1){
                                    if(s14[i].length()!=1)
                                        end = count2-s14[i].length()+pos5+input.length()-(count1-s11[i].length())-1;
                                    else
                                        end = count2-s14[i].length();
                                    ssb2.setSpan(new ForegroundColorSpan(0xff31b6e7), start, end+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    break;
                                }
                            }
                        }else if(pos1>-1){
                            //number match
                            ssb1.setSpan(new ForegroundColorSpan(0xff31b6e7), pos1, pos1+input.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
        } catch (IndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            searchResult_number.setText(s1);
            searchResult_displayname.setText(s2);
            return v;
        }catch (NullPointerException e){
            e.printStackTrace();
            ssb1 = new SpannableStringBuilder(s1);
            ssb2 = new SpannableStringBuilder(s2);
        }
        }
        searchResult_number.setText(ssb1);
        searchResult_displayname.setText(ssb2);
        return v;
    }
    public static String filterSpace(String s) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
        if (s.charAt(i) == ' ')
            continue;
        sb.append(s.charAt(i));
    }
    return sb.toString();
}

}
/*End: Modified by siliangqi for associate_dial 2012-6-1*/
