/*
 * The Acegi Security System for Spring is published under the terms
 * of the Apache Software License.
 *
 * Visit http://acegisecurity.sourceforge.net for further details.
 */

package net.sf.acegisecurity.adapters.catalina;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.AuthenticationManager;
import net.sf.acegisecurity.adapters.PrincipalAcegiUserToken;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.RealmBase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;

import java.security.Principal;
import java.security.cert.X509Certificate;

import java.util.Map;


/**
 * Adapter to enable Catalina (Tomcat) to authenticate via the Acegi Security
 * System for Spring.
 * 
 * <p>
 * Returns a {@link PrincipalAcegiUserToken} to Catalina's authentication
 * system, which is subsequently available via
 * <code>HttpServletRequest.getUserPrincipal()</code>.
 * </p>
 *
 * @author Ben Alex
 * @version $Id$
 */
public class CatalinaAcegiUserRealm extends RealmBase {
    //~ Static fields/initializers =============================================

    private static final Log logger = LogFactory.getLog(CatalinaAcegiUserRealm.class);

    //~ Instance fields ========================================================

    protected final String name = "CatalinaSpringUserRealm / $Id$";
    private AuthenticationManager authenticationManager;
    private Container container;
    private String appContextLocation;
    private String key;

    //~ Methods ================================================================

    public void setAppContextLocation(String appContextLocation) {
        this.appContextLocation = appContextLocation;
    }

    public String getAppContextLocation() {
        return appContextLocation;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public Principal authenticate(String username, String credentials) {
        if (username == null) {
            return null;
        }

        if (credentials == null) {
            credentials = "";
        }

        Authentication request = new UsernamePasswordAuthenticationToken(username,
                credentials);
        Authentication response = null;

        try {
            response = authenticationManager.authenticate(request);
        } catch (AuthenticationException failed) {
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication request for user: " + username
                    + " failed: " + failed.toString());
            }

            return null;
        }

        return new PrincipalAcegiUserToken(this.key,
            response.getPrincipal().toString(),
            response.getCredentials().toString(), response.getAuthorities());
    }

    public Principal authenticate(String username, byte[] credentials) {
        return authenticate(username, new String(credentials));
    }

    /**
     * Not supported, returns null
     *
     * @param username DOCUMENT ME!
     * @param digest DOCUMENT ME!
     * @param nonce DOCUMENT ME!
     * @param nc DOCUMENT ME!
     * @param cnonce DOCUMENT ME!
     * @param qop DOCUMENT ME!
     * @param realm DOCUMENT ME!
     * @param md5a2 DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public java.security.Principal authenticate(java.lang.String username,
        java.lang.String digest, java.lang.String nonce, java.lang.String nc,
        java.lang.String cnonce, java.lang.String qop, java.lang.String realm,
        java.lang.String md5a2) {
        return null;
    }

    /**
     * Not supported, returns null
     *
     * @param x509Certificates DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Principal authenticate(X509Certificate[] x509Certificates) {
        return null;
    }

    public boolean hasRole(Principal principal, String role) {
        if (!(principal instanceof PrincipalAcegiUserToken)) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                    "Expected passed principal to be of type PrincipalSpringUserToken but was "
                    + principal.getClass().getName());
            }

            return false;
        }

        PrincipalAcegiUserToken test = (PrincipalAcegiUserToken) principal;

        return test.isUserInRole(role);
    }

    public void start() throws LifecycleException {
        super.start();

        if (appContextLocation == null) {
            throw new LifecycleException("appContextLocation must be defined");
        }

        if (key == null) {
            throw new LifecycleException("key must be defined");
        }

        File xml = new File(System.getProperty("catalina.base"),
                appContextLocation);

        if (!xml.exists()) {
            throw new LifecycleException(
                "appContextLocation does not seem to exist - try specifying conf/springsecurity.xml");
        }

        FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext(xml
                .getAbsolutePath());
        Map beans = ctx.getBeansOfType(AuthenticationManager.class, true, true);

        if (beans.size() == 0) {
            throw new IllegalArgumentException(
                "Bean context must contain at least one bean of type AuthenticationManager");
        }

        String beanName = (String) beans.keySet().iterator().next();
        authenticationManager = (AuthenticationManager) beans.get(beanName);
        logger.info("CatalinaSpringUserRealm Started");
    }

    protected String getName() {
        return this.name;
    }

    /**
     * Always returns null (we override authenticate methods)
     *
     * @param arg0 DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    protected String getPassword(String arg0) {
        return null;
    }

    /**
     * Always returns null (we override authenticate methods)
     *
     * @param arg0 DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    protected Principal getPrincipal(String arg0) {
        return null;
    }
}
