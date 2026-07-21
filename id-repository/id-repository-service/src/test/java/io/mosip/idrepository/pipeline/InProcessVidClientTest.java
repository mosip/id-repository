package io.mosip.idrepository.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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

/**
 * Unit tests for {@link InProcessVidClient}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class InProcessVidClientTest {

	private static final String UIN = "1234567890123456";
	private static final String VID = "6591075869813708";

	@InjectMocks
	private InProcessVidClient client;

	@Mock
	private VidServiceImpl vidService;

	@Test
	public void retrieveVidsByUinDelegatesToVidService() throws IdRepoAppException {
		VidsInfosDTO expected = new VidsInfosDTO();
		when(vidService.retrieveVidsByUin(UIN)).thenReturn(expected);

		VidsInfosDTO actual = client.retrieveVidsByUin(UIN);

		assertSame(expected, actual);
		verify(vidService).retrieveVidsByUin(UIN);
	}

	@Test
	public void getUinByVidDelegatesToVidService() throws IdRepoAppException {
		VidResponseDTO vidResponse = new VidResponseDTO();
		vidResponse.setUin(UIN);
		ResponseWrapper<VidResponseDTO> wrapper = new ResponseWrapper<>();
		wrapper.setResponse(vidResponse);
		when(vidService.retrieveUinByVid(VID)).thenReturn(wrapper);

		assertEquals(UIN, client.getUinByVid(VID));
		verify(vidService).retrieveUinByVid(VID);
	}

	@Test(expected = IdRepoAppException.class)
	public void retrieveVidsByUinPropagatesException() throws IdRepoAppException {
		when(vidService.retrieveVidsByUin(UIN)).thenThrow(new IdRepoAppException("ERR", "lookup failed"));

		client.retrieveVidsByUin(UIN);
	}
}
