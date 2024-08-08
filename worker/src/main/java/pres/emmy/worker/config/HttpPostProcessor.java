package pres.emmy.worker.config;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpPostProcessor implements ResourceLoaderAware, BeanDefinitionRegistryPostProcessor {

    private ResourceLoader resourceLoader;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("classpath*:pres/emmy/worker/remote/service/*.class");
            CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory(resourceLoader);

            for (Resource resource : resources) {
                MetadataReader metadataReader = factory.getMetadataReader(resource);
                if (metadataReader.getAnnotationMetadata().hasAnnotation(HttpExchange.class.getName())) {
                    beanFactory.registerSingleton(metadataReader.getClassMetadata().getClassName(),
                            beanFactory.getBean(HttpServiceProxyFactory.class).createClient(
                                    Class.forName(metadataReader.getClassMetadata().getClassName())));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
