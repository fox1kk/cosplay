package ru.sis.cosplay.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Builder
public class ImageData {
  @Id private String id;
  private String feedId;
  private String name;
  private String hash;
}
