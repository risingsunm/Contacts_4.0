/**
 * Copy Right Ahong
 * @author mzikun
 * 2012-8-22
 *
 */
package com.android.contacts.ahong;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.android.contacts.R;


import java.util.List;

/**
 * @author mzikun
 *
 */
public class AhongFactoryActivity extends PreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> arg0) {
        loadHeadersFromResource(R.xml.ahong_factorymode, arg0);
    }

    @Override
    protected void onCreate(Bundle arg0) {
        this.getActionBar().setTitle("Factory Mode");
        this.getActionBar().setIcon(getResources().getDrawable(R.drawable.ic_menu_phone_holo_light));
        super.onCreate(arg0);
    }

}
