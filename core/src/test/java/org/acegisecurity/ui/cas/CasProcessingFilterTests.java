/* Copyright 2004 Acegi Technology Pty Limited
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

package net.sf.acegisecurity.ui.cas;

import junit.framework.TestCase;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.MockAuthenticationManager;
import net.sf.acegisecurity.MockHttpServletRequest;
import net.sf.acegisecurity.MockHttpSession;


/**
 * Tests {@link CasProcessingFilter}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class CasProcessingFilterTests extends TestCase {
    //~ Constructors ===========================================================

    public CasProcessingFilterTests() {
        super();
    }

    public CasProcessingFilterTests(String arg0) {
        super(arg0);
    }

    //~ Methods ================================================================

    public final void setUp() throws Exception {
        super.setUp();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CasProcessingFilterTests.class);
    }

    public void testGetters() {
        CasProcessingFilter filter = new CasProcessingFilter();
        assertEquals("/j_acegi_cas_security_check",
            filter.getDefaultFilterProcessesUrl());
    }

    public void testNormalOperation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(null,
                new MockHttpSession());
        request.setParameter("ticket", "ST-0-ER94xMJmn6pha35CQRoZ");

        MockAuthenticationManager authMgr = new MockAuthenticationManager(true);

        CasProcessingFilter filter = new CasProcessingFilter();
        filter.setAuthenticationManager(authMgr);
        filter.init(null);

        Authentication result = filter.attemptAuthentication(request);
        assertTrue(result != null);
    }

    public void testNullServiceTicketHandledGracefully()
        throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(null,
                new MockHttpSession());

        MockAuthenticationManager authMgr = new MockAuthenticationManager(false);

        CasProcessingFilter filter = new CasProcessingFilter();
        filter.setAuthenticationManager(authMgr);
        filter.init(null);

        try {
            filter.attemptAuthentication(request);
            fail("Should have thrown AuthenticationException");
        } catch (AuthenticationException expected) {
            assertTrue(true);
        }
    }
}
