package com.android.contacts.detail;



import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;

/*Begin: Modified by xiepengfei for delete tools class 2012/05/18*/

/**
 * The base class about the handler with progress dialog function.
 */
public abstract class BaseProgressQueryHandler extends AsyncQueryHandler {
    private ProgressDialog dialog;
    private int progress;

    public BaseProgressQueryHandler(ContentResolver resolver) {
        super(resolver);
    }

    /**
     * Sets the progress dialog.
     * @param dialog the progress dialog.
     */
    public void setProgressDialog(ProgressDialog dialog) {
        this.dialog = dialog;
    }

    /**
     * Sets the max progress.
     * @param max the max progress.
     */
    public void setMax(int max) {
        if (dialog != null) {
            dialog.setMax(max);
        }
    }

    /**
     * Shows the progress dialog. Must be in UI thread.
     */
    public void showProgressDialog() {
        if (dialog != null) {
            dialog.show();
        }
    }
    /**
     * update progress
     */
    public void updateProgress(){
        if (dialog != null) {
            dialog.setProgress(progress);
        }
    }

    /**
     * Rolls the progress as + 1.
     * @return if progress >= max.
     */
    protected boolean progress() {
        if (dialog != null) {
            return ++progress >= dialog.getMax();
        } else {
            return false;
        }
    }

    /**
     * Dismisses the progress dialog.
     */
    protected void dismissProgressDialog() {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            progress = 0;
            dialog = null;
        }
    }
}
/*End: Modified by xiepengfei for delete tools class 2012/05/18*/