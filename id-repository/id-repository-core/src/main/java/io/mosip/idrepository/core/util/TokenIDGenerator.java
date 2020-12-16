package io.mosip.idrepository.core.util;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.kernel.core.util.HMACUtils2;

@Component
public class TokenIDGenerator {

	@Value("${mosip.kernel.tokenid.uin.salt}")
	private String uinSalt;

	@Value("${mosip.kernel.tokenid.length}")
	private int tokenIDLength;

	@Value("${mosip.kernel.tokenid.partnercode.salt}")
	private String partnerCodeSalt;

	public String generateTokenID(String uin, String partnerCode) {
		try {
			String uinHash = HMACUtils2.digestAsPlainText((uin + uinSalt).getBytes());
			String hash = HMACUtils2.digestAsPlainText((partnerCodeSalt + partnerCode + uinHash).getBytes());
			return new BigInteger(hash.getBytes()).toString().substring(0, tokenIDLength);
		} catch (NoSuchAlgorithmException e) {
			// TODO to be removed
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

}
