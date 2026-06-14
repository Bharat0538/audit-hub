package in.audithub.sdk.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    String action();                      // e.g. "loan.approved"
    String actionType() default "UPDATE"; // CREATE/UPDATE/DELETE/READ
    String resourceType();                // e.g. "LoanApplication"
    String severity() default "LOW";
    String description() default "";
    boolean captureArgs() default false;  // capture method args as metadata
    boolean captureResult() default false;
}
