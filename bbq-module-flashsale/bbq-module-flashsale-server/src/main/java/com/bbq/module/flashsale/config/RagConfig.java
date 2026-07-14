package com.bbq.module.flashsale.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Bean
    public DashScopeApi dashScopeApi() {
        return DashScopeApi.builder().apiKey(apiKey).build();
    }

    @Bean
    public DocumentRetriever documentRetriever(DashScopeApi dashScopeApi) {
        DashScopeDocumentRetrieverOptions options = DashScopeDocumentRetrieverOptions.builder()
                .indexName("bbq")
                .denseSimilarityTopK(5)
                .sparseSimilarityTopK(5)
                .rerankMinScore(0.7f)
                .build();
        return new DashScopeDocumentRetriever(dashScopeApi, options);
    }
}
