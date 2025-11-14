package io.mosip.idrepository.identity.helper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils2;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@RunWith(SpringRunner.class)
public class VidDraftHelperTest {

    @InjectMocks
    private VidDraftHelper vidDraftHelper;

    @Mock
    protected EnvUtil env;

    @Mock
    private RestRequestBuilder restBuilder;

    @Mock
    private RestHelper restHelper;


    /**
     * This class tests the generateDraftVid method
     * @throws IdRepoAppException
     */
    @Test(expected = Exception.class)
    public void generateDraftVidExceptionTest() throws IdRepoAppException {
        String uin = "11223344";
        EnvUtil.setIsDraftVidTypePresent(true);
        vidDraftHelper.generateDraftVid(uin);
    }

    /**
     * This class tests the generateDraftVid method
     * @throws IdRepoAppException
     */
    @Test
    public void generateDraftVidTest() throws IdRepoAppException {
        String uin = "1122";
        VidRequestDTO vidCreationRequest = new VidRequestDTO();
        vidCreationRequest.setUin(uin);
        vidCreationRequest.setVidType(EnvUtil.getDraftVidType());
        RequestWrapper<VidRequestDTO> request = new RequestWrapper<>();
        request.setId(EnvUtil.getCreateVidId());
        request.setVersion(EnvUtil.getVidAppVersion());
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(vidCreationRequest);

        Map<String, String> response = new HashMap<>();
        String vid = "1122";
        response.put("VID", vid);
        ResponseWrapper<Map<String, String>> vidResponse = new ResponseWrapper<>();
        vidResponse.setResponse(response);

        Mockito.when(restBuilder.buildRequest(RestServicesConstants.VID_DRAFT_GENERATOR_SERVICE,
                request, ResponseWrapper.class)).thenReturn(null);
        Mockito.when(restHelper.requestSync(null)).thenReturn(vidResponse);
        EnvUtil.setIsDraftVidTypePresent(true);
        Assert.assertEquals(vid, vidDraftHelper.generateDraftVid(uin));

        EnvUtil.setIsDraftVidTypePresent(false);
        Assert.assertEquals(null, vidDraftHelper.generateDraftVid(uin));
    }

    /**
     * This class tests the activeDraftVid method
     * @throws IdRepoAppException
     */
    @Test(expected = Exception.class)
    public void activeDraftVidExceptionTest() throws IdRepoAppException {
        String uin = "1122";

        VidRequestDTO vidUpdationRequest = new VidRequestDTO();
        vidUpdationRequest.setVidStatus(EnvUtil.getVidActiveStatus());
        RequestWrapper<VidRequestDTO> request = new RequestWrapper<>();
        request.setId(EnvUtil.getUpdatedVidId());
        request.setVersion(EnvUtil.getVidAppVersion());
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(vidUpdationRequest);

        System.out.println("request2= "+request);
        RestRequestDTO restRequest = new RestRequestDTO();
        restRequest.setUri("{vid}");

        Mockito.when(restBuilder.buildRequest(RestServicesConstants.VID_UPDATE_SERVICE,
                request, ResponseWrapper.class)).thenReturn(restRequest);
        vidDraftHelper.activateDraftVid(uin);
    }

    /**
     * This class tests the activeDraftVid method
     * @throws IdRepoAppException
     */
    @Test
    public void activeDraftVidTest() throws IdRepoAppException {
        String uin = "1122";
        RestRequestDTO restRequest = new RestRequestDTO();
        restRequest.setUri("{vid}");
        Mockito.when(restBuilder.buildRequest(Mockito.any(),
                Mockito.any(), Mockito.any())).thenReturn(restRequest);
        vidDraftHelper.activateDraftVid(uin);

        uin = null;
        vidDraftHelper.activateDraftVid(uin);
    }
}
