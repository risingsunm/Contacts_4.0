/*
 * Copyright (C) 2012, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.contacts.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.ContactsContract.CommonDataKinds.LocalGroup;
import android.provider.LocalGroups;
import android.provider.ContactsContract.Data;
import android.provider.LocalGroups.Group;
import android.provider.LocalGroups.GroupColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.group.local.AddLocalGroupDialog;
import com.android.contacts.group.local.AddLocalGroupDialog.AddGroupListener;
import com.android.contacts.model.EntityDelta.ValuesDelta;

public class LocalGroupsSelector extends Button implements OnClickListener,
        DialogInterface.OnClickListener, AddGroupListener {

    private GroupsAdapter groupsAdapter;

    private Context mContext;

    private ValuesDelta mEntry;

    private String column;

    private OnGroupSelectListener onGroupSelectListener;

    public LocalGroupsSelector(Context context, ValuesDelta entry, String key) {
        super(context);
        mContext = context;
        mEntry = entry;
        column = key;
        setOnClickListener(this);

        initValue();

        /*Begin: Modified by xiepengfei for modify the button background 2012/05/22*/
        setTextColor(Color.BLACK);
        setBackgroundResource(R.drawable.edit_group_background);
        setHeight(20);
        /*End: Modified by xiepengfei for modify the button background 2012/05/22*/
    }

    public long getGroupId() {
        Long id = mEntry.getAsLong(column);
        if (id != null) {
            return id;
        } else {
            return -1;
        }
    }

    public void clear() {
        mEntry.markDeleted();
        setText(R.string.group_selector);
        onGroupSelectListener.onGroupChanged();
    }

    private void initValue() {
        Long id = mEntry.getAsLong(column);
        if (id != null) {
            Group group = Group.restoreGroupById(mContext.getContentResolver(), id);
            setText(group.getTitle());
        } else {
            setText(R.string.group_selector);
        }
    }

    public void onClick(View v) {
        getGroupsDialog().show();
    }

    private AlertDialog getGroupsDialog() {
        groupsAdapter = new GroupsAdapter(mContext);
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setSingleChoiceItems(groupsAdapter, 0, this);
        builder.setTitle(R.string.title_group_picker);
        return builder.create();
    }

    private AlertDialog getNewGroupDialog() {
        return new AddLocalGroupDialog(mContext, this);
    }

    private class GroupsAdapter extends CursorAdapter {

        public GroupsAdapter(Context context) {
            super(context, context.getContentResolver().query(LocalGroups.CONTENT_URI, null, null,
                    null, null));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1,
                    parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Group group = Group.restoreGroup(cursor);
            TextView textView = (TextView) view;
            textView.setText(group.getTitle());
            textView.setTextColor(Color.BLACK);
            view.setTag(group);
        }

        @Override
        public int getCount() {
            return super.getCount() + 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == getCount() - 1) {
                TextView textView = null;
                if (convertView == null) {
                    textView = (TextView) LayoutInflater.from(parent.getContext()).inflate(
                            android.R.layout.simple_list_item_1, parent, false);
                } else {
                    textView = (TextView) convertView;
                }
                textView.setText(R.string.title_group_add);
                textView.setTextColor(Color.BLACK);
                return textView;
            }
            return super.getView(position, convertView, parent);
        }

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == groupsAdapter.getCount() - 1) {
            getNewGroupDialog().show();
        } else {
            Cursor c = (Cursor) groupsAdapter.getItem(which);
            long groupId = c.getLong(c.getColumnIndex(GroupColumns._ID));
            String groupName = c.getString(c.getColumnIndex(GroupColumns.TITLE));
            mEntry.put(column, (int) groupId);
            mEntry.put(Data.MIMETYPE, LocalGroup.CONTENT_ITEM_TYPE);
            setText(groupName);
            if (onGroupSelectListener != null)
                onGroupSelectListener.onGroupChanged();
        }
        dialog.dismiss();
    }

    @Override
    public void onAddGroup(String name) {
        Group group = new Group();
        group.setTitle(name);
        if (group.save(mContext.getContentResolver())) {
            mEntry.put(column, (int) group.getId());
            setText(group.getTitle());
            if (onGroupSelectListener != null)
                onGroupSelectListener.onGroupChanged();
        }
    }

    public void setOnGroupSelectListener(OnGroupSelectListener onGroupSelectListener) {
        this.onGroupSelectListener = onGroupSelectListener;
    }

    public static interface OnGroupSelectListener {
        void onGroupChanged();
    }
}
