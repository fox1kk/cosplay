package ru.sis.cosplay.service;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.bson.types.ObjectId;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.sis.cosplay.dto.ParseResult;
import ru.sis.cosplay.dto.UploadResult;
import ru.sis.cosplay.dto.WeiboImgList;
import ru.sis.cosplay.exception.ServiceException;
import ru.sis.cosplay.jphash.image.radial.RadialHash;
import ru.sis.cosplay.jphash.image.radial.RadialHashAlgorithm;
import ru.sis.cosplay.jphash.util.HexUtil;
import ru.sis.cosplay.model.Cosplayer;
import ru.sis.cosplay.model.Feed;
import ru.sis.cosplay.model.ImageData;
import ru.sis.cosplay.model.Tag;
import ru.sis.cosplay.repository.CosplayerRepository;
import ru.sis.cosplay.repository.FeedRepository;
import ru.sis.cosplay.repository.ImageDataRepository;
import ru.sis.cosplay.repository.TagRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class WeiboService {
  private final ThreadPoolTaskExecutor executorService;
  private final YandexApiClient yandexApi;
  private final ImageDataRepository imageDataRepository;
  private final TagRepository tagRepository;
  private final CosplayerRepository cosplayerRepository;
  private final FeedRepository feedRepository;

  private static final Integer SPLIT_SIZE = 100;
  public static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

  public List<String> getAllTags() {
    return tagRepository.findAll().stream().map(Tag::getName).toList();
  }

  public String addTag(String tag) {
    Optional<Tag> t = tagRepository.findByName(tag);

    if (t.isPresent()) {
      return "Уже существует";
    } else {
      tagRepository.save(Tag.builder().name(tag).build());
      return "Ok!";
    }
  }

  public String publish(ParseResult parseResult, MultipartFile[] files) {
    Optional<Cosplayer> cosplayer = cosplayerRepository.findByUid(parseResult.getCosplayerUid());
    if (cosplayer.isEmpty()) {
      cosplayer = cosplayerRepository.findByName(parseResult.getCosplayerName());
    }

    String cosplayerId;
    String cosplayerName;
    if (cosplayer.isEmpty()) {
      Cosplayer cos = cosplayerRepository
              .save(
                  Cosplayer.builder()
                      .name(parseResult.getCosplayerName())
                      .uid(parseResult.getCosplayerUid())
                      .build());
      cosplayerId = cos.getId();
      cosplayerName = cos.getName();
    } else {
      cosplayerId = cosplayer.get().getId();
      cosplayerName = cosplayer.get().getId();
    }

    parseResult.getTags().add(cosplayerName);
    String feedId =
        feedRepository
            .save(
                Feed.builder()
                    .cosplayerId(cosplayerId)
                    .dateCreated(Instant.now())
                    .hidden(true)
                    .tags(parseResult.getTags())
                    .build())
            .getId();

    List<Future<UploadResult>> futures = new ArrayList<>();
    List<String> imgIds = new ArrayList<>();

    if (!ObjectUtils.isEmpty(parseResult.getImages())) {
      for (ParseResult.Image img : parseResult.getImages()) {
        if (!Boolean.TRUE.equals(img.getApproved())) {
          continue;
        }

        String imgId =
            imageDataRepository
                .save(
                    ImageData.builder()
                        .feedId(feedId)
                        .name(img.getName())
                        .hash(img.getHash())
                        .build())
                .getId();
        imgIds.add(imgId);

        futures.add(
            executorService.submit(() -> yandexApi.uploadFile(img.getName(), img.getUrl())));
      }
    }

    String error = "";

    try {
      if (!ObjectUtils.isEmpty(files)) {
        for (MultipartFile img : files) {
          String name = img.getOriginalFilename().replaceAll("[^A-Za-z0-9.]", "");

          BufferedImage buff = ImageIO.read(img.getInputStream());
          RadialHash hash = RadialHashAlgorithm.getHash(HexUtil.convertImg(buff));

          String imgId =
              imageDataRepository
                  .save(ImageData.builder().feedId(feedId).name(name).hash(hash.toString()).build())
                  .getId();
          imgIds.add(imgId);

          futures.add(executorService.submit(() -> yandexApi.uploadFile(name, img)));
        }
      }
    } catch (IOException e) {
      error = "Ошибка: " + e.getClass().getSimpleName() + " " + e.getMessage();
    }

    if (ObjectUtils.isEmpty(error)) {
      try {
        List<UploadResult> asyncUpload =
            futures.stream().map(this::waitingProcessFinishAndGetResult).toList();

        error =
            asyncUpload.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getSuccess()))
                .map(UploadResult::getError)
                .collect(Collectors.joining(", ", "[", "]"));
      } catch (ServiceException e) {
        error = e.getMessage();
      }
    }

    if (ObjectUtils.isEmpty(error) || error.equals("[]")) {
      return "Ok!";
    } else {
      feedRepository.deleteById(feedId);
      imgIds.forEach(imageDataRepository::deleteById);
      return "Имена картинок: "
          + (ObjectUtils.isEmpty(parseResult.getImages())
              ? Arrays.stream(files)
                  .map(MultipartFile::getName)
                  .collect(Collectors.joining(", ", "[", "]"))
              : parseResult.getImages().stream()
                  .map(ParseResult.Image::getName)
                  .collect(Collectors.joining(", ", "[", "]")))
          + ". Не все картинки были загружены: "
          + error;
    }
  }

  public ParseResult parseUrl(String weiboUrl) {
    String url =
        weiboUrl
            .replaceFirst("www.weibo.com", "m.weibo.cn")
            .replaceFirst("weibo.com", "m.weibo.cn");

    try {
      URLConnection connection = new URL(url).openConnection();
      connection.setRequestProperty("User-Agent", USER_AGENT);
      connection.connect();

      BufferedReader br =
          new BufferedReader(
              new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }

      String pics = getPics(sb.toString());
      if (ObjectUtils.isEmpty(pics)) {
        return ParseResult.builder().error("При парсинге не найден список изображений").build();
      }

      String cosplayerName = getCosplayerName(sb.toString());
      if (ObjectUtils.isEmpty(cosplayerName)) {
        return ParseResult.builder().error("При парсинге не найдено имя косплеера").build();
      }

      String cosplayerId = getCosplayerId(sb.toString());
      if (ObjectUtils.isEmpty(cosplayerId)) {
        return ParseResult.builder().error("При парсинге не найдено ID косплеера").build();
      }

      Gson gson = new Gson();
      List<String> imgUrls = gson.fromJson("{" + pics + "}", WeiboImgList.class).getImgUrls();

      List<ImageData> hashes = imageDataRepository.findAll();
      List<ParseResult.Image> images =
          imgUrls.stream()
              .map(item -> executorService.submit(() -> hashImage(item, hashes)))
              .map(this::waitingProcessFinishAndGetResult)
              .toList();

      return ParseResult.builder()
          .images(images)
          .cosplayerName(cosplayerName)
          .cosplayerUid(cosplayerId)
          .build();

    } catch (IOException e) {
      return ParseResult.builder()
          .error(e.getClass().getSimpleName() + " " + e.getMessage())
          .build();
    }
  }

  private String getGroupFromPattern(String text, String regex, int group) {
    Pattern pattern =
        Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    Matcher m = pattern.matcher(text);

    if (m.find()) {
      return m.group(group);
    } else {
      return "";
    }
  }

  public ParseResult.Image hashImage(String item, List<ImageData> hashes) throws IOException {
    String imgName = item.substring(item.lastIndexOf('/') + 1);
    String previewUrl = item.replaceFirst("large", "mw690");

    URLConnection imgConnection = new URL(previewUrl).openConnection();
    imgConnection.setRequestProperty("User-Agent", USER_AGENT);
    imgConnection.setRequestProperty("referer", "https://weibo.com/");
    imgConnection.connect();

    byte[] imgContent = imgConnection.getInputStream().readAllBytes();
    String encodedImg = Base64.getEncoder().encodeToString(imgContent);
    BufferedImage buff = ImageIO.read(new ByteArrayInputStream(imgContent));
    RadialHash hash = RadialHashAlgorithm.getHash(HexUtil.convertImg(buff));

    ParseResult.Image image =
        ParseResult.Image.builder()
            .name(imgName)
            .url(item)
            .preview64(encodedImg)
            .hash(hash.toString())
            .duplicate(false)
            .approved(true)
            .build();

    Optional<ImageData> dup = imageDataRepository.findByName(imgName);
    if (dup.isPresent()) {
      image.setDuplicate(true);
      image.setApproved(false);
    } else {
      image.setSim(checkSimilarity(hashes, hash));
    }

    return image;
  }

  public List<String> uniqueCheck(MultipartFile[] files) {
    List<ImageData> hashes = imageDataRepository.findAll();

    return Arrays.stream(files)
        .map(
            item ->
                executorService.submit(
                    () -> {
                      BufferedImage buff = ImageIO.read(item.getInputStream());
                      RadialHash hash = RadialHashAlgorithm.getHash(HexUtil.convertImg(buff));
                      return checkSimilarity(hashes, hash);
                    }))
        .map(this::waitingProcessFinishAndGetResult)
        .flatMap(List::stream)
        .toList();
  }

  private String getPics(String text) {
    return getGroupFromPattern(text, "\"pics\":.?\\[[^]]++]", 0);
  }

  private String getCosplayerName(String text) {
    return getGroupFromPattern(text, "\"screen_name\": *?\"(.+?)\"", 1);
  }

  private String getCosplayerId(String text) {
    return getGroupFromPattern(text, "\"user\" *?: *?\\{ *?\"id\": ?(.+?),", 1);
  }

  private List<String> checkSimilarity(List<ImageData> hashes, RadialHash hash) {
    List<String> result = new ArrayList<>();
    ListUtils.partition(hashes, SPLIT_SIZE).stream()
        .map(chunk -> executorService.submit(() -> checkChunkForSimilarity(chunk, hash)))
        .toList()
        .forEach(future -> waitingProcessFinishAndGetResult(future).ifPresent(result::add));
    return result;
  }

  private Optional<String> checkChunkForSimilarity(List<ImageData> chunk, RadialHash hash) {
    return chunk.stream()
        .filter(
            h ->
                RadialHashAlgorithm.getSimilarity(hash, RadialHash.fromString(h.getHash())) > 0.969)
        .findFirst()
        .map(ImageData::getName);
  }

  public <T> T waitingProcessFinishAndGetResult(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Ошибка: " + e.getClass().getSimpleName() + " " + e.getMessage());
      throw new ServiceException(e.getClass().getSimpleName() + " " + e.getMessage());
    } catch (ExecutionException e) {
      log.error("Ошибка: " + e.getClass().getSimpleName() + " " + e.getMessage());
      throw new ServiceException(e.getClass().getSimpleName() + " " + e.getMessage());
    }
  }
}
