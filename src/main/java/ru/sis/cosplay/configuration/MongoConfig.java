package ru.sis.cosplay.configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

  @Value("${mongo.url}")
  private String mongoUrl;

  @Value("${mongo.name}")
  private String mongoName;

  @Override
  @NonNull
  protected String getDatabaseName() {
    return mongoName;
  }

  @Override
  @NonNull
  public MongoClient mongoClient() {
    ConnectionString connectionString = new ConnectionString(mongoUrl);
    MongoClientSettings mongoClientSettings =
        MongoClientSettings.builder().applyConnectionString(connectionString).build();

    return MongoClients.create(mongoClientSettings);
  }
}