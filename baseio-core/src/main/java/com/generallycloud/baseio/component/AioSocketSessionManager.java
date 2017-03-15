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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.generallycloud.baseio.OverflowException;
import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.common.Logger;
import com.generallycloud.baseio.common.LoggerFactory;
import com.generallycloud.baseio.component.concurrent.EventLoop;

//所有涉及操作全部session的操作放在此队列中做
public class AioSocketSessionManager extends AbstractSessionManager implements SocketSessionManager {

	private AioSessionManagerEventLoop			eventLoop = null;
	private SocketChannelContext				context	= null;
	private ConcurrentMap<Integer, SocketSession>	sessions	= new ConcurrentHashMap<>();
	private Logger							logger	= LoggerFactory.getLogger(AioSocketSessionManager.class);

	public AioSocketSessionManager(SocketChannelContext context) {
		super(context.getSessionIdleTime());
		this.context = context;
	}
	
	@Override
	public void offerSessionMEvent(SocketSessionManagerEvent event) {

		this.eventLoop.dispatch(new Runnable() {
			
			@Override
			public void run() {
				
				Map<Integer, SocketSession> map = sessions;

				if (map.size() == 0) {
					return ;
				}

				try {
					event.fire(context, map);
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
	}

	@Override
	protected void sessionIdle(long lastIdleTime, long currentTime) {

		Map<Integer, SocketSession> map = sessions;

		if (map.size() == 0) {
			return;
		}

		Collection<SocketSession> es = map.values();

		SocketChannelContext context = this.context;

		for (SocketSession session : es) {

			sessionIdle(context, session, lastIdleTime, currentTime);
		}
	}

	protected void sessionIdle(SocketChannelContext context, SocketSession session, long lastIdleTime,
			long currentTime) {

		Linkable<SocketSessionIdleEventListener> linkable = context.getSessionIdleEventListenerLink();

		for (; linkable != null;) {

			try {

				linkable.getValue().sessionIdled(session, lastIdleTime, currentTime);

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			linkable = linkable.getNext();
		}
	}

	@Override
	public void stop() {

		Map<Integer, SocketSession> map = sessions;

		if (map.size() == 0) {
			return;
		}

		Collection<SocketSession> es = map.values();

		for (SocketSession session : es) {

			CloseUtil.close(session);
		}
	}

	@Override
	public void putSession(SocketSession session) throws OverflowException {

		ConcurrentMap<Integer, SocketSession> sessions = this.sessions;

		Integer sessionID = session.getSessionID();

		SocketSession old = sessions.get(sessionID);

		if (old != null) {
			CloseUtil.close(old);
			removeSession(old);
		}

		if (sessions.size() >= getSessionSizeLimit()) {
			throw new OverflowException("session size limit:"
					+getSessionSizeLimit() +",current:"+sessions.size());
		}
		
		sessions.put(sessionID, session);
	}

	@Override
	public void removeSession(SocketSession session) {
		sessions.remove(session.getSessionID());
	}

	@Override
	public int getManagedSessionSize() {
		return sessions.size();
	}

	@Override
	public SocketSession getSession(Integer sessionID) {
		return sessions.get(sessionID);
	}

	@Override
	public void initSessionManager(EventLoop eventLoop) {
		this.eventLoop = (AioSessionManagerEventLoop) eventLoop;
	}
	
}