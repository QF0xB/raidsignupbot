package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import java.lang.reflect.Field;

final class MapperTestSupport {
  private MapperTestSupport() {
  }

  static void setField(Object target, String fieldName, Object value) {
    try {
      Class<?> current = target.getClass();
      boolean updated = false;
      while (current != null) {
        try {
          Field field = current.getDeclaredField(fieldName);
          field.setAccessible(true);
          field.set(target, value);
          updated = true;
        } catch (NoSuchFieldException ignored) {
          // keep walking up the hierarchy
        }
        current = current.getSuperclass();
      }

      if (!updated) {
        throw new NoSuchFieldException(fieldName);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set field " + fieldName + " on " + target.getClass(), e);
    }
  }
}
