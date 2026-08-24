package io.mosip.idrepository.identity.test.controller;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.DraftResponseDto;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.spi.IdRepoDraftService;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.controller.IdRepoDraftController;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.regex.Pattern;

import io.mosip.idrepository.identity.dto.CreateDraftV2RequestDto;
import io.mosip.idrepository.identity.dto.UpdateDraftUinDataRequestDto;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class IdRepoDraftControllerTest {

	@InjectMocks
	private IdRepoDraftController controller;

	@Mock
	private IdRequestValidator validator;

	@Mock
	private IdRepoDraftService<IdRequestDTO, IdResponseDTO> draftService;

	@Mock
	private AuditHelper auditHelper;

	@Mock
	private Environment environment;

	private Errors errors;

	@Before
	public void init() {
		errors = new BeanPropertyBindingResult(new IdRequestDTO(), "idRequestDto");
		ReflectionTestUtils.setField(controller, "ridCompiledPattern", Pattern.compile("\\d*"));
	}

	@Test
	public void testInitBinder() {
		WebDataBinder binderMock = mock(WebDataBinder.class);
		IdRequestDTO target = new IdRequestDTO();
		when(binderMock.getTarget()).thenReturn(target);
		when(validator.supports(target.getClass())).thenReturn(true);
		controller.initBinder(binderMock);
		ArgumentCaptor<IdRequestValidator> argCapture = ArgumentCaptor.forClass(IdRequestValidator.class);
		verify(binderMock).addValidators(argCapture.capture());
		assertEquals(validator, argCapture.getValue());
	}

	@Test
	public void testCreateDraft() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.createDraft(any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.createDraft("", null);
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testUpdateDraft() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.updateDraft(any(), any())).thenReturn(responseDTO);
		IdRequestDTO idRequest = new IdRequestDTO();
		RequestDTO request = new RequestDTO();
		idRequest.setRequest(request);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.updateDraft("", idRequest, errors);
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testUpdateDraftException() throws IdRepoAppException {
		when(draftService.updateDraft(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			IdRequestDTO idRequest = new IdRequestDTO();
			RequestDTO request = new RequestDTO();
			idRequest.setRequest(request);
			controller.updateDraft("", idRequest, errors);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testPublishDraft() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.publishDraft(any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.publishDraft("");
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testpublishDraftException() throws IdRepoAppException {
		when(draftService.publishDraft(any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.publishDraft("");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testDiscardDraft() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.discardDraft(any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.discardDraft("");
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testDiscardDraftException() throws IdRepoAppException {
		when(draftService.discardDraft(any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.discardDraft("");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testDiscardDraftV2() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.discardDraftV2(any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.discardDraftV2("");
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testDiscardDraftV2Exception() throws IdRepoAppException {
		when(draftService.discardDraftV2(any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.discardDraftV2("");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testHasDraft() throws IdRepoAppException {
		when(draftService.hasDraft(any())).thenReturn(Boolean.TRUE);
		ResponseEntity<Void> createDraftResponse = controller.hasDraft("");
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
	}

	@Test
	public void testHasDraftNoDraft() throws IdRepoAppException {
		when(draftService.hasDraft(any())).thenReturn(Boolean.FALSE);
		ResponseEntity<Void> createDraftResponse = controller.hasDraft("");
		assertEquals(HttpStatus.NO_CONTENT, createDraftResponse.getStatusCode());
	}

	@Test
	public void testHasDraftException() throws IdRepoAppException {
		when(draftService.hasDraft(any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.hasDraft("");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetDraftWithoutExtractionFormats() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.getDraft(any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.getDraft("", null, null, null);
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testGetDraftWithExtractionFormats() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.getDraft(any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.getDraft("", "format", "format", "format");
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testGetDraftException() throws IdRepoAppException {
		when(draftService.getDraft(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.getDraft("", null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testExtractBiometricsWithoutExtractionFormats() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.extractBiometrics(any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.extractBiometrics("", null, null, null);
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testExtractBiometricsWithExtractionFormats() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.extractBiometrics(any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> createDraftResponse = controller.extractBiometrics("", "format", "format", "format");
		assertEquals(HttpStatus.OK, createDraftResponse.getStatusCode());
		assertEquals(responseDTO, createDraftResponse.getBody());
	}

	@Test
	public void testExtractBiometricsException() throws IdRepoAppException {
		when(draftService.extractBiometrics(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.extractBiometrics("", null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateDraftV2() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.createDraftV2(any(), any(), Mockito.anyBoolean())).thenReturn(responseDTO);
		CreateDraftV2RequestDto request = new CreateDraftV2RequestDto();
		ResponseEntity<IdResponseDTO> response = controller.createDraftV2("10001100770000320200720094735", request);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(responseDTO, response.getBody());
	}

	@Test
	public void testCreateDraftV2Exception() throws IdRepoAppException {
		when(draftService.createDraftV2(any(), any(), Mockito.anyBoolean()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			CreateDraftV2RequestDto request = new CreateDraftV2RequestDto();
			controller.createDraftV2("10001100770000320200720094735", request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateDraft_InvalidRidPattern() throws IdRepoAppException {
		try {
			controller.createDraft("REG-ABC-123", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "Registration Id"), e.getErrorText());
		}
	}

	@Test
	public void testCreateDraftV2_InvalidRidPattern() throws IdRepoAppException {
		try {
			CreateDraftV2RequestDto request = new CreateDraftV2RequestDto();
			controller.createDraftV2("REG-ABC-123", request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "Registration Id"), e.getErrorText());
		}
	}

	@Test
	public void testUpdateDraftUinData() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.updateDraftUinData(any(), any())).thenReturn(responseDTO);
		UpdateDraftUinDataRequestDto request = new UpdateDraftUinDataRequestDto();
		request.setUin("274390482564");
		ResponseEntity<IdResponseDTO> response = controller.updateDraftUinData("10001100770000320200720094735", request);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(responseDTO, response.getBody());
	}

	@Test
	public void testUpdateDraftUinDataException() throws IdRepoAppException {
		when(draftService.updateDraftUinData(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			UpdateDraftUinDataRequestDto request = new UpdateDraftUinDataRequestDto();
			request.setUin("274390482564");
			controller.updateDraftUinData("10001100770000320200720094735", request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetDraftV2() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.getDraftV2(any(), any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> response = controller.getDraftV2(
				"10001100770000320200720094735", null, null, null, "demographics");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(responseDTO, response.getBody());
	}

	@Test
	public void testGetDraftV2Exception() throws IdRepoAppException {
		when(draftService.getDraftV2(any(), any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.getDraftV2("10001100770000320200720094735", null, null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetDraftUinException() throws IdRepoAppException {
		when(environment.getProperty(Mockito.anyString())).thenReturn("id");
		when(validator.validateUin(Mockito.anyString()))
				.thenReturn(false);
		ResponseWrapper<DraftResponseDto> responseWrapper = controller.getDraftUIN("123").getBody();
		assert responseWrapper != null;
		assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), responseWrapper.getErrors().get(0).getErrorCode());
		assertEquals(String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), IdType.UIN), responseWrapper.getErrors().get(0).getMessage());
	}

	@Test
	public void testGetDraftUinSuccess() throws IdRepoAppException {
		when(environment.getProperty(Mockito.anyString())).thenReturn("id");
		when(validator.validateUin(Mockito.anyString()))
				.thenReturn(true);
		when(draftService.getDraftUin(Mockito.anyString())).thenReturn(new DraftResponseDto());
		ResponseEntity<ResponseWrapper<DraftResponseDto>> response = controller.getDraftUIN("123");
		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@Test
	public void testGetDraftUinSuccessIdRepoAppException() throws IdRepoAppException {
		when(environment.getProperty(Mockito.anyString())).thenReturn("id");
		when(validator.validateUin(Mockito.anyString()))
				.thenReturn(true);
		when(draftService.getDraftUin(Mockito.anyString())).thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.getDraftUIN("123");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateDraftV2() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.updateDraftV2(any(), any())).thenReturn(responseDTO);
		IdRequestDTO idRequest = new IdRequestDTO();
		idRequest.setRequest(new RequestDTO());
		ResponseEntity<IdResponseDTO> response = controller.updateDraftV2("10001100770000320200720094735", idRequest, errors);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(responseDTO, response.getBody());
	}

	@Test
	public void testUpdateDraftV2Exception() throws IdRepoAppException {
		when(draftService.updateDraftV2(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			IdRequestDTO idRequest = new IdRequestDTO();
			idRequest.setRequest(new RequestDTO());
			controller.updateDraftV2("10001100770000320200720094735", idRequest, errors);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testPublishDraftV2() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.publishDraftV2(any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> response = controller.publishDraftV2("10001100770000320200720094735");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(responseDTO, response.getBody());
	}

	@Test
	public void testPublishDraftV2Exception() throws IdRepoAppException {
		when(draftService.publishDraftV2(any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.publishDraftV2("10001100770000320200720094735");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testExtractBiometricsV2WithoutExtractionFormats() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.extractBiometricsV2(any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> response = controller.extractBiometricsV2("10001100770000320200720094735", null, null, null);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(responseDTO, response.getBody());
	}

	@Test
	public void testExtractBiometricsV2WithExtractionFormats() throws IdRepoAppException {
		IdResponseDTO responseDTO = new IdResponseDTO();
		when(draftService.extractBiometricsV2(any(), any())).thenReturn(responseDTO);
		ResponseEntity<IdResponseDTO> response = controller.extractBiometricsV2("10001100770000320200720094735", "format", "format", "format");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(responseDTO, response.getBody());
	}

	@Test
	public void testExtractBiometricsV2Exception() throws IdRepoAppException {
		when(draftService.extractBiometricsV2(any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			controller.extractBiometricsV2("10001100770000320200720094735", null, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

}
