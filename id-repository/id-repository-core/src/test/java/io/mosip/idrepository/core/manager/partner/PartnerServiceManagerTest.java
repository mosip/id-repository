package io.mosip.idrepository.core.manager.partner;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@RunWith(SpringRunner.class)
public class PartnerServiceManagerTest {

    @InjectMocks
    private PartnerServiceManager partnerServiceManager;

    /** The rest helper. */
    @Mock
    private RestHelper restHelper;

    /** The rest builder. */
    @Mock
    private RestRequestBuilder restBuilder;

    /** The dummy check. */
    @Mock
    private DummyPartnerCheckUtil dummyCheck;

    /** The ctx. */
    @Mock
    private ApplicationContext ctx;

    /**
     * This class tests the initTest method
     */
    @Test
    public void initTest(){
        partnerServiceManager.init();

        ReflectionTestUtils.setField(partnerServiceManager, "restHelper", null);
        partnerServiceManager.init();
    }

    /**
     * This class tests the getOLVPartnerIdsException method
     *
     * @throws IdRepoDataValidationException
     * @throws RestServiceException
     */
    @Test
    public void getOLVPartnerIdsTest() throws IdRepoDataValidationException, RestServiceException {
        Map<String, Object> responseWrapperMap = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> partnersObj = new ArrayList<>();
        response.put("partners", partnersObj);
        responseWrapperMap.put("response", response);
        RestRequestDTO request = new RestRequestDTO();
        Mockito.when(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null, Map.class)).thenReturn(request);
        Mockito.when(restHelper.requestSync(request)).thenReturn(responseWrapperMap);
        Mockito.when(dummyCheck.getDummyOLVPartnerId()).thenReturn("test");
        partnerServiceManager.getOLVPartnerIds();
    }

    /**
     * This class tests the getOLVPartnerIdsException method
     *
     * @throws IdRepoDataValidationException
     * @throws RestServiceException
     */
    @Test
    public void getOLVPartnerIdsExceptionTest() throws IdRepoDataValidationException, RestServiceException {
        Map<String, Object> responseWrapperMap = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> partnersObj = new ArrayList<>();
        response.put("partners", partnersObj);
        responseWrapperMap.put("response", response);
        RestRequestDTO request = new RestRequestDTO();
        Mockito.when(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null, Map.class)).thenReturn(request);
        Mockito.doThrow(RestServiceException.class).when(restHelper).requestSync(request);
        Mockito.when(dummyCheck.getDummyOLVPartnerId()).thenReturn("Exceptiontest");
        partnerServiceManager.getOLVPartnerIds();
    }
}