"""
Global 500 alert for a real degradation (storage outage, database timeouts,
broken deployment). It fires only when the absolute number of 5xx responses
over the window reaches min_error_count (e.g. 200 per hour).

No per-route ratio: low-traffic routes with 1 error out of 14 requests used to
trigger an alert every hour, which drowned real incidents. The /error fallback
route is ignored. The message lists the top routes to point at the affected area.
"""

EXCLUDED_ROUTES = ["/error"]
DEFAULT_MIN_ERROR_COUNT = 200
TOP_ROUTES = 5


def run(es, es_index, _error_threshold, env, application, start_time, end_time, send_alert, options=None):
    # _error_threshold is kept for the controller call signature; this rule uses an absolute count
    options = options or {}
    min_error_count = int(options.get("min_error_count", DEFAULT_MIN_ERROR_COUNT))

    base_query = {
        "bool": {
            "filter": [
                {"match_phrase": {"environment": env}},
                {"match_phrase": {"application": application}},
                {"range": {"@timestamp": {"gte": start_time.isoformat(), "lt": end_time.isoformat()}}},
            ],
            "must_not": [{"terms": {"normalized_uri.keyword": EXCLUDED_ROUTES}}],
        }
    }

    # One query: total requests, 5xx errors, and the top routes among those errors
    query = {
        "size": 0,
        "track_total_hits": True,  # otherwise hits.total is capped at 10000
        "query": base_query,
        "aggs": {
            "errors": {
                "filter": {"range": {"response_status": {"gte": 500, "lt": 600}}},
                "aggs": {
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

    if total == 0 or error_count < min_error_count:
        return

    ratio = error_count / total
    top_routes = sorted(errors_agg["top_routes"]["buckets"], key=lambda b: b["doc_count"], reverse=True)[:TOP_ROUTES]

    window_minutes = int((end_time - start_time).total_seconds() / 60)

    metadata_lines = [
        f"**Environment**: `{env}`",
        f"**Application**: `{application}`",
        f"**Threshold**: `{min_error_count}` errors",
        f"**Window**: `{window_minutes} minutes`",
        f"**500 errors**: `{error_count}` ({ratio:.2%} of {total} requests)",
    ]
    metadata_text = "\n".join(metadata_lines)

    table_headers = "| Method | Route | 500 Count |"
    table_divider = "| :--- | :--- | :--- |"
    table_rows = [
        f"| {b['key']['method']} | `{b['key']['normalized_uri']}` | {b['doc_count']} |"
        for b in top_routes
    ]
    table_text = "\n".join([table_headers, table_divider] + table_rows)

    title_text = f"🚨 Critical 500 Error Volume: {error_count} errors in {window_minutes} min on {application}"

    attachments = [
        {
            "fallback": title_text,
            "pretext": f":rotating_light: *Critical 500 Error Volume: {error_count} errors in {window_minutes} min on {application}* (possible storage or database outage)",
            "color": "danger",
            "text": f"{metadata_text}\n\nTop routes:\n{table_text}",
            "mrkdwn_in": ["text", "pretext"],
        }
    ]
    send_alert(title_text, attachments)
