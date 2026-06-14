package in.audithub.alert.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "condition_type", nullable = false)
    @Builder.Default
    private String conditionType = "THRESHOLD";

    @Column(name = "condition_config", nullable = false, columnDefinition = "TEXT")
    private String conditionConfig; // JSON string of settings

    @Column(nullable = false)
    @Builder.Default
    private String severity = "HIGH";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alert_rule_channels", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "channel")
    private List<String> notificationChannels;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
