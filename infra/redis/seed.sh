#!/bin/sh
# Development Home views for the local stack. They mirror what the recommendation
# worker writes (spec.md 2.4). The catalog service fills `categories` with Trending
# because the worker leaves that list empty.
set -eu

REDIS_HOST="${REDIS_HOST:-redis}"

HOME_PROF_9921='{"profile_id":"prof_9921","generated_at":"2026-10-06T19:58:00Z","categories":[],"continue_watching":[{"content_id":"mov_8832","episode_id":"ep_102","progress_seconds":1420},{"content_id":"mov_77","progress_seconds":3200}],"recommended":["mov_8832","mov_77","mov_91","mov_42","mov_105","mov_64"]}'
HOME_PROF_9922='{"profile_id":"prof_9922","generated_at":"2026-10-06T19:58:00Z","categories":[],"continue_watching":[{"content_id":"mov_105","progress_seconds":900}],"recommended":["mov_105","mov_31","mov_2","mov_58"]}'
HOME_PROF_9931='{"profile_id":"prof_9931","generated_at":"2026-10-06T19:58:00Z","categories":[],"continue_watching":[{"content_id":"mov_1","progress_seconds":600}],"recommended":["mov_1","mov_58","mov_64","mov_12","mov_7"]}'

set_view() {
  profile_id="$1"
  payload="$2"
  redis-cli -h "$REDIS_HOST" SET "home:${profile_id}" "$payload" >/dev/null
  redis-cli -h "$REDIS_HOST" EXPIRE "home:${profile_id}" 86400 >/dev/null
  echo "Seeded home:${profile_id}"
}

set_view "prof_9921" "$HOME_PROF_9921"
set_view "prof_9922" "$HOME_PROF_9922"
set_view "prof_9931" "$HOME_PROF_9931"
