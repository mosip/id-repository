package io.mosip.credential.request.generator.validator;

import io.mosip.idrepository.core.dto.CredentialIssueRequest;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.kernel.core.http.RequestWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class RequestValidatorTest {

    @InjectMocks
    private RequestValidator requestValidator;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void validateRequestGeneratorRequest_ValidRequest_NoException() {
        RequestWrapper<CredentialIssueRequest> requestWrapper = new RequestWrapper<>();
        CredentialIssueRequest credentialIssueRequest = new CredentialIssueRequest();
        credentialIssueRequest.setCredentialType("type");
        credentialIssueRequest.setIssuer("issuer");
        requestWrapper.setRequest(credentialIssueRequest);
        try {
            requestValidator.validateRequestGeneratorRequest(requestWrapper);
            Assert.assertTrue(true);
        } catch (IdRepoAppException e) {
            Assert.fail();
        }
    }

    @Test
    public void validateRequestGeneratorRequest_NullRequest_ExceptionThrown() {
        RequestWrapper<CredentialIssueRequest> requestWrapper = new RequestWrapper<>();
        requestWrapper.setRequest(null);

        try {
            requestValidator.validateRequestGeneratorRequest(requestWrapper);
            Assert.fail();
        } catch (IdRepoAppException e) {
            assertEquals("IDR-IDC-002", e.getErrorCode());

        }
    }

    @Test
    public void validateRequestGeneratorRequest_NullCredentialType_ExceptionThrown() {
        RequestWrapper<CredentialIssueRequest> requestWrapper = new RequestWrapper<>();
        CredentialIssueRequest credentialIssueRequest = new CredentialIssueRequest();
        credentialIssueRequest.setCredentialType(null);
        credentialIssueRequest.setIssuer("issuer");
        requestWrapper.setRequest(credentialIssueRequest);
        try {
            requestValidator.validateRequestGeneratorRequest(requestWrapper);
            Assert.fail();
        } catch (IdRepoAppException e) {
            assertEquals("IDR-IDC-002", e.getErrorCode());
        }
    }

    @Test
    public void validateRequestGeneratorRequest_NullIssuer_ExceptionThrown() {
        RequestWrapper<CredentialIssueRequest> requestWrapper = new RequestWrapper<>();
        CredentialIssueRequest credentialIssueRequest = new CredentialIssueRequest();
        credentialIssueRequest.setCredentialType("type");
        credentialIssueRequest.setIssuer(null);
        requestWrapper.setRequest(credentialIssueRequest);
        try {
            requestValidator.validateRequestGeneratorRequest(requestWrapper);
            Assert.fail();
        } catch (IdRepoAppException e) {
            assertEquals("IDR-IDC-002", e.getErrorCode());
        }
    }

}
