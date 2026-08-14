package com.bbq.module.flashsale.config;

import com.alibaba.fastjson2.JSON;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 MySQL 的 langchain4j EmbeddingStore 实现。
 * 向量以逗号分隔的 float 字符串存于 TEXT 列，相似度（余弦）在内存中计算，
 * 适合 BBQ 这种中小体量、且无需 MySQL 8 原生向量特性的知识库。
 */
public class MySqlEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final DataSource dataSource;
    private final String table;

    public MySqlEmbeddingStore(DataSource dataSource, String table) {
        this.dataSource = dataSource;
        this.table = table;
        initTable();
    }

    private void initTable() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "embedding TEXT NOT NULL, "
                    + "content TEXT, "
                    + "metadata TEXT)");
        } catch (Exception e) {
            throw new IllegalStateException("初始化向量表失败: " + table, e);
        }
    }

    public void clear() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DELETE FROM " + table);
        } catch (Exception e) {
            throw new IllegalStateException("清空向量表失败: " + table, e);
        }
    }

    @Override
    public String add(Embedding embedding) {
        return add(embedding, null);
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment embedded) {
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, embedded);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return addAll(embeddings, null);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            TextSegment seg = (embedded != null && i < embedded.size()) ? embedded.get(i) : null;
            ids.add(add(embeddings.get(i), seg));
        }
        return ids;
    }

    private void addInternal(String id, Embedding embedding, TextSegment embedded) {
        String vec = encode(embedding.vector());
        String content = embedded != null ? embedded.text() : null;
        String meta = (embedded != null && embedded.metadata() != null)
                ? JSON.toJSONString(embedded.metadata().toMap()) : "{}";
        String sql = "INSERT INTO " + table + " (id, embedding, content, metadata) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE embedding=?, content=?, metadata=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, vec);
            ps.setString(3, content);
            ps.setString(4, meta);
            ps.setString(5, vec);
            ps.setString(6, content);
            ps.setString(7, meta);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("写入向量失败", e);
        }
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        float[] query = request.queryEmbedding().vector();
        int max = request.maxResults();
        double minScore = request.minScore();
        String sql = "SELECT id, embedding, content, metadata FROM " + table;
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                float[] vec = decode(rs.getString("embedding"));
                double score = cosine(query, vec);
                if (score >= minScore) {
                    TextSegment seg = TextSegment.from(
                            rs.getString("content"),
                            parseMeta(rs.getString("metadata")));
                    matches.add(new EmbeddingMatch(score, rs.getString("id"), Embedding.from(vec), seg));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("检索向量失败", e);
        }
        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (matches.size() > max) {
            matches = matches.subList(0, max);
        }
        return new EmbeddingSearchResult<>(matches);
    }

    private static String encode(float[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.toString();
    }

    private static float[] decode(String s) {
        if (s == null || s.isEmpty()) {
            return new float[0];
        }
        String[] parts = s.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            v[i] = Float.parseFloat(parts[i]);
        }
        return v;
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static Metadata parseMeta(String s) {
        if (s == null || s.isEmpty() || "{}".equals(s)) {
            return new Metadata();
        }
        return Metadata.from(JSON.parseObject(s, Map.class));
    }
}
