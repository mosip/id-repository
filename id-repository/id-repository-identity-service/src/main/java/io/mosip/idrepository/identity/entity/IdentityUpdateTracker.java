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
	@Column(name = "id")
	private String id;
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Column(name = "identity_update_count")
	private byte[] identityUpdateCount;

	public byte[] getIdentityUpdateCount() {
		return identityUpdateCount.clone();
	}

	public void setIdentityUpdateCount(byte[] identityUpdateCount) {
		this.identityUpdateCount = identityUpdateCount;
	}
}
