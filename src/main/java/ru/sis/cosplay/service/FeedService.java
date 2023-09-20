package ru.sis.cosplay.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.sis.cosplay.model.Feed;
import ru.sis.cosplay.repository.CosplayerRepository;
import ru.sis.cosplay.repository.FeedPageableRepository;
import ru.sis.cosplay.repository.ImageDataRepository;

@Service
@RequiredArgsConstructor
public class FeedService {
  private final ImageDataRepository imageDataRepository;
  private final CosplayerRepository cosplayerRepository;
  private final FeedPageableRepository feedRepository;

  public Page<Feed> findPaginated(Optional<List<String>> tags, Pageable pageable) {
    Page<Feed> feeds;
    if (tags.isEmpty()) {
      feeds = feedRepository.findAll(pageable);
    } else {
      feeds = feedRepository.findAllByTagsIn(tags.get(), pageable);
    }

    feeds.forEach(
        item -> {
          item.setCosplayer(cosplayerRepository.findById(item.getCosplayerId()).orElse(null));
          item.setImages(imageDataRepository.findByFeedId(item.getId()));
        });

    return feeds;
  }
}
