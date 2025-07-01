package io.mosip.credential.request.generator.interceptor;


import io.mosip.credential.request.generator.context.CryptoContext;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorUncheckedException;
import io.mosip.credential.request.generator.util.CryptoUtil;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.hibernate.type.Type;
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

import java.io.Serializable;
import java.util.*;

@WebMvcTest
@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@RunWith(SpringRunner.class)
public class CredentialTransactionInterceptorTest {

    @InjectMocks
    private CredentialTransactionInterceptor credentialTransactionInterceptor;

    @Mock
    private RestUtil restUtil;

    @Mock
    private CryptoUtil cryptoUtil;

    /**
     * This class tests the onSave method
     * @throws Exception
     */
    @Test
    public void onSaveTest() throws Exception {
        CredentialEntity entity = new CredentialEntity();
        entity.setRequest("request");
        Serializable id = new Serializable() {};
        String REQUEST = "request";
        Object[] state = {"a", "b", REQUEST};
        String[] propertyNames ={"a","b",REQUEST};
        Type[] types = {};
        Mockito.when(cryptoUtil.encryptData(Mockito.anyString())).thenReturn("encrypted-secret");
        Assert.assertFalse(credentialTransactionInterceptor.onSave(entity, id, state, propertyNames, types));
    }

    /**
     * This class tests the onLoad method
     * @throws Exception
     */
    @Test
    public void onLoadTest() throws Exception {
        CredentialEntity entity = new CredentialEntity();
        Serializable id = new Serializable() {};
        String REQUEST = "request";
        Object[] state = {"a", "b", REQUEST};
        String[] propertyNames ={"a","b",REQUEST};
        Type[] types = {};
        Mockito.when(cryptoUtil.decryptData(Mockito.any())).thenReturn("decrypted-data");
        Assert.assertFalse(credentialTransactionInterceptor.onLoad(entity, id, state, propertyNames, types));
    }

    /**
     * This class tests the onLoad method
     * @throws Exception
     */
    @Test
    public void onLoadTest2() throws Exception {
        Object entity = new Object();
        Serializable id = new Serializable() {};
        String REQUEST = "request";
        Object[] state = {"a", "b", REQUEST};
        String[] propertyNames ={"a","b",REQUEST};
        Type[] types = {};
        Assert.assertFalse(credentialTransactionInterceptor.onLoad(entity, id, state, propertyNames, types));
    }

    /**
     * This class tests the onLoad method
     * @throws Exception
     */
    @Test
    public void onLoadExceptionTest() throws Exception {
        CredentialEntity entity = new CredentialEntity();
        Serializable id = new Serializable() {};
        String REQUEST = "request";
        Object[] state = {"a", "b", REQUEST};
        String[] propertyNames ={"a","b",REQUEST};
        Type[] types = {};

        Mockito.when(cryptoUtil.decryptData(Mockito.any()))
                .thenThrow(new RuntimeException("Simulated internal failure"));
        Assert.assertFalse(credentialTransactionInterceptor.onLoad(entity, id, state, propertyNames, types));
    }

    @Test
    public void onLoad_SkipDecryption() throws Exception {
        CredentialEntity entity = new CredentialEntity();
        Serializable id = new Serializable() {
        };
        String REQUEST = "request";
        Object[] state = {"a", "b", REQUEST};
        String[] propertyNames = {"a", "b", REQUEST};
        Type[] types = {};
        CryptoContext.setSkipDecryption(true);
        Assert.assertFalse(credentialTransactionInterceptor.onLoad(entity, id, state, propertyNames, types));
    }

    /**
     * This class tests the onFlushDirty method
     */
    @Test
    public void onFlushDirtyTest(){
        Object entity = new Object();
        Serializable id = new Serializable() {};
        Object[] currentState = {};
        Object[] previousState = {};
        String[] propertyNames ={};
        Type[] types = {};
        Assert.assertFalse( credentialTransactionInterceptor.onFlushDirty(entity, id, currentState, propertyNames, propertyNames, types));
    }

    /**
     * This class tests the setRestUtil method
     */
    @Test
    public void setRestUtilTest(){
        RestUtil restUtil = new RestUtil();
        credentialTransactionInterceptor.setRestUtil(restUtil);
    }

}
