package objective.taskboard.testUtils;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Disables default @Autowired(required=true), that obliges that all dependencies be satisfied before running a test
 * Source: https://stackoverflow.com/a/31033598
 */
public class OptionalAutowiredDependenciesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Register the AutowiredAnnotationBeanPostProcessor while initalizing
        // the context so we get there before any @Autowire resolution happens
        // We set the "requiredParameterValue" so that @Autowire fields are not
        // required to be resolved. Very useful for a test context
        GenericApplicationContext ctx = (GenericApplicationContext) applicationContext;
        ctx.registerBeanDefinition(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
                BeanDefinitionBuilder
                        .rootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class)
                        .addPropertyValue("requiredParameterValue", false)
                        .getBeanDefinition());

    }
}