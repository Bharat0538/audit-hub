package in.audithub.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private long totalEventsToday;
    private long totalEventsThisMonth;
    private double quotaUsedPercent;
    private long monthlyQuota;
    private long criticalEventsToday;
    private long failedEventsToday;
    private int activeApplications;
    private List<ActorStats> topActors;
    private List<Object> topResources;
    private List<Object> eventsTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActorStats {
        private String userId;
        private String userName;
        private long eventCount;
    }
}
