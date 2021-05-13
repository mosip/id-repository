package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_OLV_PARTNER;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author Manoj SP
 *
 */
public class DummyPartnerCheckUtil {

	@Value("${" + IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID + ":" + MOSIP_OLV_PARTNER + "}")
	private String dummyOLVPartnerId;
	
	public String getDummyOLVPartnerId() {
		return dummyOLVPartnerId;
	}

	public boolean isDummyOLVPartner(String partnerId) {
		return getDummyOLVPartnerId().equals(partnerId);
	}

}
