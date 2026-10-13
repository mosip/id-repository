package io.mosip.idrepository.pipeline;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.idrepository.core.dto.VidResponseDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.vid.service.impl.VidServiceImpl;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;

/**
 * Unit tests for {@link InProcessVidClient}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class InProcessVidClientTest {

	private static final String UIN = "1234567890123456";
	private static final String VID = "6591075869813708";
	private static final String GARBAGE_ID = "$123ds";

	@InjectMocks
	private InProcessVidClient client;

	@Mock
	private VidServiceImpl vidService;

	@Mock
	private VidValidator<String> vidValidator;

	@Mock
	private UinValidator<String> uinValidator;

	@Test
	public void retrieveVidsByUinDelegatesToVidService() throws IdRepoAppException {
		VidsInfosDTO expected = new VidsInfosDTO();
		when(uinValidator.validateId(UIN)).thenReturn(true);
		when(vidService.retrieveVidsByUin(UIN)).thenReturn(expected);

		VidsInfosDTO actual = client.retrieveVidsByUin(UIN);

		assertSame(expected, actual);
		verify(uinValidator).validateId(UIN);
		verify(vidService).retrieveVidsByUin(UIN);
	}

	@Test
	public void retrieveVidsByUinRejectsInvalidUinFormat() throws IdRepoAppException {
		when(uinValidator.validateId(GARBAGE_ID)).thenThrow(new InvalidIDException("KER-IDV-202", "Invalid UIN"));

		try {
			client.retrieveVidsByUin(GARBAGE_ID);
			fail("expected IdRepoAppException");
		} catch (IdRepoAppException e) {
			assertEquals(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "uin"), e.getErrorText());
		}
		verify(vidService, never()).retrieveVidsByUin(any());
	}

	@Test
	public void getUinByVidDelegatesToVidService() throws IdRepoAppException {
		VidResponseDTO vidResponse = new VidResponseDTO();
		vidResponse.setUin(UIN);
		ResponseWrapper<VidResponseDTO> wrapper = new ResponseWrapper<>();
		wrapper.setResponse(vidResponse);
		when(vidValidator.validateId(VID)).thenReturn(true);
		when(vidService.retrieveUinByVid(VID)).thenReturn(wrapper);

		assertEquals(UIN, client.getUinByVid(VID));
		verify(vidValidator).validateId(VID);
		verify(vidService).retrieveUinByVid(VID);
	}

	@Test
	public void getUinByVidRejectsInvalidVidFormat() throws IdRepoAppException {
		when(vidValidator.validateId(GARBAGE_ID)).thenThrow(new InvalidIDException("KER-IDV-004", "Invalid VID"));

		try {
			client.getUinByVid(GARBAGE_ID);
			fail("expected IdRepoAppException");
		} catch (IdRepoAppException e) {
			assertEquals(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "vid"), e.getErrorText());
		}
		verify(vidService, never()).retrieveUinByVid(any());
	}

	@Test(expected = IdRepoAppException.class)
	public void retrieveVidsByUinPropagatesException() throws IdRepoAppException {
		when(uinValidator.validateId(UIN)).thenReturn(true);
		when(vidService.retrieveVidsByUin(UIN)).thenThrow(new IdRepoAppException("ERR", "lookup failed"));

		client.retrieveVidsByUin(UIN);
	}
}
