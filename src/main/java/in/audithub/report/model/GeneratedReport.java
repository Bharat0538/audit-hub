package in.audithub.report.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generated_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(nullable = false, columnDefinition = "TEXT")
    private String filters; // JSON string of query filters

    @Column(nullable = false)
    private String format; // PDF, CSV

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "presigned_url")
    private String presignedUrl;

    @Column(name = "presigned_url_expires_at")
    private Instant presignedUrlExpiresAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
