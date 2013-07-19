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

package com.android.contacts.format;

import android.database.Cursor;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import java.util.Arrays;

import com.android.contacts.R.string;
import com.android.contacts.dialpad.MyAdapter;

/**
 * Highlights the text in a text field.
 */
public class PrefixHighlighter {
    private final int mPrefixHighlightColor;

    private ForegroundColorSpan mPrefixColorSpan;

    public PrefixHighlighter(int prefixHighlightColor) {
        mPrefixHighlightColor = prefixHighlightColor;
    }

    /**
     * Sets the text on the given text view, highlighting the word that matches the given prefix.
     *
     * @param view the view on which to set the text
     * @param text the string to use as the text
     * @param prefix the prefix to look for
     */
    public void setText(TextView view, String text, char[] prefix) {
        view.setText(apply(text, prefix));
    }

    /**
     * Returns a CharSequence which highlights the given prefix if found in the given text.
     *
     * @param text the text to which to apply the highlight
     * @param prefix the prefix to look for
     */
    public CharSequence apply(CharSequence text, char[] prefix) {
        int index = FormatUtils.indexOfWordPrefix(text, prefix);
        if (index != -1) {
            if (mPrefixColorSpan == null) {
                mPrefixColorSpan = new ForegroundColorSpan(mPrefixHighlightColor);
            }

            SpannableString result = new SpannableString(text);
            /*Begin: Modified by siliangqi for contact_searchresult_color 2012-7-25*/
            result.setSpan(new ForegroundColorSpan(0xff31b6e7), index, index + prefix.length, 0 /* flags */);
            /*End: Modified by siliangqi for contact_searchresult_color 2012-7-25*/
            return result;
        } else {
            return text;
        }
    }
    /*Begin: Modified by siliangqi for contact_searchresult_color 2012-7-25*/
    public CharSequence apply(CharSequence text, Cursor cursor, char[] prefix) {
        try {
            int index = FormatUtils.indexOfWordPrefix(text, prefix);
            CharSequence shortNameNormal;
            try {
                shortNameNormal = cursor.getString(cursor.getColumnIndex("short_name_normal")).toUpperCase();
            } catch (NullPointerException e) {
                // TODO Auto-generated catch block
                return apply(text,prefix);
            }
            CharSequence fullNameNormal = cursor.getString(cursor.getColumnIndex("full_name_normal")).toUpperCase();
            SpannableString result = new SpannableString(text);
            int myPrefixHighlightColor = Color.argb(255, 255, 0, 0);
            if (index != -1) {
                if (mPrefixColorSpan == null) {
                    mPrefixColorSpan = new ForegroundColorSpan(myPrefixHighlightColor);
                }

                result.setSpan(new ForegroundColorSpan(0xff31b6e7), index, index + prefix.length, 0 /* flags */);
                return result;
            } else if(text.toString().length()==shortNameNormal.toString().length()){
                index = shortNameNormal.toString().indexOf(String.valueOf(prefix));
                if(index!=-1){
                    result.setSpan(new ForegroundColorSpan(0xff31b6e7), index, index + prefix.length, 0 /* flags */);
                    return result;
                }else{
                    String[] s1 = fullNameNormal.toString().split("\\s+");
                    if(shortNameNormal.toString().length()==s1.length){
                        String s2 = MyAdapter.filterSpace(fullNameNormal.toString());
                        index = s2.indexOf(String.valueOf(prefix));
                        if(index!=-1){
                            int count=0,start=0,end=0;
                            for(int i=0;i<s1.length;i++){
                                count += s1[i].length();
                                if(index>=(count-s1[i].length())&&index<count){
                                    start = i;
                                }
                                if((index+prefix.length)>(count-s1[i].length())&&(index+prefix.length)<=count){
                                    end = i;
                                    result.setSpan(new ForegroundColorSpan(0xff31b6e7), start, end+1, 0);
                                    return result;
                                }
                            }
                        }
                    }
                }
            }else{
                index = shortNameNormal.toString().indexOf(String.valueOf(prefix));
                String hanziName = cursor.getString(cursor.getColumnIndex("hanzi_name")).toUpperCase();
                if(index!=-1){
                    int count = 0;
                    String s1[] = hanziName.split("\\s+");
                    for(int i=0;i<(index+prefix.length);i++){
                        count += s1[i].length();
                        if(i>=index){
                            result.setSpan(new ForegroundColorSpan(0xff31b6e7), count-s1[i].length(), count-s1[i].length()+1, 0);
                        }
                    }
                    return result;
                }else{
                    String s1[] = fullNameNormal.toString().split("\\s+");
                    String s2 = MyAdapter.filterSpace(fullNameNormal.toString());
                    String s3[] = hanziName.split("\\s+");
                    int pos5 = s2.indexOf(String.valueOf(prefix));
                    if(pos5>-1){
                        int count1=0,count2=0,start=0,end=0;
                        for(int i=0;i<s1.length;i++){
                            count1 += s1[i].length();
                            count2 += s3[i].length();
                            if(pos5>=(count1-s1[i].length())&&pos5<count1){
                                start = count2-s3[i].length()+pos5-(count1-s1[i].length());
                            }
                            if((pos5+prefix.length)>(count1-s1[i].length())&&(pos5+prefix.length)<=count1){
                                if(s3[i].length()!=1)
                                    end = count2-s3[i].length()+pos5+prefix.length-(count1-s1[i].length())-1;
                                else{
                                    end = count2-s3[i].length();
                                }
                                result.setSpan(new ForegroundColorSpan(0xff31b6e7), start, end+1, 0);
                                return result;
                            }
                        }
                    }
                }
            }
            return text;
        } catch (IndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return text;
        }
    }
    /*End: Modified by siliangqi for contact_searchresult_color 2012-7-25*/
}
