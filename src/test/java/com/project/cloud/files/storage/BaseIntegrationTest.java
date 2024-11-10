package com.project.cloud.files.storage;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
public abstract class BaseIntegrationTest {

    private static final String MYSQL_IMAGE = "mysql:9.0.0";
    private static final String REDIS_IMAGE = "redis:7.4.0-bookworm";
    private static final String MINIO_IMAGE = "minio/minio:latest";

    @Container
    protected static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>(
            DockerImageName.parse(MYSQL_IMAGE))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withCreateContainerCmdModifier(
                    cmd -> cmd.withName("mysql-test"));

    @Container
    protected static final GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)
            .withCreateContainerCmdModifier(
                    cmd -> cmd.withName("redis-test"));

    @Container
    protected static final MinIOContainer minioContainer = new MinIOContainer(
            DockerImageName.parse(MINIO_IMAGE))
            .withUserName("minioadmin")
            .withPassword("minioadmin")
            .withCreateContainerCmdModifier(
                    cmd -> cmd.withName("minio-test"));


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "");

        registry.add("minio.url", minioContainer::getS3URL);
        registry.add("minio.accessKey", minioContainer::getUserName);
        registry.add("minio.secretKey", minioContainer::getPassword);
        registry.add("minio.bucket", () -> "test-bucket");
    }
}