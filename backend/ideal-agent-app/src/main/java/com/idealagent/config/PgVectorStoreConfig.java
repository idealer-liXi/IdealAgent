package com.idealagent.config;

import com.idealagent.domain.ai.repository.IRagTagRepository;
import com.idealagent.domain.ai.service.rag.IRagVectorStore;
import com.idealagent.infrastructure.rag.PgVectorRagTagRepository;
import com.idealagent.infrastructure.rag.PgVectorStoreAdapter;
import com.idealagent.properties.EmbeddingProperties;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class PgVectorStoreConfig {
    @Bean
    @Primary
    @ConditionalOnBean(name = "postgresqlTemplate")
    public PgVectorStore pgVectorStore(EmbeddingProperties embeddingProperties, @Qualifier("postgresqlTemplate") JdbcTemplate jdbcTemplate) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(embeddingProperties.getBaseUrl())
                .apiKey(embeddingProperties.getApiKey())
                .build();
        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(embeddingProperties.getModel())
                .dimensions(embeddingProperties.getDimensions())
                .encodingFormat(embeddingProperties.getEncodingFormat())
                .build();
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, embeddingOptions);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(false)
                .schemaName(embeddingProperties.getSchemaName())
                .vectorTableName(embeddingProperties.getTableName())
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    @ConditionalOnBean(PgVectorStore.class)
    public IRagVectorStore ragVectorStore(PgVectorStore pgVectorStore) {
        return new PgVectorStoreAdapter(pgVectorStore);
    }

    @Bean
    @ConditionalOnBean(name = "postgresqlTemplate")
    public IRagTagRepository ragTagRepository(@Qualifier("postgresqlTemplate") JdbcTemplate jdbcTemplate, EmbeddingProperties embeddingProperties) {
        return new PgVectorRagTagRepository(jdbcTemplate, embeddingProperties.tableRef());
    }
}
