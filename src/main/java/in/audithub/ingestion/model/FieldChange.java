package in.audithub.ingestion.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldChange {
    @NotBlank(message = "Field name is required")
    private String fieldName;
    private Object oldValue;
    private Object newValue;
}
