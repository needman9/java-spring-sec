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

package net.sf.acegisecurity.providers.dao.jdbc;

import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.providers.dao.AuthenticationDao;
import net.sf.acegisecurity.providers.dao.User;
import net.sf.acegisecurity.providers.dao.UsernameNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextException;

import org.springframework.dao.DataAccessException;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.object.MappingSqlQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.List;

import javax.sql.DataSource;


/**
 * <p>
 * Retrieves user details (username, password, enabled flag, and authorities)
 * from a JDBC location.
 * </p>
 * 
 * <p>
 * A default database structure is assumed, (see {@link
 * #DEF_USERS_BY_USERNAME_QUERY} and {@link
 * #DEF_AUTHORITIES_BY_USERNAME_QUERY}, which most users of this class will
 * need to override, if using an existing scheme. This may be done by setting
 * the default query strings used. If this does not provide enough
 * flexibility, another strategy would be to subclass this class and override
 * the {@link MappingSqlQuery} instances used, via the {@link
 * #initMappingSqlQueries()} extension point.
 * </p>
 * 
 * <p>
 * In order to minimise backward compatibility issues, this DAO does not
 * recognise the expiration of user accounts or the expiration of user
 * credentials. However, it does recognise and honour the user
 * enabled/disabled column.
 * </p>
 *
 * @author Ben Alex
 * @author colin sampaleanu
 * @version $Id$
 */
public class JdbcDaoImpl extends JdbcDaoSupport implements AuthenticationDao {
    //~ Static fields/initializers =============================================

    public static final String DEF_USERS_BY_USERNAME_QUERY = "SELECT username,password,enabled FROM users WHERE username = ?";
    public static final String DEF_AUTHORITIES_BY_USERNAME_QUERY = "SELECT username,authority FROM authorities WHERE username = ?";
    private static final Log logger = LogFactory.getLog(JdbcDaoSupport.class);

    //~ Instance fields ========================================================

    private MappingSqlQuery authoritiesByUsernameMapping;
    private MappingSqlQuery usersByUsernameMapping;
    private String authoritiesByUsernameQuery;
    private String rolePrefix = "";
    private String usersByUsernameQuery;

    //~ Constructors ===========================================================

    public JdbcDaoImpl() {
        usersByUsernameQuery = DEF_USERS_BY_USERNAME_QUERY;
        authoritiesByUsernameQuery = DEF_AUTHORITIES_BY_USERNAME_QUERY;
    }

    //~ Methods ================================================================

    public void setAuthoritiesByUsernameMapping(
        MappingSqlQuery authoritiesByUsernameQuery) {
        this.authoritiesByUsernameMapping = authoritiesByUsernameQuery;
    }

    public MappingSqlQuery getAuthoritiesByUsernameMapping() {
        return authoritiesByUsernameMapping;
    }

    /**
     * Allows the default query string used to retrieve authorities based on
     * username to be overriden, if default table or column names need to be
     * changed. The default query is {@link
     * #DEF_AUTHORITIES_BY_USERNAME_QUERY}; when modifying this query, ensure
     * that all returned columns are mapped back to the same column names as
     * in the default query.
     *
     * @param queryString The query string to set
     */
    public void setAuthoritiesByUsernameQuery(String queryString) {
        authoritiesByUsernameQuery = queryString;
    }

    public String getAuthoritiesByUsernameQuery() {
        return authoritiesByUsernameQuery;
    }

    /**
     * Allows a default role prefix to be specified. If this is set to a
     * non-empty value, then it is automatically prepended to any roles read
     * in from the db. This may for example be used to add the
     * <code>ROLE_</code> prefix expected to exist in role names (by default)
     * by some other Acegi Security framework classes, in the case that the
     * prefix is not already present in the db.
     *
     * @param rolePrefix the new prefix
     */
    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setUsersByUsernameMapping(MappingSqlQuery usersByUsernameQuery) {
        this.usersByUsernameMapping = usersByUsernameQuery;
    }

    public MappingSqlQuery getUsersByUsernameMapping() {
        return usersByUsernameMapping;
    }

    /**
     * Allows the default query string used to retrieve users based on username
     * to be overriden, if default table or column names need to be changed.
     * The default query is {@link #DEF_USERS_BY_USERNAME_QUERY}; when
     * modifying this query, ensure that all returned columns are mapped back
     * to the same column names as in the default query. If the 'enabled'
     * column does not exist in the source db, a permanent true value for this
     * column may be returned by using a query similar to <br>
     * <pre>
     * "SELECT username,password,'true' as enabled FROM users WHERE username = ?"
     * </pre>
     *
     * @param usersByUsernameQueryString The query string to set
     */
    public void setUsersByUsernameQuery(String usersByUsernameQueryString) {
        this.usersByUsernameQuery = usersByUsernameQueryString;
    }

    public String getUsersByUsernameQuery() {
        return usersByUsernameQuery;
    }

    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException, DataAccessException {
        List users = usersByUsernameMapping.execute(username);

        if (users.size() == 0) {
            throw new UsernameNotFoundException("User not found");
        }

        UserDetails user = (UserDetails) users.get(0); // contains no GrantedAuthority[]

        List dbAuths = authoritiesByUsernameMapping.execute(user.getUsername());

        if (dbAuths.size() == 0) {
            throw new UsernameNotFoundException("User has no GrantedAuthority");
        }

        GrantedAuthority[] arrayAuths = {};

        addCustomAuthorities(user.getUsername(), dbAuths);

        arrayAuths = (GrantedAuthority[]) dbAuths.toArray(arrayAuths);

        return new User(user.getUsername(), user.getPassword(),
            user.isEnabled(), true, true, arrayAuths);
    }

    /**
     * Allows subclasses to add their own granted authorities to the list to be
     * returned in the <code>User</code>.
     *
     * @param username the username, for use by finder methods
     * @param authorities the current granted authorities, as populated from
     *        the <code>authoritiesByUsername</code> mapping
     */
    protected void addCustomAuthorities(String username, List authorities) {}

    protected void initDao() throws ApplicationContextException {
        initMappingSqlQueries();
    }

    /**
     * Extension point to allow other MappingSqlQuery objects to be substituted
     * in a subclass
     */
    protected void initMappingSqlQueries() {
        setUsersByUsernameMapping(new UsersByUsernameMapping(getDataSource()));
        setAuthoritiesByUsernameMapping(new AuthoritiesByUsernameMapping(
                getDataSource()));
    }

    //~ Inner Classes ==========================================================

    /**
     * Query object to look up a user's authorities.
     */
    protected class AuthoritiesByUsernameMapping extends MappingSqlQuery {
        protected AuthoritiesByUsernameMapping(DataSource ds) {
            super(ds, authoritiesByUsernameQuery);
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum)
            throws SQLException {
            String roleName = rolePrefix + rs.getString(2);
            GrantedAuthorityImpl authority = new GrantedAuthorityImpl(roleName);

            return authority;
        }
    }

    /**
     * Query object to look up a user.
     */
    protected class UsersByUsernameMapping extends MappingSqlQuery {
        protected UsersByUsernameMapping(DataSource ds) {
            super(ds, usersByUsernameQuery);
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum)
            throws SQLException {
            String username = rs.getString(1);
            String password = rs.getString(2);
            boolean enabled = rs.getBoolean(3);
            UserDetails user = new User(username, password, enabled, true,
                    true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("HOLDER")});

            return user;
        }
    }
}
