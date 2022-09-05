package com.onfido.qa.test;

import com.onfido.qa.client.dto.Applicant;
import com.onfido.qa.client.dto.ApplicantRequest;
import com.onfido.qa.client.dto.WorkflowLink;
import com.onfido.qa.client.dto.WorkflowLinkRequest;
import com.onfido.qa.framedsdk.FramedPage;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WorkflowLinksIT extends FramedWebSdkIT {

    private static WorkflowLink workflowLink = null;

    @DataProvider
    public static Object[][] regions() {
        return new Object[][]{
                {"EU"}, {"NA"}, {"CA"}
        };
    }

    @BeforeClass
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public void beforeClass() {
        var name = faker.name();
        Applicant applicant = api().createApplicant(new ApplicantRequest(name.firstName(), name.lastName()));
        workflowLink = api().createWorkflowLink(new WorkflowLinkRequest(applicant.id, workflowId()));
    }

    @Test(description = "NoRegionLeadsToError")
    public void testNoRegionLeadsToError() {

        assertThatThrownBy(() -> {
            onfido()
                    .withWorkflowLinkId(workflowLink.id)
                    .init();
        })
                .isInstanceOf(JavascriptException.class)
                .hasMessageContaining("'workflowLinkId' required to provide the 'region' as well");
    }

    @Test(description = "happy path of workflowLinks", dataProvider = "regions")
    public void testHappyPathOfWorkflowLinks(String region) {

        var framedPage = onfido()
                .withWorkflowLinkId(workflowLink.id)
                .withRegion(region)
                .init();

        var src = framedPage.frameSrc();

        assertThat(src).contains(String.format("sdk.%s.onfido.app", region.toLowerCase(Locale.ROOT)));
        assertThat(src).contains(workflowLink.id.toString());

    }

    protected UUID workflowId() {
        return UUID.fromString(properties().getProperty("workflowId"));
    }

    @Test(description = "error is invoked, if workflow link id doesn't exist")
    public void testErrorIsInvokedIfWorkflowLinkIdNotExist() {
        var framedPage = onfido()
                .withWorkflowLinkId(UUID.randomUUID())
                .withRegion("EU")
                .withErrorHandler(FramedPage.ERROR_HANDLER)
                .init();

        driver().waitFor.waitFor((o) -> {
                return framedPage.getError().isPresent();
            }
        );

        var error = framedPage.getError();
        assertThat(error).isPresent();
        assertThat(error.get().message).contains("Session or Workflow not found");

    }
}
