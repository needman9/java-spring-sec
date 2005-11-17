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

package org.acegisecurity.context;

import junit.framework.TestCase;


/**
 * Tests {@link SecurityContextHolder}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class SecurityContextHolderTests extends TestCase {
    //~ Constructors ===========================================================

    public SecurityContextHolderTests() {
        super();
    }

    public SecurityContextHolderTests(String arg0) {
        super(arg0);
    }

    //~ Methods ================================================================

    public final void setUp() throws Exception {
        super.setUp();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SecurityContextHolderTests.class);
    }

    public void testContextHolderGetterSetter() {
        SecurityContext sc = new SecurityContextImpl();
        SecurityContextHolder.setContext(sc);
        assertEquals(sc, SecurityContextHolder.getContext());
    }

    public void testNeverReturnsNull() {
        assertNotNull(SecurityContextHolder.getContext());
    }

    public void testRejectsNulls() {
        try {
            SecurityContextHolder.setContext(null);
            fail("Should have rejected null");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }
}
