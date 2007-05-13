package org.acegisecurity.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.providers.AuthenticationProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;

public class AuthenticationProviderOrderResolver implements BeanFactoryPostProcessor {
	
	/**
	 * 
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// retrieve all the AuthenticationProvider instances
		List providers = retrieveAllAuthenticationProviders(beanFactory);
		String[] names = beanFactory.getBeanNamesForType(AuthenticationManager.class);
		RootBeanDefinition definition = (RootBeanDefinition)beanFactory.getBeanDefinition(names[0]);
		definition.getPropertyValues().addPropertyValue("providers",providers);
	}
	/**
	 * 
	 * @param beanFactory
	 * @return
	 */
	private List retrieveAllAuthenticationProviders(ConfigurableListableBeanFactory beanFactory) {
		Map m = beanFactory.getBeansOfType(AuthenticationProvider.class);
		List l = new ArrayList(m.values());
		Collections.sort(l, new OrderComparator());
		return l;
	}

	
}
