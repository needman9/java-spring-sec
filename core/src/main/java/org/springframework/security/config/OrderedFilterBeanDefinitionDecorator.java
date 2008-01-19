package org.springframework.security.config;

import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import java.io.IOException;

/**
 * Replaces a Spring bean of type "Filter" with a wrapper class which implements the <tt>Ordered</tt>
 * interface. This allows user to add their own filter to the security chain. If the user's filter
 * already implements Ordered
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class OrderedFilterBeanDefinitionDecorator implements BeanDefinitionDecorator {

    public static final String ATT_ORDER = "order";

    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder holder, ParserContext parserContext) {
        Element elt = (Element)node;
        String order = elt.getAttribute(ATT_ORDER);
        BeanDefinition filter = holder.getBeanDefinition();
        BeanDefinition wrapper = new RootBeanDefinition(OrderedFilterDecorator.class);
        wrapper.getConstructorArgumentValues().addIndexedArgumentValue(0, holder.getBeanName());
        wrapper.getConstructorArgumentValues().addIndexedArgumentValue(1, filter);

        if (StringUtils.hasText(order)) {
            wrapper.getPropertyValues().addPropertyValue("order", order);
        }

        return new BeanDefinitionHolder(wrapper, holder.getBeanName());
    }

    static class OrderedFilterDecorator implements Filter, Ordered {
        private Integer order = null;
        private Filter delegate;
        private String beanName;

        OrderedFilterDecorator(String beanName, Filter delegate) {
            this.delegate = delegate;
            this.beanName = beanName;
        }

        public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            delegate.doFilter(request, response, chain);
        }

        public final void init(FilterConfig filterConfig) throws ServletException {
            delegate.init(filterConfig);
        }

        public final void destroy() {
            delegate.destroy();
        }

        public final int getOrder() {
            if(order == null) {
                Assert.isInstanceOf(Ordered.class, "Filter '"+ beanName +"' must implement the 'Ordered' interface " +
                        " or you must specify an order in <user-filter>");

                return ((Ordered)delegate).getOrder();
            }
            return order.intValue();
        }

        public final void setOrder(int order) {
            this.order = new Integer(order);
        }
    }
}
