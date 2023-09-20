package ru.sis.cosplay.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.sis.cosplay.model.Cosplayer;

public interface CosplayerRepository extends MongoRepository<Cosplayer, String> {
  Optional<Cosplayer> findByUid(String uid);
  Optional<Cosplayer> findByName(String name);
}
