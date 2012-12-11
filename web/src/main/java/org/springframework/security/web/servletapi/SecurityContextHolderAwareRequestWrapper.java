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

package org.springframework.security.web.servletapi;


import java.security.Principal;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;


/**
 * A Spring Security-aware <code>HttpServletRequestWrapper</code>, which uses the
 * <code>SecurityContext</code>-defined <code>Authentication</code> object to implement the servlet API security
 * methods:
 *
 * <ul>
 * <li>{@link #getUserPrincipal()}</li>
 * <li>{@link SecurityContextHolderAwareRequestWrapper#isUserInRole(String)}</li>
 * <li>{@link HttpServletRequestWrapper#getRemoteUser()}.</li>
 * </ul>
 *
 * @see SecurityContextHolderAwareRequestFilter
 *
 * @author Orlando Garcia Carmona
 * @author Ben Alex
 * @author Luke Taylor
 */
public class SecurityContextHolderAwareRequestWrapper extends HttpServletRequestWrapper {
    //~ Instance fields ================================================================================================

    private final AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    /**
     * The prefix passed by the filter. It will be prepended to any supplied role values before
     * comparing it with the roles obtained from the security context.
     */
    private final String rolePrefix;

    //~ Constructors ===================================================================================================

    public SecurityContextHolderAwareRequestWrapper(HttpServletRequest request, String rolePrefix) {
        super(request);

        this.rolePrefix = rolePrefix;
    }

    //~ Methods ========================================================================================================

    /**
     * Obtain the current active <code>Authentication</code>
     *
     * @return the authentication object or <code>null</code>
     */
    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!authenticationTrustResolver.isAnonymous(auth)) {
            return auth;
        }

        return null;
    }

    /**
     * Returns the principal's name, as obtained from the <code>SecurityContextHolder</code>. Properly handles
     * both <code>String</code>-based and <code>UserDetails</code>-based principals.
     *
     * @return the username or <code>null</code> if unavailable
     */
    @Override
    public String getRemoteUser() {
        Authentication auth = getAuthentication();

        if ((auth == null) || (auth.getPrincipal() == null)) {
            return null;
        }

        if (auth.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) auth.getPrincipal()).getUsername();
        }

        return auth.getPrincipal().toString();
    }

    /**
     * Returns the <code>Authentication</code> (which is a subclass of <code>Principal</code>), or
     * <code>null</code> if unavailable.
     *
     * @return the <code>Authentication</code>, or <code>null</code>
     */
    @Override
    public Principal getUserPrincipal() {
        Authentication auth = getAuthentication();

        if ((auth == null) || (auth.getPrincipal() == null)) {
            return null;
        }

        return auth;
    }

    private boolean isGranted(String role) {
        Authentication auth = getAuthentication();

        if( rolePrefix != null ) {
            role = rolePrefix + role;
        }

        if ((auth == null) || (auth.getPrincipal() == null)) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        if (authorities == null) {
            return false;
        }


        for (GrantedAuthority grantedAuthority : authorities) {
            if (role.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Simple searches for an exactly matching {@link org.springframework.security.core.GrantedAuthority#getAuthority()}.
     * <p>
     * Will always return <code>false</code> if the <code>SecurityContextHolder</code> contains an
     * <code>Authentication</code> with <code>null</code><code>principal</code> and/or <code>GrantedAuthority[]</code>
     * objects.
     *
     * @param role the <code>GrantedAuthority</code><code>String</code> representation to check for
     *
     * @return <code>true</code> if an <b>exact</b> (case sensitive) matching granted authority is located,
     *         <code>false</code> otherwise
     */
    @Override
    public boolean isUserInRole(String role) {
        return isGranted(role);
    }

    @Override
    public String toString() {
        return "SecurityContextHolderAwareRequestWrapper[ " + getRequest() + "]";
    }
}
