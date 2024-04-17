package io.mosip.idrepository.identity.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

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
	private String id;
	
	@Lob
	@Type(type = "org.hibernate.type.BinaryType")
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private byte[] identityUpdateCount;

	public byte[] getIdentityUpdateCount() {
		return identityUpdateCount.clone();
	}

	public void setIdentityUpdateCount(byte[] identityUpdateCount) {
		this.identityUpdateCount = identityUpdateCount;
	}
}
