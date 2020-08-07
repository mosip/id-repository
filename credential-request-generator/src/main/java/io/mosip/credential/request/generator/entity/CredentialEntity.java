package io.mosip.credential.request.generator.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "credential", schema = "creden")
public class CredentialEntity {

	@Column(name = "id", nullable = false)
	private String requestId;


	@Column(name = "request")
	private String request;


	@Column(name = "status_code", nullable = false)
	private String statusCode;
	
	@Column(name = "cell_name", nullable = false)
	private String cellName;
	

	@Column(name = "trn_retry_count")
	private Integer retryCount;

	/** The created by. */
	@Column(name = "cr_by")
	private String createdBy;

	/** The create date time. */
	@Column(name = "cr_dtimes", updatable = false)

	private LocalDateTime createDateTime;

	/** The updated by. */
	@Column(name = "upd_by")
	private String updatedBy;

	/** The update date time. */
	@Column(name = "upd_dtimes")
	private LocalDateTime updateDateTime;

	/** The is deleted. */
	@Column(name = "is_deleted")
	private Boolean isDeleted;

	/** The deleted date time. */
	@Column(name = "del_dtimes")
	private LocalDateTime deletedDateTime;

}
