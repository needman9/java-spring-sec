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

package org.acegisecurity.providers.anonymous;

import org.acegisecurity.GrantedAuthority;

import org.acegisecurity.providers.AbstractAuthenticationToken;

import java.io.Serializable;


/**
 * Represents an anonymous <code>Authentication</code>.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class AnonymousAuthenticationToken extends AbstractAuthenticationToken
    implements Serializable {
    //~ Instance fields ========================================================

    private Object details;
    private Object principal;
    private boolean authenticated;
    private int keyHash;

    //~ Constructors ===========================================================

    /**
     * Constructor.
     *
     * @param key to identify if this object made by an authorised client
     * @param principal the principal (typically a <code>UserDetails</code>)
     * @param authorities the authorities granted to the principal
     *
     * @throws IllegalArgumentException if a <code>null</code> was passed
     */
    public AnonymousAuthenticationToken(String key, Object principal,
        GrantedAuthority[] authorities) {
        super(authorities);

        if ((key == null) || ("".equals(key)) || (principal == null)
            || "".equals(principal) || (authorities == null)
            || (authorities.length == 0)) {
            throw new IllegalArgumentException(
                "Cannot pass null or empty values to constructor");
        }

        this.keyHash = key.hashCode();
        this.principal = principal;
        this.authenticated = true;
    }

    //~ Methods ================================================================

    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof AnonymousAuthenticationToken) {
            AnonymousAuthenticationToken test = (AnonymousAuthenticationToken) obj;

            if (this.getKeyHash() != test.getKeyHash()) {
                return false;
            }

            if ((this.details == null) && (test.getDetails() == null)) {
                return true;
            }

            if ((this.details == null) && (test.getDetails() != null)) {
                return false;
            }

            if ((this.details != null) && (test.getDetails() == null)) {
                return false;
            }

            return this.details.equals(test.getDetails());
        }

        return false;
    }

    /**
     * Always returns an empty <code>String</code>
     *
     * @return an empty String
     */
    public Object getCredentials() {
        return "";
    }

    public Object getDetails() {
        return details;
    }

    public int getKeyHash() {
        return this.keyHash;
    }

    public Object getPrincipal() {
        return this.principal;
    }

    public boolean isAuthenticated() {
        return this.authenticated;
    }

    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}
