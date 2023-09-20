package ru.sis.cosplay.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    List<Image> images;
    String error;
    String cosplayerName;
    String cosplayerUid;
    List<String> tags;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        String name;
        String url;
        String hash;
        String preview64;
        Boolean duplicate;
        List<String> sim;
        Boolean approved;
    }
}
