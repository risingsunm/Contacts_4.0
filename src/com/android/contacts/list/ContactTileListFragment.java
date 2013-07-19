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
package com.android.contacts.list;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactTileLoaderFactory;
import com.android.contacts.R;
import com.android.contacts.list.ContactTileAdapter.ContactEntry;
import com.android.contacts.list.ContactTileAdapter.DisplayType;
import com.android.contacts.widget.TouchListView;
import com.android.contacts.widget.TouchListView.TriggerListener;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.provider.Settings;
import com.android.internal.telephony.SubscriptionManager;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;

/**
 * Fragment containing a list of starred contacts followed by a list of frequently contacted.
 *
 * TODO: Make this an abstract class so that the favorites, frequent, and group list functionality
 * can be separated out. This will make it easier to customize any of those lists if necessary
 * (i.e. adding header views to the ListViews in the fragment). This work was started
 * by creating {@link ContactTileFrequentFragment}.
 */
public class ContactTileListFragment extends Fragment {
    private static final String TAG = ContactTileListFragment.class.getSimpleName();

    public interface Listener {
        public void onContactSelected(Uri contactUri, Rect targetRect);
    }

    private static int LOADER_CONTACTS = 1;

    private Listener mListener;
    private ContactTileAdapter mAdapter;
    private DisplayType mDisplayType;
    private TextView mEmptyView;
    private ListView mListView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Resources res = getResources();
        int columnCount = res.getInteger(R.integer.contact_tile_column_count);

        mAdapter = new ContactTileAdapter(activity, mAdapterListener,
                columnCount, mDisplayType);
        mAdapter.setPhotoLoader(ContactPhotoManager.getInstance(activity));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflateAndSetupView(inflater, container, savedInstanceState,
                R.layout.contact_tile_list);
    }

    protected View inflateAndSetupView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState, int layoutResourceId) {
        View listLayout = inflater.inflate(layoutResourceId, container, false);

        mEmptyView = (TextView) listLayout.findViewById(R.id.contact_tile_list_empty);
        mListView = (ListView) listLayout.findViewById(R.id.contact_tile_list);

        mListView.setItemsCanFocus(true);
        mListView.setAdapter(mAdapter);
        /*Begin: Modified by bxinchun, add touch view style listview 2012/07/31*/
        if (mListView instanceof TouchListView) {
            TouchListView touchList = (TouchListView) mListView;
            touchList.setTouchMode(true);
            touchList.setTriggerListener(mTriggerListener);
        }
        /*End: Modified by bxinchun, add touch view style listview 2012/07/31*/
        return listLayout;
    }
    /*Begin: Added by bxinchun, add touch view style listview 2012/08/01*/
    private TriggerListener mTriggerListener = new TriggerListener() {

        public boolean isTriggable(int position) {
            if (!super.isTriggable(position)) {
                return false;
            }
            return mAdapter.isTouchViewItem(position);
        }

        public void onTrigger(int position, int actionType) {
            String number = null;

            Cursor cursor = null;
            try {
                Object obj = mAdapter.getItem(position);
                //Log.d("Tp", "favorite in people activity, position :" + position);
                if (obj != null && obj instanceof ArrayList<?>) {
                    ArrayList<ContactEntry> entries = (ArrayList<ContactEntry>) obj;
                    if (entries.size() > 0) {
                        ContactEntry entry = entries.get(0);
                        if (entry != null) {
                            if (entry.phoneNumber != null) {
                                number = entry.phoneNumber;
                            } else if (entry.lookupKey != null) {

                                //Log.d("Tp", "favorite in people activity, contact entry lookup key :" + entry.lookupKey);
                                ContentResolver resolver = getActivity().getContentResolver();
                                cursor = resolver.query(entry.lookupKey, new String[]{Contacts._ID}, null, null, null);
                                if (cursor == null || !cursor.moveToFirst()) {
                                    return;
                                }

                                long contactId = cursor.getLong(0);
                                cursor.close();

                                String [ ] proj = { Phone.NUMBER, Phone.DISPLAY_NAME, Phone._ID, Phone.IS_SUPER_PRIMARY };

                                String firstNumber = null;
                                String primaryNumber = null;

                                cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        proj, Phone.CONTACT_ID + " = ?", new String [ ] { String.valueOf(contactId) }, null);

                                if (cursor != null && cursor.moveToFirst()) {
                                    firstNumber = cursor.getString(0);
                                    //System.out.println("firstNumber is:"+firstNumber);
                                    final int indexIsPrimary = cursor.getColumnIndex(Phone.IS_SUPER_PRIMARY);
                                    cursor.moveToPosition(-1);
                                    while (cursor.moveToNext()) {
                                        if (cursor.getInt(indexIsPrimary) != 0) {
                                            // Found super primary, call it.
                                            primaryNumber = cursor.getString(0);
                                            //System.out.println("primaryNumber:" + primaryNumber);
                                            break;
                                        }
                                    }
                                }

                                if(!TextUtils.isEmpty(firstNumber)){
                                    number = firstNumber;
                                }
                                if(!TextUtils.isEmpty(primaryNumber)){
                                    number = primaryNumber;
                                }
                            }
                        }
                    }
                }
                //Log.i("Tp", "favorite in people activity, number :" + number);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (TextUtils.isEmpty(number)) return;
            //System.out.println("number is:"+number);

            switch (actionType) {
                case LEFT: //sms
                    Intent smsIntent = new Intent(
                            Intent.ACTION_SENDTO, Uri.fromParts("sms", number, null));
                    startActivity(smsIntent);
                    break;
                case RIGHT: //call
                    Intent phoneIntent = new Intent(
                            Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel", number, null));
                    /*Begin: Modified by sliangqi for main_call 2012-8-25*/
                    int subscription = 0;
                    if (isMultiSimAvailable()) {
                        try {
                            subscription = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.MULTI_SIM_VOICE_CALL_SUBSCRIPTION);
                        } catch (SettingNotFoundException snfe) {
                            Log.e(TAG, "Settings Exception Reading Dual Sim Voice Call Values");
                        }
                    } else {
                        subscription = getDefaultSubscription();
                    }
                    phoneIntent.putExtra("subscription", subscription);
                    phoneIntent.putExtra("directDial", true);
                    /*End: Modified by sliangqi for main_call 2012-8-25*/
                    startActivity(phoneIntent);
                    break;

                default:
                        break;
            }
        }
    };
    /*End: Added by bxinchun, add touch view style listview 2012/08/01*/
    /*Begin: Modified by sliangqi for main_call 2012-8-25*/
    public int getDefaultSubscription() {
        int subscription = 0;
        try {
            subscription = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.DEFAULT_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Default Subscription");
        }

        return subscription;
    }
    private boolean isMultiSimAvailable() {
        return TelephonyManager.getTelephonyProperty("gsm.sim.state",0,"").equals("READY")&&TelephonyManager.getTelephonyProperty("gsm.sim.state",1,"").equals("READY");
    }
    /*End: Modified by sliangqi for main_call 2012-8-25*/
    @Override
    public void onStart() {
        super.onStart();
        // TODO: Use initLoader?
        getLoaderManager().restartLoader(LOADER_CONTACTS, null, mContactTileLoaderListener);
    }

    public void setColumnCount(int columnCount) {
        mAdapter.setColumnCount(columnCount);
    }

    public void setDisplayType(DisplayType displayType) {
        mDisplayType = displayType;
        mAdapter.setDisplayType(mDisplayType);
    }

    public void enableQuickContact(boolean enableQuickContact) {
        mAdapter.enableQuickContact(enableQuickContact);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mContactTileLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            switch (mDisplayType) {
              case STARRED_ONLY:
                  return ContactTileLoaderFactory.createStarredLoader(getActivity());
              case STREQUENT:
                  return ContactTileLoaderFactory.createStrequentLoader(getActivity());
              case STREQUENT_PHONE_ONLY:
                  return ContactTileLoaderFactory.createStrequentPhoneOnlyLoader(getActivity());
              case FREQUENT_ONLY:
                  return ContactTileLoaderFactory.createFrequentLoader(getActivity());
              default:
                  throw new IllegalStateException(
                      "Unrecognized DisplayType " + mDisplayType);
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.setContactCursor(data);
            mEmptyView.setText(getEmptyStateText());
            mListView.setEmptyView(mEmptyView);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    private String getEmptyStateText() {
        String emptyText;
        switch (mDisplayType) {
            case STREQUENT:
            case STREQUENT_PHONE_ONLY:
            case STARRED_ONLY:
                emptyText = getString(R.string.listTotalAllContactsZeroStarred);
                break;
            case FREQUENT_ONLY:
            case GROUP_MEMBERS:
                emptyText = getString(R.string.noContacts);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized DisplayType " + mDisplayType);
        }
        return emptyText;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private ContactTileAdapter.Listener mAdapterListener =
            new ContactTileAdapter.Listener() {
        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            if (mListener != null) {
                mListener.onContactSelected(contactUri, targetRect);
            }
        }
    };
}
