/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.detail;

import com.android.contacts.ContactLoader;
import com.android.contacts.ContactLoader.Result;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.util.ContactBadgeUtil;
import com.android.contacts.util.HtmlUtils;
import com.android.contacts.util.StreamItemEntry;
import com.android.contacts.util.StreamItemPhotoEntry;
import com.google.common.annotations.VisibleForTesting;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.Entity.NamedContentValues;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.StreamItems;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/*Begin: Modified by xiepengfei for add some help function 2012/05/23*/
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.provider.CallLog.Calls;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
/*End: Modified by xiepengfei for add some help function 2012/05/23*/
/**
 * This class contains utility methods to bind high-level contact details
 * (meaning name, phonetic name, job, and attribution) from a
 * {@link ContactLoader.Result} data object to appropriate {@link View}s.
 */
public class ContactDetailDisplayUtils {
    private static final String TAG = "ContactDetailDisplayUtils";

    private static final int PHOTO_FADE_IN_ANIMATION_DURATION_MILLIS = 100;

    /**
     * Tag object used for stream item photos.
     */
    public static class StreamPhotoTag {
        public final StreamItemEntry streamItem;
        public final StreamItemPhotoEntry streamItemPhoto;

        public StreamPhotoTag(StreamItemEntry streamItem, StreamItemPhotoEntry streamItemPhoto) {
            this.streamItem = streamItem;
            this.streamItemPhoto = streamItemPhoto;
        }

        public Uri getStreamItemPhotoUri() {
            final Uri.Builder builder = StreamItems.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, streamItem.getId());
            builder.appendPath(StreamItems.StreamItemPhotos.CONTENT_DIRECTORY);
            ContentUris.appendId(builder, streamItemPhoto.getId());
            return builder.build();
        }
    }

    private ContactDetailDisplayUtils() {
        // Disallow explicit creation of this class.
    }

    /**
     * Returns the display name of the contact, using the current display order setting.
     * Returns res/string/missing_name if there is no display name.
     */
    public static CharSequence getDisplayName(Context context, Result contactData) {
        CharSequence displayName = contactData.getDisplayName();
        CharSequence altDisplayName = contactData.getAltDisplayName();
        ContactsPreferences prefs = new ContactsPreferences(context);
        CharSequence styledName = "";
        if (!TextUtils.isEmpty(displayName) && !TextUtils.isEmpty(altDisplayName)) {
            if (prefs.getDisplayOrder() == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY) {
                styledName = displayName;
            } else {
                styledName = altDisplayName;
            }
        } else {
            styledName = context.getResources().getString(R.string.missing_name);
        }
        return styledName;
    }

    /**
     * Returns the phonetic name of the contact or null if there isn't one.
     */
    public static String getPhoneticName(Context context, Result contactData) {
        String phoneticName = contactData.getPhoneticName();
        if (!TextUtils.isEmpty(phoneticName)) {
            return phoneticName;
        }
        return null;
    }

    /**
     * Returns the attribution string for the contact, which may specify the contact directory that
     * the contact came from. Returns null if there is none applicable.
     */
    public static String getAttribution(Context context, Result contactData) {
        if (contactData.isDirectoryEntry()) {
            String directoryDisplayName = contactData.getDirectoryDisplayName();
            String directoryType = contactData.getDirectoryType();
            String displayName = !TextUtils.isEmpty(directoryDisplayName)
                    ? directoryDisplayName
                    : directoryType;
            return context.getString(R.string.contact_directory_description, displayName);
        }
        return null;
    }

    /**
     * Returns the organization of the contact. If several organizations are given,
     * the first one is used. Returns null if not applicable.
     */
    public static String getCompany(Context context, Result contactData) {
        final boolean displayNameIsOrganization = contactData.getDisplayNameSource()
                == DisplayNameSources.ORGANIZATION;
        for (Entity entity : contactData.getEntities()) {
            for (NamedContentValues subValue : entity.getSubValues()) {
                final ContentValues entryValues = subValue.values;
                final String mimeType = entryValues.getAsString(Data.MIMETYPE);

                if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    final String company = entryValues.getAsString(Organization.COMPANY);
                    final String title = entryValues.getAsString(Organization.TITLE);
                    final String combined;
                    // We need to show company and title in a combined string. However, if the
                    // DisplayName is already the organization, it mirrors company or (if company
                    // is empty title). Make sure we don't show what's already shown as DisplayName
                    if (TextUtils.isEmpty(company)) {
                        combined = displayNameIsOrganization ? null : title;
                    } else {
                        if (TextUtils.isEmpty(title)) {
                            combined = displayNameIsOrganization ? null : company;
                        } else {
                            if (displayNameIsOrganization) {
                                combined = title;
                            } else {
                                combined = context.getString(
                                        R.string.organization_company_and_title,
                                        company, title);
                            }
                        }
                    }

                    if (!TextUtils.isEmpty(combined)) {
                        return combined;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the contact photo to display in the given {@link ImageView}. If bitmap is null, the
     * default placeholder image is shown.
     */
    public static void setPhoto(Context context, Result contactData, ImageView photoView) {
        if (contactData.isLoadingPhoto()) {
            photoView.setImageBitmap(null);
            return;
        }
        byte[] photo = contactData.getPhotoBinaryData();
        /*Begin: Modified by bxinchun for setting a default contact photo 2012/07/25*/
        /*Bitmap bitmap = photo != null ? BitmapFactory.decodeByteArray(photo, 0, photo.length)
                : ContactBadgeUtil.loadDefaultAvatarPhoto(context, true, false);*/

        Bitmap bitmap;
        if (photo != null) {
            bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
        } else {
            int contactId = 0;
            Uri contactUri = contactData.getUri();
            if (contactUri != null) {
                try {
                    contactId = (int) ContentUris.parseId(contactUri);
                } catch (Exception e) {
                }
            }
            bitmap = ContactBadgeUtil.loadDefaultAvatarPhoto(context, contactId);
        }
        /*End: Modified by bxinchun for setting a default contact photo 2012/07/25*/
        boolean fadeIn = contactData.isDirectoryEntry();
        if (photoView.getDrawable() == null && fadeIn) {
            AlphaAnimation animation = new AlphaAnimation(0, 1);
            animation.setDuration(PHOTO_FADE_IN_ANIMATION_DURATION_MILLIS);
            animation.setInterpolator(new AccelerateInterpolator());
            photoView.startAnimation(animation);
        }
        photoView.setImageBitmap(bitmap);
    }

    /**
     * Sets the starred state of this contact.
     */
    public static void setStarred(Result contactData, CheckBox starredView) {
        // Check if the starred state should be visible
        if (!contactData.isDirectoryEntry() && !contactData.isUserProfile()) {
            starredView.setVisibility(View.VISIBLE);
            starredView.setChecked(contactData.getStarred());
        } else {
            starredView.setVisibility(View.GONE);
        }
    }

    /**
     * Set the social snippet text. If there isn't one, then set the view to gone.
     */
    public static void setSocialSnippet(Context context, Result contactData, TextView statusView,
            ImageView statusPhotoView) {
        if (statusView == null) {
            return;
        }

        CharSequence snippet = null;
        String photoUri = null;
        if (!contactData.getStreamItems().isEmpty()) {
            StreamItemEntry firstEntry = contactData.getStreamItems().get(0);
            snippet = HtmlUtils.fromHtml(context, firstEntry.getText());
            if (!firstEntry.getPhotos().isEmpty()) {
                StreamItemPhotoEntry firstPhoto = firstEntry.getPhotos().get(0);
                photoUri = firstPhoto.getPhotoUri();

                // If displaying an image, hide the snippet text.
                snippet = null;
            }
        }
        setDataOrHideIfNone(snippet, statusView);
        if (photoUri != null) {
            ContactPhotoManager.getInstance(context).loadPhoto(
                    statusPhotoView, Uri.parse(photoUri), true, false,
                    ContactPhotoManager.DEFAULT_BLANK);
            statusPhotoView.setVisibility(View.VISIBLE);
        } else {
            statusPhotoView.setVisibility(View.GONE);
        }
    }

    /** Creates the view that represents a stream item. */
    public static View createStreamItemView(LayoutInflater inflater, Context context,
            StreamItemEntry streamItem, LinearLayout parent,
            View.OnClickListener photoClickListener) {
        View container = inflater.inflate(R.layout.stream_item_container, parent, false);
        ViewGroup contentTable = (ViewGroup) container.findViewById(R.id.stream_item_content);

        ContactPhotoManager contactPhotoManager = ContactPhotoManager.getInstance(context);
        List<StreamItemPhotoEntry> photos = streamItem.getPhotos();
        final int photoCount = photos.size();

        // This stream item only has text.
        if (photoCount == 0) {
            View textOnlyContainer = inflater.inflate(R.layout.stream_item_row_text, contentTable,
                    false);
            addStreamItemText(context, streamItem, textOnlyContainer);
            contentTable.addView(textOnlyContainer);
        } else {
            // This stream item has text and photos. Process the photos, two at a time.
            for (int index = 0; index < photoCount; index += 2) {
                final StreamItemPhotoEntry firstPhoto = photos.get(index);
                if (index + 1 < photoCount) {
                    // Put in two photos, side by side.
                    final StreamItemPhotoEntry secondPhoto = photos.get(index + 1);
                    View photoContainer = inflater.inflate(R.layout.stream_item_row_two_images,
                            contentTable, false);
                    loadPhoto(contactPhotoManager, streamItem, firstPhoto, photoContainer,
                            R.id.stream_item_first_image, photoClickListener);
                    loadPhoto(contactPhotoManager, streamItem, secondPhoto, photoContainer,
                            R.id.stream_item_second_image, photoClickListener);
                    contentTable.addView(photoContainer);
                } else {
                    // Put in a single photo
                    View photoContainer = inflater.inflate(
                            R.layout.stream_item_row_one_image, contentTable, false);
                    loadPhoto(contactPhotoManager, streamItem, firstPhoto, photoContainer,
                            R.id.stream_item_first_image, photoClickListener);
                    contentTable.addView(photoContainer);
                }
            }

            // Add text, comments, and attribution if applicable
            View textContainer = inflater.inflate(R.layout.stream_item_row_text, contentTable,
                    false);
            // Add extra padding between the text and the images
            int extraVerticalPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.detail_update_section_between_items_vertical_padding);
            textContainer.setPadding(textContainer.getPaddingLeft(),
                    textContainer.getPaddingTop() + extraVerticalPadding,
                    textContainer.getPaddingRight(),
                    textContainer.getPaddingBottom());
            addStreamItemText(context, streamItem, textContainer);
            contentTable.addView(textContainer);
        }

        if (parent != null) {
            parent.addView(container);
        }

        return container;
    }

    /** Loads a photo into an image view. The image view is identified by the given id. */
    private static void loadPhoto(ContactPhotoManager contactPhotoManager,
            final StreamItemEntry streamItem, final StreamItemPhotoEntry streamItemPhoto,
            View photoContainer, int imageViewId, View.OnClickListener photoClickListener) {
        final View frame = photoContainer.findViewById(imageViewId);
        final View pushLayerView = frame.findViewById(R.id.push_layer);
        final ImageView imageView = (ImageView) frame.findViewById(R.id.image);
        if (photoClickListener != null) {
            pushLayerView.setOnClickListener(photoClickListener);
            pushLayerView.setTag(new StreamPhotoTag(streamItem, streamItemPhoto));
            pushLayerView.setFocusable(true);
            pushLayerView.setEnabled(true);
        } else {
            pushLayerView.setOnClickListener(null);
            pushLayerView.setTag(null);
            pushLayerView.setFocusable(false);
            // setOnClickListener makes it clickable, so we need to overwrite it
            pushLayerView.setClickable(false);
            pushLayerView.setEnabled(false);
        }
        contactPhotoManager.loadPhoto(imageView, Uri.parse(streamItemPhoto.getPhotoUri()), true,
                false, ContactPhotoManager.DEFAULT_BLANK);
    }

    @VisibleForTesting
    static View addStreamItemText(Context context, StreamItemEntry streamItem, View rootView) {
        TextView htmlView = (TextView) rootView.findViewById(R.id.stream_item_html);
        TextView attributionView = (TextView) rootView.findViewById(
                R.id.stream_item_attribution);
        TextView commentsView = (TextView) rootView.findViewById(R.id.stream_item_comments);
        ImageGetter imageGetter = new DefaultImageGetter(context.getPackageManager());

        // Stream item text
        setDataOrHideIfNone(HtmlUtils.fromHtml(context, streamItem.getText(), imageGetter, null),
                htmlView);
        // Attribution
        setDataOrHideIfNone(ContactBadgeUtil.getSocialDate(streamItem, context),
                attributionView);
        // Comments
        setDataOrHideIfNone(HtmlUtils.fromHtml(context, streamItem.getComments(), imageGetter,
                null), commentsView);
        return rootView;
    }

    /**
     * Sets the display name of this contact to the given {@link TextView}. If
     * there is none, then set the view to gone.
     */
    public static void setDisplayName(Context context, Result contactData, TextView textView) {
        if (textView == null) {
            return;
        }
        setDataOrHideIfNone(getDisplayName(context, contactData), textView);
    }

    /**
     * Sets the company and job title of this contact to the given {@link TextView}. If
     * there is none, then set the view to gone.
     */
    public static void setCompanyName(Context context, Result contactData, TextView textView) {
        if (textView == null) {
            return;
        }
        setDataOrHideIfNone(getCompany(context, contactData), textView);
    }

    /**
     * Sets the phonetic name of this contact to the given {@link TextView}. If
     * there is none, then set the view to gone.
     */
    public static void setPhoneticName(Context context, Result contactData, TextView textView) {
        if (textView == null) {
            return;
        }
        setDataOrHideIfNone(getPhoneticName(context, contactData), textView);
    }

    /**
     * Sets the attribution contact to the given {@link TextView}. If
     * there is none, then set the view to gone.
     */
    public static void setAttribution(Context context, Result contactData, TextView textView) {
        if (textView == null) {
            return;
        }
        setDataOrHideIfNone(getAttribution(context, contactData), textView);
    }

    /**
     * Helper function to display the given text in the {@link TextView} or
     * hides the {@link TextView} if the text is empty or null.
     */
    private static void setDataOrHideIfNone(CharSequence textToDisplay, TextView textView) {
        if (!TextUtils.isEmpty(textToDisplay)) {
            textView.setText(textToDisplay);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setText(null);
            textView.setVisibility(View.GONE);
        }
    }

    /** Fetcher for images from resources to be included in HTML text. */
    private static class DefaultImageGetter implements Html.ImageGetter {
        /** The scheme used to load resources. */
        private static final String RES_SCHEME = "res";

        private final PackageManager mPackageManager;

        public DefaultImageGetter(PackageManager packageManager) {
            mPackageManager = packageManager;
        }

        @Override
        public Drawable getDrawable(String source) {
            // Returning null means that a default image will be used.
            Uri uri;
            try {
                uri = Uri.parse(source);
            } catch (Throwable e) {
                Log.d(TAG, "Could not parse image source: " + source);
                return null;
            }
            if (!RES_SCHEME.equals(uri.getScheme())) {
                Log.d(TAG, "Image source does not correspond to a resource: " + source);
                return null;
            }
            // The URI authority represents the package name.
            String packageName = uri.getAuthority();

            Resources resources = getResourcesForResourceName(packageName);
            if (resources == null) {
                Log.d(TAG, "Could not parse image source: " + source);
                return null;
            }

            List<String> pathSegments = uri.getPathSegments();
            if (pathSegments.size() != 1) {
                Log.d(TAG, "Could not parse image source: " + source);
                return null;
            }

            final String name = pathSegments.get(0);
            final int resId = resources.getIdentifier(name, "drawable", packageName);

            if (resId == 0) {
                // Use the default image icon in this case.
                Log.d(TAG, "Cannot resolve resource identifier: " + source);
                return null;
            }

            try {
                return getResourceDrawable(resources, resId);
            } catch (NotFoundException e) {
                Log.d(TAG, "Resource not found: " + source, e);
                return null;
            }
        }

        /** Returns the drawable associated with the given id. */
        private Drawable getResourceDrawable(Resources resources, int resId)
                throws NotFoundException {
            Drawable drawable = resources.getDrawable(resId);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;
        }

        /** Returns the {@link Resources} of the package of the given resource name. */
        private Resources getResourcesForResourceName(String packageName) {
            try {
                return mPackageManager.getResourcesForApplication(packageName);
            } catch (NameNotFoundException e) {
                Log.d(TAG, "Could not find package: " + packageName);
                return null;
            }
        }
    }

    /**
     * Sets an alpha value on the view.
     */
    public static void setAlphaOnViewBackground(View view, float alpha) {
        if (view != null) {
            // Convert alpha layer to a black background HEX color with an alpha value for better
            // performance (i.e. use setBackgroundColor() instead of setAlpha())
            view.setBackgroundColor((int) (alpha * 255) << 24);
        }
    }

    /**
     * Returns the top coordinate of the first item in the {@link ListView}. If the first item
     * in the {@link ListView} is not visible or there are no children in the list, then return
     * Integer.MIN_VALUE. Note that the returned value will be <= 0 because the first item in the
     * list cannot have a positive offset.
     */
    public static int getFirstListItemOffset(ListView listView) {
        if (listView == null || listView.getChildCount() == 0 ||
                listView.getFirstVisiblePosition() != 0) {
            return Integer.MIN_VALUE;
        }
        return listView.getChildAt(0).getTop();
    }

    /**
     * Tries to scroll the first item in the list to the given offset (this can be a no-op if the
     * list is already in the correct position).
     * @param listView that should be scrolled
     * @param offset which should be <= 0
     */
    public static void requestToMoveToOffset(ListView listView, int offset) {
        // We try to offset the list if the first item in the list is showing (which is presumed
        // to have a larger height than the desired offset). If the first item in the list is not
        // visible, then we simply do not scroll the list at all (since it can get complicated to
        // compute how many items in the list will equal the given offset). Potentially
        // some animation elsewhere will make the transition smoother for the user to compensate
        // for this simplification.
        if (listView == null || listView.getChildCount() == 0 ||
                listView.getFirstVisiblePosition() != 0 || offset > 0) {
            return;
        }

        // As an optimization, check if the first item is already at the given offset.
        if (listView.getChildAt(0).getTop() == offset) {
            return;
        }

        listView.setSelectionFromTop(0, offset);
    }


    /*Begin: Modified by xiepengfei for  2012/03/26*/
    /**
     * the settings system time now is 12 return true,else false;
     */
    private static boolean is12Hour(Context mContext){
        ContentResolver cr = mContext.getContentResolver();
        int time = 24;
        try {
            time = android.provider.Settings.System.getInt(cr, Settings.System.TIME_12_24);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        if(time == 12){
            return true;
        }else{
            return false;
        }
    }
    /*Begin: Modified by zxiaona for ContactsHistory 2012/08/24*/
    //public static String getDurationString(long duration){
    //    return new SimpleDateFormat("HH:mm:ss").format(new Date(duration*1000));
    //}
    public static String getDurationString(Resources res, long duration) {
        String mTimerFormat2 = res
                .getString(R.string.callDetailsDurationFormat);
        String durStr = String.format(mTimerFormat2, duration / 1000 / 3600,
                duration / 1000 % 3600 / 60, duration / 1000 % 60);
        return durStr;
    }

    /* End: Modified by zxiaona for ContactsHistory 2012/08/24 */

/*Begin: Modified by xiepengfei for for show time pm am 2012/05/30*/
    public static String getDateTime(Context mContext,long date){
        boolean is12Hour = is12Hour(mContext);
        /* Begin: Modified by zxiaona for ContactsHistory 2012/08/24 */
        // if(is12Hour){
        // return new SimpleDateFormat("hh:mm a").format(new Date(date*1000));
        // }else{
        // return new SimpleDateFormat("HH:mm").format(new Date(date*1000));
        // }
        if (is12Hour) {
            return new SimpleDateFormat("hh:mm a")
                    .format(new Date(date));
        } else {
            return new SimpleDateFormat("HH:mm").format(new Date(date));
        }
        /* End: Modified by zxiaona for ContactsHistory 2012/08/24 */

    }
/*End: Modified by xiepengfei for for show time pm am 2012/05/30*/

    public static String getDateString(Context mContext,long date){
        boolean is12Hour = is12Hour(mContext);
        Date time = new Date(date);
        if(is12Hour){
            return new PrettyDateFormat(PrettyDateFormat.FORMAT_12,PrettyDateFormat.FULL_FORMAT_12).format(time);
        }else{
            return new PrettyDateFormat(PrettyDateFormat.FORMAT_24,PrettyDateFormat.FULL_FORMAT_24).format(time);
        }
    }


    private static class PrettyDateFormat extends SimpleDateFormat {
        private static final long serialVersionUID = 1L;
        private Pattern pattern = Pattern.compile("('*)(#{1,2}|@)");
        private FormatType formatType = FormatType.DEAFULT;
        private SimpleDateFormat simpleDateFormat;

/*Begin: Modified by xiepengfei for error bug 2012/05/22*/
        public final static String FORMAT_12 = "##=hh:mm";
        public final static String FULL_FORMAT_12 = "yyyy/MM/dd=hh:mm a";

        public final static String FORMAT_24 = "##=HH:mm";
        public final static String FULL_FORMAT_24 = "yyyy/MM/dd=HH:mm";
/*End: Modified by xiepengfei for error bug 2012/05/22*/


        private enum FormatType {
            DEAFULT, TIME, DAY
        };

        /**
         * format ## is [today,yesterday]
         *
         * format: ## HH:mm a   fullFormat: yy-MM-dd HH:mm a
         */
        private PrettyDateFormat(String format, String fullFormat) {
            super(fullFormat);
            Matcher m = pattern.matcher(format);
            while (m.find()) {
                if (m.group(1).length() % 2 == 0) {
                    if ("@".equals(m.group(2))) {
                        if (formatType == FormatType.DAY) {
                            throw new IllegalArgumentException(" # and @ can not use at one time");
                        }
                        formatType = FormatType.TIME;
                    } else {
                        if (formatType == FormatType.TIME) {
                            throw new IllegalArgumentException("# and @ can not use at one time");
                        }
                        formatType = FormatType.DAY;
                    }
                }
            }
            this.simpleDateFormat = new SimpleDateFormat(format.replace("'", "''"));
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            throw new UnsupportedOperationException("can not use in this way now");
        }

        /*
         * (non-Javadoc)
         *
         * @see java.text.SimpleDateFormat#format(java.util.Date,
         * java.lang.StringBuffer, java.text.FieldPosition)
         */
        public StringBuffer format(Date date, StringBuffer toAppendTo,
                FieldPosition pos) {
            if (formatType == FormatType.DEAFULT) {
                return super.format(date, toAppendTo, pos);
            }

            long curTime = System.currentTimeMillis();

            long diffDay = 0L;
            long diffSecond = 0L;
            if (formatType == FormatType.TIME) {
                diffSecond = (curTime - date.getTime()) / 1000L;
                if (diffSecond < 0 || diffSecond >= 86400) {
                    return super.format(date, toAppendTo, pos);
                }
            }
            if (formatType == FormatType.DAY) {
                Calendar curDate = new GregorianCalendar();
                curDate.setTime(new Date(curTime));
                curDate.set(Calendar.HOUR_OF_DAY, 23);
                curDate.set(Calendar.MINUTE, 59);
                curDate.set(Calendar.SECOND, 59);
                curDate.set(Calendar.MILLISECOND, 999);
                diffDay = (curDate.getTimeInMillis() - date.getTime()) / 86400000L;
                if (diffDay != 0 && diffDay != 1) {
                    return super.format(date, toAppendTo, pos);
                }
            }
            StringBuffer sb = new StringBuffer();
            Matcher m = pattern.matcher(simpleDateFormat.format(date));
            if (m.find()) {
                String group2 = m.group(2);
                String replacement = "";
                while (true) {
                    if ("@".equals(group2)) {
//                        if (diffSecond < 60) {
//                            replacement = diffSecond == 0 ? "1 s before" : diffSecond
//                                    + "s before";
//                        } else if (diffSecond < 3600) {
//                            replacement = diffSecond / 60 + "min before";
//                        } else if (diffSecond < 86400) {
//                            replacement = diffSecond / 3600 + "hour before";
//                        }
                    } else {
                        if (diffDay == 0) {
                            replacement = group2.length() == 2 ? ContactDetailHistoryFragment.strToday : "";
                        } else if (diffDay == 1) {
                            replacement = ContactDetailHistoryFragment.strYesterday;
                        }
                    }
                    m.appendReplacement(sb, replacement);
                    if (!m.find()) {
                        break;
                    }
                }
                m.appendTail(sb);
            }
            return toAppendTo.append(sb.toString());
        }
    }
    /*End: Modified by xiepengfei for  2012/03/26*/


    /*Begin: Modified by xiepengfei for  2012/03/27*/
    public static String getRingtoneTitle(Context context,String strUri){
        Ringtone r = RingtoneManager.getRingtone(context, Uri.parse(strUri));
        return r.getTitle(context);
    }
    /*End: Modified by xiepengfei for  2012/03/27*/


    /*Begin: Modified by xiepengfei for delete history info 2012/04/18*/
    /**
     * Gets a delete progress dialog.
     * @param context the activity context.
     * @return the delete progress dialog.
     */
    public static ProgressDialog getProgressDialog(Context context) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage(context.getString(R.string.deleting));
        dialog.setMax(1); /* default is one complete */
        return dialog;
    }
    /*Begin: Modified by xiepengfei for add delete ms info 2012/06/04*/

    /**
     * @param handler
     * @param token
     * @param msgType mms or sms
     * @param msgIds ms info id
     */
    public static void startDeleteMS(AsyncQueryHandler handler, int token,int msgType, long msgIds) {
        Uri uri = null;
        System.out.println("VIEW_TYPE_MS msgType:"+msgType+", msgId:"+msgIds);
        if(msgType == 0){//sms
            uri = Sms.CONTENT_URI;
        }else if(msgType == 1){//mms
            uri = Mms.CONTENT_URI;
        }
        handler.startDelete(token, null, uri, Calls._ID + " IN ("+ msgIds + ")", null);
    }

    /*End: Modified by xiepengfei for add delete ms info 2012/06/04*/
    public static void startDelete(AsyncQueryHandler handler, int token,int msgType, long msgIds) {
        Uri uri = null;
        if (msgType == HistoryViewAdapter.VIEW_TYPE_CALL_LOG) {
            System.out.println("delete VIEW_TYPE_CALL_LOG id:"+msgIds);
            uri = Calls.CONTENT_URI;
            handler.startDelete(token, null, uri, Calls._ID + " IN ("+ msgIds + ")", null);
        }
    }
    /*End: Modified by xiepengfei for delete history info 2012/04/18*/
}
