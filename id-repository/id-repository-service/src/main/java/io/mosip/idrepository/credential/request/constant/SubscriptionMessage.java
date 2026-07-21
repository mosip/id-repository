package io.mosip.idrepository.credential.request.constant;

/**
 * Human-readable outcome strings returned after WebSub topic subscription during
 * credential-request service startup.
 * <p>
 * On application bootstrap, {@link io.mosip.idrepository.credential.request.init.CredentialInstializer}
 * and {@link io.mosip.idrepository.credential.request.init.SubscribeEvent} register the service
 * as a subscriber to {@code CREDENTIAL_STATUS_UPDATE} (and related topics) so partners can push
 * credential status callbacks. These constants are logged and returned from subscribe helpers to
 * distinguish a newly established subscription from an idempotent "already registered" response from
 * the WebSub hub.
 * </p>
 */
public class SubscriptionMessage {

	/**
	 * WebSub subscription was created successfully for the credential-request callback URL.
	 */
	public static final String SUCCESS = "Success";

	/**
	 * WebSub hub reported the callback is already subscribed; treated as a non-fatal startup outcome.
	 */
	public static final String ALREADY_SUBSCRIBED = "Already Subscribed";
}
