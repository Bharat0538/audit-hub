package in.audithub.ingestion.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {
    @NotNull(message = "Action type is required")
    private ActionType type;
    private String name;
    private String description;
}
