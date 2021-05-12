package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_OLV_PARTNER;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * @author Manoj SP
 *
 */
@Component
@ConditionalOnBean(name = { "idRepoDataSource" })
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
