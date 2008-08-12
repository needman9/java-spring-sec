package org.springframework.security.config;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.config.ConfigTestUtils.*;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.AuthenticationCredentialsNotFoundException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.annotation.BusinessService;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.util.InMemoryXmlApplicationContext;

/**
 * @author Ben Alex
 * @author Luke Taylor
 * @version $Id$
 */
public class GlobalMethodSecurityBeanDefinitionParserTests {
    private AbstractXmlApplicationContext appContext;

    private BusinessService target;

    public void loadContext() {
        setContext(
                "<b:bean id='target' class='org.springframework.security.annotation.BusinessServiceImpl'/>" +
                "<global-method-security>" +
                "    <protect-pointcut expression='execution(* *.someUser*(..))' access='ROLE_USER'/>" +
                "    <protect-pointcut expression='execution(* *.someAdmin*(..))' access='ROLE_ADMIN'/>" +
                "</global-method-security>" + ConfigTestUtils.AUTH_PROVIDER_XML
                    );
        target = (BusinessService) appContext.getBean("target");
   }

    @After
    public void closeAppContext() {
        if (appContext != null) {
            appContext.close();
        }
        SecurityContextHolder.clearContext();
    }

    @Test(expected=AuthenticationCredentialsNotFoundException.class)
    public void targetShouldPreventProtectedMethodInvocationWithNoContext() {
        loadContext();
        target.someUserMethod1();
    }

    @Test
    public void targetShouldAllowProtectedMethodInvocationWithCorrectRole() {
        loadContext();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_USER")});
        SecurityContextHolder.getContext().setAuthentication(token);

        target.someUserMethod1();
    }

    @Test(expected=AccessDeniedException.class)
    public void targetShouldPreventProtectedMethodInvocationWithIncorrectRole() {
        loadContext();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_SOMEOTHERROLE")});
        SecurityContextHolder.getContext().setAuthentication(token);

        target.someAdminMethod();
    }

    @Test
    public void doesntInterfereWithBeanPostProcessing() {
        setContext(
                "<b:bean id='myUserService' class='org.springframework.security.config.PostProcessedMockUserDetailsService'/>" +
                "<global-method-security />" +
                "<authentication-provider user-service-ref='myUserService'/>" +
                "<b:bean id='beanPostProcessor' class='org.springframework.security.config.MockUserServiceBeanPostProcessor'/>"
        );

        PostProcessedMockUserDetailsService service = (PostProcessedMockUserDetailsService)appContext.getBean("myUserService");

        assertEquals("Hello from the post processor!", service.getPostProcessorWasHere());
    }

    @Test(expected=AccessDeniedException.class)
    public void worksWithAspectJAutoproxy() {
        setContext(
                "<global-method-security>" +
                "  <protect-pointcut expression='execution(* org.springframework.security.config.*Service.*(..))'" +
                "       access='ROLE_SOMETHING' />" +
                "</global-method-security>" +
                "<b:bean id='myUserService' class='org.springframework.security.config.PostProcessedMockUserDetailsService'/>" +
                "<aop:aspectj-autoproxy />" +
                "<authentication-provider user-service-ref='myUserService'/>"
        );

        UserDetailsService service = (UserDetailsService) appContext.getBean("myUserService");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_SOMEOTHERROLE")});
        SecurityContextHolder.getContext().setAuthentication(token);

        service.loadUserByUsername("notused");
    }

    @Test
    public void supportsMethodArgumentsInPointcut() {
        setContext(
                "<b:bean id='target' class='org.springframework.security.annotation.BusinessServiceImpl'/>" +
                "<global-method-security>" +
                "   <protect-pointcut expression='execution(* *.someOther(String))' access='ROLE_ADMIN'/>" +
                "   <protect-pointcut expression='execution(* *.BusinessService*(..))' access='ROLE_USER'/>" +
                "</global-method-security>" + ConfigTestUtils.AUTH_PROVIDER_XML
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));
        target = (BusinessService) appContext.getBean("target");
        // someOther(int) should not be matched by someOther(String)
        target.someOther(0);
    }

    @Test(expected=BeanDefinitionParsingException.class)
    public void duplicateElementCausesError() {
        setContext(
                "<global-method-security />" +
                "<global-method-security />"
        );
    }

    @Test(expected=AccessDeniedException.class)
    public void worksWithoutTargetOrClass() {
        setContext(
                "<global-method-security secured-annotations='enabled'/>" +
                "<b:bean id='businessService' class='org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean'>" +
                "    <b:property name='serviceUrl' value='http://localhost:8080/SomeService'/>" +
                "    <b:property name='serviceInterface' value='org.springframework.security.annotation.BusinessService'/>" +
                "</b:bean>" + AUTH_PROVIDER_XML
                );

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_SOMEOTHERROLE")});
        SecurityContextHolder.getContext().setAuthentication(token);
        target = (BusinessService) appContext.getBean("businessService");
        target.someUserMethod1();
    }

    private void setContext(String context) {
        appContext = new InMemoryXmlApplicationContext(context);
    }
}


