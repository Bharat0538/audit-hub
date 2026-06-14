package in.audithub.sdk.util;

import in.audithub.ingestion.model.FieldChange;
import in.audithub.sdk.annotation.AuditIgnore;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DiffUtils {

    public static List<FieldChange> diff(Object before, Object after) {
        List<FieldChange> changes = new ArrayList<>();
        if (before == null && after == null) {
            return changes;
        }

        Class<?> clazz = before != null ? before.getClass() : after.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(AuditIgnore.class)) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object beforeVal = before != null ? field.get(before) : null;
                Object afterVal = after != null ? field.get(after) : null;

                if (!Objects.equals(beforeVal, afterVal)) {
                    FieldChange change = new FieldChange(
                            field.getName(),
                            beforeVal != null ? beforeVal.toString() : null,
                            afterVal != null ? afterVal.toString() : null
                    );
                    changes.add(change);
                }
            } catch (IllegalAccessException e) {
                log.warn("Failed to access field {} for diff: {}", field.getName(), e.getMessage());
            }
        }
        return changes;
    }
}
