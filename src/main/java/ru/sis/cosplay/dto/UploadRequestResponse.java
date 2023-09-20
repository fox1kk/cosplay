package ru.sis.cosplay.dto;

import lombok.Data;

@Data
public class UploadRequestResponse {
    private String operation_id;
    private String href;
    private String method;
    private Boolean templated;
}
