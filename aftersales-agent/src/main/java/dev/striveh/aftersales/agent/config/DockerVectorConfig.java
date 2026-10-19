package dev.striveh.aftersales.agent.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import dev.striveh.aftersales.agent.retrieval.HashingEmbeddingModel;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("docker")
public class DockerVectorConfig {

    @Bean
    DataSource dataSource(@Value("${spring.datasource.url}") String url,
                          @Value("${spring.datasource.username}") String username,
                          @Value("${spring.datasource.password}") String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitializationFailTimeout(60_000);
        return dataSource;
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    PgVectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) throws Exception {
        PgVectorStore store = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(HashingEmbeddingModel.DIMENSIONS)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
        if (store instanceof InitializingBean initializing) {
            initializing.afterPropertiesSet();
        }
        return store;
    }
}
