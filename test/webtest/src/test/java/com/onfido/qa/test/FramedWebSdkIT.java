package com.onfido.qa.test;

import com.github.javafaker.Faker;
import com.onfido.qa.Region;
import com.onfido.qa.annotation.Browser;
import com.onfido.qa.client.ApiClient;
import com.onfido.qa.configuration.Property;
import com.onfido.qa.webdriver.WebTest;
import com.onfido.qa.webdriver.listener.BrowserStackListener;
import com.onfido.qa.webdriver.listener.ScreenshotListener;
import com.onfido.sdk.Onfido;
import org.openqa.selenium.Point;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;

import java.util.Locale;
import java.util.stream.Collectors;

@Listeners({ScreenshotListener.class, BrowserStackListener.class})
@Browser(acceptInsureCertificates = true)
@SuppressWarnings("AbstractClassExtendsConcreteClass")
public abstract class FramedWebSdkIT extends WebTest {

    private static final Logger log = LoggerFactory.getLogger(FramedWebSdkIT.class);

    protected static Faker faker = new Faker();

    @BeforeSuite(alwaysRun = true)
    public static void beforeSuite() {
        if (!System.getenv().containsKey("CI")) {
            logProperties();
        }
    }

    @SuppressWarnings("CallToSystemGetenv")
    ApiClient api(Region region) {

        var subDomain = region.name().toLowerCase(Locale.ROOT);
        var apiEndpoint = String.format("https://api.%s.%s", subDomain, properties().getProperty("apiHost"));

        return new ApiClient(apiEndpoint, properties().getProperty("apiToken." + subDomain, System.getenv("API_TOKEN_" + region.name())));
    }

    @SuppressWarnings("HardcodedLineSeparator")
    private static void logProperties() {
        log.debug("Properties: {}", Property.properties().entrySet()
                                            .stream()
                                            .map(x -> x.getKey() + "=" + x.getValue())
                                            .collect(Collectors.joining("\n")));
    }

    @DataProvider
    public static Object[][] booleans() {
        return new Object[][]{{true}, {false}};
    }

    @SuppressWarnings("CallToSystemGetenv")
    @Override
    protected DesiredCapabilities extendCapabilities(DesiredCapabilities capabilities) {
        // https://www.browserstack.com/docs/automate/selenium/debugging-options#network-logs

        capabilities.setCapability("browserstack.debug", "true");
        capabilities.setCapability("browserstack.console", "warnings");
        capabilities.setCapability("browserstack.networkLogs", properties().getProperty("networkLogs", "true"));
        capabilities.setCapability("browserstack.wsLocalSupport", "true");
        capabilities.setCapability("acceptSslCerts", "true");

        capabilities.setCapability("project", "framed-web-sdk");
        capabilities.setCapability("build", System.getenv("BUILD"));

        var appId = Property.get("BROWSERSTACK_APP_ID");
        if (appId != null) {
            capabilities.setCapability("app", appId);
        }

        return capabilities;
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod() {

        driver().waitFor.timeout(15);
        driver().driver.manage().window().setPosition(new Point(0, 0));
        driver().maximize();

    }

    protected Onfido onfido() {
        return new Onfido(driver());
    }
}
