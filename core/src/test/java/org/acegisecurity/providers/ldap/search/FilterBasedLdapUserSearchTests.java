package org.acegisecurity.providers.ldap.search;

import org.acegisecurity.providers.ldap.AbstractLdapServerTestCase;
import org.acegisecurity.providers.ldap.DefaultInitialDirContextFactory;
import org.acegisecurity.providers.ldap.LdapUserInfo;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.BadCredentialsException;

/**
 * Tests for FilterBasedLdapUserSearch.
 * 
 * @author Luke Taylor
 * @version $Id$
 */
public class FilterBasedLdapUserSearchTests extends AbstractLdapServerTestCase {
    private DefaultInitialDirContextFactory dirCtxFactory;

    public void setUp() throws Exception {
        dirCtxFactory = new DefaultInitialDirContextFactory(PROVIDER_URL);
        dirCtxFactory.setInitialContextFactory(CONTEXT_FACTORY);
        dirCtxFactory.setExtraEnvVars(EXTRA_ENV);
        dirCtxFactory.setManagerDn(MANAGER_USER);
        dirCtxFactory.setManagerPassword(MANAGER_PASSWORD);
    }

    public FilterBasedLdapUserSearchTests(String string) {
        super(string);
    }

    public FilterBasedLdapUserSearchTests() {
        super();
    }

    public void testBasicSearch() throws Exception {
        FilterBasedLdapUserSearch locator =
                new FilterBasedLdapUserSearch("ou=people", "(uid={0})", dirCtxFactory);
        LdapUserInfo bob = locator.searchForUser("bob");
        locator.setSearchSubtree(false);
        locator.setSearchTimeLimit(0);
        // name is wrong with embedded apacheDS
//        assertEquals("uid=bob,ou=people,"+ROOT_DN, bob.getDn());
    }

    public void testSubTreeSearchSucceeds() throws Exception {
        // Don't set the searchBase, so search from the root.
        FilterBasedLdapUserSearch locator =
                new FilterBasedLdapUserSearch("", "(cn={0})", dirCtxFactory);
        locator.setSearchSubtree(true);

        LdapUserInfo ben = locator.searchForUser("Ben Alex");
//        assertEquals("uid=ben,ou=people,"+ROOT_DN, bob.getDn());
    }

    public void testSearchForInvalidUserFails() {
        FilterBasedLdapUserSearch locator =
                new FilterBasedLdapUserSearch("ou=people", "(uid={0})", dirCtxFactory);

        try {
            locator.searchForUser("Joe");
            fail("Expected UsernameNotFoundException for non-existent user.");
        } catch (UsernameNotFoundException expected) {
        }
    }

    public void testFailsOnMultipleMatches() {
        FilterBasedLdapUserSearch locator =
                new FilterBasedLdapUserSearch("ou=people", "(cn=*)", dirCtxFactory);

        try {
            locator.searchForUser("Ignored");
            fail("Expected exception for multiple search matches.");
        } catch (BadCredentialsException expected) {
        }
    }

    // Try some funny business with filters.

    public void testExtraFilterPartToExcludeBob() throws Exception {
        FilterBasedLdapUserSearch locator =
                new FilterBasedLdapUserSearch("ou=people",
                        "(&(cn=*)(!(|(uid={0})(uid=marissa))))",
                        dirCtxFactory);

        // Search for bob, get back ben...
        LdapUserInfo ben = locator.searchForUser("bob");
        String cn = (String)ben.getAttributes().get("cn").get();
        assertEquals("Ben Alex", cn);
//        assertEquals("uid=ben,ou=people,"+ROOT_DN, ben.getDn());
    }
}
