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

package org.springframework.security.providers.ldap;

import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.ldap.LdapAuthoritiesPopulator;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.LdapUserDetailsMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;

import junit.framework.TestCase;

import java.util.ArrayList;


/**
 * Tests {@link LdapAuthenticationProvider}.
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class LdapAuthenticationProviderTests extends TestCase {
    //~ Constructors ===================================================================================================

    public LdapAuthenticationProviderTests(String string) {
        super(string);
    }

    public LdapAuthenticationProviderTests() {
    }

    //~ Methods ========================================================================================================


    public void testSupportsUsernamePasswordAuthenticationToken() {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(new MockAuthenticator(),
                new MockAuthoritiesPopulator());

        assertTrue(ldapProvider.supports(UsernamePasswordAuthenticationToken.class));
    }

    public void testDefaultMapperIsSet() {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(new MockAuthenticator(),
                new MockAuthoritiesPopulator());

        assertTrue(ldapProvider.getUserDetailsContextMapper() instanceof LdapUserDetailsMapper);
    }

    public void testEmptyOrNullUserNameThrowsException() {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(new MockAuthenticator(),
                new MockAuthoritiesPopulator());

        try {
            ldapProvider.authenticate(new UsernamePasswordAuthenticationToken(null, "password"));
            fail("Expected BadCredentialsException for empty username");
        } catch (BadCredentialsException expected) {}

        try {
            ldapProvider.authenticate(new UsernamePasswordAuthenticationToken("", "bobspassword"));
            fail("Expected BadCredentialsException for null username");
        } catch (BadCredentialsException expected) {}
    }

    public void testEmptyPasswordIsRejected() {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(new MockAuthenticator());
        try {
            ldapProvider.authenticate(new UsernamePasswordAuthenticationToken("jen", ""));
            fail("Expected BadCredentialsException for empty password");
        } catch (BadCredentialsException expected) {}
    }

    public void testNormalUsage() {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(new MockAuthenticator(),
                new MockAuthoritiesPopulator());
        LdapUserDetailsMapper userMapper = new LdapUserDetailsMapper();
        userMapper.setRoleAttributes(new String[] {"ou"});
        ldapProvider.setUserDetailsContextMapper(userMapper);

        assertNotNull(ldapProvider.getAuthoritiesPopulator());

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken("ben", "benspassword");
        UserDetails user = (UserDetails) ldapProvider.authenticate(authRequest).getPrincipal();
        assertEquals(2, user.getAuthorities().length);
        assertEquals("{SHA}nFCebWjxfaLbHHG1Qk5UU4trbvQ=", user.getPassword());
        assertEquals("ben", user.getUsername());

        ArrayList authorities = new ArrayList();
        authorities.add(user.getAuthorities()[0].getAuthority());
        authorities.add(user.getAuthorities()[1].getAuthority());

        assertTrue(authorities.contains("ROLE_FROM_ENTRY"));
        assertTrue(authorities.contains("ROLE_FROM_POPULATOR"));
    }

    public void testUseWithNullAuthoritiesPopulatorReturnsCorrectRole() {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(new MockAuthenticator());
        LdapUserDetailsMapper userMapper = new LdapUserDetailsMapper();
        userMapper.setRoleAttributes(new String[] {"ou"});
        ldapProvider.setUserDetailsContextMapper(userMapper);
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken("ben", "benspassword");
        UserDetails user = (UserDetails) ldapProvider.authenticate(authRequest).getPrincipal();
        assertEquals(1, user.getAuthorities().length);
        assertEquals("ROLE_FROM_ENTRY", user.getAuthorities()[0].getAuthority());
    }

    //~ Inner Classes ==================================================================================================

    class MockAuthenticator implements LdapAuthenticator {

        public DirContextOperations authenticate(Authentication authentication) {
            DirContextAdapter ctx = new DirContextAdapter();
            ctx.setAttributeValue("ou", "FROM_ENTRY");
            String username = authentication.getName();
            String password = (String) authentication.getCredentials();


            if (username.equals("ben") && password.equals("benspassword")) {
                ctx.setDn(new DistinguishedName("cn=ben,ou=people,dc=springframework,dc=org"));
                ctx.setAttributeValue("userPassword","{SHA}nFCebWjxfaLbHHG1Qk5UU4trbvQ=");

                return ctx;
            } else if (username.equals("jen") && password.equals("")) {
                ctx.setDn(new DistinguishedName("cn=jen,ou=people,dc=springframework,dc=org"));

                return ctx;
            }

            throw new BadCredentialsException("Authentication failed.");
        }
    }

// This test kills apacheDS in embedded mode because the search returns an invalid DN
//    public void testIntegration() throws Exception {
//        BindAuthenticator authenticator = new BindAuthenticator(getInitialCtxFactory());
//        //PasswordComparisonAuthenticator authenticator = new PasswordComparisonAuthenticator();
//        //authenticator.setUserDnPatterns("cn={0},ou=people");
//
//        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch("ou=people", "(cn={0})", getInitialCtxFactory());
//
//        authenticator.setUserSearch(userSearch);
//        authenticator.afterPropertiesSet();
//
//        DefaultLdapAuthoritiesPopulator populator;
//        populator = new DefaultLdapAuthoritiesPopulator(getInitialCtxFactory(), "ou=groups");
//        populator.setRolePrefix("ROLE_");
//
//        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(authenticator, populator);
//
//        Authentication auth = ldapProvider.authenticate(new UsernamePasswordAuthenticationToken("Ben Alex","benspassword"));
//        assertEquals(2, auth.getAuthorities().length);
//    }
    class MockAuthoritiesPopulator implements LdapAuthoritiesPopulator {
        public GrantedAuthority[] getGrantedAuthorities(DirContextOperations userCtx, String username) {
            return new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_FROM_POPULATOR")};
        }
    }
}
