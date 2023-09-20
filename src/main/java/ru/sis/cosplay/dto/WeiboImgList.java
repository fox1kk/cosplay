package ru.sis.cosplay.dto;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class WeiboImgList {
  private List<WeiboImgMap> pics;

  public List<String> getImgUrls() {
    return pics.stream().map(item -> item.getLarge().getUrl()).collect(Collectors.toList());
  }

  @Data
  static class WeiboImgMap {
    private String pid;
    private WeiboImgLargeSizeData large;
  }

  @Data
  static class WeiboImgLargeSizeData {
    private String url;
  }
}
