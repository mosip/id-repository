package io.mosip.idrepository.identity.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite key for history tables ({@code uin_ref_id} + effective datetime).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryPK implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1124172782509039861L;

	/** Uin ref id. */
	private String uinRefId;

	/** Effective date time. */
	private LocalDateTime effectiveDateTime;

}
