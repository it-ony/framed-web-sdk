package com.onfido.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onfido.qa.Region;
import com.onfido.qa.configuration.Property;
import com.onfido.qa.framedsdk.FramedPage;
import com.onfido.qa.webdriver.Driver;
import com.onfido.qa.webdriver.common.Component;
import com.onfido.qa.webdriver.common.Page;
import com.onfido.qa.webdriver.driver.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Onfido {

    private static final Logger log = LoggerFactory.getLogger(Onfido.class);
    private final static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
    }

    private final Map<String, Object> parameters = new HashMap<>();
    private final Driver driver;
    private String errorHandler;

    public Onfido(Driver driver) {
        this.driver = driver;
    }

    public Onfido withToken(String token) {
        return put("token", token);
    }

    public Onfido withWorkflowLinkId(UUID id) {
        return put("workflowLinkId", id);
    }

    public Onfido withRegion(Region region) {
        return put("region", region.name());
    }

    public Onfido withErrorHandler(String raw) {
        errorHandler = raw;
        return this;
    }

    public Onfido withOnComplete(Raw raw) {
        return put("onComplete", raw);
    }


    private Onfido put(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    public Onfido withLanguage(String language) {
        return put("language", language);
    }


    public FramedPage init() {

        // navigate to the base url
        driver.get(Property.get("baseUrl"));

        // and wait for the page to be ready
        driver.waitFor(ExpectedConditions.pageReady());
        // then call the onfido.init method with the parameters

        var parameters = serializedParameters();
        log.info("Initializing web sdk with: Onfido.init({})", parameters);

        var script = "window.onfido = Onfido.init(" + parameters + ")";
        if (errorHandler != null) {
            script += String.format("; window.onfido.addEventListener(\"error\", %s);", errorHandler);
        }

        driver.executeScript(script);

        return new FramedPage(driver);

    }

    public <T extends Page> T init(Class<T> pageClass) {
        init();

        return Component.createComponent(driver, pageClass);
    }

    private String serializedParameters() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
