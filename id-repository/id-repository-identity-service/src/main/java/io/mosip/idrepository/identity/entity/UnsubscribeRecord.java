package io.mosip.idrepository.identity.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "unsubscribe_record", schema = "idrepo")
public class UnsubscribeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "comments")
    private String comments;

    @Column(name = "unsubscribed_at", nullable = false)
    private LocalDateTime unsubscribedAt;

    @Column(name = "cr_by", nullable = false)
    private String createdBy;
}