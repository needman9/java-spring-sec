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

package org.springframework.security.providers.ldap.authenticator;

import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.util.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;


/**
 * An {@link org.springframework.security.providers.ldap.LdapAuthenticator LdapAuthenticator} which compares the login
 * password with the value stored in the directory.
 *
 * <p>
 * This can be achieved either by retrieving the password attribute for the user and comparing it locally,
 * or by peforming an LDAP "compare" operation. If the password attribute (default "userPassword") is found in the
 * retrieved attributes it will be compared locally. If not, the remote comparison will be attempted.
 * </p>
 * <p>
 * If passwords are stored in digest form in the repository, then a suitable {@link PasswordEncoder}
 * implementation must be supplied. By default, passwords are encoded using the {@link LdapShaPasswordEncoder}.
 * </p>
 *
 * @author Luke Taylor
 * @version $Id$
 */
public final class PasswordComparisonAuthenticator extends AbstractLdapAuthenticator {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(PasswordComparisonAuthenticator.class);

    //~ Instance fields ================================================================================================

    private PasswordEncoder passwordEncoder = new LdapShaPasswordEncoder();
    private String passwordAttributeName = "userPassword";

    //~ Constructors ===================================================================================================

    public PasswordComparisonAuthenticator(BaseLdapPathContextSource contextSource) {
        super(contextSource);
    }

    //~ Methods ========================================================================================================

    public DirContextOperations authenticate(final Authentication authentication) {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");
        // locate the user and check the password

        DirContextOperations user = null;
        String username = authentication.getName();
        String password = (String)authentication.getCredentials();

        Iterator dns = getUserDns(username).iterator();

        SpringSecurityLdapTemplate ldapTemplate = new SpringSecurityLdapTemplate(getContextSource());

        while (dns.hasNext() && user == null) {
            final String userDn = (String) dns.next();

            try {
                user = ldapTemplate.retrieveEntry(userDn, getUserAttributes());
            } catch (NameNotFoundException ignore) {
            }
        }

        if (user == null && getUserSearch() != null) {
            user = getUserSearch().searchForUser(username);
        }

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        Object retrievedPassword = user.getObjectAttribute(passwordAttributeName);

        if (retrievedPassword != null) {
            if (!verifyPassword(password, retrievedPassword)) {
                throw new BadCredentialsException(messages.getMessage(
                        "PasswordComparisonAuthenticator.badCredentials", "Bad credentials"));
            }

            return user;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Password attribute wasn't retrieved for user '" + authentication
                    + "'. Performing LDAP compare of password attribute '" + passwordAttributeName + "'");
        }

        String encodedPassword = passwordEncoder.encodePassword(password, null);
        byte[] passwordBytes = LdapUtils.getUtf8Bytes(encodedPassword);

        if (!ldapTemplate.compare(user.getDn().toString(), passwordAttributeName, passwordBytes)) {
            throw new BadCredentialsException(messages.getMessage("PasswordComparisonAuthenticator.badCredentials",
                    "Bad credentials"));
        }

        return user;
    }

    public void setPasswordAttributeName(String passwordAttribute) {
        Assert.hasLength(passwordAttribute, "passwordAttributeName must not be empty or null");
        this.passwordAttributeName = passwordAttribute;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder must not be null.");
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Allows the use of both simple and hashed passwords in the directory.
     *
     * @param password the password supplied by the user
     * @param ldapPassword the (possibly hashed) password (from the directory)
     *
     * @return true if they match
     */
    protected boolean verifyPassword(String password, Object ldapPassword) {
        if (!(ldapPassword instanceof String)) {
            // Assume it's binary
            ldapPassword = new String((byte[]) ldapPassword);
        }

        if (ldapPassword.equals(password)) {
            return true;
        }

        if (passwordEncoder.isPasswordValid((String)ldapPassword, password, null)) {
            return true;
        }

        return false;
    }
}
