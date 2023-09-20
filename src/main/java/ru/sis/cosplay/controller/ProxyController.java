package ru.sis.cosplay.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.sis.cosplay.exception.ServiceException;
import ru.sis.cosplay.service.YandexApiClient;

@RestController
@RequiredArgsConstructor
public class ProxyController {

  @Value("${api.yandex.token}")
  String token;

  private final YandexApiClient apiClient;

  private final Pattern pattern =
      Pattern.compile("^[a-z0-9.]*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

  @GetMapping(value = "/img/{imgName}", produces = MediaType.IMAGE_JPEG_VALUE)
  public @ResponseBody
  byte[] proxyImage(@PathVariable String imgName) throws IOException {
    Matcher m = pattern.matcher(imgName);
    if (!m.find()) {
      throw new ServiceException("404");
    }
    
    return IOUtils.toByteArray(new ByteArrayInputStream(apiClient.getPreview(imgName)));
  }
}
