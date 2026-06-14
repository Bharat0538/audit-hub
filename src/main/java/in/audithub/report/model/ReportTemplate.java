package in.audithub.report.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId; // Null for system-wide templates

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "template_type", nullable = false)
    private String templateType; // e.g. RBI_AUDIT_TRAIL, CUSTOM

    @Column(nullable = false)
    private String format; // PDF, CSV

    @Column(nullable = false, columnDefinition = "TEXT")
    private String config; // JSON configuration mapping filters, fields

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
