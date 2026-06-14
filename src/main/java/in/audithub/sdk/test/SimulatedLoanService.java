package in.audithub.sdk.test;

import in.audithub.sdk.annotation.Audited;
import in.audithub.sdk.annotation.AuditResourceId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class SimulatedLoanService {

    @Audited(
            action = "loan.status.approved",
            actionType = "UPDATE",
            resourceType = "LoanApplication",
            severity = "HIGH",
            description = "Loan application approved via SDK",
            captureArgs = true,
            captureResult = true
    )
    public String approveLoan(@AuditResourceId String loanId, BigDecimal amount) {
        log.info("SimulatedLoanService: Approving loan {} with amount {}", loanId, amount);
        return "Loan " + loanId + " approved for " + amount;
    }

    @Audited(
            action = "loan.updated",
            actionType = "UPDATE",
            resourceType = "LoanApplication",
            severity = "MEDIUM",
            description = "Loan application updated via SDK auto-diff"
    )
    public String updateLoan(@AuditResourceId String loanId, 
                             @in.audithub.sdk.annotation.AuditBefore LoanState before, 
                             @in.audithub.sdk.annotation.AuditAfter LoanState after) {
        log.info("SimulatedLoanService: Updating loan {} from status {} to {}", loanId, before.getStatus(), after.getStatus());
        return "Loan " + loanId + " updated successfully";
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class LoanState {
        private String status;
        private BigDecimal amount;
        @in.audithub.sdk.annotation.AuditIgnore
        private String internalNotes;
    }
}
