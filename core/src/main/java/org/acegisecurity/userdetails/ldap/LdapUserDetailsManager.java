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
package org.acegisecurity.userdetails.ldap;

import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.userdetails.UserDetailsManager;
import org.acegisecurity.ldap.LdapUtils;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;
import org.springframework.ldap.support.DistinguishedName;
import org.springframework.ldap.support.DirContextAdapter;
import org.springframework.ldap.LdapTemplate;
import org.springframework.ldap.AttributesMapper;
import org.springframework.ldap.ContextSource;
import org.springframework.ldap.ContextExecutor;
import org.springframework.ldap.SearchExecutor;
import org.springframework.ldap.EntryNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.*;
import javax.naming.ldap.LdapContext;
import javax.naming.directory.*;
import java.util.*;

/**
 * An Ldap implementation of UserDetailsManager.
 * <p>
 * It is designed around a standard setup where users and groups/roles are stored under separate contexts,
 * defined by the "userDnBase" and "groupSearchBase" properties respectively.
 * </p>
 * <p>
 * In this case, LDAP is being used purely to retrieve information and this class can be used in place of any other
 * UserDetailsService for authentication. Authentication isn't performed directly against the directory, unlike with the
 * LDAP authentication provider setup.
 * </p>
 *
 *
 * @author Luke Taylor
 * @since 2.0
 */
public class LdapUserDetailsManager implements UserDetailsManager {
    private final Log logger = LogFactory.getLog(LdapUserDetailsManager.class);

    /** The DN under which users entries are stored */
    private DistinguishedName userDnBase = new DistinguishedName("cn=users");
    /** The DN under which groups are stored */
    private DistinguishedName groupSearchBase = new DistinguishedName("cn=groups");

    /** The attribute which contains the user login name, and which is used by default to build the DN for new users */
    private String usernameAttributeName = "uid";
    /** Password attribute name */
    private String passwordAttributeName = "userPassword";

    /** The attribute which corresponds to the role name of a group. */
    private String groupRoleAttributeName ="cn";
    /** The attribute which contains members of a group */
    private String groupMemberAttributeName = "uniquemember";

    private String rolePrefix = "ROLE_";

    /** The pattern to be used for the user search. {0} is the user's DN */
    private String groupSearchFilter = "(uniquemember={0})";
    /**
     * The strategy used to create a UserDetails object from the LDAP context, username and list of authorities.
     * This should be set to match the required UserDetails implementation.
     */
    private UserDetailsContextMapper userDetailsMapper = new InetOrgPersonContextMapper();

    private LdapTemplate template;

    /** Default context mapper used to create a set of roles from a list of attributes */
    private AttributesMapper roleMapper = new AttributesMapper() {

        public Object mapFromAttributes(Attributes attributes) throws NamingException {
            Attribute roleAttr = attributes.get(groupRoleAttributeName);

            NamingEnumeration ne = roleAttr.getAll();
            // assert ne.hasMore();
            Object group = ne.next();
            String role = group.toString();

            return new GrantedAuthorityImpl(rolePrefix + role.toUpperCase());
        }
    };

    private String[] attributesToRetrieve = null;

    public LdapUserDetailsManager(ContextSource contextSource) {
        template = new LdapTemplate(contextSource);
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        DistinguishedName dn = buildDn(username);
        GrantedAuthority[] authorities = getUserAuthorities(dn, username);

        logger.debug("Loading user '"+ username + "' with DN '" + dn + "'");

        DirContextAdapter userCtx = loadUserAsContext(dn, username);

        return userDetailsMapper.mapUserFromContext(userCtx, username, authorities);
    }

    private UserContext loadUserAsContext(final DistinguishedName dn, final String username) {
        return (UserContext) template.executeReadOnly(new ContextExecutor() {
            public Object executeWithContext(DirContext ctx) throws NamingException {
                try {
                    Attributes attrs = ctx.getAttributes(dn, attributesToRetrieve);
                    return new UserContext(attrs, LdapUtils.getFullDn(dn, ctx));
                } catch(NameNotFoundException notFound) {
                    throw new UsernameNotFoundException("User " + username + " not found", notFound);
                }
            }
        });
    }

    /**
     * Changes the password for the current user. The username is obtained from the security context.
     * <p>
     * If the old password is supplied, the update will be made by rebinding as the user, thus modifying the password
     * using the user's permissions. If <code>oldPassword</code> is null, the update will be attempted using a
     * standard read/write context supplied by the context source.
     * </p>
     *
     * @param oldPassword the old password
     * @param newPassword the new value of the password.
     */
    public void changePassword(final String oldPassword, final String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.notNull(authentication,
                "No authentication object found in security context. Can't change current user's password!");

        String username = authentication.getName();




        logger.debug("Changing password for user '"+ username);

        final DistinguishedName dn = buildDn(username);
        final ModificationItem[] passwordChange = new ModificationItem[] {
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(passwordAttributeName, newPassword))
        };

        if(oldPassword == null) {
            template.modifyAttributes(dn, passwordChange);
            return;
        }

        template.executeReadWrite(new ContextExecutor() {

            public Object executeWithContext(DirContext dirCtx) throws NamingException {
                LdapContext ctx = (LdapContext) dirCtx;
                ctx.removeFromEnvironment("com.sun.jndi.ldap.connect.pool");
                ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, LdapUtils.getFullDn(dn, ctx).toUrl());
                ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, oldPassword);
                ctx.reconnect(null);

                ctx.modifyAttributes(dn, passwordChange);

                return null;
            }
        });
    }

    /**
     *
     * @param dn the distinguished name of the entry - may be either relative to the base context
     * or a complete DN including the name of the context (either is supported).
     * @param username the user whose roles are required.
     * @return the granted authorities returned by the group search
     */
    GrantedAuthority[] getUserAuthorities(final DistinguishedName dn, final String username) {
        SearchExecutor se = new SearchExecutor() {
            public NamingEnumeration executeSearch(DirContext ctx) throws NamingException {
                DistinguishedName fullDn = LdapUtils.getFullDn(dn, ctx);
                SearchControls ctrls = new SearchControls();
                ctrls.setReturningAttributes(new String[] {groupRoleAttributeName});

                return ctx.search(groupSearchBase, groupSearchFilter, new String[] {fullDn.toUrl(), username}, ctrls);
            }
        };

        LdapTemplate.AttributesMapperCallbackHandler roleCollector =
                template.new AttributesMapperCallbackHandler(roleMapper);

        template.search(se, roleCollector);
        List authorities = roleCollector.getList();

        return (GrantedAuthority[]) authorities.toArray(new GrantedAuthority[authorities.size()]);
    }

//    protected String getRoleFilter(DistinguishedName dn, String username) {
//        return new EqualsFilter("uniquemember", dn.toString()).encode();
//    }

    public void createUser(UserDetails user) {
        DirContextAdapter ctx = new DirContextAdapter();
        copyToContext(user, ctx);
        DistinguishedName dn = buildDn(user.getUsername());
        // Check for any existing authorities which might be set for this DN
        GrantedAuthority[] authorities = getUserAuthorities(dn, user.getUsername());

        if(authorities.length > 0) {
            removeAuthorities(dn, authorities);
        }

        logger.debug("Creating new user '"+ user.getUsername() + "' with DN '" + dn + "'");

        template.bind(dn, ctx, null);

        addAuthorities(dn, user.getAuthorities());
    }

    public void updateUser(UserDetails user) {
//        Assert.notNull(attributesToRetrieve, "Configuration must specify a list of attributes in order to use update.");
        DistinguishedName dn = buildDn(user.getUsername());

        logger.debug("Updating user '"+ user.getUsername() + "' with DN '" + dn + "'");

        GrantedAuthority[] authorities = getUserAuthorities(dn, user.getUsername());

        UserContext ctx = loadUserAsContext(dn, user.getUsername());
        ctx.setUpdateMode(true);
        copyToContext(user, ctx);

        // Remove the objectclass attribute from the list of mods (if present).
        List mods = new LinkedList(Arrays.asList(ctx.getModificationItems()));

        ListIterator modIt = mods.listIterator();
        while(modIt.hasNext()) {
            ModificationItem mod = (ModificationItem) modIt.next();
            Attribute a = mod.getAttribute();
            if("objectclass".equalsIgnoreCase(a.getID())) {
                modIt.remove();
            }
        }

        template.modifyAttributes(dn, (ModificationItem[]) mods.toArray(new ModificationItem[mods.size()]));

//        template.rebind(dn, ctx, null);
        // Remove the old authorities and replace them with the new one
        removeAuthorities(dn, authorities);
        addAuthorities(dn, user.getAuthorities());
    }

    public void deleteUser(String username) {
        DistinguishedName dn = buildDn(username);
        removeAuthorities(dn, getUserAuthorities(dn, username));
        template.unbind(dn);
    }

    public boolean userExists(String username) {
        DistinguishedName dn = buildDn(username);

        try {
            Object obj = template.lookup(dn);
            if (obj instanceof Context) {
                LdapUtils.closeContext((Context) obj);
            }
            return true;
        } catch(EntryNotFoundException e) {
            return false;
        }
    }

    /**
     * Constructs a DN from a username.
     * <p>
     * The default implementation appends a name component to the <tt>userDnBase</tt> context using the
     * <tt>usernameAttributeName</tt> property. So if the <tt>uid</tt> attribute is used to store the username, and the
     * base DN is <tt>cn=users</tt> and we are creating a new user called "sam", then the DN will be
     * <tt>uid=sam,cn=users</tt>.
     *
     * @param username the user name used for authentication.
     * @return the corresponding DN, relative to the base context.
     */
    protected DistinguishedName buildDn(String username) {
        DistinguishedName dn = new DistinguishedName(userDnBase);

        dn.add(usernameAttributeName, username);

        return dn;
    }

    /**
     * Creates a DN from a group name.
     *
     * @param group the name of the group
     * @return the DN of the corresponding group, including the groupSearchBase
     */
    protected DistinguishedName buildGroupDn(String group) {
        DistinguishedName dn = new DistinguishedName(groupSearchBase);
        dn.add(groupRoleAttributeName, group.toLowerCase());

        return dn;
    }

    protected void copyToContext(UserDetails user, DirContextAdapter ctx) {
        userDetailsMapper.mapUserToContext(user, ctx);
    }

    private void addAuthorities(DistinguishedName userDn, GrantedAuthority[] authorities) {
        modifyAuthorities(userDn, authorities, DirContext.ADD_ATTRIBUTE);
    }

    private void removeAuthorities(DistinguishedName userDn, GrantedAuthority[] authorities) {
        modifyAuthorities(userDn, authorities, DirContext.REMOVE_ATTRIBUTE);
    }

    private void modifyAuthorities(final DistinguishedName userDn, final GrantedAuthority[] authorities, final int modType) {
        template.executeReadWrite(new ContextExecutor() {
            public Object executeWithContext(DirContext ctx) throws NamingException {
                for(int i=0; i < authorities.length; i++) {
                    GrantedAuthority authority = authorities[i];
                    String group = convertAuthorityToGroup(authority);
                    DistinguishedName fullDn = LdapUtils.getFullDn(userDn, ctx);
                    ModificationItem addGroup = new ModificationItem(modType,
                            new BasicAttribute(groupMemberAttributeName, fullDn.toUrl()));

                    ctx.modifyAttributes(buildGroupDn(group), new ModificationItem[] {addGroup});
                }
                return null;
            }
        });
    }

    private String convertAuthorityToGroup(GrantedAuthority authority) {
        String group = authority.getAuthority();

        if(group.startsWith(rolePrefix)) {
            group = group.substring(rolePrefix.length());
        }

        return group;
    }

    public void setUsernameAttributeName(String usernameAttributeName) {
        this.usernameAttributeName = usernameAttributeName;
    }

    public void setPasswordAttributeName(String passwordAttributeName) {
        this.passwordAttributeName = passwordAttributeName;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = new DistinguishedName(groupSearchBase);
    }

    public void setGroupRoleAttributeName(String groupRoleAttributeName) {
        this.groupRoleAttributeName = groupRoleAttributeName;
    }

    public void setUserDnBase(String userDnBase) {
        this.userDnBase = new DistinguishedName(userDnBase);
    }

    public void setAttributesToRetrieve(String[] attributesToRetrieve) {
        Assert.notNull(attributesToRetrieve);
        this.attributesToRetrieve = attributesToRetrieve;
    }

    public void setUserDetailsMapper(UserDetailsContextMapper userDetailsMapper) {
        this.userDetailsMapper = userDetailsMapper;
    }

    /**
     * Sets the name of the multi-valued attribute which holds the DNs of users who are members of a group.
     * <p>
     * Usually this will be <tt>uniquemember</tt> (the default value) or <tt>member</tt>.
     * </p>
     *
     * @param groupMemberAttributeName the name of the attribute used to store group members.
     */
    public void setGroupMemberAttributeName(String groupMemberAttributeName) {
        Assert.hasText(groupMemberAttributeName);
        this.groupMemberAttributeName = groupMemberAttributeName;
        this.groupSearchFilter = "(" + groupMemberAttributeName + "={0})";
    }

    public void setRoleMapper(AttributesMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * This class allows us to set the <tt>updateMode</tt> property of DirContextAdapter when updating existing users.
     */
    private static class UserContext extends DirContextAdapter {
        public UserContext(Attributes pAttrs, Name dn) {
            super(pAttrs, dn);
        }

        protected void setUpdateMode(boolean mode) {
            super.setUpdateMode(mode);
        }
    }
}
