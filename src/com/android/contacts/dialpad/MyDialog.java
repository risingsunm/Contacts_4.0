package com.android.contacts.dialpad;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.android.contacts.R;

/*Begin: Modified by siliangqi for associate_dial 2012-6-1*/
public class MyDialog extends Dialog {
    private Context mContext;
    private Cursor myCursor;
    private static final String TAG = "MyDialog";
    private int myPosition = -1;
    private String input;
    private Button cancellButton;
    public MyDialog(Context context, int theme,Cursor cursor,String input) {
        super(context, theme);
        // TODO Auto-generated constructor stub
        mContext = context;
        myCursor = cursor;
        this.input = input;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.mydialog);
        if(myCursor!=null){
            LayoutInflater li = LayoutInflater.from(mContext);
            LinearLayout ll = (LinearLayout)li.inflate(R.layout.bottom_single_toolbar, null);
            cancellButton = (Button)ll.findViewById(R.id.btn_cancel);
            cancellButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    dismiss();
                }
            });
            ListView lv = (ListView) ll.findViewById(R.id.myListView);
            myCursor.moveToFirst();
            lv.setClickable(true);
            lv.setAdapter(new MyAdapter(mContext, myCursor,input));
            lv.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // TODO Auto-generated method stub
                    myPosition = position;
                    dismiss();
                }
            });
            setContentView(ll);
        }
    }
    public int getMyPosition(){
        return myPosition;
    }
}
/*End: Modified by siliangqi for associate_dial 2012-6-1*/
