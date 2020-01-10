package com.ariescat.hotswap.javasource.spring;

import com.ariescat.hotswap.javasource.ScriptClassLoader;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.ClassUtils;
import sun.font.Script;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author Ariescat
 * @version 2020/1/10 12:26
 */
public class JavaCodeFactory implements ScriptFactory, BeanClassLoaderAware {

    private final String scriptSourceLocator;

    private JavaCodeCustomizer customize;

    private ScriptClassLoader classLoader;

    private Class<?> scriptClass;

    private Class<?> scriptResultClass;

    private CachedResultHolder cachedResult;

    private final Object scriptClassMonitor = new Object();

    private boolean wasModifiedForTypeCheck = false;

    public JavaCodeFactory(String scriptSourceLocator) {
        this.scriptSourceLocator = scriptSourceLocator;
    }

    public JavaCodeFactory(String scriptSourceLocator, JavaCodeCustomizer customize) {
        this.scriptSourceLocator = scriptSourceLocator;
        this.customize = customize;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = AccessController.doPrivileged(
                (PrivilegedAction<ScriptClassLoader>) () -> new ScriptClassLoader(ClassUtils.getDefaultClassLoader())
        );
    }

    private ScriptClassLoader getClassLoader() {
        synchronized (this.scriptClassMonitor) {
            if (this.classLoader == null) {
                this.classLoader = AccessController.doPrivileged(
                        (PrivilegedAction<ScriptClassLoader>) () -> new ScriptClassLoader(ClassUtils.getDefaultClassLoader())
                );
            }
            return this.classLoader;
        }
    }

    @Override
    public String getScriptSourceLocator() {
        return this.scriptSourceLocator;
    }

    @Override
    public Class<?>[] getScriptInterfaces() {
        return null;
    }

    @Override
    public boolean requiresConfigInterface() {
        return false;
    }

    @Override
    public Object getScriptedObject(ScriptSource scriptSource, Class<?>... actualInterfaces) throws IOException, ScriptCompilationException {
        synchronized (this.scriptClassMonitor) {
            try {
                Class<?> scriptClassToExecute;
                this.wasModifiedForTypeCheck = false;

                if (this.cachedResult != null) {
                    Object result = this.cachedResult.object;
                    this.cachedResult = null;
                    return result;
                }

                if (this.scriptClass == null || scriptSource.isModified()) {
                    // New script content...
                    // TODO 1.这里如果不每次new一个新的ClassLoader的话，就不会重新加载，但是如果每次都new，那ClassLoader会不会很多？
                    // TODO 2.scriptSource.suggestedClassName() 获取的不是全限定类名，就无法加载
                    this.scriptClass = getClassLoader().parseClass(
                            scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

                    if (Script.class.isAssignableFrom(this.scriptClass)) {
                        // A Java script, probably creating an instance: let's execute it.
                        Object result = executeScript(scriptSource, this.scriptClass);
                        this.scriptResultClass = (result != null ? result.getClass() : null);
                        return result;
                    } else {
                        this.scriptResultClass = this.scriptClass;
                    }
                }
                scriptClassToExecute = this.scriptClass;

                // Process re-execution outside of the synchronized block.
                return executeScript(scriptSource, scriptClassToExecute);
            } catch (Exception ex) {
                this.scriptClass = null;
                this.scriptResultClass = null;
                throw new ScriptCompilationException(scriptSource, ex);
            }
        }
    }

    @Override
    public Class<?> getScriptedObjectType(ScriptSource scriptSource) throws ScriptCompilationException {
        synchronized (this.scriptClassMonitor) {
            try {
                if (this.scriptClass == null || scriptSource.isModified()) {
                    // New script content...
                    this.wasModifiedForTypeCheck = true;
                    this.scriptClass = getClassLoader().parseClass(
                            scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

                    if (Script.class.isAssignableFrom(this.scriptClass)) {
                        // A Java script, probably creating an instance: let's execute it.
                        Object result = executeScript(scriptSource, this.scriptClass);
                        this.scriptResultClass = (result != null ? result.getClass() : null);
                        this.cachedResult = new CachedResultHolder(result);
                    } else {
                        this.scriptResultClass = this.scriptClass;
                    }
                }
                return this.scriptResultClass;
            } catch (Exception ex) {
                this.scriptClass = null;
                this.scriptResultClass = null;
                this.cachedResult = null;
                throw new ScriptCompilationException(scriptSource, ex);
            }
        }
    }

    @Override
    public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
        synchronized (this.scriptClassMonitor) {
            return (scriptSource.isModified() || this.wasModifiedForTypeCheck);
        }
    }

    private Object executeScript(ScriptSource scriptSource, Class<?> scriptClass) throws ScriptCompilationException {
        try {
            Object goo = scriptClass.newInstance();

            if (this.customize != null) {
                // Allow other customization.
                this.customize.customize(goo);
            }

            // An instance of the scripted class: let's return it as-is.
            return goo;

        } catch (InstantiationException ex) {
            throw new ScriptCompilationException(
                    scriptSource, "Unable to instantiate Java script class: " + scriptClass.getName(), ex);
        } catch (IllegalAccessException ex) {
            throw new ScriptCompilationException(
                    scriptSource, "Could not access Java script constructor: " + scriptClass.getName(), ex);
        }
    }

    @Override
    public String toString() {
        return "JavaSourceScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
    }

    /**
     * Wrapper that holds a temporarily cached result object.
     */
    private static class CachedResultHolder {

        final Object object;

        CachedResultHolder(Object object) {
            this.object = object;
        }
    }
}
