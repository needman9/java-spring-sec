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

package net.sf.acegisecurity.providers.dao;

import junit.framework.TestCase;

import net.sf.acegisecurity.AccountExpiredException;
import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationServiceException;
import net.sf.acegisecurity.BadCredentialsException;
import net.sf.acegisecurity.CredentialsExpiredException;
import net.sf.acegisecurity.DisabledException;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.LockedException;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.providers.TestingAuthenticationToken;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.dao.cache.EhCacheBasedUserCache;
import net.sf.acegisecurity.providers.dao.cache.NullUserCache;
import net.sf.acegisecurity.providers.dao.salt.SystemWideSaltSource;
import net.sf.acegisecurity.providers.encoding.ShaPasswordEncoder;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.HashMap;
import java.util.Map;


/**
 * Tests {@link DaoAuthenticationProvider}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class DaoAuthenticationProviderTests extends TestCase {
    //~ Methods ================================================================

    public final void setUp() throws Exception {
        super.setUp();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DaoAuthenticationProviderTests.class);
    }

    public void testAuthenticateFailsForIncorrectPasswordCase() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "KOala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());
        provider.setApplicationContext(new ClassPathXmlApplicationContext(
                "net/sf/acegisecurity/util/filtertest-valid.xml"));

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfAccountExpired() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserPeterAccountExpired());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown AccountExpiredException");
        } catch (AccountExpiredException expected) {
            assertTrue(true);
        }

        provider.setApplicationContext(new ClassPathXmlApplicationContext(
                "net/sf/acegisecurity/util/filtertest-valid.xml"));

        try {
            provider.authenticate(token);
            fail("Should have thrown AccountExpiredException");
        } catch (AccountExpiredException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfAccountLocked() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserPeterAccountLocked());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown LockedException");
        } catch (LockedException expected) {
            assertTrue(true);
        }

        provider.setApplicationContext(new ClassPathXmlApplicationContext(
                "net/sf/acegisecurity/util/filtertest-valid.xml"));

        try {
            provider.authenticate(token);
            fail("Should have thrown CredentialsExpiredException");
        } catch (LockedException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfCredentialsExpired() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserPeterCredentialsExpired());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown CredentialsExpiredException");
        } catch (CredentialsExpiredException expected) {
            assertTrue(true);
        }

        provider.setApplicationContext(new ClassPathXmlApplicationContext(
                "net/sf/acegisecurity/util/filtertest-valid.xml"));

        try {
            provider.authenticate(token);
            fail("Should have thrown CredentialsExpiredException");
        } catch (CredentialsExpiredException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfUserDisabled() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserPeter());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown DisabledException");
        } catch (DisabledException expected) {
            assertTrue(true);
        }

        provider.setApplicationContext(new ClassPathXmlApplicationContext(
                "net/sf/acegisecurity/util/filtertest-valid.xml"));

        try {
            provider.authenticate(token);
            fail("Should have thrown DisabledException");
        } catch (DisabledException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWhenAuthenticationDaoHasBackendFailure() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoSimulateBackendError());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown AuthenticationServiceException");
        } catch (AuthenticationServiceException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithEmptyUsername() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null,
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithInvalidPassword() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "INVALID_PASSWORD");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithInvalidUsernameAndHideUserNotFoundExceptionFalse() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("INVALID_USER",
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setHideUserNotFoundExceptions(false); // we want UsernameNotFoundExceptions
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown UsernameNotFoundException");
        } catch (UsernameNotFoundException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithInvalidUsernameAndHideUserNotFoundExceptionsWithDefaultOfTrue() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("INVALID_USER",
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        assertTrue(provider.isHideUserNotFoundExceptions());
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithMixedCaseUsernameIfDefaultChanged() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("MaRiSSA",
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticates() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");
        token.setDetails("192.168.0.1");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        Authentication result = provider.authenticate(token);

        if (!(result instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        UsernamePasswordAuthenticationToken castResult = (UsernamePasswordAuthenticationToken) result;
        assertEquals(User.class, castResult.getPrincipal().getClass());
        assertEquals("koala", castResult.getCredentials());
        assertEquals("ROLE_ONE", castResult.getAuthorities()[0].getAuthority());
        assertEquals("ROLE_TWO", castResult.getAuthorities()[1].getAuthority());
        assertEquals("192.168.0.1", castResult.getDetails());
    }

    public void testAuthenticatesASecondTime() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        Authentication result = provider.authenticate(token);

        if (!(result instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        // Now try to authenticate with the previous result (with its UserDetails)
        Authentication result2 = provider.authenticate(result);

        if (!(result2 instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        assertEquals(result.getCredentials(), result2.getCredentials());
    }

    public void testAuthenticatesWhenASaltIsUsed() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        SystemWideSaltSource salt = new SystemWideSaltSource();
        salt.setSystemWideSalt("SYSTEM_SALT_VALUE");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissaWithSalt());
        provider.setSaltSource(salt);
        provider.setUserCache(new MockUserCache());

        Authentication result = provider.authenticate(token);

        if (!(result instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        UsernamePasswordAuthenticationToken castResult = (UsernamePasswordAuthenticationToken) result;
        assertEquals(User.class, castResult.getPrincipal().getClass());

        // We expect original credentials user submitted to be returned
        assertEquals("koala", castResult.getCredentials());
        assertEquals("ROLE_ONE", castResult.getAuthorities()[0].getAuthority());
        assertEquals("ROLE_TWO", castResult.getAuthorities()[1].getAuthority());
    }

    public void testAuthenticatesWithForcePrincipalAsString() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());
        provider.setForcePrincipalAsString(true);

        Authentication result = provider.authenticate(token);

        if (!(result instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        UsernamePasswordAuthenticationToken castResult = (UsernamePasswordAuthenticationToken) result;
        assertEquals(String.class, castResult.getPrincipal().getClass());
        assertEquals("marissa", castResult.getPrincipal());
    }

    public void testDetectsNullBeingReturnedFromAuthenticationDao() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoReturnsNull());

        try {
            provider.authenticate(token);
            fail("Should have thrown AuthenticationServiceException");
        } catch (AuthenticationServiceException expected) {
            assertEquals("AuthenticationDao returned null, which is an interface contract violation",
                expected.getMessage());
        }
    }

    public void testGettersSetters() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new ShaPasswordEncoder());
        assertEquals(ShaPasswordEncoder.class,
            provider.getPasswordEncoder().getClass());

        provider.setSaltSource(new SystemWideSaltSource());
        assertEquals(SystemWideSaltSource.class,
            provider.getSaltSource().getClass());

        provider.setUserCache(new EhCacheBasedUserCache());
        assertEquals(EhCacheBasedUserCache.class,
            provider.getUserCache().getClass());

        assertFalse(provider.isForcePrincipalAsString());
        provider.setForcePrincipalAsString(true);
        assertTrue(provider.isForcePrincipalAsString());

        provider.setApplicationContext(new ClassPathXmlApplicationContext(
                "net/sf/acegisecurity/util/filtertest-valid.xml"));
        assertEquals(ClassPathXmlApplicationContext.class.getName(),
            provider.getContext().getClass().getName());
    }

    public void testGoesBackToAuthenticationDaoToObtainLatestPasswordIfCachedPasswordSeemsIncorrect() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        MockAuthenticationDaoUserMarissa authenticationDao = new MockAuthenticationDaoUserMarissa();
        MockUserCache cache = new MockUserCache();
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(authenticationDao);
        provider.setUserCache(cache);

        // This will work, as password still "koala"
        provider.authenticate(token);

        // Check "marissa = koala" ended up in the cache
        assertEquals("koala", cache.getUserFromCache("marissa").getPassword());

        // Now change the password the AuthenticationDao will return
        authenticationDao.setPassword("easternLongNeckTurtle");

        // Now try authentication again, with the new password
        token = new UsernamePasswordAuthenticationToken("marissa",
                "easternLongNeckTurtle");
        provider.authenticate(token);

        // To get this far, the new password was accepted
        // Check the cache was updated
        assertEquals("easternLongNeckTurtle",
            cache.getUserFromCache("marissa").getPassword());
    }

    public void testStartupFailsIfNoAuthenticationDao()
        throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        try {
            provider.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    public void testStartupFailsIfNoUserCacheSet() throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        assertEquals(NullUserCache.class, provider.getUserCache().getClass());
        provider.setUserCache(null);

        try {
            provider.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    public void testStartupSuccess() throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        AuthenticationDao dao = new MockAuthenticationDaoUserMarissa();
        provider.setAuthenticationDao(dao);
        provider.setUserCache(new MockUserCache());
        assertEquals(dao, provider.getAuthenticationDao());
        provider.afterPropertiesSet();
        assertTrue(true);
    }

    public void testSupports() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertTrue(!provider.supports(TestingAuthenticationToken.class));
    }

    //~ Inner Classes ==========================================================

    private class MockAuthenticationDaoReturnsNull implements AuthenticationDao {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            return null;
        }
    }

    private class MockAuthenticationDaoSimulateBackendError
        implements AuthenticationDao {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            throw new DataRetrievalFailureException(
                "This mock simulator is designed to fail");
        }
    }

    private class MockAuthenticationDaoUserMarissa implements AuthenticationDao {
        private String password = "koala";

        public void setPassword(String password) {
            this.password = password;
        }

        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            if ("marissa".equals(username)) {
                return new User("marissa", password, true, true, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockAuthenticationDaoUserMarissaWithSalt
        implements AuthenticationDao {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            if ("marissa".equals(username)) {
                return new User("marissa", "koala{SYSTEM_SALT_VALUE}", true,
                    true, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockAuthenticationDaoUserPeter implements AuthenticationDao {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            if ("peter".equals(username)) {
                return new User("peter", "opal", false, true, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockAuthenticationDaoUserPeterAccountExpired
        implements AuthenticationDao {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            if ("peter".equals(username)) {
                return new User("peter", "opal", true, false, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockAuthenticationDaoUserPeterAccountLocked
        implements AuthenticationDao {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            if ("peter".equals(username)) {
                return new User("peter", "opal", true, true, true, false,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockAuthenticationDaoUserPeterCredentialsExpired
        implements AuthenticationDao {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            if ("peter".equals(username)) {
                return new User("peter", "opal", true, true, false, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockUserCache implements UserCache {
        private Map cache = new HashMap();

        public UserDetails getUserFromCache(String username) {
            return (User) cache.get(username);
        }

        public void putUserInCache(UserDetails user) {
            cache.put(user.getUsername(), user);
        }

        public void removeUserFromCache(String username) {}
    }
}
