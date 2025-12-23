from utils import build_table, build_alert_msg


def run(es, es_index, error_threshold, env, application, start_time, end_time, send_alert):

    allowed_referers = [
        "cdn.dossierfacile.logement.gouv.fr",
        "http://cdn.dossierfacile.logement.gouv.fr",
        "https://cdn.dossierfacile.logement.gouv.fr",
    ]

    # Build base query for all matching logs in the time window,
    # excluding the "official" CDN referers.
    base_query = {
        "bool": {
            "filter": [
                {"match_phrase": {"process.name": application}},
                {"range": {"@timestamp": {"gte": start_time.isoformat(), "lt": end_time.isoformat()}}},
            ],
            "must_not": [
                {"terms": {"referer.keyword": allowed_referers}},
            ],
        }
    }

    # Aggregate counts grouped by referer and IP
    agg_query = {
        "size": 0,
        "query": base_query,
        "aggs": {
            "referers": {
                "terms": {
                    "field": "referer.keyword",
                    "size": 50,
                    "missing": "(missing)",
                    "order": {"_count": "desc"},
                },
                "aggs": {
                    "remote_addrs": {
                        "terms": {
                            "field": "remote_addr.keyword",
                            "size": 50,
                            "missing": "(missing)",
                            "order": {"_count": "desc"},
                        }
                    }
                }
            },
        },
    }

    response = es.search(index=es_index, body=agg_query)

    # Extract nested buckets
    referer_buckets = response.get("aggregations", {}).get("referers", {}).get("buckets", [])
    
    if not referer_buckets:
        return
    
    # Flatten nested structure and filter by threshold
    rows = []
    for referer_bucket in referer_buckets:
        referer = referer_bucket.get("key") or "(missing)"
        ip_buckets = referer_bucket.get("remote_addrs", {}).get("buckets", [])
        
        for ip_bucket in ip_buckets:
            ip = ip_bucket.get("key") or "(missing)"
            hits = ip_bucket.get("doc_count", 0)
            
            if hits >= error_threshold:
                rows.append([referer, ip, hits])
    
    if not rows:
        return

    # Sort by hits descending
    rows.sort(key=lambda x: x[2], reverse=True)

    # Build table using generic utility
    table = build_table(
        title="Hits by Referer and IP",
        headers=["Referer", "IP", "Hits"],
        rows=rows,
        right_align_last=True
    )

    # Prepare metadata and info
    time_window = int((end_time - start_time).total_seconds() / 60)
    start_time_str = start_time.strftime("%Y-%m-%d %H:%M:%S UTC")
    end_time_str = end_time.strftime("%Y-%m-%d %H:%M:%S UTC")

    metadata = {
        "env": env,
        "application": application,
        "time_window": time_window
    }

    # Build alert message using generic utility
    alert_msg = build_alert_msg(
        metadata=metadata,
        tables=[table]
    )

    send_alert(
        f"CDN suspicious traffic detected between {start_time_str} and {end_time_str}",
        alert_msg,
        "warning",
    )
