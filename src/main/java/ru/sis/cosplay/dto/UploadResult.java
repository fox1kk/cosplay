package ru.sis.cosplay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResult {
  Boolean success;
  String error;
}
