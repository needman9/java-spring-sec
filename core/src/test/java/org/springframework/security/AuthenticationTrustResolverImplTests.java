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

package org.springframework.security;

import junit.framework.TestCase;

import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;
import org.springframework.security.providers.rememberme.RememberMeAuthenticationToken;


/**
 * Tests {@link org.springframework.security.AuthenticationTrustResolverImpl}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class AuthenticationTrustResolverImplTests extends TestCase {

    //~ Methods ========================================================================================================

    public void testCorrectOperationIsAnonymous() {
        AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();
        assertTrue(trustResolver.isAnonymous(
                new AnonymousAuthenticationToken("ignored", "ignored",
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ignored")})));
        assertFalse(trustResolver.isAnonymous(
                new TestingAuthenticationToken("ignored", "ignored",
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ignored")})));
    }

    public void testCorrectOperationIsRememberMe() {
        AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();
        assertTrue(trustResolver.isRememberMe(
                new RememberMeAuthenticationToken("ignored", "ignored",
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ignored")})));
        assertFalse(trustResolver.isAnonymous(
                new TestingAuthenticationToken("ignored", "ignored",
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ignored")})));
    }

    public void testGettersSetters() {
        AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();

        assertEquals(AnonymousAuthenticationToken.class, trustResolver.getAnonymousClass());
        trustResolver.setAnonymousClass(TestingAuthenticationToken.class);
        assertEquals(TestingAuthenticationToken.class, trustResolver.getAnonymousClass());

        assertEquals(RememberMeAuthenticationToken.class, trustResolver.getRememberMeClass());
        trustResolver.setRememberMeClass(TestingAuthenticationToken.class);
        assertEquals(TestingAuthenticationToken.class, trustResolver.getRememberMeClass());
    }
}
