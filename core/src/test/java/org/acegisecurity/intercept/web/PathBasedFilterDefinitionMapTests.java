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

package net.sf.acegisecurity.intercept.web;

import junit.framework.TestCase;

import net.sf.acegisecurity.ConfigAttributeDefinition;
import net.sf.acegisecurity.MockFilterChain;
import net.sf.acegisecurity.MockHttpServletRequest;
import net.sf.acegisecurity.MockHttpServletResponse;
import net.sf.acegisecurity.SecurityConfig;


/**
 * Tests parts of {@link PathBasedFilterInvocationDefinitionMap} not tested by
 * {@link FilterInvocationDefinitionSourceEditorWithPathsTests}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class PathBasedFilterDefinitionMapTests extends TestCase {
    //~ Constructors ===========================================================

    public PathBasedFilterDefinitionMapTests() {
        super();
    }

    public PathBasedFilterDefinitionMapTests(String arg0) {
        super(arg0);
    }

    //~ Methods ================================================================

    public final void setUp() throws Exception {
        super.setUp();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PathBasedFilterDefinitionMapTests.class);
    }

    public void testConvertUrlToLowercaseIsFalseByDefault() {
        PathBasedFilterInvocationDefinitionMap map = new PathBasedFilterInvocationDefinitionMap();
        assertFalse(map.isConvertUrlToLowercaseBeforeComparison());
    }

    public void testConvertUrlToLowercaseSetterRespected() {
        PathBasedFilterInvocationDefinitionMap map = new PathBasedFilterInvocationDefinitionMap();
        map.setConvertUrlToLowercaseBeforeComparison(true);
        assertTrue(map.isConvertUrlToLowercaseBeforeComparison());
    }

    public void testLookupNotRequiringExactMatchSuccessIfNotMatching() {
        PathBasedFilterInvocationDefinitionMap map = new PathBasedFilterInvocationDefinitionMap();
        map.setConvertUrlToLowercaseBeforeComparison(true);
        assertTrue(map.isConvertUrlToLowercaseBeforeComparison());

        ConfigAttributeDefinition def = new ConfigAttributeDefinition();
        def.addConfigAttribute(new SecurityConfig("ROLE_ONE"));
        map.addSecureUrl("/secure/super/**", def);

        // Build a HTTP request
        MockHttpServletRequest req = new MockHttpServletRequest(null);
        req.setServletPath("/SeCuRE/super/somefile.html");

        FilterInvocation fi = new FilterInvocation(req,
                new MockHttpServletResponse(), new MockFilterChain());

        ConfigAttributeDefinition response = map.lookupAttributes(fi);
        assertEquals(def, response);
    }

    public void testLookupRequiringExactMatchFailsIfNotMatching() {
        PathBasedFilterInvocationDefinitionMap map = new PathBasedFilterInvocationDefinitionMap();
        assertFalse(map.isConvertUrlToLowercaseBeforeComparison());

        ConfigAttributeDefinition def = new ConfigAttributeDefinition();
        def.addConfigAttribute(new SecurityConfig("ROLE_ONE"));
        map.addSecureUrl("/secure/super/**", def);

        // Build a HTTP request
        MockHttpServletRequest req = new MockHttpServletRequest(null);
        req.setServletPath("/SeCuRE/super/somefile.html");

        FilterInvocation fi = new FilterInvocation(req,
                new MockHttpServletResponse(), new MockFilterChain());

        ConfigAttributeDefinition response = map.lookupAttributes(fi);
        assertEquals(null, response);
    }

    public void testLookupRequiringExactMatchIsSuccessful() {
        PathBasedFilterInvocationDefinitionMap map = new PathBasedFilterInvocationDefinitionMap();
        assertFalse(map.isConvertUrlToLowercaseBeforeComparison());

        ConfigAttributeDefinition def = new ConfigAttributeDefinition();
        def.addConfigAttribute(new SecurityConfig("ROLE_ONE"));
        map.addSecureUrl("/secure/super/**", def);

        // Build a HTTP request
        MockHttpServletRequest req = new MockHttpServletRequest(null);
        req.setServletPath("/secure/super/somefile.html");

        FilterInvocation fi = new FilterInvocation(req,
                new MockHttpServletResponse(), new MockFilterChain());

        ConfigAttributeDefinition response = map.lookupAttributes(fi);
        assertEquals(def, response);
    }
}
