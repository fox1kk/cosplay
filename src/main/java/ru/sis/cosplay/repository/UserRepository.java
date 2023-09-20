package ru.sis.cosplay.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.sis.cosplay.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findUserByUsername(String username);
}
