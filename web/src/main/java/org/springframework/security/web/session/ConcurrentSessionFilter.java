/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.web.session;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Filter required by concurrent session handling package.
 * <p>
 * This filter performs two functions. First, it calls
 * {@link org.springframework.security.core.session.SessionRegistry#refreshLastRequest(String)}
 * for each request so that registered sessions always have a correct "last update"
 * date/time. Second, it retrieves a
 * {@link org.springframework.security.core.session.SessionInformation} from the
 * <code>SessionRegistry</code> for each request and checks if the session has been marked
 * as expired. If it has been marked as expired, the configured logout handlers will be
 * called (as happens with
 * {@link org.springframework.security.web.authentication.logout.LogoutFilter}), typically
 * to invalidate the session. To handle the expired session a call to the {@link ExpiredSessionStrategy} is made.
 * The session invalidation will cause an
 * {@link org.springframework.security.web.session.HttpSessionDestroyedEvent} to be
 * published via the
 * {@link org.springframework.security.web.session.HttpSessionEventPublisher} registered
 * in <code>web.xml</code>.
 * </p>
 *
 * @author Ben Alex
 * @author Eddú Meléndez
 * @author Marten Deinum
 */
public class ConcurrentSessionFilter extends GenericFilterBean {
	// ~ Instance fields
	// ================================================================================================

	private final SessionRegistry sessionRegistry;
	private LogoutHandler handlers = new CompositeLogoutHandler(new SecurityContextLogoutHandler());
	private ExpiredSessionStrategy expiredSessionStrategy;

	// ~ Methods
	// ========================================================================================================

	public ConcurrentSessionFilter(SessionRegistry sessionRegistry) {
		Assert.notNull(sessionRegistry, "SessionRegistry required");
		this.sessionRegistry = sessionRegistry;
	}

	public ConcurrentSessionFilter(SessionRegistry sessionRegistry, String expiredUrl) {
		Assert.notNull(sessionRegistry, "SessionRegistry required");
		this.sessionRegistry = sessionRegistry;
		this.expiredSessionStrategy = new SimpleRedirectExpiredSessionStrategy(expiredUrl);
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(sessionRegistry, "SessionRegistry required");
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		HttpSession session = request.getSession(false);

		if (session != null) {
			SessionInformation info = sessionRegistry.getSessionInformation(session
					.getId());

			if (info != null) {
				if (info.isExpired()) {
					// Expired - abort processing
					if (logger.isDebugEnabled()) {
						logger.debug("Requested session ID "
								+ request.getRequestedSessionId() + " has expired.");
					}
					doLogout(request, response);

					if (this.expiredSessionStrategy != null) {
						this.expiredSessionStrategy.onExpiredSessionDetected(request, response);

						return;
					}
					else {
						response.getWriter().print(
								"This session has been expired (possibly due to multiple concurrent "
										+ "logins being attempted as the same user).");
						response.flushBuffer();
					}

					return;
				}
				else {
					// Non-expired - update last request date/time
					sessionRegistry.refreshLastRequest(info.getSessionId());
				}
			}
		}

		chain.doFilter(request, response);
	}


	private void doLogout(HttpServletRequest request, HttpServletResponse response) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		this.handlers.logout(request, response, auth);
	}

	public void setLogoutHandlers(LogoutHandler[] handlers) {
		this.handlers = new CompositeLogoutHandler(handlers);
	}

	public void setExpiredSessionStrategy(ExpiredSessionStrategy expiredSessionStrategy) {
		this.expiredSessionStrategy=expiredSessionStrategy;
	}
}
