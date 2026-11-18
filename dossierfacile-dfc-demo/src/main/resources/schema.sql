-- Same table as the original PHP demo so an existing database keeps its history
CREATE TABLE IF NOT EXISTS webhook_log (
    id SERIAL PRIMARY KEY,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    body TEXT NOT NULL
);
