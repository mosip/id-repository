package io.mosip.idrepository.core.entity;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "handle", schema = "idrepo")
@ConditionalOnBean(name = { "idRepoDataSource" })
public class Handle implements HandleInfo {

    @Id
    @Column(name = "id")
    private String id; //UUID

    @Column(name = "uin_hash")
    private String uinHash;

    @Column(name = "handle")
    private String handle;

    @Column(name = "handle_hash")
    private String handleHash;

    @Column(name = "cr_by")
    private String createdBy;

    @Column(name = "cr_dtimes")
    private LocalDateTime createdDateTime;

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
