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

    # === Step 2: Aggregate error 500 counts per route/method ===
    base_query_errors = {
        "bool": {
            "must": base_filter + [{"range": {"response_status": {"gte": 500, "lt": 600}}}],
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
    flagged_routes = []
    for route_method, error_count in buckets_errors.items():
        total = buckets_total.get(route_method, 0)
        if total == 0:
            continue
        ratio = error_count / total
        if ratio >= error_threshold:
            uri, method = route_method.split("|")
            flagged_routes.append({
                "uri": uri,
                "method": method,
                "total": total,
                "error_count": error_count,
                "ratio": ratio
            })

    if not flagged_routes:
        return

    # Sort routes by error rate descending
    flagged_routes.sort(key=lambda r: r["ratio"], reverse=True)

    window_minutes = int((end_time - start_time).total_seconds() / 60)

    metadata_lines = [
        f"**Environment**: `{env}`",
        f"**Application**: `{application}`",
        f"**Threshold**: `{error_threshold:.2%}`",
        f"**Window**: `{window_minutes} minutes`",
    ]
    metadata_text = "\n".join(metadata_lines)

    table_headers = "| Method | Route | Error Rate | 500 Count | Total Requests |"
    table_divider = "| :--- | :--- | :--- | :--- | :--- |"
    table_rows = [
        f"| {r['method']} | `{r['uri']}` | {r['ratio']:.2%} | {r['error_count']} | {r['total']} |"
        for r in flagged_routes
    ]
    table_text = "\n".join([table_headers, table_divider] + table_rows)

    title_text = f"📛 High 500 Error Rate Detected ({len(flagged_routes)} routes affected)"

    # Corps de l'attachement : métadonnées + tableau uniquement
    attachment_text = f"{metadata_text}\n\n{table_text}"

    attachments = [
        {
            "fallback": title_text,
            "pretext": f":warning: *High 500 Error Rate Detected ({len(flagged_routes)} routes affected)*",
            "color": "danger",
            "text": attachment_text,
            "mrkdwn_in": ["text", "pretext"],
        }
    ]
    send_alert(title_text, attachments)
