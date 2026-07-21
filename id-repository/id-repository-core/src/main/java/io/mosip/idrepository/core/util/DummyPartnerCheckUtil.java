package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_OLV_PARTNER;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;

/**
 * Identifies the configured dummy Online Verification (OLV) partner ID.
 *
 * <p>
 * MOSIP deployments use a placeholder OLV partner for development and testing. Credential
 * issuance and WebSub publishing skip or alter behaviour when the target partner matches
 * this dummy ID (for example, do not publish IDA events to the dummy partner).
 * </p>
 *
 * <h2>Configuration</h2>
 * <p>
 * Property {@link IdRepoConstants#IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID}
 * ({@code idrepo-dummy-online-verification-partner-id}), default
 * {@link IdRepoConstants#MOSIP_OLV_PARTNER}.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * if (dummyPartnerCheckUtil.isDummyOLVPartner(partnerId)) {
 *     // skip WebSub publish / treat as placeholder
 * }
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link PartnerServiceManager} — filter dummy from active OLV partner lists</li>
 *   <li>{@link IdRepoWebSubHelper} — skip publish for dummy partners</li>
 *   <li>Credential / identity managers — issuance and notification paths</li>
 * </ul>
 *
 * @author Manoj SP
 * @see IdRepoConstants#IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID
 * @see PartnerServiceManager
 * @see IdRepoWebSubHelper
 */
@Getter
public class DummyPartnerCheckUtil {

	/**
	 * Configured dummy OLV partner identifier.
	 * <p>
	 * Injected from {@code idrepo-dummy-online-verification-partner-id} with default
	 * {@code MOSIP_OLV_PARTNER}. Exposed via Lombok {@link Getter} as
	 * {@link #getDummyOLVPartnerId()}.
	 * </p>
	 */
	@Value("${" + IDREPO_DUMMY_ONLINE_VERIFICATION_PARTNER_ID + ":" + MOSIP_OLV_PARTNER + "}")
	private String dummyOLVPartnerId;

	/**
	 * Determines whether the given partner ID is the configured dummy OLV partner.
	 * <p>
	 * Comparison is exact string equality with {@link #getDummyOLVPartnerId()}.
	 * </p>
	 *
	 * @param partnerId partner identifier to compare (typically from credential or WebSub
	 *                  context); may be {@code null} (then returns {@code false} unless the
	 *                  configured id is also {@code null})
	 * @return {@code true} if {@code partnerId} equals the configured dummy OLV partner ID
	 * @see #getDummyOLVPartnerId()
	 */
	public boolean isDummyOLVPartner(String partnerId) {
		return getDummyOLVPartnerId().equals(partnerId);
	}
}
