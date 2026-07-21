package io.mosip.idrepository.saltgenerator.service;

import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.CHUNK_SIZE;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.END_SEQ;
import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.START_SEQ;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.HMACUtils2;

/**
 * Orchestrates chunked generation of hash and encrypt salts for identity and VID databases.
 *
 * <p>
 * For each id in {@code [effectiveStart, endSeq]}, creates one {@link SaltRow} containing:
 * </p>
 * <ul>
 *   <li>shared hash salt (written to both {@code idrepo.uin_hash_salt} and
 *       {@code idmap.uin_hash_salt})</li>
 *   <li>identity encrypt salt ({@code idrepo.uin_encrypt_salt})</li>
 *   <li>VID encrypt salt ({@code idmap.uin_encrypt_salt})</li>
 * </ul>
 * Chunks are flushed through {@link SaltJdbcWriter}. Progress is logged every
 * {@value #PROGRESS_LOG_INTERVAL} ids.
 *
 * <h2>Config</h2>
 * See {@link SaltGeneratorConstant}. Missing {@code start-sequence} / {@code end-sequence}
 * fails fast with {@link IllegalStateException}.
 *
 * <h2>Resume</h2>
 * {@link SaltJdbcWriter#resolveResumeStart(long, long)} advances the start past already
 * populated ids so re-runs after Job failure do not regenerate existing buckets.
 *
 * @author MOSIP
 * @see SaltJdbcWriter
 * @see SaltRow
 * @see io.mosip.idrepository.saltgenerator.SaltGeneratorRunner
 */
@Component
public class SaltGenerator {

	private static final Logger LOGGER = IdRepoLogger.getLogger(SaltGenerator.class);

	/** Default JDBC batch size when {@link SaltGeneratorConstant#CHUNK_SIZE} is unset or invalid. */
	private static final int DEFAULT_CHUNK_SIZE = 500;

	/** Log a progress line when the current id is a multiple of this value. */
	private static final long PROGRESS_LOG_INTERVAL = 10_000L;

	@Autowired
	private Environment env;

	@Autowired
	private SaltJdbcWriter saltJdbcWriter;

	/**
	 * Reads sequence bounds, resumes if needed, generates salts, and writes them in chunks.
	 *
	 * @throws IllegalStateException if start/end sequence properties are missing
	 * @throws Exception             if a JDBC write fails (logged then rethrown)
	 */
	public void start() throws Exception {
		Long startSeq = env.getProperty(START_SEQ.getValue(), Long.class);
		Long endSeq = env.getProperty(END_SEQ.getValue(), Long.class);
		if (startSeq == null || endSeq == null) {
			throw new IllegalStateException("Salt sequence bounds must be configured: "
					+ START_SEQ.getValue() + ", " + END_SEQ.getValue());
		}
		int chunkSize = env.getProperty(CHUNK_SIZE.getValue(), Integer.class, DEFAULT_CHUNK_SIZE);
		if (chunkSize < 1) {
			chunkSize = DEFAULT_CHUNK_SIZE;
		}

		long effectiveStart = saltJdbcWriter.resolveResumeStart(startSeq, endSeq);
		if (effectiveStart > endSeq) {
			LOGGER.info("SALT_GENERATOR", "SaltGenerator", "start", "No salt rows to generate.");
			return;
		}

		LOGGER.info("SALT_GENERATOR", "SaltGenerator", "start",
				"Generating salts for ids " + effectiveStart + " to " + endSeq + " (chunkSize=" + chunkSize + ")");

		List<SaltRow> chunk = new ArrayList<>(chunkSize);
		long processed = 0;
		try {
			for (long id = effectiveStart; id <= endSeq; id++) {
				chunk.add(createRow(id));
				if (chunk.size() >= chunkSize) {
					saltJdbcWriter.writeChunk(chunk);
					processed += chunk.size();
					chunk.clear();
					logProgressIfNeeded(id, processed, endSeq - effectiveStart + 1);
				}
			}
			if (!chunk.isEmpty()) {
				saltJdbcWriter.writeChunk(chunk);
				processed += chunk.size();
			}
			LOGGER.info("SALT_GENERATOR", "SaltGenerator", "start",
					"Salt generation completed. Rows processed in this run: " + processed);
		} catch (Exception e) {
			LOGGER.error("SALT_GENERATOR", "SaltGenerator", "start",
					e.getClass().getName() + ": " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Builds one salt row: Base64-encoded HMAC salts and UTC create timestamp.
	 *
	 * @param id salt bucket id
	 * @return immutable row ready for JDBC insert
	 */
	private SaltRow createRow(long id) {
		String hashSalt = CryptoUtil.encodeToPlainBase64(HMACUtils2.generateSalt());
		String identityEncryptSalt = CryptoUtil.encodeToPlainBase64(HMACUtils2.generateSalt());
		String vidEncryptSalt = CryptoUtil.encodeToPlainBase64(HMACUtils2.generateSalt());
		return new SaltRow(id, hashSalt, identityEncryptSalt, vidEncryptSalt, DateUtils2.getUTCCurrentDateTime());
	}

	/**
	 * Emits an INFO progress log when {@code currentId} is aligned to
	 * {@link #PROGRESS_LOG_INTERVAL}.
	 *
	 * @param currentId last id flushed in the current chunk
	 * @param processed rows written in this run so far
	 * @param total     total rows planned for this run
	 */
	private void logProgressIfNeeded(long currentId, long processed, long total) {
		if (currentId % PROGRESS_LOG_INTERVAL == 0) {
			LOGGER.info("SALT_GENERATOR", "SaltGenerator", "progress",
					"Processed " + processed + " / " + total + " (last id=" + currentId + ")");
		}
	}
}
