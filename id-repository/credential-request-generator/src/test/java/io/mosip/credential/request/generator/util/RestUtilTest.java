package io.mosip.credential.request.generator.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.idrepository.core.util.EnvUtil;


@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@RunWith(SpringRunner.class)
public class RestUtilTest {

    @InjectMocks
    private RestUtil restUtil = new RestUtil();

    @Mock
    private EnvUtil environment;

    @Before
    public void Before(){
        MockitoAnnotations.initMocks(this);
    }


    /**
     * This class tests the  postApi method
     * @throws Exception
     */
    @Test
    public void postApiTest() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        String queryParamName = "name"; String queryParamValue ="value";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object requestType = new Object(); Class<?> responseClass = null;
        restUtil.postApi(apiName, pathsegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);
    }

    /**
     * This class tests the  postApi method
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void postApiExceptionTest() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        pathsegments.add("");
        String queryParamName = ""; String queryParamValue ="value";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object requestType = new Object(); Class<?> responseClass = null;
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.postApi(apiName, pathsegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);

        queryParamName = "";
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        RestUtil restUtilSpy = Mockito.spy(restUtil);

        RestTemplate restTemplate = new RestTemplate();
        Mockito.doReturn(restTemplate).when(restUtilSpy).getRestTemplate();
        restUtilSpy.postApi(apiName, pathsegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);
    }

    /**
     * This class tests the  postApi method
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void postApiExceptionTest1() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        pathsegments.add("");
        String queryParamName = "name"; String queryParamValue ="value";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object requestType = new Object(); Class<?> responseClass = null;
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.postApi(apiName, pathsegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);
    }

    /**
     * This class tests the postApi method
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void postApiExceptionTest2() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        String queryParamName = ""; String queryParamValue ="value";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object requestType = new Object(); Class<?> responseClass = null;
        pathsegments.add("segment");
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.postApi(apiName, pathsegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);

        queryParamName = null;
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.postApi(apiName, pathsegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);
    }

    /**
     * This class tests the getApi method
     * @throws Exception
     */
    @Test
    public void getApiTest() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        String queryParamName = "name"; String queryParamValue ="value";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object responseType = new Object();
        restUtil.getApi(apiName, pathsegments, queryParamName, queryParamValue, null);
    }

    /**
     * This class tests the getApi method
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void getApiExceptionTest1() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        pathsegments.add("");
        String queryParamName = ""; String queryParamValue ="value";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.getApi(apiName, pathsegments, queryParamName, queryParamValue, null);

        queryParamName = "";
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        RestUtil restUtilSpy = Mockito.spy(restUtil);
        RestTemplate restTemplate = new RestTemplate();
        Mockito.doReturn(restTemplate).when(restUtilSpy).getRestTemplate();
        restUtilSpy.getApi(apiName, pathsegments, queryParamName, queryParamValue, null);
    }

    /**
     * This class tests the getApi method
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void getApiExceptionTest2() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        pathsegments.add("");
        String queryParamName = "name"; String queryParamValue ="value";
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.getApi(apiName, pathsegments, queryParamName, queryParamValue, null);
    }

    /**
     * This class tests the getApi method
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void getApiExceptionTest3() throws Exception {
        ApiName apiName = ApiName.CRDENTIALSERVICE;
        List<String> pathsegments = new ArrayList<>();
        pathsegments.add("");
        String queryParamName = ""; String queryParamValue ="value";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object requestType = new Object(); Class<?> responseClass = null;
        pathsegments.add("segment");
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.getApi(apiName, pathsegments, queryParamName, queryParamValue, null);

        queryParamName = null;
        Mockito.when(environment.getProperty(apiName.name())).thenReturn("aa");
        restUtil.getApi(apiName, pathsegments, queryParamName, queryParamValue, null);
    }

    /**
     * This class tests the setRequestHeader method
     * @throws IOException
     */
    @Test
    public void setRequestHeaderTest1() throws IOException {
        Map<String, String> headers = new HashMap<>();
        Map<String, String > contentType = new HashMap<>();
        contentType.put("0", "1");
        headers.put("Content-Type", "1");
        HttpEntity<Object> requestType = new HttpEntity<Object>(headers);
        System.out.println("here= "+requestType.getHeaders());
        MediaType mediaType = MediaType.ALL;
        HttpPost post = new HttpPost();
        Mockito.when(environment.getProperty("KEYBASEDTOKENAPI")).thenReturn("1212");
        RestUtil restUtilSpy = Mockito.spy(restUtil);
        Mockito.doReturn("1122").when(restUtilSpy).getToken();
        ReflectionTestUtils.invokeMethod(restUtilSpy, "setRequestHeader", requestType, mediaType);
        //ClassCastException
        ReflectionTestUtils.invokeMethod(restUtilSpy, "setRequestHeader", new Object(), mediaType);
        //null
        ReflectionTestUtils.invokeMethod(restUtilSpy, "setRequestHeader", null, null);
    }

    /**
     * This class tests the setRequestHeader method
     * @throws IOException
     */
    @Test
    public void setRequestHeaderTest2() throws IOException {
        Map<String, String> headers = new HashMap<>();
        Map<String, String > contentType = new HashMap<>();
        contentType.put("0", "1");
        headers.put("Content-Type", "1");
        Object requestType = new Object();
        MediaType mediaType = MediaType.ALL;
        RestUtil restUtilSpy = Mockito.spy(restUtil);
        Mockito.doReturn("1122").when(restUtilSpy).getToken();
        ReflectionTestUtils.invokeMethod(restUtilSpy, "setRequestHeader", requestType, mediaType);
    }

    /**
     * This class tests the getToken method
     * @throws IOException
     */
    @Ignore
    @Test
    public void getTokenTest() throws IOException {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpPost httpPost = Mockito.mock(HttpPost.class);
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpClient.execute(null)).thenReturn(httpResponse);
        Mockito.when(environment.getProperty("KEYBASEDTOKENAPI")).thenReturn("http://localhost");
		String token="eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJzNmYxcDYwYWVDTTBrNy1NaW9sN0Zib2FTdXlRYm95UC03S1RUTmVWLWZNIn0.eyJqdGkiOiJmYTU4Y2NjMC00ZDRiLTQ2ZjAtYjgwOC0yMWI4ZTdhNmMxNDMiLCJleHAiOjE2NDAxODc3MTksIm5iZiI6MCwiaWF0IjoxNjQwMTUxNzE5LCJpc3MiOiJodHRwczovL2Rldi5tb3NpcC5uZXQva2V5Y2xvYWsvYXV0aC9yZWFsbXMvbW9zaXAiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiOWRiZTE0MDEtNTQ1NC00OTlhLTlhMWItNzVhZTY4M2Q0MjZhIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibW9zaXAtcmVzaWRlbnQtY2xpZW50IiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiY2QwYjU5NjEtOTYzMi00NmE0LWIzMzgtODc4MWEzNDVmMTZiIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2Rldi5tb3NpcC5uZXQiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIkNSRURFTlRJQUxfUkVRVUVTVCIsIlJFU0lERU5UIiwib2ZmbGluZV9hY2Nlc3MiLCJQQVJUTkVSX0FETUlOIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJtb3NpcC1yZXNpZGVudC1jbGllbnQiOnsicm9sZXMiOlsidW1hX3Byb3RlY3Rpb24iXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImNsaWVudEhvc3QiOiIxMC4yNDQuNS4xNDgiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudElkIjoibW9zaXAtcmVzaWRlbnQtY2xpZW50IiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LW1vc2lwLXJlc2lkZW50LWNsaWVudCIsImNsaWVudEFkZHJlc3MiOiIxMC4yNDQuNS4xNDgifQ.xZq1m3mBTEvFDENKFOI59QsSl3sd_TSDNbhTAOq4x_x_4voPc4hh08gIxUdsVHfXY4T0P8DdZ1xNt8xd1VWc33Hc4b_3kK7ksGY4wwqtb0-pDLQGajCGuG6vebC1rYcjsGRbJ1Gnrj_F2RNY4Ky6Nq5SAJ1Lh_NVKNKFghAXb3YrlmqlmCB1fCltC4XBqNnF5_k4uzLCu_Wr0lt_M87X97DktaRGLOD2_HY1Ire9YPsWkoO8y7X_DRCY59yQDVgYs2nAiR6Am-c55Q0fEQ0HuB4IJHlhtMHm27dXPdOEhFhR8ZPOyeO6ZIcIm0ZTDjusrruqWy2_yO5fe3XIHkCOAw";
        System.setProperty("token", token);
        restUtil.getToken();
    }

    /**
     * This class tests the getTokenException method
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void getTokenExceptionTest1() throws IOException {
        Mockito.when(environment.getProperty("KEYBASEDTOKENAPI")).thenReturn("1122");
        restUtil.getToken();
    }

    /**
     * This class tests the getTokenException method
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void getTokenExceptionTest2() throws IOException {
        Mockito.when(environment.getProperty("KEYBASEDTOKENAPI")).thenReturn("1122");
        System.setProperty("token", "1122");
        restUtil.getToken();
    }

    /**
     * This class tests the getToken method
     * @throws IOException
     */
    @Test(expected = Exception.class)
    public void getTokenExceptionTest3() throws IOException {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpClient.execute(null)).thenReturn(httpResponse);
        Mockito.when(environment.getProperty("KEYBASEDTOKENAPI")).thenReturn("https://www.google.com/");
        System.setProperty("token", "1122");
        restUtil.getToken();
    }
}
