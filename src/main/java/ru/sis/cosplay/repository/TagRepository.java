package ru.sis.cosplay.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.sis.cosplay.model.Tag;

public interface TagRepository extends MongoRepository<Tag, String> {
    Optional<Tag> findByName(String name);
}
