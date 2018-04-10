package io.nordstrom.org.scaevents.exception;

/**
 * Created by bmwi on 4/9/18.
 */


import io.nordstrom.org.scaevents.annotation.AsyncRetryable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.util.ClassUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;




public class SimpleAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAsyncExceptionHandler.class);

    private ApplicationContext applicationContext;

    public SimpleAsyncExceptionHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... objects) {
        LOGGER.info("Using SimpleAsyncExceptionHandler");
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(String.format("Unexpected error occurred invoking async " +
                    "method '%s'.", method), ex);
        }

        Class declaringClass = method.getDeclaringClass();
        AsyncRetryable asyncRetryable = AnnotationUtils.findAnnotation(method, AsyncRetryable.class);
        //If Method or methods from superClass don't have this annotation find at class level
        //This needs to be done after method level annotation is not to be found since method level
        // overrides at class level
        if (null == asyncRetryable) {
            asyncRetryable = AnnotationUtils.findAnnotation(declaringClass, AsyncRetryable.class);
        }

        if (null != asyncRetryable) {
            LOGGER.info("Retryable annotation is present and checking if exception is configured to be retryable ");
            int maxAttempts = asyncRetryable.maxAttempts();
            List<Class<? extends Throwable>> retryableExceptions = Arrays.asList(asyncRetryable.value());
            Backoff backOff = asyncRetryable.backoff();

            double sleepTimeInMilliSeconds = backOff.delay();
            double multiplierInEachIteration = backOff.multiplier();

            Optional<Class<? extends Throwable>> retryableExceptionOptional = retryableExceptions.stream().filter(
                    retryableException -> (retryableException.isAssignableFrom(ex.getClass()) ||
                            (ex.getCause() != null && retryableException.isAssignableFrom(ex.getCause().getClass()))))
                    .findAny();


            if (retryableExceptionOptional.isPresent()) {
                LOGGER.info("Retrying for : " + ex.getClass().getName());
                for (int i = 1; i <= maxAttempts; i++) {
                    String logString = new StringBuilder().append(ex.getClass().getName())
                            .append(" encountered. Retrying ").append(i).append(" times after ")
                            .append(sleepTimeInMilliSeconds).append(" milliseconds.").toString();
                    LOGGER.info(logString);
                    try {
                        Thread.sleep((long) sleepTimeInMilliSeconds);
                        try {
                            executeRetry(declaringClass, method, objects);
                            //Break on Successful retry
                            LOGGER.info("Got success after " + i + " retry attempt. Hence Exiting.");
                            break;
                        } catch (InvocationTargetException e) {
                            LOGGER.warn("Exception encountered on " + i + " retry attempt ");
                            if (i == maxAttempts) {
                                LOGGER.warn("Final retry attempt reached and hence giving up.");
                            }
                        }
                        if (multiplierInEachIteration > 0) {
                            sleepTimeInMilliSeconds = multiplierInEachIteration * sleepTimeInMilliSeconds;
                        }
                    } catch (InterruptedException e) {
                        LOGGER.warn("InterruptedException while retrying : " + ExceptionUtils.getStackTrace(e));
                    }
                }
            } else {
                LOGGER.info("Not a retryable exception");
            }
        } else {
            LOGGER.info("Retriable annotation is not present and hence not retrying. ");

        }


    }


    private Object executeRetry(Class declaringClass, Method method, Object... objects) throws InvocationTargetException {
        Object bean = null;
        LOGGER.info("Declaring class : " + declaringClass.getName());
        try {
            //When there is no interface declared for the bean then declaringClass's proxy will be used to get target
            // object and invoke method
            bean = applicationContext.getBean(declaringClass);
            bean = getTargetObject(bean);
            try {
                return method.invoke(bean, objects);
            } catch (IllegalAccessException | IllegalArgumentException e2) {
                LOGGER.error("Error invoking methods via reflections API  : " + ExceptionUtils.getStackTrace(e2));
            }
        } catch (NoSuchBeanDefinitionException e1) {
            //When interface is declared then find proxy from implemented interface and super class' proxy will be used
            // to get target object and invoke method

            //A class can implement many interfaces. Loop through all to invoke method with first success
            Class<?>[] superClasses = ClassUtils.getAllInterfacesForClass(declaringClass);

            for (int i = 0; i < superClasses.length; i++) {
                Class<?> superClass = superClasses[i];
                bean = applicationContext.getBean(superClass);
                LOGGER.warn("No Application Bean found  for  : " + declaringClass.getName() +
                        " Trying super class Bean with name : " + superClass.getName());

                LOGGER.warn("Super class is Proxy : " + superClass.getName());

                try {
                    bean = getTargetObject(bean);
                    LOGGER.warn("Target class is  : " + bean.getClass().getName());
                } catch (Throwable t) {
                    LOGGER.error("Error invoking methods via Proxy  : " + ExceptionUtils.getStackTrace(t));
                }

                if (null != bean) {
                    LOGGER.warn("Trying to invoke  : " + method.getName() + " on bean  : " + bean.getClass());

                    try {
                        //return on first successful execution of correct match of interface
                        return method.invoke(bean, objects);
                    } catch (IllegalAccessException | IllegalArgumentException e2) {
                        LOGGER.error("Error invoking methods via reflections API  : "
                                + ExceptionUtils.getStackTrace(e2));
                    }
                }

            }
        }
        return null;

    }

    private <T> T getTargetObject(Object proxy) {
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            try {
                return (T) ((Advised) proxy).getTargetSource().getTarget();
            } catch (Exception e) {
                LOGGER.error("Error getting target object from proxy : " + ExceptionUtils.getStackTrace(e));
            }
        }
        return (T) proxy; // expected to be cglib proxy then, which is simply a specialized class
    }


}

