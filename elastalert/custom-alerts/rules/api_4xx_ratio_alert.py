def run(es, es_index, error_threshold, env, application, start_time, end_time, send_alert):

    # Build base query for all matching logs
    base_filter = [
        {"match_phrase": {"environment": env}},
        {"match_phrase": {"application": application}},
        {"range": {"@timestamp": {"gte": start_time.isoformat(), "lt": end_time.isoformat()}}}
    ]

    base_query = {"bool": {"filter": base_filter}}

    # === Step 1: Aggregate total counts per route/method ===
    agg_query_total = {
        "size": 0,
        "query": base_query,
        "aggs": {
            "routes": {
                "composite": {
                    "size": 1000,
                    "sources": [
                        {"normalized_uri": {"terms": {"field": "normalized_uri.keyword"}}},
                        {"method": {"terms": {"field": "method.keyword"}}}
                    ]
                }
            }
        }
    }

    response_total = es.search(
        index=es_index,
        body=agg_query_total
    )

    buckets_total = {f"{b['key']['normalized_uri']}|{b['key']['method']}": b['doc_count']
                     for b in response_total['aggregations']['routes']['buckets']}

    # === Step 2: Aggregate error 4xx counts (excluding 401, 404, 409) per route/method ===
    base_query_errors = {
        "bool": {
            "must": base_filter + [{"range": {"response_status": {"gte": 400, "lt": 500}}}],
            "must_not": [{"terms": {"response_status": [401, 404, 409]}}]
        }
    }

    agg_query_errors = {
        "size": 0,
        "query": base_query_errors,
        "aggs": {
            "routes": {
                "composite": {
                    "size": 1000,
                    "sources": [
                        {"normalized_uri": {"terms": {"field": "normalized_uri.keyword"}}},
                        {"method": {"terms": {"field": "method.keyword"}}}
                    ]
                }
            }
        }
    }

    response_errors = es.search(index=es_index, body=agg_query_errors)
    buckets_errors = {f"{b['key']['normalized_uri']}|{b['key']['method']}": b['doc_count']
                      for b in response_errors['aggregations']['routes']['buckets']}

    # === Step 3: Compute and alert on high ratios ===
    for route_method, error_count in buckets_errors.items():
        total = buckets_total.get(route_method, 0)
        if total == 0:
            continue
        ratio = error_count / total
        if ratio >= error_threshold:
            uri, method = route_method.split("|")
            alert_msg = (
                f"**Environment**: `{env}`\n"
                f"**Application**: `{application}`\n"
                f"**URI**: `{uri}`\n"
                f"**Method**: `{method}`\n"
                f"**Total Requests**: {total}\n"
                f"**4xx Errors**: {error_count}\n"
                f"**Error Rate**: {ratio:.2%} over the last {int((end_time - start_time).total_seconds() / 60)} minutes"
            )
            send_alert("High 4xx Error Rate Detected", alert_msg, "warning")
