package ru.misterparser.config;

import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Log4j2
public class ConfigurableHandlerBeanPostProcessor implements BeanPostProcessor {

    private Map<String, Class> saveAfterSetterClassMap = new LinkedHashMap<>();
    private Map<Object, Method> populateFromConfigMap = new LinkedHashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Configurable configurable = beanClass.getAnnotation(Configurable.class);
        if (configurable != null) {
            saveAfterSetterClassMap.put(beanName, beanClass);
        }
        return bean;
    }

    private class SaveAfterSetterMethodInterceptor implements MethodInterceptor {

        private Object bean;

        public SaveAfterSetterMethodInterceptor(Object bean) {
            this.bean = bean;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Object retVal = invocation.proceed();
            Method method = invocation.getMethod();
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                {
                    try {
                        Method saveMethod = bean.getClass().getMethod("save");
                        saveMethod.invoke(bean);
                    } catch (NoSuchMethodException ignored) {
                        log.debug("Не найден метод save в классе: {}", bean.getClass().getCanonicalName());
                    }
                }
                for (Map.Entry<Object, Method> entry : populateFromConfigMap.entrySet()) {
                    entry.getValue().invoke(entry.getKey());
                }
            }
            return retVal;
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {
            Method method = bean.getClass().getMethod("populateFromConfig");
            populateFromConfigMap.put(bean, method);
        } catch (NoSuchMethodException ignored) {
        }
        Class beanClass = saveAfterSetterClassMap.get(beanName);
        if (beanClass != null) {
            try {
                PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(beanClass, "config");
                ProxyFactory proxyFactory = new ProxyFactory();
                proxyFactory.setTarget(propertyDescriptor.getReadMethod().invoke(bean));
                proxyFactory.addAdvice(new SaveAfterSetterMethodInterceptor(bean));
                propertyDescriptor.getWriteMethod().invoke(bean, proxyFactory.getProxy());
            } catch (Exception e) {
                log.debug(e);
            }
        }
        return bean;
    }
}
