package com.bbq.module.flashsale.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class OpenAiRagConfig {

    private final QwenEmbeddingModel embeddingModel;
    private final DataSource dataSource;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 持久化到 MySQL（表名 embedding_store），重启不丢；启动时清空再灌库保证幂等
        MySqlEmbeddingStore store = new MySqlEmbeddingStore(dataSource, "embedding_store");
        store.clear();
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("doc");
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 0))
                .embeddingModel(embeddingModel)          // 使用自动配置的嵌入模型
                .embeddingStore(store)
                .build();
        ingestor.ingest(documents);
        return store;
    }

    @Bean
    public EmbeddingStoreContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)          // 同样注入
                .maxResults(3)
                .build();
    }

}
