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
        //Mockito.doReturn(restTemplate).when(restUtilSpy).getRestTemplate();
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
        //Mockito.doReturn(restTemplate).when(restUtilSpy).getRestTemplate();
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
        MediaType mediaType = MediaType.ALL;
        HttpPost post = new HttpPost();
        Mockito.when(environment.getProperty("KEYBASEDTOKENAPI")).thenReturn("1212");
        RestUtil restUtilSpy = Mockito.spy(restUtil);
        //Mockito.doReturn("1122").when(restUtilSpy).getToken();
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
        //Mockito.doReturn("1122").when(restUtilSpy).getToken();
        ReflectionTestUtils.invokeMethod(restUtilSpy, "setRequestHeader", requestType, mediaType);
    }

}
