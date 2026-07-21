package io.mosip.idrepository.credential.request.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.credential.request.constant.CredentialRequestErrorCodes;
import io.mosip.idrepository.credential.request.exception.CredentialRequestGeneratorUncheckedException;

@RunWith(MockitoJUnitRunner.class)
public class CryptoUtilTest {

	private static final String CRYPTO_REF_ID = "test-cred-crypto-ref";

	@InjectMocks
	private io.mosip.idrepository.credential.request.util.CryptoUtil cryptoUtil;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Before
	public void init() {
		EnvUtil.setCredCryptoRefId(CRYPTO_REF_ID);
	}

	@Test
	public void decryptDataSuccess() throws Exception {
		byte[] plainBytes = "plain-data".getBytes(StandardCharsets.UTF_8);
		String cipherText = "cipher-text";
		when(securityManager.decrypt(eq(cipherText.getBytes(StandardCharsets.UTF_8)), eq(CRYPTO_REF_ID)))
				.thenReturn(plainBytes);

		assertEquals(io.mosip.kernel.core.util.CryptoUtil.encodeToURLSafeBase64(plainBytes), cryptoUtil.decryptData(cipherText));
	}

	@Test
	public void decryptDataFailureWrapsException() throws Exception {
		when(securityManager.decrypt(eq("bad".getBytes(StandardCharsets.UTF_8)), eq(CRYPTO_REF_ID)))
				.thenThrow(new IdRepoAppException("ERR", "decrypt failed"));

		CredentialRequestGeneratorUncheckedException ex = assertThrows(
				CredentialRequestGeneratorUncheckedException.class, () -> cryptoUtil.decryptData("bad"));
		assertEquals(CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED.getErrorCode(), ex.getErrorCode());
	}

	@Test
	public void encryptDataSuccess() throws Exception {
		String plainBase64 = io.mosip.kernel.core.util.CryptoUtil.encodeToURLSafeBase64("secret".getBytes(StandardCharsets.UTF_8));
		byte[] cipherBytes = "encrypted".getBytes(StandardCharsets.UTF_8);
		when(securityManager.encrypt(eq(io.mosip.kernel.core.util.CryptoUtil.decodeURLSafeBase64(plainBase64)), eq(CRYPTO_REF_ID)))
				.thenReturn(cipherBytes);

		assertEquals(new String(cipherBytes, StandardCharsets.UTF_8), cryptoUtil.encryptData(plainBase64));
	}

	@Test
	public void encryptDataFailureWrapsException() throws Exception {
		String plainBase64 = io.mosip.kernel.core.util.CryptoUtil.encodeToURLSafeBase64("secret".getBytes(StandardCharsets.UTF_8));
		when(securityManager.encrypt(eq(io.mosip.kernel.core.util.CryptoUtil.decodeURLSafeBase64(plainBase64)), eq(CRYPTO_REF_ID)))
				.thenThrow(new IdRepoAppException("ERR", "encrypt failed"));

		CredentialRequestGeneratorUncheckedException ex = assertThrows(
				CredentialRequestGeneratorUncheckedException.class, () -> cryptoUtil.encryptData(plainBase64));
		assertEquals(CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED.getErrorCode(), ex.getErrorCode());
	}
}
