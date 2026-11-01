-- StreamHub Interactions DB (ADR 006). Cassandra is the durable source of truth;
-- Redis is a read model that can be rebuilt from these tables.

CREATE KEYSPACE IF NOT EXISTS streamhub
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

-- Idempotency / raw event log keyed by the client event_id.
CREATE TABLE IF NOT EXISTS streamhub.events_by_id (
  event_id   uuid PRIMARY KEY,
  profile_id text,
  content_id text,
  event_type text,
  payload    text,
  received_at timestamp
);

-- Query-driven history used to rebuild preferences and recommendations.
CREATE TABLE IF NOT EXISTS streamhub.event_history_by_profile (
  profile_id        text,
  event_timestamp   timestamp,
  event_id          uuid,
  event_type        text,
  content_id        text,
  episode_id        text,
  progress_seconds  int,
  rating            text,
  PRIMARY KEY ((profile_id), event_timestamp, event_id)
) WITH CLUSTERING ORDER BY (event_timestamp DESC, event_id ASC);

-- Current playback state.
CREATE TABLE IF NOT EXISTS streamhub.progress_by_profile (
  profile_id            text,
  content_id            text,
  episode_id            text,
  progress_seconds      int,
  last_event_timestamp  timestamp,
  PRIMARY KEY ((profile_id), content_id)
);

-- Latest rating per title (last write wins by event timestamp).
CREATE TABLE IF NOT EXISTS streamhub.ratings_by_profile (
  profile_id  text,
  content_id  text,
  rating      text,
  updated_at  timestamp,
  PRIMARY KEY ((profile_id), content_id)
);

-- Durable preference vector so it does not depend on the Redis cache.
CREATE TABLE IF NOT EXISTS streamhub.prefs_by_profile (
  profile_id  text,
  content_id  text,
  weight      double,
  updated_at  timestamp,
  PRIMARY KEY ((profile_id), content_id)
);

-- Registry of profiles with data, so a full rebuild can enumerate them.
CREATE TABLE IF NOT EXISTS streamhub.profile_registry (
  profile_id    text PRIMARY KEY,
  last_event_at timestamp
);
