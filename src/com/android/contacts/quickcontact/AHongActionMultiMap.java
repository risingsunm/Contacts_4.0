/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.contacts.quickcontact;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Provide a simple way of collecting one or more {@link Action} objects
 * under a MIME-type key.
 */
 /*Begin: Modified by wqiang for QuickContact 2012/04/13*/
public class AHongActionMultiMap extends HashMap<Integer, ArrayList<Action>> {
    public void put(Integer index, Action info) {
        // Create list for this MIME-type when needed
        ArrayList<Action> collectList = get(index);
        if (collectList == null) {
            collectList = new ArrayList<Action>();
            put(index, collectList);
        }
        collectList.add(info);
    }
}
/*End: Modified by wqiang for QuickContact 2012/04/13*/