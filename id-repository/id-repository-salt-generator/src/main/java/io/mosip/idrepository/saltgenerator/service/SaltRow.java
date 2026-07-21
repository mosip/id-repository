package io.mosip.idrepository.saltgenerator.service;

import java.time.LocalDateTime;

/**
 * Immutable salt values for one bucket id, ready for JDBC insert into idrepo and idmap tables.
 *
 * <p>
 * Produced by {@link SaltGenerator} and consumed by {@link SaltJdbcWriter}. The same
 * {@link #hashSalt()} is written to both databases; encrypt salts differ per schema.
 * </p>
 *
 * @param id                   salt bucket primary key
 * @param hashSalt             Base64 hash salt for {@code uin_hash_salt} (both DBs)
 * @param identityEncryptSalt  Base64 encrypt salt for {@code idrepo.uin_encrypt_salt}
 * @param vidEncryptSalt       Base64 encrypt salt for {@code idmap.uin_encrypt_salt}
 * @param createdAt            UTC create timestamp bound to {@code cr_dtimes}
 * @author MOSIP
 * @see SaltGenerator
 * @see SaltJdbcWriter
 */
public record SaltRow(long id, String hashSalt, String identityEncryptSalt, String vidEncryptSalt,
		LocalDateTime createdAt) {
}
