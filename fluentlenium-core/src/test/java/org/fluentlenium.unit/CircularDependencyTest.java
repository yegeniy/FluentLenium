package org.fluentlenium.unit;

import org.fluentlenium.adapter.FluentTest;
import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.annotation.Page;
import org.fluentlenium.core.domain.FluentWebElement;
import org.fluentlenium.integration.localtest.LocalFluentCase;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: rigabertm
 * Date: 5/19/12
 * Time: 9:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class CircularDependencyTest extends FluentTest {

    @Page
    PageAccueil pageAccueil;

    @Test
    public void check_circular_dependency() {
       pageAccueil.goToNexPage().goToNexPage().goToNexPage().goToNexPage3().goToNexPage().goToNexPage();
    }

}
 class  PageAccueil extends FluentPage {
    FluentWebElement linkToPage2;

    @Page
    PageAccueil2 pageAccueil2;

    @Override
    public String getUrl() {
        System.out.println(this);
        return LocalFluentCase.DEFAULT_URL;
    }

    @Override
    public void isAt() {
        assertThat($("title").first().getText()).contains("Selenium");
    }

    public PageAccueil2 goToNexPage() {
       return pageAccueil2;
    }
    public void click(){
          linkToPage2.click();
      }
}

 class PageAccueil2 extends FluentPage {

    FluentWebElement linkToPage2;

    @Page
    PageAccueil pageAccueil;

    @Page
    PageAccueil3 pageAccueil3;

    @Override
    public String getUrl() {
        return LocalFluentCase.DEFAULT_URL;
    }

    @Override
    public void isAt() {
        assertThat($("title").first().getText()).contains("Selenium");
    }

    public PageAccueil goToNexPage() {
        return pageAccueil;
    }
    public PageAccueil3 goToNexPage3() {
           return pageAccueil3;
       }

    public void click(){
        linkToPage2.click();
    }
}

class PageAccueil3 extends FluentPage {

      FluentWebElement linkToPage2;

    @Page
     PageAccueil pageAccueil;

    @Override
    public  String getUrl() {
        System.out.println(this);
        return LocalFluentCase.DEFAULT_URL;
    }

    @Override
    public  void isAt() {
        assertThat($("title").first().getText()).contains("Selenium");
    }

    public PageAccueil goToNexPage() {
        return pageAccueil;
    }

    public  void click(){
        linkToPage2.click();
    }
}