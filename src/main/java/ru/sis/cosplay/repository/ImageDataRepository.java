package ru.sis.cosplay.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.sis.cosplay.model.ImageData;

public interface ImageDataRepository extends MongoRepository<ImageData, String> {
  Optional<ImageData> findByName(String name);

  List<ImageData> findByFeedId(String feedId);
}
