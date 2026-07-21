package io.mosip.idrepository.identity.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link UinDocumentDraft}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDraftPK implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1124172782509039861L;

	/** Reg id. */
	private String regId;

	/** Doccat code. */
	private String doccatCode;
}
