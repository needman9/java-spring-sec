/* Copyright 2004, 2005 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.acegisecurity.intercept.web;

import net.sf.acegisecurity.*;
import net.sf.acegisecurity.context.security.SecureContextUtils;
import net.sf.acegisecurity.ui.AbstractProcessingFilter;
import net.sf.acegisecurity.util.PortResolver;
import net.sf.acegisecurity.util.PortResolverImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;

import org.springframework.util.Assert;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Wraps requests to the {@link FilterSecurityInterceptor}.
 * 
 * <p>
 * This filter is necessary because it provides the bridge between incoming
 * requests and the <code>FilterSecurityInterceptor</code> instance.
 * </p>
 * 
 * <p>
 * If an {@link AuthenticationException} is detected, the filter will launch
 * the <code>authenticationEntryPoint</code>. This allows common handling of
 * authentication failures originating from any subclass of {@link
 * net.sf.acegisecurity.intercept.AbstractSecurityInterceptor}.
 * </p>
 * 
 * <p>
 * If an {@link AccessDeniedException} is detected, the filter will determine
 * whether or not the user is an anonymous user. If they are an anonymous
 * user, the <code>authenticationEntryPoint</code> will be launched. If they
 * are not an anonymous user, the filter will respond with a
 * <code>HttpServletResponse.SC_FORBIDDEN</code> (403 error).  In addition,
 * the <code>AccessDeniedException</code> itself will be placed in the
 * <code>HttpSession</code> attribute keyed against {@link
 * #ACEGI_SECURITY_ACCESS_DENIED_EXCEPTION_KEY} (to allow access to the stack
 * trace etc). Again, this allows common access denied handling irrespective
 * of the originating security interceptor.
 * </p>
 * 
 * <p>
 * To use this filter, it is necessary to specify the following properties:
 * </p>
 * 
 * <ul>
 * <li>
 * <code>filterSecurityInterceptor</code> indicates the
 * <code>FilterSecurityInterceptor</code> to delegate HTTP security decisions
 * to.
 * </li>
 * <li>
 * <code>authenticationEntryPoint</code> indicates the handler that should
 * commence the authentication process if an
 * <code>AuthenticationException</code> is detected. Note that this may also
 * switch the current protocol from http to https for an SSL login.
 * </li>
 * <li>
 * <code>portResolver</code> is used to determine the "real" port that a
 * request was received on.
 * </li>
 * </ul>
 * 
 * <P>
 * <B>Do not use this class directly.</B> Instead configure
 * <code>web.xml</code> to use the {@link
 * net.sf.acegisecurity.util.FilterToBeanProxy}.
 * </p>
 *
 * @author Ben Alex
 * @author colin sampaleanu
 * @version $Id$
 */
public class SecurityEnforcementFilter implements Filter, InitializingBean {
    //~ Static fields/initializers =============================================

    private static final Log logger = LogFactory.getLog(SecurityEnforcementFilter.class);
    public static final String ACEGI_SECURITY_ACCESS_DENIED_EXCEPTION_KEY = "ACEGI_SECURITY_403_EXCEPTION";

    //~ Instance fields ========================================================

    private AuthenticationEntryPoint authenticationEntryPoint;
    private AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();
    private FilterSecurityInterceptor filterSecurityInterceptor;
    private PortResolver portResolver = new PortResolverImpl();

    //~ Methods ================================================================

    public void setAuthenticationEntryPoint(
        AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    public void setAuthenticationTrustResolver(
        AuthenticationTrustResolver authenticationTrustResolver) {
        this.authenticationTrustResolver = authenticationTrustResolver;
    }

    public AuthenticationTrustResolver getAuthenticationTrustResolver() {
        return authenticationTrustResolver;
    }

    public void setFilterSecurityInterceptor(
        FilterSecurityInterceptor filterSecurityInterceptor) {
        this.filterSecurityInterceptor = filterSecurityInterceptor;
    }

    public FilterSecurityInterceptor getFilterSecurityInterceptor() {
        return filterSecurityInterceptor;
    }

    public void setPortResolver(PortResolver portResolver) {
        this.portResolver = portResolver;
    }

    public PortResolver getPortResolver() {
        return portResolver;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(authenticationEntryPoint,
            "authenticationEntryPoint must be specified");
        Assert.notNull(filterSecurityInterceptor,
            "filterSecurityInterceptor must be specified");
        Assert.notNull(portResolver, "portResolver must be specified");
        Assert.notNull(authenticationTrustResolver,
            "authenticationTrustResolver must be specified");
    }

    public void destroy() {}

    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            throw new ServletException("HttpServletRequest required");
        }

        if (!(response instanceof HttpServletResponse)) {
            throw new ServletException("HttpServletResponse required");
        }

        FilterInvocation fi = new FilterInvocation(request, response, chain);

        try {
            filterSecurityInterceptor.invoke(fi);

            if (logger.isDebugEnabled()) {
                logger.debug("Chain processed normally");
            }
        } catch (AuthenticationException authentication) {
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication exception occurred; redirecting to authentication entry point",
                    authentication);
            }

            sendStartAuthentication(fi, authentication);
        } catch (AccessDeniedException accessDenied) {
            if (authenticationTrustResolver.isAnonymous(
                    SecureContextUtils.getSecureContext().getAuthentication())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access is denied (user is anonymous); redirecting to authentication entry point",
                        accessDenied);
                }

                sendStartAuthentication(fi,
                    new InsufficientAuthenticationException(
                        "Full authentication is required to access this resource"));
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access is denied (user is not anonymous); sending back forbidden response",
                        accessDenied);
                }

                sendAccessDeniedError(fi, accessDenied);
            }
        } catch (ServletException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Throwable otherException) {
            throw new ServletException(otherException);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {}

    protected void sendAccessDeniedError(FilterInvocation fi,
        AccessDeniedException accessDenied)
        throws ServletException, IOException {
        ((HttpServletRequest) fi.getRequest()).getSession().setAttribute(ACEGI_SECURITY_ACCESS_DENIED_EXCEPTION_KEY,
            accessDenied);
        ((HttpServletResponse) fi.getResponse()).sendError(HttpServletResponse.SC_FORBIDDEN,
            accessDenied.getMessage()); // 403
    }

    protected void sendStartAuthentication(FilterInvocation fi,
        AuthenticationException reason) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) fi.getRequest();

        int port = portResolver.getServerPort(request);
        boolean includePort = true;

        if ("http".equals(request.getScheme().toLowerCase()) && (port == 80)) {
            includePort = false;
        }

        if ("https".equals(request.getScheme().toLowerCase()) && (port == 443)) {
            includePort = false;
        }

        String targetUrl = request.getScheme() + "://"
            + request.getServerName() + ((includePort) ? (":" + port) : "")
            + request.getContextPath() + fi.getRequestUrl();

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Authentication entry point being called; target URL added to Session: "
                + targetUrl);
        }

        ((HttpServletRequest) request).getSession().setAttribute(AbstractProcessingFilter.ACEGI_SECURITY_TARGET_URL_KEY,
            targetUrl);
        authenticationEntryPoint.commence(request,
            (HttpServletResponse) fi.getResponse(), reason);
    }
}
