package ru.sis.cosplay.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.sis.cosplay.model.Feed;

public interface FeedPageableRepository extends PagingAndSortingRepository<Feed, String> {
  Page<Feed> findAll(Pageable pageable);

  Page<Feed> findAllByTagsIn(List<String> tags, Pageable pageable);
}
