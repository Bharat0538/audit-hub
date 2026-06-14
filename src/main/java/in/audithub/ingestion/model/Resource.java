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
public class Resource {
    @NotBlank(message = "Resource type is required")
    private String type;
    @NotBlank(message = "Resource id is required")
    private String id;
    private String name;
    private String path;
}
