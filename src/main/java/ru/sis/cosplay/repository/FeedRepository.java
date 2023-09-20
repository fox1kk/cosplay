package ru.sis.cosplay.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.sis.cosplay.model.Feed;

public interface FeedRepository extends MongoRepository<Feed, String> {}
