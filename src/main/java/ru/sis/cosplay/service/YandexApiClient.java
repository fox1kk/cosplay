package ru.sis.cosplay.service;

import static ru.sis.cosplay.service.WeiboService.USER_AGENT;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import ru.sis.cosplay.dto.UploadRequestResponse;
import ru.sis.cosplay.dto.UploadResult;

@Service
@Log4j2
public class YandexApiClient {

  private final String uploadBasePath;
  private final RestTemplate restTemplate;

  private static final String UPLOAD_URL = "https://cloud-api.yandex.net/v1/disk/resources/upload";
  private static final String PREVIEW_URL = "https://webdav.yandex.ru/{0}/{1}?preview&size=XXXL";

  @Autowired
  public YandexApiClient(
      // https://yandex.ru/dev/disk/api/reference/upload.html
      // https://yandex.ru/dev/disk/doc/dg/reference/preview.html
      // Для получения токена:
      // https://oauth.yandex.ru/
      // https://oauth.yandex.ru/authorize?response_type=token&client_id=айди-созданного-приложения
      @Value("${api.yandex.token}") String token,
      @Value("${api.yandex.path}") String uploadBasePath) {
    this.uploadBasePath = uploadBasePath;

    restTemplate = new RestTemplate();

    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    interceptors.add(new YandexApiHttpRequestInterceptor(token));
    restTemplate.setInterceptors(interceptors);
  }

  public byte[] getPreview(String imageName) {
    return restTemplate.getForObject(
        MessageFormat.format(PREVIEW_URL, uploadBasePath, imageName), byte[].class);
  }

  public UploadResult uploadFile(String name, String url) {
    String uploadUrl;
    try {
      uploadUrl = requestUploadUrl(name);
    } catch (HttpClientErrorException.Conflict e) {
      return UploadResult.builder().success(true).build();
    }

    if (uploadUrl == null) {
      return UploadResult.builder().success(false).error("Ошибка при запросе в яндекс").build();
    }

    try {
      URLConnection imgConnection = new URL(url).openConnection();
      imgConnection.setRequestProperty("User-Agent", USER_AGENT);
      imgConnection.setRequestProperty("referer", "https://weibo.com/");
      imgConnection.connect();

      byte[] file = imgConnection.getInputStream().readAllBytes();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", file);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
    } catch (Exception e) {
      e.printStackTrace();
      return UploadResult.builder()
          .success(false)
          .error(
              "Ошибка при загрузке файла: " + e.getClass().getSimpleName() + " " + e.getMessage())
          .build();
    }

    return UploadResult.builder().success(true).build();
  }

  public UploadResult uploadFile(String name, MultipartFile img) {
    String uploadUrl;
    try {
      uploadUrl = requestUploadUrl(name);
    } catch (HttpClientErrorException.Conflict e) {
      return UploadResult.builder().success(true).build();
    }

    if (uploadUrl == null) {
      return UploadResult.builder().success(false).error("Ошибка при запросе в яндекс").build();
    }

    try {
      byte[] file = img.getInputStream().readAllBytes();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", file);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
    } catch (Exception e) {
      e.printStackTrace();
      return UploadResult.builder()
          .success(false)
          .error(
              "Ошибка при загрузке файла: " + e.getClass().getSimpleName() + " " + e.getMessage())
          .build();
    }

    return UploadResult.builder().success(true).build();
  }

  private String requestUploadUrl(String fileName) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(UPLOAD_URL)
            .queryParam("path", this.uploadBasePath + "/" + fileName);

    UploadRequestResponse result =
        restTemplate.getForObject(builder.build().encode().toUri(), UploadRequestResponse.class);

    return result != null ? result.getHref() : null;
  }

  static class YandexApiHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final String token;

    YandexApiHttpRequestInterceptor(String token) {
      this.token = token;
    }

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
      HttpHeaders httpHeaders = request.getHeaders();
      httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
      httpHeaders.set("Authorization", "OAuth " + this.token);

      HttpRequest requestWrapper = new HttpRequestWrapper(request);

      return execution.execute(requestWrapper, body);
    }
  }
}
