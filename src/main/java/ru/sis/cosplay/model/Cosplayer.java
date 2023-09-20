package ru.sis.cosplay.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Builder
public class Cosplayer {
  @Id String id;
  String uid;
  String name;
}
