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
package com.generallycloud.baseio.container.http11;

import com.generallycloud.baseio.common.StringUtil;
import com.generallycloud.baseio.container.startup.ApplicationBootstrap;

/**
 * @author wangkai
 *
 */
public class HttpStartup {

    public static void main(String[] args) throws Exception {
        
        if (args != null && args.length > 1) {
            throw new Exception("args must be one , true or flase");
        }

        boolean deployModel = Boolean.parseBoolean(StringUtil.getValueFromArray(args, 0, "false"));

        String className = "com.generallycloud.baseio.container.http11.HttpApplicationBootstrapEngine";

        ApplicationBootstrap.startup(className, deployModel);

    }

}
