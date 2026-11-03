package com.kazi.kazi.search;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JobEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    JobEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findContentHash(UUID jobId, String model) {
        List<String> hashes = jdbcTemplate.query(
            "SELECT content_hash FROM job_embedding WHERE job_id = ? AND model = ?",
            (resultSet, rowNum) -> resultSet.getString(1),
            jobId,
            model
        );
        return hashes.stream().findFirst();
    }

    public void save(UUID jobId, String model, String contentHash, float[] embedding, Instant now) {
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO job_embedding (job_id, model, content_hash, embedding, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (job_id) DO UPDATE SET
                    model = EXCLUDED.model,
                    content_hash = EXCLUDED.content_hash,
                    embedding = EXCLUDED.embedding,
                    updated_at = EXCLUDED.updated_at
                """);
            statement.setObject(1, jobId);
            statement.setString(2, model);
            statement.setString(3, contentHash);
            statement.setObject(4, vector(embedding));
            statement.setObject(5, java.sql.Timestamp.from(now));
            return statement;
        });
    }

    public List<UUID> search(float[] embedding, int limit) {
        return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                SELECT e.job_id FROM job_embedding e
                JOIN job j ON j.id = e.job_id
                WHERE j.status = 'active'
                ORDER BY e.embedding <=> ?
                LIMIT ?
                """);
            statement.setObject(1, vector(embedding));
            statement.setInt(2, limit);
            return statement;
        }, (resultSet, rowNum) -> resultSet.getObject(1, UUID.class));
    }

    private PGobject vector(float[] embedding) {
        try {
            String[] values = new String[embedding.length];
            for (int index = 0; index < embedding.length; index++) {
                values[index] = Float.toString(embedding[index]);
            }
            var vector = new PGobject();
            vector.setType("vector");
            vector.setValue("[" + String.join(",", values) + "]");
            return vector;
        } catch (java.sql.SQLException exception) {
            throw new IllegalArgumentException("Invalid embedding vector", exception);
        }
    }
}
