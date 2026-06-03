package com.idealagent.config;

import com.idealagent.domain.ai.repository.IRagTagRepository;
import com.idealagent.domain.ai.service.rag.IRagVectorStore;
import com.idealagent.infrastructure.rag.PgVectorRagTagRepository;
import com.idealagent.properties.EmbeddingProperties;
import com.idealagent.properties.HikariDataSourceProperties;
import com.idealagent.properties.JwtProperties;
import com.idealagent.properties.MySQLProperties;
import com.idealagent.properties.PostgreSQLProperties;
import com.idealagent.infrastructure.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AppInfrastructureConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(DataSourceConfig.class, MyBatisConfig.class)
            .withPropertyValues(
                    "datasource.hikari.minimum-idle=1",
                    "datasource.hikari.maximum-pool-size=2",
                    "datasource.hikari.idle-timeout=180000",
                    "datasource.hikari.max-lifetime=1800000",
                    "datasource.hikari.connection-timeout=30000",
                    "datasource.hikari.connection-test-query=SELECT 1",
                    "datasource.hikari.initialization-fail-timeout=-1",
                    "datasource.mysql.username=root",
                    "datasource.mysql.password=123456",
                    "datasource.mysql.driver-class-name=com.mysql.cj.jdbc.Driver",
                    "datasource.mysql.jdbc-url=jdbc:mysql://127.0.0.1:13306/ideal_agent",
                    "datasource.mysql.pool-name=MYSQL_HIKARI",
                    "datasource.postgresql.username=idealagent",
                    "datasource.postgresql.password=123456",
                    "datasource.postgresql.driver-class-name=org.postgresql.Driver",
                    "datasource.postgresql.jdbc-url=jdbc:postgresql://127.0.0.1:5432/ideal_agent",
                    "datasource.postgresql.pool-name=POSTGRESQL_HIKARI"
            );

    @Test
    void appConfigCreatesMiniAgentStyleDataSourceBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MySQLProperties.class);
            assertThat(context).hasSingleBean(PostgreSQLProperties.class);
            assertThat(context).hasSingleBean(HikariDataSourceProperties.class);
            assertThat(context).hasBean("mysqlDataSource");
            assertThat(context).hasBean("postgresqlDataSource");
            assertThat(context).hasBean("postgresqlTemplate");
            assertThat(context.getBean("mysqlDataSource", DataSource.class)).isNotNull();
            assertThat(context.getBean("postgresqlTemplate", JdbcTemplate.class)).isNotNull();
            assertThat(context.getBean("mysqlTemplate", SqlSessionTemplate.class)).isNotNull();
        });
    }

    @Test
    void embeddingPropertiesBindMiniAgentStyleValues() {
        new ApplicationContextRunner()
                .withUserConfiguration(EmbeddingPropertiesConfig.class)
                .withPropertyValues(
                        "ideal-agent.embedding.base-url=https://api.example.com",
                        "ideal-agent.embedding.api-key=sk-test",
                        "ideal-agent.embedding.model=text-embedding-v4",
                        "ideal-agent.embedding.dimensions=1024",
                        "ideal-agent.embedding.encoding-format=float",
                        "ideal-agent.embedding.embeddings-path=/v1/embeddings",
                        "ideal-agent.embedding.schema-name=public",
                        "ideal-agent.embedding.table-name=vector_store_openai"
                )
                .run(context -> {
                    EmbeddingProperties properties = context.getBean(EmbeddingProperties.class);
                    assertThat(properties.getBaseUrl()).isEqualTo("https://api.example.com");
                    assertThat(properties.getApiKey()).isEqualTo("sk-test");
                    assertThat(properties.getModel()).isEqualTo("text-embedding-v4");
                    assertThat(properties.getDimensions()).isEqualTo(1024);
                    assertThat(properties.getEncodingFormat()).isEqualTo("float");
                    assertThat(properties.getEmbeddingsPath()).isEqualTo("/v1/embeddings");
                    assertThat(properties.getSchemaName()).isEqualTo("public");
                    assertThat(properties.getTableName()).isEqualTo("vector_store_openai");
                    assertThat(properties.tableRef()).isEqualTo("public.vector_store_openai");
                });
    }

    @Test
    void embeddingPropertiesRejectUnsafeTableIdentifiers() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setSchemaName("public");
        properties.setTableName("vector_store_openai;drop table x");

        assertThatThrownBy(properties::tableRef)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableName");
    }

    @Test
    void applicationDevDisablesUnusedOpenAiAutoConfiguration() {
        Properties properties = loadYamlProperties("application-dev.yml");

        assertThat(properties)
                .containsEntry("spring.ai.model.chat", "none")
                .containsEntry("spring.ai.model.embedding", "none")
                .containsEntry("spring.ai.model.audio.speech", "none")
                .containsEntry("spring.ai.model.audio.transcription", "none")
                .containsEntry("spring.ai.model.image", "none")
                .containsEntry("spring.ai.model.moderation", "none");
    }

    @Test
    void pgVectorStoreConfigCreatesOnlyTokenTextSplitterWithoutPostgresqlTemplate() {
        new ApplicationContextRunner()
                .withUserConfiguration(PgVectorStoreConfig.class)
                .withPropertyValues(
                        "ideal-agent.embedding.base-url=https://api.example.com",
                        "ideal-agent.embedding.api-key=sk-test",
                        "ideal-agent.embedding.model=text-embedding-v4",
                        "ideal-agent.embedding.dimensions=1024",
                        "ideal-agent.embedding.encoding-format=float",
                        "ideal-agent.embedding.embeddings-path=/v1/embeddings",
                        "ideal-agent.embedding.schema-name=public",
                        "ideal-agent.embedding.table-name=vector_store_openai"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(org.springframework.ai.transformer.splitter.TokenTextSplitter.class);
                    assertThat(context).doesNotHaveBean(PgVectorStore.class);
                    assertThat(context).doesNotHaveBean(IRagVectorStore.class);
                    assertThat(context).doesNotHaveBean(IRagTagRepository.class);
                });
    }

    @Test
    void pgVectorStoreConfigCreatesPgVectorRagBeansWithPostgresqlTemplate() {
        new ApplicationContextRunner()
                .withUserConfiguration(PgVectorStoreConfig.class)
                .withBean("postgresqlTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withPropertyValues(
                        "ideal-agent.embedding.base-url=https://api.example.com",
                        "ideal-agent.embedding.api-key=sk-test",
                        "ideal-agent.embedding.model=text-embedding-v4",
                        "ideal-agent.embedding.dimensions=1024",
                        "ideal-agent.embedding.encoding-format=float",
                        "ideal-agent.embedding.embeddings-path=/v1/embeddings",
                        "ideal-agent.embedding.schema-name=public",
                        "ideal-agent.embedding.table-name=vector_store_openai"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PgVectorStore.class);
                    assertThat(context).hasSingleBean(IRagVectorStore.class);
                    assertThat(context).hasSingleBean(IRagTagRepository.class);
                    assertThat(context.getBean(IRagTagRepository.class)).isInstanceOf(PgVectorRagTagRepository.class);
                });
    }

    @Test
    void jwtConfigCreatesTokenBeansFromProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtConfig.class)
                .withPropertyValues(
                        "ideal-agent.jwt.issuer=ideal-agent-test",
                        "ideal-agent.jwt.secret=test-secret",
                        "ideal-agent.jwt.expire-hours=24"
                )
                .run(context -> {
                    JwtProperties properties = context.getBean(JwtProperties.class);
                    assertThat(properties.getIssuer()).isEqualTo("ideal-agent-test");
                    assertThat(context).hasSingleBean(JwtUtil.class);
                    assertThat(context).hasBean("tokenService");
                    assertThat(context).hasBean("tokenParser");
                });
    }

    @Configuration
    @EnableConfigurationProperties(EmbeddingProperties.class)
    static class EmbeddingPropertiesConfig {
    }

    private Properties loadYamlProperties(String resourceName) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        return factory.getObject();
    }
}
