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

package org.acegisecurity.providers.ldap.authenticator;

import junit.framework.TestCase;


/**
 * Tests {@link LdapShaPasswordEncoder}.
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class LdapShaPasswordEncoderTests extends TestCase {
    //~ Instance fields ================================================================================================

    LdapShaPasswordEncoder sha;

    //~ Methods ========================================================================================================

    protected void setUp() throws Exception {
        super.setUp();
        sha = new LdapShaPasswordEncoder();
    }

    public void testInvalidPasswordFails() {
        assertFalse(sha.isPasswordValid("{SHA}ddSFGmjXYPbZC+NXR2kCzBRjqiE=", "wrongpassword", null));
    }

    public void testInvalidSaltedPasswordFails() {
        assertFalse(sha.isPasswordValid("{SSHA}25ro4PKC8jhQZ26jVsozhX/xaP0suHgX", "wrongpassword", null));
        assertFalse(sha.isPasswordValid("{SSHA}PQy2j+6n5ytA+YlAKkM8Fh4p6u2JxfVd", "wrongpassword", null));
    }

    public void testNonByteArraySaltThrowsException() {
        try {
            sha.encodePassword("password", "AStringNotAByteArray");
        } catch (IllegalArgumentException expected) {}
    }

    /**
     * Test values generated by 'slappasswd -h {SHA} -s boabspasswurd'
     */
    public void testValidPasswordSucceeds() {
        assertTrue(sha.isPasswordValid("{SHA}ddSFGmjXYPbZC+NXR2kCzBRjqiE=", "boabspasswurd", null));
    }

    /**
     * Test values generated by 'slappasswd -s boabspasswurd'
     */
    public void testValidSaltedPasswordSucceeds() {
        assertTrue(sha.isPasswordValid("{SSHA}25ro4PKC8jhQZ26jVsozhX/xaP0suHgX", "boabspasswurd", null));
        assertTrue(sha.isPasswordValid("{SSHA}PQy2j+6n5ytA+YlAKkM8Fh4p6u2JxfVd", "boabspasswurd", null));
    }
}
