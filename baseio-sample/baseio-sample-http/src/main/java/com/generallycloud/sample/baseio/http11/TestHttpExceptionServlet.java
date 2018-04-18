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
package com.generallycloud.sample.baseio.http11;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.generallycloud.baseio.codec.http11.future.HttpFuture;
import com.generallycloud.baseio.container.http11.HttpFutureAcceptorService;
import com.generallycloud.baseio.container.http11.HttpSession;

@Service("/test-http-exception")
public class TestHttpExceptionServlet extends HttpFutureAcceptorService {

    @Override
    protected void doAccept(HttpSession session, HttpFuture future) throws Exception {
        throw new IOException("test http error");
    }

}
