package io.mosip.idrepository.core.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "handle", schema = "idrepo")
@ConditionalOnBean(name = { "idRepoDataSource" })
public class Handle implements HandleInfo {

    @Id
    private String id; //UUID

    @Column(name = "uin_hash")
    private String uinHash;

    private String handle;

    @Column(name = "handle_hash")
    private String handleHash;

    @Column(name = "cr_by")
    private String createdBy;

    @Column(name = "cr_dtimes")
    private LocalDateTime createdDateTime;

	@NotNull
	@Column(name = "is_active")
	private Boolean isActive = true;

	@NotNull
	@Column(name = "is_deleted")
	private Boolean isDeleted = false;

    @Override
    public String getHandle() {
        return handle;
    }

    @Override
    public void setHandle(String handle) {
        this.handle = handle;
    }

    @Override
    public String getUin() { return null; }

    @Override
    public void setUin(String uin) { }

    @Override
    public byte[] getUinData() { return null; }

    @Override
    public void setUinData(byte[] uinData) { }

    @Override
    public void setUpdatedBy(String updatedBy) { }

    @Override
    public void setUpdatedDateTime(LocalDateTime updatedDTimes) { }
}
