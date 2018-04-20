/*
 * Copyright 2015-2017 GenerallyCloud.com
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
package com.generallycloud.baseio.component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public interface Parameters {

    boolean getBooleanParameter(String key);

    int getIntegerParameter(String key);

    int getIntegerParameter(String key, int defaultValue);

    JSONArray getJSONArray(String key);

    JSONObject getJsonObject();

    JSONObject getJSONObject(String key);

    long getLongParameter(String key);

    long getLongParameter(String key, long defaultValue);

    Object getObjectParameter(String key);

    String getParameter(String key);

    String getParameter(String key, String defaultValue);

    int size();
    
}
