/**
 * Copy Right Ahong
 * @author mzikun
 * 2012-8-13
 *
 */
package com.android.contacts.ahong;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author mzikun
 *
 */
public class AhongDialogInputQRDToolsReceiver extends BroadcastReceiver {
    private static final boolean DEBUG_CONST = true;
    private static final String TAG = "AhongDialogInputQRDToolsReceiver";

//    private static final String AHONGDIALOGINPUTACTION = "android.intent.action.qrdtools_mainui";
    private static final String AHONGDIALOGINPUTACTION = "android.intent.action.factoryaction";

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        if (DEBUG_CONST) {
            Log.d(TAG, "onReceive");
        }
        if (arg1.getAction().equals("com.android.contacts.SPECIALCHARSEQUENCEMGR.ACTION")) {
            if (DEBUG_CONST) {
                Log.d(TAG, "onReceive->arg1.getAction().equals->begin");
            }
            Intent intent = new Intent();
            intent.setAction(AHONGDIALOGINPUTACTION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            arg0.startActivity(intent);
            if (DEBUG_CONST) {
                Log.d(TAG, "onReceive->arg1.getAction().equals->end");
            }
        }
    }
}
