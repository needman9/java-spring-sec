package org.springframework.security.ui.preauth.j2ee;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.MutableGrantedAuthoritiesContainer;
import org.springframework.security.authoritymapping.Attributes2GrantedAuthoritiesMapper;
import org.springframework.security.authoritymapping.MappableAttributesRetriever;
import org.springframework.security.authoritymapping.SimpleAttributes2GrantedAuthoritiesMapper;
import org.springframework.security.ui.AuthenticationDetailsSourceImpl;
import org.springframework.util.Assert;

/**
 * Base implementation for classes scenarios where the authentication details object is used
 * to store a list of authorities obtained from the context object (such as an HttpServletRequest) 
 * passed to {@link #buildDetails(Object)}.
 * <p>
 * 
 * 
 * @author Luke Taylor
 * @since 2.0
 */
public abstract class AbstractPreAuthenticatedAuthenticationDetailsSource extends AuthenticationDetailsSourceImpl {
    protected final Log logger = LogFactory.getLog(getClass());
    protected String[] j2eeMappableRoles;
    protected Attributes2GrantedAuthoritiesMapper j2eeUserRoles2GrantedAuthoritiesMapper = 
        new SimpleAttributes2GrantedAuthoritiesMapper();

    public AbstractPreAuthenticatedAuthenticationDetailsSource() {
    }

    /**
     * Check that all required properties have been set.
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(j2eeMappableRoles, "No mappable roles available");
        Assert.notNull(j2eeUserRoles2GrantedAuthoritiesMapper, "Roles to granted authorities mapper not set");
    }

    /**
     * Build the authentication details object. If the specified authentication
     * details class implements {@link MutableGrantedAuthoritiesContainer}, a
     * list of pre-authenticated Granted Authorities will be set based on the
     * roles for the current user.
     *
     * @see org.springframework.security.ui.AuthenticationDetailsSource#buildDetails(Object)
     */
    public Object buildDetails(Object context) {
        Object result = super.buildDetails(context);
        
        if (result instanceof MutableGrantedAuthoritiesContainer) {
            String[] j2eeUserRoles = getUserRoles(context, j2eeMappableRoles);            
            GrantedAuthority[] userGas = j2eeUserRoles2GrantedAuthoritiesMapper.getGrantedAuthorities(j2eeUserRoles);

            if (logger.isDebugEnabled()) {
                logger.debug("J2EE user roles [" + Arrays.asList(j2eeUserRoles) + "] mapped to Granted Authorities: ["
                        + Arrays.asList(userGas) + "]");
            }
            
            ((MutableGrantedAuthoritiesContainer) result).setGrantedAuthorities(userGas);
        }
        return result;
    }
    
    /**
     * Allows the roles of the current user to be determined from the context object
     * 
     * @param context the context object (an HttpRequest, PortletRequest etc)
     * @param mappableRoles the possible roles as determined by the MappableAttributesRetriever
     * @return the subset of mappable roles which the current user has.
     */
    protected abstract String[] getUserRoles(Object context, String[] mappableRoles);

    /**
     * @param aJ2eeMappableRolesRetriever
     *            The MappableAttributesRetriever to use
     */
    public void setMappableRolesRetriever(MappableAttributesRetriever aJ2eeMappableRolesRetriever) {
        this.j2eeMappableRoles = aJ2eeMappableRolesRetriever.getMappableAttributes();
    }

    /**
     * @param mapper
     *            The Attributes2GrantedAuthoritiesMapper to use
     */
    public void setUserRoles2GrantedAuthoritiesMapper(Attributes2GrantedAuthoritiesMapper mapper) {
        j2eeUserRoles2GrantedAuthoritiesMapper = mapper;
    }
}