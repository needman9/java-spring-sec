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

package org.springframework.security.event.authorization;

import java.util.List;

import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;


/**
 * Indicates a secure object invocation failed because the principal could not
 * be authorized for the request.
 *
 * <p>This event might be thrown as a result of either an
 * {@link org.springframework.security.AccessDecisionManager AccessDecisionManager} or an
 * {@link org.springframework.security.AfterInvocationManager AfterInvocationManager}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class AuthorizationFailureEvent extends AbstractAuthorizationEvent {
    //~ Instance fields ================================================================================================

    private AccessDeniedException accessDeniedException;
    private Authentication authentication;
    private List<ConfigAttribute> configAttributeDefinition;

    //~ Constructors ===================================================================================================

    /**
     * Construct the event.
     *
     * @param secureObject the secure object
     * @param configAttribs that apply to the secure object
     * @param authentication that was found in the <code>SecurityContextHolder</code>
     * @param accessDeniedException that was returned by the
     *        <code>AccessDecisionManager</code>
     *
     * @throws IllegalArgumentException if any null arguments are presented.
     */
    public AuthorizationFailureEvent(Object secureObject, List<ConfigAttribute> configAttribs,
        Authentication authentication, AccessDeniedException accessDeniedException) {
        super(secureObject);

        if ((configAttribs == null) || (authentication == null) || (accessDeniedException == null)) {
            throw new IllegalArgumentException("All parameters are required and cannot be null");
        }

        this.configAttributeDefinition = configAttribs;
        this.authentication = authentication;
        this.accessDeniedException = accessDeniedException;
    }

    //~ Methods ========================================================================================================

    public AccessDeniedException getAccessDeniedException() {
        return accessDeniedException;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public List<ConfigAttribute> getConfigAttributes() {
        return configAttributeDefinition;
    }
}
