package io.mosip.idrepository.identity.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link UinDocument} ({@code uin_ref_id}, {@code doc_id}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPK implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1124172782509039861L;

	/** Uin ref id. */
	private String uinRefId;

	/** Doccat code. */
	private String doccatCode;
}
