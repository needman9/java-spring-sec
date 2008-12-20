package org.springframework.security.providers.cas.cache;

import java.util.ArrayList;
import java.util.List;

import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.AssertionImpl;
import org.springframework.security.providers.cas.CasAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.util.AuthorityUtils;

/**
 *
 * @author Scott Battaglia
 * @version $Id$
 * @since 2.0
 *
 */
public abstract class AbstractStatelessTicketCacheTests {

    protected CasAuthenticationToken getToken() {
        List<String> proxyList = new ArrayList<String>();
        proxyList.add("https://localhost/newPortal/j_spring_cas_security_check");

        User user = new User("rod", "password", true, true, true, true, AuthorityUtils.createAuthorityList("ROLE_ONE", "ROLE_TWO"));
        final Assertion assertion = new AssertionImpl("rod");

        return new CasAuthenticationToken("key", user, "ST-0-ER94xMJmn6pha35CQRoZ",
                AuthorityUtils.createAuthorityList("ROLE_ONE", "ROLE_TWO"), user, assertion);
    }

}
