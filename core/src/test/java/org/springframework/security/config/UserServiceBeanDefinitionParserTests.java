package org.springframework.security.config;

import org.springframework.security.util.InMemoryXmlApplicationContext;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.beans.FatalBeanException;

import org.junit.Test;
import org.junit.After;

/**
 * @author Luke Taylor
 * @version $Id$
 */
public class UserServiceBeanDefinitionParserTests {
    private AbstractXmlApplicationContext appContext;

    @After
    public void closeAppContext() {
        if (appContext != null) {
            appContext.close();
        }
    }

    @Test
    public void userServiceWithValidPropertiesFileWorksSuccessfully() {
        setContext(
                "<user-service id='service' " +
                        "properties='classpath:org/springframework/security/config/users.properties'/>");
        UserDetailsService userService = (UserDetailsService) appContext.getBean("service");
        userService.loadUserByUsername("bob");
        userService.loadUserByUsername("joe");
    }

    @Test
    public void userServiceWithEmbeddedUsersWorksSuccessfully() {
        setContext(
                "<user-service id='service'>" +
                "    <user name='joe' password='joespassword' authorities='ROLE_A'/>" +
                "</user-service>");
        UserDetailsService userService = (UserDetailsService) appContext.getBean("service");
        userService.loadUserByUsername("joe");
    }

    @Test(expected=FatalBeanException.class)
    public void userWithBothPropertiesAndEmbeddedUsersThrowsException() {
        setContext(
                "<user-service id='service' properties='doesntmatter.props'>" +
                "    <user name='joe' password='joespassword' authorities='ROLE_A'/>" +
                "</user-service>");
        UserDetailsService userService = (UserDetailsService) appContext.getBean("service");
        userService.loadUserByUsername("joe");
    }

    @Test(expected= FatalBeanException.class)
    public void multipleTopLevelUseWithoutIdThrowsException() {
        setContext(
                "<user-service properties='classpath:org/springframework/security/config/users.properties'/>" +
                "<user-service properties='classpath:org/springframework/security/config/users.properties'/>");

    }

    @Test(expected= FatalBeanException.class)
    public void userServiceWithMissingPropertiesFileThrowsException() {
        setContext("<user-service id='service' properties='classpath:doesntexist.properties'/>");
    }

    private void setContext(String context) {
        appContext = new InMemoryXmlApplicationContext(context);
    }
}
