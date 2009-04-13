package org.springframework.security.web.authentication.preauth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.SpringSecurityFilter;
import org.springframework.security.web.authentication.AbstractProcessingFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

/**
 * Base class for processing filters that handle pre-authenticated authentication requests. Subclasses must implement
 * the getPreAuthenticatedPrincipal() and getPreAuthenticatedCredentials() methods.
 * <p>
 * By default, the filter chain will proceed when an authentication attempt fails in order to allow other 
 * authentication mechanisms to process the request. To reject the credentials immediately, set the
 * <tt>continueFilterChainOnUnsuccessfulAuthentication</tt> flag to false. The exception raised by the
 * <tt>AuthenticationManager</tt> will the be re-thrown. Note that this will not affect cases where the principal
 * returned by {@link #getPreAuthenticatedPrincipal} is null, when the chain will still proceed as normal.
 * 
 *
 * @author Luke Taylor
 * @author Ruud Senden
 * @since 2.0
 */
public abstract class AbstractPreAuthenticatedProcessingFilter extends SpringSecurityFilter implements
        InitializingBean, ApplicationEventPublisherAware {

    private ApplicationEventPublisher eventPublisher = null;

    private AuthenticationDetailsSource authenticationDetailsSource = new WebAuthenticationDetailsSource();

    private AuthenticationManager authenticationManager = null;
    
    private boolean continueFilterChainOnUnsuccessfulAuthentication = true;

    /**
     * Check whether all required properties have been set.
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(authenticationManager, "An AuthenticationManager must be set");
    }

    /**
     * Try to authenticate a pre-authenticated user with Spring Security if the user has not yet been authenticated.
     */
    public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug("Checking secure context token: " + SecurityContextHolder.getContext().getAuthentication());
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            doAuthenticate(request, response);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Do the actual authentication for a pre-authenticated user.
     */
    private void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {
        Authentication authResult = null;

        Object principal = getPreAuthenticatedPrincipal(request);
        Object credentials = getPreAuthenticatedCredentials(request);

        if (principal == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No pre-authenticated principal found in request");
            }

            return;            
        }

        if (logger.isDebugEnabled()) {
            logger.debug("preAuthenticatedPrincipal = " + principal + ", trying to authenticate");
        }

        try {
            PreAuthenticatedAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(principal, credentials);
            authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
            authResult = authenticationManager.authenticate(authRequest);
            successfulAuthentication(request, response, authResult);
        } catch (AuthenticationException failed) {
            unsuccessfulAuthentication(request, response, failed);
            
            if (!continueFilterChainOnUnsuccessfulAuthentication) {
                throw failed;
            }
        }
    }

    /**
     * Puts the <code>Authentication</code> instance returned by the
     * authentication manager into the secure context.
     */
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) {
        if (logger.isDebugEnabled()) {
            logger.debug("Authentication success: " + authResult);
        }
        SecurityContextHolder.getContext().setAuthentication(authResult);
        // Fire event
        if (this.eventPublisher != null) {
            eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
        }
    }

    /**
     * Ensures the authentication object in the secure context is set to null
     * when authentication fails.
     */
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        SecurityContextHolder.clearContext();

        if (logger.isDebugEnabled()) {
            logger.debug("Cleared security context due to exception", failed);
        }
        request.getSession().setAttribute(AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY, failed);
    }

    /**
     * @param anApplicationEventPublisher
     *            The ApplicationEventPublisher to use
     */
    public void setApplicationEventPublisher(ApplicationEventPublisher anApplicationEventPublisher) {
        this.eventPublisher = anApplicationEventPublisher;
    }

    /**
     * @param authenticationDetailsSource
     *            The AuthenticationDetailsSource to use
     */
    public void setAuthenticationDetailsSource(AuthenticationDetailsSource authenticationDetailsSource) {
        Assert.notNull(authenticationDetailsSource, "AuthenticationDetailsSource required");
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    /**
     * @param authenticationManager
     *            The AuthenticationManager to use
     */
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    
    public void setContinueFilterChainOnUnsuccessfulAuthentication(boolean shouldContinue) {
        continueFilterChainOnUnsuccessfulAuthentication = shouldContinue;
    }

    /**
     * Override to extract the principal information from the current request 
     */
    protected abstract Object getPreAuthenticatedPrincipal(HttpServletRequest request);

    /**
     * Override to extract the credentials (if applicable) from the current request. Some implementations
     * may return a dummy value.
     */    
    protected abstract Object getPreAuthenticatedCredentials(HttpServletRequest request);
}
