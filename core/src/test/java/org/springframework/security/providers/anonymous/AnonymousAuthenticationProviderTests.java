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

package org.springframework.security.providers.anonymous;

import junit.framework.TestCase;

import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

import org.springframework.security.providers.TestingAuthenticationToken;


/**
 * Tests {@link AnonymousAuthenticationProvider}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class AnonymousAuthenticationProviderTests extends TestCase {
    //~ Constructors ===================================================================================================

    public AnonymousAuthenticationProviderTests() {
    }

    public AnonymousAuthenticationProviderTests(String arg0) {
        super(arg0);
    }

    //~ Methods ========================================================================================================

    public void testDetectsAnInvalidKey() throws Exception {
        AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();
        aap.setKey("qwerty");

        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken("WRONG_KEY", "Test",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl("ROLE_TWO")});

        try {
            Authentication result = aap.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
        }
    }

    public void testDetectsMissingKey() throws Exception {
        AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();

        try {
            aap.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    public void testGettersSetters() throws Exception {
        AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();
        aap.setKey("qwerty");
        aap.afterPropertiesSet();
        assertEquals("qwerty", aap.getKey());
    }

    public void testIgnoresClassesItDoesNotSupport() throws Exception {
        AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();
        aap.setKey("qwerty");

        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "password",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_A")});
        assertFalse(aap.supports(TestingAuthenticationToken.class));

        // Try it anyway
        assertNull(aap.authenticate(token));
    }

    public void testNormalOperation() throws Exception {
        AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();
        aap.setKey("qwerty");

        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken("qwerty", "Test",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl("ROLE_TWO")});

        Authentication result = aap.authenticate(token);

        assertEquals(result, token);
    }

    public void testSupports() {
        AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();
        assertTrue(aap.supports(AnonymousAuthenticationToken.class));
        assertFalse(aap.supports(TestingAuthenticationToken.class));
    }
}
