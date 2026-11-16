package io.mosip.idrepository.credential.store.provider;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.credential.store.dto.AllowedKycDto;
import io.mosip.idrepository.credential.store.dto.PartnerCredentialTypePolicyDto;
import io.mosip.idrepository.credential.store.dto.PolicyAttributesDto;
import io.mosip.idrepository.credential.store.dto.Source;

public class CredentialProviderVerifiedAttributesTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void verifiedAttributesRespectSelectionAndPreserveMetadata() throws Exception {
        CredentialProvider provider = new CredentialProvider();
        org.springframework.test.util.ReflectionTestUtils.setField(
                provider, "env", new org.springframework.mock.env.MockEnvironment());
        List metadata = List.of(Map.of(
                "trustFramework", "kyc",
                "verificationProcess", "otp_verified",
                "claims", List.of("phone")));

        ResponseDTO response = new ResponseDTO();
        response.setIdentity(Map.of("phone", "1234567890"));
        response.setVerifiedAttributes(metadata);
        IdResponseDTO identity = new IdResponseDTO();
        identity.setResponse(response);

        Source source = new Source();
        source.setAttribute("verifiedAttributes");
        AllowedKycDto attribute = new AllowedKycDto();
        attribute.setAttributeName("verifiedAttributes");
        attribute.setSource(List.of(source));
        PolicyAttributesDto policyAttributes = new PolicyAttributesDto();
        policyAttributes.setShareableAttributes(List.of(attribute));
        PartnerCredentialTypePolicyDto policy = new PartnerCredentialTypePolicyDto();
        policy.setPolicies(policyAttributes);

        CredentialServiceRequestDto request = new CredentialServiceRequestDto();
        request.setAdditionalData(new HashMap<>());

        assertEquals(metadata, provider.prepareSharableAttributes(identity, policy, request).get(attribute));

        request.setSharableAttributes(List.of("phone"));
        assertFalse(provider.prepareSharableAttributes(identity, policy, request).containsKey(attribute));

        request.setSharableAttributes(null);
        policyAttributes.setShareableAttributes(List.of());
        assertFalse(provider.prepareSharableAttributes(identity, policy, request).containsKey(attribute));

        policyAttributes.setShareableAttributes(List.of(attribute));
        response.setVerifiedAttributes(null);
        assertFalse(provider.prepareSharableAttributes(identity, policy, request).containsKey(attribute));

        response.setVerifiedAttributes(List.of());
        assertEquals(List.of(), provider.prepareSharableAttributes(identity, policy, request).get(attribute));
    }
}
