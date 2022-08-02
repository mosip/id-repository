package io.mosip.credentialstore.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.credentialstore.util.RestUtil;
import io.mosip.credentialstore.util.VIDUtil;
import io.mosip.idrepository.core.dto.VidInfoDTO;
import io.mosip.idrepository.core.dto.VidResponseDTO;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class VIDUtilTest {

    @Mock
    private RestUtil restUtil;

    /** The mapper. */
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    VIDUtil vidUtil;

    @Mock
    private Environment environment;

    VidResponseDTO vidResponseDTO=null;

    List<VidInfoDTO> vidInfoDTOS=null;

    ResponseWrapper<VidResponseDTO> vidResponseDTOResponseWrapper=null;

    ResponseWrapper<List<VidInfoDTO>> vidInfoResponseWrapper=null;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        vidResponseDTO=new VidResponseDTO();
        vidResponseDTO.setVid("123456789");
        vidResponseDTO.setUin("4554888654");
        VidInfoDTO vidInfoDTO=new VidInfoDTO();
        vidInfoResponseWrapper=new ResponseWrapper<>();
        vidInfoDTOS=new ArrayList<>();
        vidInfoDTO.setVid("4452541213124");
        vidInfoDTO.setExpiryTimestamp(LocalDateTime.now());
        vidInfoDTO.setVidType("PERPETUAL");
        vidInfoDTOS.add(vidInfoDTO);
        VidInfoDTO vidInfoDTO1=new VidInfoDTO();
        vidInfoDTO1.setVid("4452541213125");
        vidInfoDTO1.setVidType("PERPETUAL");
        vidInfoDTO1.setExpiryTimestamp(LocalDateTime.now());
        vidInfoDTOS.add(vidInfoDTO1);
        vidInfoResponseWrapper.setResponse(vidInfoDTOS);
        vidResponseDTOResponseWrapper=new ResponseWrapper<>();
        vidResponseDTOResponseWrapper.setResponse(vidResponseDTO);
        Mockito.when(environment.getProperty("mosip.credential.service.datetime.pattern"))
                .thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Mockito.when(environment.getProperty("GENERATE_VID"))
                .thenReturn("https://dev.mosip.net/idrepository/v1/vid");
        Mockito.when(environment.getProperty("RETRIEVE_VID"))
                .thenReturn("https://dev.mosip.net/idrepository/v1/vid/uin");
        Mockito.when(objectMapper.readValue(objectMapper.writeValueAsString(vidResponseDTOResponseWrapper.getResponse()), VidResponseDTO.class)).thenReturn(vidResponseDTO);
        Mockito.when(objectMapper.readValue(objectMapper.writeValueAsString(vidInfoResponseWrapper.getResponse()), List.class)).thenReturn(vidInfoDTOS);
        Mockito.when(objectMapper.readValue(objectMapper.writeValueAsString(vidInfoDTOS), VidInfoDTO.class)).thenReturn(vidInfoDTO);
        Mockito.when(restUtil.postApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(vidInfoResponseWrapper);
        Mockito.when(restUtil.getApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(vidInfoResponseWrapper);
    }
    @Test
    public void generateVIDTest() throws Exception {
        VidResponseDTO vidResponseDTO1 = vidUtil.generateVID("4554888654", "PERPETUAL");
        assertEquals(vidResponseDTO1.getVid(), "123456789");

    }

    @Test
    public void getVIDTest() throws Exception {
        VidInfoDTO vidInfoDTO = vidUtil.getVIDData("4554888654", "PERPETUAL","4452541213124");
        assertEquals(vidInfoDTO.getVid(), "4452541213124");

    }

    @SuppressWarnings("unchecked")
    @Test(expected = IdRepoException.class)
    public void testHttpClientException() throws Exception {
        HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "error");
        Exception e=new Exception(httpClientErrorException);
        Mockito.when(restUtil.getApi(Mockito.any(ApiName.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(e);
        vidUtil.getVIDData("4554888654", "PERPETUAL","4452541213124");
    }

}
