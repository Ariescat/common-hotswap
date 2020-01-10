package com.ariescat.hotswap.javasource.spring;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import static org.springframework.scripting.support.ScriptFactoryPostProcessor.REFRESH_CHECK_DELAY_ATTRIBUTE;

/**
 * @author Ariescat
 * @version 2020/1/10 13:23
 */
public class JavaCodeScriptRegister implements ApplicationContextAware {

    private final static Logger log = LoggerFactory.getLogger(JavaCodeScriptRegister.class);

    /**
     * 脚本目录
     */
    private String directory;

    /**
     * 检测脚本是否修改时间间隔
     */
    private int refreshCheckDelay = 5000;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        // 只有这个对象才能注册bean到spring容器
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();

        URL url = Thread.currentThread().getContextClassLoader().getResource(directory);
        if (url == null) {
            log.error("directory is null, script register exit !");
            return;
        }
        File scriptDir = new File(url.getFile());
        if (!scriptDir.exists()) {
            return;
        }
        Collection<File> files = FileUtils.listFiles(scriptDir, new String[]{"java"}, true);
        files.forEach(file -> {
            GenericBeanDefinition bd = new GenericBeanDefinition();
            bd.setBeanClass(JavaCodeFactory.class);
            // 刷新时间
            bd.setAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE, refreshCheckDelay);
            String path = file.getPath();
            String scriptLocator = path.substring(path.indexOf(directory));
            bd.getConstructorArgumentValues().addIndexedArgumentValue(0, scriptLocator);
            // 注册到spring容器
            beanFactory.registerBeanDefinition(file.getName().replace(".java", ""), bd);

            log.info("Register Java Source Script:{}", scriptLocator);
        });
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setRefreshCheckDelay(int refreshCheckDelay) {
        this.refreshCheckDelay = refreshCheckDelay;
    }
}
