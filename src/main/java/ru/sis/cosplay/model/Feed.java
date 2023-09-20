package ru.sis.cosplay.model;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Feed {
  @Id private String id;
  private String cosplayerId;
  private Instant dateCreated;
  private Boolean hidden;
  private List<String> tags;

  @Transient private Cosplayer cosplayer;
  @Transient private List<ImageData> images;
}
