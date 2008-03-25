/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.ui.basicauth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;
import org.springframework.security.ui.AuthenticationDetailsSource;
import org.springframework.security.ui.WebAuthenticationDetailsSource;
import org.springframework.security.ui.AuthenticationEntryPoint;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;
import org.springframework.security.ui.rememberme.RememberMeServices;
import org.springframework.util.Assert;


/**
 * Processes a HTTP request's BASIC authorization headers, putting the result into the
 * <code>SecurityContextHolder</code>.
 *
 * <p>
 * For a detailed background on what this filter is designed to process, refer to
 * <a href="http://www.faqs.org/rfcs/rfc1945.html">RFC 1945, Section 11.1</a>. Any realm name presented in
 * the HTTP request is ignored.
 *
 * <p>
 * In summary, this filter is responsible for processing any request that has a HTTP request header of
 * <code>Authorization</code> with an authentication scheme of <code>Basic</code> and a Base64-encoded
 * <code>username:password</code> token. For example, to authenticate user "Aladdin" with password "open sesame" the
 * following header would be presented:
 * <pre>
 *
 * Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
 * </pre>
 *
 * <p>
 * This filter can be used to provide BASIC authentication services to both remoting protocol clients (such as
 * Hessian and SOAP) as well as standard user agents (such as Internet Explorer and Netscape).
 * <p>
 * If authentication is successful, the resulting {@link Authentication} object will be placed into the
 * <code>SecurityContextHolder</code>.
 *
 * <p>
 * If authentication fails and <code>ignoreFailure</code> is <code>false</code> (the default), an {@link
 * AuthenticationEntryPoint} implementation is called (unless the <tt>ignoreFailure</tt> property is set to
 * <tt>true</tt>). Usually this should be {@link BasicProcessingFilterEntryPoint}, which will prompt the user to
 * authenticate again via BASIC authentication.
 *
 * <p>
 * Basic authentication is an attractive protocol because it is simple and widely deployed. However, it still
 * transmits a password in clear text and as such is undesirable in many situations. Digest authentication is also
 * provided by Spring Security and should be used instead of Basic authentication wherever possible. See {@link
 * org.springframework.security.ui.digestauth.DigestProcessingFilter}.
 * <p>
 * Note that if a {@link RememberMeServices} is set, this filter will automatically send back remember-me
 * details to the client. Therefore, subsequent requests will not need to present a BASIC authentication header as
 * they will be authenticated using the remember-me mechanism.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class BasicProcessingFilter extends SpringSecurityFilter implements InitializingBean {

    //~ Instance fields ================================================================================================

    private AuthenticationDetailsSource authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private AuthenticationEntryPoint authenticationEntryPoint;
    private AuthenticationManager authenticationManager;
    private RememberMeServices rememberMeServices;
    private boolean ignoreFailure = false;

    //~ Methods ========================================================================================================

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.authenticationManager, "An AuthenticationManager is required");

        if(!isIgnoreFailure()) {
            Assert.notNull(this.authenticationEntryPoint, "An AuthenticationEntryPoint is required");
        }
    }

    public void doFilterHttp(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain)
            throws IOException, ServletException {

        String header = httpRequest.getHeader("Authorization");

        if (logger.isDebugEnabled()) {
            logger.debug("Authorization header: " + header);
        }

        if ((header != null) && header.startsWith("Basic ")) {
            String base64Token = header.substring(6);
            String token = new String(Base64.decodeBase64(base64Token.getBytes()));

            String username = "";
            String password = "";
            int delim = token.indexOf(":");

            if (delim != -1) {
                username = token.substring(0, delim);
                password = token.substring(delim + 1);
            }

            if (authenticationIsRequired(username)) {
                UsernamePasswordAuthenticationToken authRequest =
                        new UsernamePasswordAuthenticationToken(username, password);
                authRequest.setDetails(authenticationDetailsSource.buildDetails(httpRequest));

                Authentication authResult;

                try {
                    authResult = authenticationManager.authenticate(authRequest);
                } catch (AuthenticationException failed) {
                    // Authentication failed
                    if (logger.isDebugEnabled()) {
                        logger.debug("Authentication request for user: " + username + " failed: " + failed.toString());
                    }

                    SecurityContextHolder.getContext().setAuthentication(null);

                    if (rememberMeServices != null) {
                        rememberMeServices.loginFail(httpRequest, httpResponse);
                    }

                    if (ignoreFailure) {
                        chain.doFilter(httpRequest, httpResponse);
                    } else {
                        authenticationEntryPoint.commence(httpRequest, httpResponse, failed);
                    }

                    return;
                }

                // Authentication success
                if (logger.isDebugEnabled()) {
                    logger.debug("Authentication success: " + authResult.toString());
                }

                SecurityContextHolder.getContext().setAuthentication(authResult);

                if (rememberMeServices != null) {
                    rememberMeServices.loginSuccess(httpRequest, httpResponse, authResult);
                }
            }
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    private boolean authenticationIsRequired(String username) {
        // Only reauthenticate if username doesn't match SecurityContextHolder and user isn't authenticated
        // (see SEC-53)
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

        if(existingAuth == null || !existingAuth.isAuthenticated()) {
            return true;
        }

        // Limit username comparison to providers which use usernames (ie UsernamePasswordAuthenticationToken)
        // (see SEC-348)

        if (existingAuth instanceof UsernamePasswordAuthenticationToken && !existingAuth.getName().equals(username)) {
            return true;
        }

        // Handle unusual condition where an AnonymousAuthenticationToken is already present
        // This shouldn't happen very often, as BasicProcessingFitler is meant to be earlier in the filter
        // chain than AnonymousProcessingFilter. Nevertheless, presence of both an AnonymousAuthenticationToken
        // together with a BASIC authentication request header should indicate reauthentication using the
        // BASIC protocol is desirable. This behaviour is also consistent with that provided by form and digest,
        // both of which force re-authentication if the respective header is detected (and in doing so replace
        // any existing AnonymousAuthenticationToken). See SEC-610.
        if (existingAuth instanceof AnonymousAuthenticationToken) {
        	return true;
        }

        return false;
    }

    protected AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    protected AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    protected boolean isIgnoreFailure() {
        return ignoreFailure;
    }

    public void setIgnoreFailure(boolean ignoreFailure) {
        this.ignoreFailure = ignoreFailure;
    }    

    public void setAuthenticationDetailsSource(AuthenticationDetailsSource authenticationDetailsSource) {
        Assert.notNull(authenticationDetailsSource, "AuthenticationDetailsSource required");
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    public void setRememberMeServices(RememberMeServices rememberMeServices) {
        this.rememberMeServices = rememberMeServices;
    }

    public int getOrder() {
        return FilterChainOrder.BASIC_PROCESSING_FILTER;
    }
}
