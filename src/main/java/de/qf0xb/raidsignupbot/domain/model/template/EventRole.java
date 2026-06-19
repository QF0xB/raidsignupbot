package de.qf0xb.raidsignupbot.domain.model.template;

import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = "key")
@ToString
public class EventRole {
  private final String key;
  private String name;
  private String emojiSlug;
  private int requiredCount;
}
