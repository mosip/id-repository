package io.mosip.idrepository.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.Type;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Table(name = "identity_update_count_tracker", schema = "idrepo")
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityUpdateTracker {

	@Id
	/** Id ({@code id} column). */
	@Column(name = "id")
	/** Id. */
	private String id;
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	/** Identity update count ({@code identity_update_count} column). */
	@Column(name = "identity_update_count")
	private byte[] identityUpdateCount;

	/**
	 * @return identity update count
	 */
	public byte[] getIdentityUpdateCount() {
		return identityUpdateCount.clone();
	}

	/**
	 * @param identityUpdateCount identity update count
	 */
	public void setIdentityUpdateCount(byte[] identityUpdateCount) {
		this.identityUpdateCount = identityUpdateCount;
	}
}
