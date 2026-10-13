"""
Global 4xx alert for a critical, transverse failure (SSO down, broken deployment,
security regression). It fires only when BOTH conditions hold over the window:
  - global 4xx ratio >= error_threshold (e.g. 20%)
  - absolute 4xx count >= min_error_count (e.g. 1000)

Only 400, 401 and 403 are counted. 404, 409, 417 and 429 are expected business
responses (expired link, existing account, PDF not ready, rate limit) and are ignored.
Scope: routes under /api/ only, excluding the hybrid-pap partner client whose
403/404 on /dfc/api/v1/tenants/{tenantId} are chronic and expected.
"""

COUNTED_STATUSES = [400, 401, 403]
EXCLUDED_CLIENTS = ["hybrid-pap"]
DEFAULT_MIN_ERROR_COUNT = 1000
TOP_ROUTES = 5


def run(es, es_index, error_threshold, env, application, start_time, end_time, send_alert, options=None):
    options = options or {}
    min_error_count = int(options.get("min_error_count", DEFAULT_MIN_ERROR_COUNT))

    base_query = {
        "bool": {
            "filter": [
                {"match_phrase": {"environment": env}},
                {"match_phrase": {"application": application}},
                {"prefix": {"normalized_uri.keyword": "/api/"}},
                {"range": {"@timestamp": {"gte": start_time.isoformat(), "lt": end_time.isoformat()}}},
            ],
            "must_not": [{"terms": {"client.keyword": EXCLUDED_CLIENTS}}],
        }
    }

    # One query: total requests, counted 4xx errors, and the top routes among those errors
    query = {
        "size": 0,
        "track_total_hits": True,  # otherwise hits.total is capped at 10000
        "query": base_query,
        "aggs": {
            "errors": {
                "filter": {"terms": {"response_status": COUNTED_STATUSES}},
                "aggs": {
                    "by_status": {"terms": {"field": "response_status", "size": len(COUNTED_STATUSES)}},
                    "top_routes": {
                        "composite": {
                            "size": 1000,
                            "sources": [
                                {"normalized_uri": {"terms": {"field": "normalized_uri.keyword"}}},
                                {"method": {"terms": {"field": "method.keyword"}}},
                            ],
                        }
                    },
                },
            }
        },
    }

    response = es.search(index=es_index, body=query)

    total = response["hits"]["total"]["value"]
    errors_agg = response["aggregations"]["errors"]
    error_count = errors_agg["doc_count"]

    if total == 0:
        return

    ratio = error_count / total
    if ratio < error_threshold or error_count < min_error_count:
        return

    by_status = {b["key"]: b["doc_count"] for b in errors_agg["by_status"]["buckets"]}
    top_routes = sorted(errors_agg["top_routes"]["buckets"], key=lambda b: b["doc_count"], reverse=True)[:TOP_ROUTES]

    window_minutes = int((end_time - start_time).total_seconds() / 60)

    metadata_lines = [
        f"**Environment**: `{env}`",
        f"**Application**: `{application}`",
        f"**Threshold**: `{error_threshold:.0%}` and `{min_error_count}` errors",
        f"**Window**: `{window_minutes} minutes`",
        f"**4xx ratio**: `{ratio:.2%}` ({error_count} / {total} requests)",
        "**By status**: " + ", ".join(f"{s}: {by_status.get(s, 0)}" for s in COUNTED_STATUSES),
    ]
    metadata_text = "\n".join(metadata_lines)

    table_headers = "| Method | Route | 4xx Count |"
    table_divider = "| :--- | :--- | :--- |"
    table_rows = [
        f"| {b['key']['method']} | `{b['key']['normalized_uri']}` | {b['doc_count']} |"
        for b in top_routes
    ]
    table_text = "\n".join([table_headers, table_divider] + table_rows)

    title_text = f"🚨 Critical 4xx Error Rate: {ratio:.1%} on {application}"

    attachments = [
        {
            "fallback": title_text,
            "pretext": f":rotating_light: *Critical 4xx Error Rate: {ratio:.1%} on {application}* (possible SSO outage or broken deployment)",
            "color": "danger",
            "text": f"{metadata_text}\n\nTop routes:\n{table_text}",
            "mrkdwn_in": ["text", "pretext"],
        }
    ]
    send_alert(title_text, attachments)
