package fr.dossierfacile.dfcdemo.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WebhookLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insert(String body) {
        jdbcTemplate.update("INSERT INTO webhook_log (body) VALUES (?)", body);
    }

    public List<WebhookLog> findLatest(int limit) {
        return jdbcTemplate.query(
                "SELECT id, created_date, body FROM webhook_log ORDER BY created_date DESC LIMIT ?",
                (rs, rowNum) -> new WebhookLog(
                        rs.getLong("id"),
                        rs.getTimestamp("created_date").toLocalDateTime(),
                        rs.getString("body")),
                limit);
    }
}
