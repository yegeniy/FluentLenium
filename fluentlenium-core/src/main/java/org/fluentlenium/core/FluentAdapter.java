/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.fluentlenium.core;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.fluentlenium.core.annotation.AjaxElement;
import org.fluentlenium.core.annotation.Page;
import org.fluentlenium.core.domain.FluentWebElement;
import org.fluentlenium.core.exception.ConstructionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.internal.LocatingElementHandler;

import java.lang.reflect.*;


public class FluentAdapter extends Fluent {

    public FluentAdapter(WebDriver webDriver) {
        super(webDriver);
    }

    public FluentAdapter() {
        super();
    }

    protected void initTest() {
        Class cls = null;
        try {
            cls = Class.forName(this.getClass().getName());
            for (Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(Page.class)) {
                    field.setAccessible(true);
                    Class clsField = field.getType();
                    cls = Class.forName(clsField.getName());
                    Object page = initClass(cls);
                    field.set(this, page);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Class " + (cls != null ? cls.getName() : " null") + "not found", e);
        } catch (IllegalAccessException e) {
            throw new ConstructionException("IllegalAccessException on class " + (cls != null ? cls.getName() : " null"), e);
        }
    }

    public <T extends FluentPage> T createPage(Class<T> classOfPage) {
        return initClass(classOfPage);
    }


    protected <T extends FluentPage> T initClass(Class<T> cls) {
        T page = null;
        try {
            Constructor construct = cls.getDeclaredConstructor();
            construct.setAccessible(true);
            page = (T) construct.newInstance();
            Class parent = Class.forName(Fluent.class.getName());
            Method m = parent.getDeclaredMethod("initFluent", WebDriver.class);
            m.setAccessible(true);
            m.invoke(page, getDriver());

            //init fields with default proxies
            Field[] fields = cls.getDeclaredFields();
            for (Field fieldFromPage : fields) {
                if (!FluentWebElement.class.isAssignableFrom(fieldFromPage.getType()) && !FluentPage.class.isAssignableFrom(fieldFromPage.getType())) {
                    continue;
                }
                if (FluentWebElement.class.isAssignableFrom(fieldFromPage.getType())) {
                    fieldFromPage.setAccessible(true);
                    AjaxElement elem = fieldFromPage.getAnnotation(AjaxElement.class);
                    if (elem == null) {
                        proxyElement(new DefaultElementLocatorFactory(getDriver()), page, fieldFromPage);
                    } else {
                        proxyElement(new AjaxElementLocatorFactory(getDriver(), elem.timeOutInSeconds()), page, fieldFromPage);
                    }
                }else{
                    proxyPage(getDriver(), page, fieldFromPage);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new ConstructionException("Class " + (cls != null ? cls.getName() : " null") + "not found", e);
        } catch (IllegalAccessException e) {
            throw new ConstructionException("IllegalAccessException on class " + (cls != null ? cls.getName() : " null"), e);
        } catch (NoSuchMethodException e) {
            throw new ConstructionException("No constructor found on class " + (cls != null ? cls.getName() : " null"), e);
        } catch (InstantiationException e) {
            throw new ConstructionException("Unable to instantiate " + (cls != null ? cls.getName() : " null"), e);
        } catch (InvocationTargetException e) {
            throw new ConstructionException("Cannot invoke method setDriver on " + (cls != null ? cls.getName() : " null"), e);
        }
        return page;
    }

    private <T extends FluentPage> void proxyPage(WebDriver driver, T page,  Field field) {
         ProxyFactory f = new ProxyFactory();
         f.setSuperclass(field.getType());
         f.setFilter(new MethodFilter() {
             public boolean isHandled(Method m) {
                 // ignore finalize()
                 return !m.getName().equals("finalize");
             }


         });
         Class c = f.createClass();
         MethodHandler mi = new FluentMethodHandler(page,field,driver);

        field.setAccessible(true);

        try {
            T o =(T) c.newInstance();
            ((ProxyObject)o).setHandler(mi);
            field.set(page, o);
        } catch (IllegalAccessException e) {
            throw new ConstructionException("IllegalAccessException on class " + (c != null ? c.getName() : " null"), e);
        } catch (InstantiationException e) {
            throw new ConstructionException("Unable to instantiate " + (c != null ? c.getName() : " null"), e);
        }





    }

    private static void proxyElement(ElementLocatorFactory factory, Object page, Field field) {
        ElementLocator locator = factory.createLocator(field);
        if (locator == null) {
            return;
        }

        InvocationHandler handler = new LocatingElementHandler(locator);
        WebElement proxy = (WebElement) Proxy.newProxyInstance(
                page.getClass().getClassLoader(), new Class[]{WebElement.class}, handler);
        try {
            field.setAccessible(true);
            field.set(page, new FluentWebElement(proxy));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override this method to change the driver
     *
     * @return
     */
    public WebDriver getDefaultDriver() {
        return new FirefoxDriver();
    }


    public static void assertAt(FluentPage fluent) {
        fluent.isAt();
    }

    public void quit() {
        getDriver().quit();
    }

}
