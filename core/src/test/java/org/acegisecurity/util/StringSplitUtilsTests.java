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

package net.sf.acegisecurity.util;

import junit.framework.TestCase;

import org.springframework.util.StringUtils;

import java.util.Map;


/**
 * Tests {@link net.sf.acegisecurity.util.StringSplitUtils}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class StringSplitUtilsTests extends TestCase {
    //~ Constructors ===========================================================

    // ===========================================================
    public StringSplitUtilsTests() {
        super();
    }

    public StringSplitUtilsTests(String arg0) {
        super(arg0);
    }

    //~ Methods ================================================================

    // ================================================================
    public static void main(String[] args) {
        junit.textui.TestRunner.run(StringSplitUtilsTests.class);
    }

    public void testSplitEachArrayElementAndCreateMapNormalOperation() {
        // note it ignores malformed entries (ie those without an equals sign)
        String unsplit = "username=\"marissa\", invalidEntryThatHasNoEqualsSign, realm=\"Contacts Realm\", nonce=\"MTEwOTAyMzU1MTQ4NDo1YzY3OWViYWM5NDNmZWUwM2UwY2NmMDBiNDQzMTQ0OQ==\", uri=\"/acegi-security-sample-contacts-filter/secure/adminPermission.htm?contactId=4\", response=\"38644211cf9ac3da63ab639807e2baff\", qop=auth, nc=00000004, cnonce=\"2b8d329a8571b99a\"";
        String[] headerEntries = StringUtils.commaDelimitedListToStringArray(unsplit);
        Map headerMap = StringSplitUtils.splitEachArrayElementAndCreateMap(headerEntries,
                "=", "\"");

        assertEquals("marissa", headerMap.get("username"));
        assertEquals("Contacts Realm", headerMap.get("realm"));
        assertEquals("MTEwOTAyMzU1MTQ4NDo1YzY3OWViYWM5NDNmZWUwM2UwY2NmMDBiNDQzMTQ0OQ==",
            headerMap.get("nonce"));
        assertEquals("/acegi-security-sample-contacts-filter/secure/adminPermission.htm?contactId=4",
            headerMap.get("uri"));
        assertEquals("38644211cf9ac3da63ab639807e2baff",
            headerMap.get("response"));
        assertEquals("auth", headerMap.get("qop"));
        assertEquals("00000004", headerMap.get("nc"));
        assertEquals("2b8d329a8571b99a", headerMap.get("cnonce"));
        assertEquals(8, headerMap.size());
    }

    public void testSplitEachArrayElementAndCreateMapRespectsInstructionNotToRemoveCharacters() {
        String unsplit = "username=\"marissa\", realm=\"Contacts Realm\", nonce=\"MTEwOTAyMzU1MTQ4NDo1YzY3OWViYWM5NDNmZWUwM2UwY2NmMDBiNDQzMTQ0OQ==\", uri=\"/acegi-security-sample-contacts-filter/secure/adminPermission.htm?contactId=4\", response=\"38644211cf9ac3da63ab639807e2baff\", qop=auth, nc=00000004, cnonce=\"2b8d329a8571b99a\"";
        String[] headerEntries = StringUtils.commaDelimitedListToStringArray(unsplit);
        Map headerMap = StringSplitUtils.splitEachArrayElementAndCreateMap(headerEntries,
                "=", null);

        assertEquals("\"marissa\"", headerMap.get("username"));
        assertEquals("\"Contacts Realm\"", headerMap.get("realm"));
        assertEquals("\"MTEwOTAyMzU1MTQ4NDo1YzY3OWViYWM5NDNmZWUwM2UwY2NmMDBiNDQzMTQ0OQ==\"",
            headerMap.get("nonce"));
        assertEquals("\"/acegi-security-sample-contacts-filter/secure/adminPermission.htm?contactId=4\"",
            headerMap.get("uri"));
        assertEquals("\"38644211cf9ac3da63ab639807e2baff\"",
            headerMap.get("response"));
        assertEquals("auth", headerMap.get("qop"));
        assertEquals("00000004", headerMap.get("nc"));
        assertEquals("\"2b8d329a8571b99a\"", headerMap.get("cnonce"));
        assertEquals(8, headerMap.size());
    }

    public void testSplitEachArrayElementAndCreateMapReturnsNullIfArrayEmptyOrNull() {
        assertNull(StringSplitUtils.splitEachArrayElementAndCreateMap(null,
                "=", "\""));
        assertNull(StringSplitUtils.splitEachArrayElementAndCreateMap(
                new String[] {}, "=", "\""));
    }

    public void testSplitNormalOperation() {
        String unsplit = "username=\"marissa==\"";
        assertEquals("username", StringSplitUtils.split(unsplit, "=")[0]);
        assertEquals("\"marissa==\"", StringSplitUtils.split(unsplit, "=")[1]); // should not remove quotes or extra equals
    }

    public void testSplitRejectsNullsAndIncorrectLengthStrings() {
        try {
            StringSplitUtils.split(null, "="); // null
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }

        try {
            StringSplitUtils.split("", "="); // empty string
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }

        try {
            StringSplitUtils.split("sdch=dfgf", null); // null
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }

        try {
            StringSplitUtils.split("fvfv=dcdc", ""); // empty string
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }

        try {
            StringSplitUtils.split("dfdc=dcdc", "BIGGER_THAN_ONE_CHARACTER");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }
}
