/* Begin: Modified by siliangqi for 1lev_search 2012-4-10 */
package com.android.contacts.list;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import android.widget.SearchView.OnQueryTextListener;
import com.android.contacts.activities.ActionBarAdapter;
import android.graphics.Rect;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class EditText_Search extends EditText {
    private OnQueryTextListener q;
    private static final String TAG = "EditText_Search";

    public EditText_Search(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public void setQueryTextListener(OnQueryTextListener q) {
        this.q = q;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start,
            int lengthBefore, int lengthAfter) {
        // TODO Auto-generated method stub
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        String s = text.toString();
        if (q == null) {
            return;
        }
        q.onQueryTextChange(s);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction,
            Rect previouslyFocusedRect) {
        // TODO Auto-generated method stub
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (q == null)
            return;
        if(q.getClass().getName().equals("com.android.contacts.activities.ContactSelectionActivity"))
            return;
        if (focused == true && ((ActionBarAdapter) q).getSearchMode() == false)
            ((ActionBarAdapter) q).setSearchMode(true);
    }

}
/* End: Modified by siliangqi for 1lev_search 2012-4-10 */