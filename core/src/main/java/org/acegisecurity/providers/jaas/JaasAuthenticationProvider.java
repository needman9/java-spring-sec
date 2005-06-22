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

package net.sf.acegisecurity.providers.jaas;

import net.sf.acegisecurity.AcegiSecurityException;
import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.providers.AuthenticationProvider;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.jaas.event.JaasAuthenticationFailedEvent;
import net.sf.acegisecurity.providers.jaas.event.JaasAuthenticationSuccessEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.springframework.core.io.Resource;

import org.springframework.util.Assert;

import java.io.IOException;

import java.security.Principal;
import java.security.Security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;


/**
 * An {@link AuthenticationProvider} implementation that retrieves user details
 * from a JAAS login configuration.
 * 
 * <p>
 * This <code>AuthenticationProvider</code> is capable of validating {@link
 * net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken}
 * requests contain the correct username and password.
 * </p>
 * 
 * <p>
 * This implementation is backed by a <a
 * href="http://java.sun.com/j2se/1.4.2/docs/guide/security/jaas/JAASRefGuide.html">JAAS</a>
 * configuration. The loginConfig property must be set to a given JAAS
 * configuration file. This setter accepts a Spring {@link
 * org.springframework.core.io.Resource} instance. It should point to a JAAS
 * configuration file containing an index matching the {@link
 * #setLoginContextName(java.lang.String) loginContextName} property.
 * </p>
 * 
 * <p>
 * For example: If this JaasAuthenticationProvider were configured in a Spring
 * WebApplicationContext the xml to set the loginConfiguration could be as
 * follows...
 * <pre>
 *  &lt;property name="loginConfig"&gt;
 *  &lt;value&gt;/WEB-INF/login.conf&lt;/value&gt;
 *  &lt;/property&gt;
 *  </pre>
 * </p>
 * 
 * <p>
 * The loginContextName should coincide with a given index in the loginConfig
 * specifed. The loginConfig file used in the JUnit tests appears as the
 * following...
 * <pre>
 *  JAASTest {
 *  net.sf.acegisecurity.providers.jaas.TestLoginModule required;
 *  };
 *  </pre>
 * Using the example login configuration above, the loginContextName property
 * would be set as <i>JAASTest</i>...
 * <pre>
 *  &lt;property name="loginContextName"&gt;
 *  &lt;value&gt;JAASTest&lt;/value&gt;
 *  &lt;/property&gt;
 *  </pre>
 * </p>
 * 
 * <p>
 * When using JAAS login modules as the authentication source, sometimes the <a
 * href="http://java.sun.com/j2se/1.4.2/docs/api/javax/security/auth/login/LoginContext.html">LoginContext</a>
 * will require <i>CallbackHandler</i>s. The JaasAuthenticationProvider uses
 * an internal <a
 * href="http://java.sun.com/j2se/1.4.2/docs/api/javax/security/auth/callback/CallbackHandler.html">CallbackHandler</a>
 * to wrap the {@link JaasAuthenticationCallbackHandler}s configured in the
 * ApplicationContext. When the LoginContext calls the internal
 * CallbackHandler, control is passed to each {@link
 * JaasAuthenticationCallbackHandler} for each Callback passed.
 * </p>
 * 
 * <p>
 * {{@link JaasAuthenticationCallbackHandler}s are passed to the
 * JaasAuthenticationProvider through the {@link
 * #setCallbackHandlers(net.sf.acegisecurity.providers.jaas.JaasAuthenticationCallbackHandler[])
 * callbackHandlers} property. }
 * <pre>
 *  &lt;property name="callbackHandlers"&gt;
 *  &lt;list&gt;
 *  &lt;bean class="net.sf.acegisecurity.providers.jaas.TestCallbackHandler"/&gt;
 *  &lt;bean class="{@link JaasNameCallbackHandler net.sf.acegisecurity.providers.jaas.JaasNameCallbackHandler}"/&gt;
 *  &lt;bean class="{@link JaasPasswordCallbackHandler net.sf.acegisecurity.providers.jaas.JaasPasswordCallbackHandler}"/&gt;
 *  &lt;/list&gt;
 *  &lt;/property&gt;
 *  </pre>
 * </p>
 * 
 * <p>
 * After calling LoginContext.login(), the JaasAuthenticationProvider will
 * retrieve the returned Principals from the Subject
 * (LoginContext.getSubject().getPrincipals). Each returned principal is then
 * passed to the configured {@link AuthorityGranter}s. An AuthorityGranter is
 * a mapping between a returned Principal, and a role name. If an
 * AuthorityGranter wishes to grant an Authorization a role, it returns that
 * role name from it's {@link AuthorityGranter#grant(java.security.Principal)}
 * method. The returned role will be applied to the Authorization object as a
 * {@link GrantedAuthority}.
 * </p>
 * 
 * <p>
 * AuthorityGranters are configured in spring xml as follows...
 * <pre>
 *  &lt;property name="authorityGranters"&gt;
 *  &lt;list&gt;
 *  &lt;bean class="net.sf.acegisecurity.providers.jaas.TestAuthorityGranter"/&gt;
 *  &lt;/list&gt;
 *  &lt;/property&gt;
 *  <p/>
 *  </pre>
 * </p>
 *
 * @author Ray Krueger
 * @version $Id$
 */
public class JaasAuthenticationProvider implements AuthenticationProvider,
    InitializingBean, ApplicationContextAware {
    //~ Static fields/initializers =============================================

    protected static final Log log = LogFactory.getLog(JaasAuthenticationProvider.class);

    //~ Instance fields ========================================================

    private ApplicationContext context;
    private LoginExceptionResolver loginExceptionResolver = new DefaultLoginExceptionResolver();
    private Resource loginConfig;
    private final String SYSPROP = "java.security.auth.login.config";
    private String loginContextName = "ACEGI";
    private AuthorityGranter[] authorityGranters;
    private JaasAuthenticationCallbackHandler[] callbackHandlers;

    //~ Methods ================================================================

    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        this.context = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * Set the AuthorityGranters that should be consulted for role names to be
     * granted to the Authentication.
     *
     * @param authorityGranters AuthorityGranter array
     *
     * @see JaasAuthenticationProvider
     */
    public void setAuthorityGranters(AuthorityGranter[] authorityGranters) {
        this.authorityGranters = authorityGranters;
    }

    /**
     * Returns the AuthorityGrannter array that was passed to the {@link
     * #setAuthorityGranters(AuthorityGranter[])} method, or null if it none
     * were ever set.
     *
     * @return The AuthorityGranter array, or null
     *
     * @see #setAuthorityGranters(net.sf.acegisecurity.providers.jaas.AuthorityGranter[])
     */
    public AuthorityGranter[] getAuthorityGranters() {
        return authorityGranters;
    }

    /**
     * Set the JAASAuthentcationCallbackHandler array to handle callback
     * objects generated by the LoginContext.login method.
     *
     * @param callbackHandlers Array of JAASAuthenticationCallbackHandlers
     */
    public void setCallbackHandlers(
        JaasAuthenticationCallbackHandler[] callbackHandlers) {
        this.callbackHandlers = callbackHandlers;
    }

    /**
     * Returns the current JaasAuthenticationCallbackHandler array, or null if
     * none are set.
     *
     * @return the JAASAuthenticationCallbackHandlers.
     *
     * @see #setCallbackHandlers(net.sf.acegisecurity.providers.jaas.JaasAuthenticationCallbackHandler[])
     */
    public JaasAuthenticationCallbackHandler[] getCallbackHandlers() {
        return callbackHandlers;
    }

    /**
     * Set the JAAS login configuration file.
     *
     * @param loginConfig <a
     *        href="http://www.springframework.org/docs/api/org/springframework/core/io/Resource.html">Spring
     *        Resource</a>
     *
     * @see <a
     *      href="http://java.sun.com/j2se/1.4.2/docs/guide/security/jaas/JAASRefGuide.html">JAAS
     *      Reference</a>
     */
    public void setLoginConfig(Resource loginConfig) {
        this.loginConfig = loginConfig;
    }

    public Resource getLoginConfig() {
        return loginConfig;
    }

    /**
     * Set the loginContextName, this name is used as the index to the
     * configuration specified in the loginConfig property.
     *
     * @param loginContextName
     */
    public void setLoginContextName(String loginContextName) {
        this.loginContextName = loginContextName;
    }

    public String getLoginContextName() {
        return loginContextName;
    }

    public void setLoginExceptionResolver(
        LoginExceptionResolver loginExceptionResolver) {
        this.loginExceptionResolver = loginExceptionResolver;
    }

    public LoginExceptionResolver getLoginExceptionResolver() {
        return loginExceptionResolver;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(loginConfig, "loginConfig must be set on " + getClass());
        Assert.hasLength(loginContextName,
            "loginContextName must be set on " + getClass());

        String loginConfigStr = null;

        try {
            loginConfigStr = loginConfig.getFile().toString();
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not resolve loginConfig [" + loginConfig
                    + "] as a File, using URL");
            }

            loginConfigStr = loginConfig.getURL().toString();
        }

        boolean allowed = "true".equalsIgnoreCase(Security.getProperty(
                    "policy.allowSystemProperty"));

        if (allowed && (System.getProperty(SYSPROP) == null)) {
            log.debug("Setting system property [" + SYSPROP + "] to: "
                + loginConfigStr);
            System.setProperty(SYSPROP, loginConfigStr);
        } else {
            setPropertyUsingLoop(loginConfigStr);
        }

        Assert.notNull(Configuration.getConfiguration(),
            "As per http://java.sun.com/j2se/1.5.0/docs/api/javax/security/auth/login/Configuration.html \"If a Configuration object was set via the Configuration.setConfiguration method, then that object is returned. Otherwise, a default Configuration object is returned\". Your JRE returned null to Configuration.getConfiguration().");
    }

    /**
     * Attempts to login the user given the Authentication objects principal
     * and credential
     *
     * @param auth The Authentication object to be authenticated.
     *
     * @return The authenticated Authentication object, with it's
     *         grantedAuthorities set.
     *
     * @throws AuthenticationException This implementation does not handle
     *         'locked' or 'disabled' accounts. This method only throws a
     *         AuthenticationServiceException, with the message of the
     *         LoginException that will be thrown, should the
     *         loginContext.login() method fail.
     */
    public Authentication authenticate(Authentication auth)
        throws AuthenticationException {
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken request = (UsernamePasswordAuthenticationToken) auth;

            try {
                //Create the LoginContext object, and pass our InternallCallbackHandler
                LoginContext lc = new LoginContext(loginContextName,
                        new InternalCallbackHandler(auth));

                //Attempt to login the user, the LoginContext will call our InternalCallbackHandler at this point.
                lc.login();

                //create a set to hold the authorities, and add any that have already been applied.
                Set authorities = new HashSet();

                if (request.getAuthorities() != null) {
                    authorities.addAll(Arrays.asList(request.getAuthorities()));
                }

                //get the subject principals and pass them to each of the AuthorityGranters
                Set principals = lc.getSubject().getPrincipals();

                for (Iterator iterator = principals.iterator();
                    iterator.hasNext();) {
                    Principal principal = (Principal) iterator.next();

                    for (int i = 0; i < authorityGranters.length; i++) {
                        AuthorityGranter granter = authorityGranters[i];
                        Set roles = granter.grant(principal);

                        //If the granter doesn't wish to grant any authorities, it should return null.
                        if ((roles != null) && !roles.isEmpty()) {
                            for (Iterator roleIterator = roles.iterator();
                                roleIterator.hasNext();) {
                                String role = roleIterator.next().toString();
                                authorities.add(new JaasGrantedAuthority(role,
                                        principal));
                            }
                        }
                    }
                }

                //Convert the authorities set back to an array and apply it to the token.
                UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(request
                        .getPrincipal(), request.getCredentials(),
                        (GrantedAuthority[]) authorities.toArray(
                            new GrantedAuthority[authorities.size()]));

                //Publish the success event
                publishSuccessEvent(result);

                //we're done, return the token.
                return result;
            } catch (LoginException loginException) {
                AcegiSecurityException ase = loginExceptionResolver
                    .resolveException(loginException);

                publishFailureEvent(request, ase);
                throw ase;
            }
        }

        return null;
    }

    public boolean supports(Class aClass) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(aClass);
    }

    /**
     * Publishes the {@link JaasAuthenticationFailedEvent}. Can be overridden
     * by subclasses for different functionality
     *
     * @param token The {@link UsernamePasswordAuthenticationToken} being
     *        processed
     * @param ase The {@link AcegiSecurityException} that caused the failure
     */
    protected void publishFailureEvent(
        UsernamePasswordAuthenticationToken token, AcegiSecurityException ase) {
        getApplicationContext().publishEvent(new JaasAuthenticationFailedEvent(
                token, ase));
    }

    /**
     * Publishes the {@link JaasAuthenticationSuccessEvent}. Can be overridden
     * by subclasses for different functionality.
     *
     * @param token The {@link UsernamePasswordAuthenticationToken} being
     *        processed
     */
    protected void publishSuccessEvent(
        UsernamePasswordAuthenticationToken token) {
        getApplicationContext().publishEvent(new JaasAuthenticationSuccessEvent(
                token));
    }

    /**
     * Loops through the login.config.url.1,login.config.url.2 properties
     * looking for the login configuration. If it is not set, it will be set
     * to the last available login.config.url.X property.
     *
     * @param loginConfigStr
     */
    private void setPropertyUsingLoop(String loginConfigStr) {
        boolean alreadySet = false;

        int n = 1;
        String prefix = "login.config.url.";
        String existing = null;

        while ((existing = Security.getProperty(prefix + n)) != null) {
            alreadySet = existing.equals(loginConfigStr);

            if (alreadySet) {
                break;
            }

            n++;
        }

        if (!alreadySet) {
            String key = prefix + n;
            log.debug("Setting security property [" + key + "] to: "
                + loginConfigStr);
            Security.setProperty(key, loginConfigStr);
        }
    }

    //~ Inner Classes ==========================================================

    /**
     * Wrapper class for JAASAuthenticationCallbackHandlers
     */
    private class InternalCallbackHandler implements CallbackHandler {
        private Authentication authentication;

        public InternalCallbackHandler(Authentication authentication) {
            this.authentication = authentication;
        }

        public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbackHandlers.length; i++) {
                JaasAuthenticationCallbackHandler handler = callbackHandlers[i];

                for (int j = 0; j < callbacks.length; j++) {
                    Callback callback = callbacks[j];

                    handler.handle(callback, authentication);
                }
            }
        }
    }
}
