package in.audithub.sdk.test;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/test/sdk")
@RequiredArgsConstructor
public class SdkTestController {

    private final SimulatedLoanService loanService;
    private final in.audithub.sdk.client.AuditHubClient sdkClient;

    private static volatile boolean webhookReceived = false;
    private static volatile boolean signatureVerified = false;
    private static volatile String lastWebhookError = null;

    @PostMapping("/config")
    public void configureSdk(@RequestParam String apiKey, @RequestParam java.util.UUID appId) {
        sdkClient.updateConfig(apiKey, appId);
    }

    @PostMapping("/loan")
    public String approveLoan(@RequestParam String loanId, @RequestParam BigDecimal amount) {
        return loanService.approveLoan(loanId, amount);
    }

    @PostMapping("/loan/update")
    public String updateLoan(@RequestParam String loanId,
                             @RequestParam String oldStatus, @RequestParam BigDecimal oldAmount,
                             @RequestParam String newStatus, @RequestParam BigDecimal newAmount) {
        SimulatedLoanService.LoanState before = new SimulatedLoanService.LoanState(oldStatus, oldAmount, "confidential-before");
        SimulatedLoanService.LoanState after = new SimulatedLoanService.LoanState(newStatus, newAmount, "confidential-after");
        return loanService.updateLoan(loanId, before, after);
    }

    @PostMapping("/callback")
    public void receiveWebhook(@RequestBody String body, @RequestHeader("X-AuditHub-Signature-256") String signature) {
        webhookReceived = true;
        // In our local test, we use "test-webhook-secret"
        boolean verified = in.audithub.sdk.util.WebhookVerifier.verify(body, signature, "test-webhook-secret");
        signatureVerified = verified;
        if (!verified) {
            lastWebhookError = "Signature verification failed. Signature header: " + signature;
        } else {
            lastWebhookError = null;
        }
    }

    @GetMapping("/callback/status")
    public String getCallbackStatus() {
        return String.format("{\"received\": %b, \"verified\": %b, \"error\": %s}",
                webhookReceived, signatureVerified, lastWebhookError != null ? "\"" + lastWebhookError + "\"" : "null");
    }

    @PostMapping("/callback/reset")
    public void resetCallback() {
        webhookReceived = false;
        signatureVerified = false;
        lastWebhookError = null;
    }
}
