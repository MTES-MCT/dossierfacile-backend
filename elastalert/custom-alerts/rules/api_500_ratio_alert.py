from utils import build_table, build_alert_msg


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
                        {"method": {"terms": {"field": "method.keyword"}}},
                        {"status_code": {"terms": {"field": "response_status"}}}
                    ]
                }
            }
        }
    }

    response_errors = es.search(index=es_index, body=agg_query_errors)

    # Structure: {uri|method: {status_code: count}}
    errors_by_route_status = {}
    for b in response_errors['aggregations']['routes']['buckets']:
        uri = b['key']['normalized_uri']
        method = b['key']['method']
        status_code = b['key']['status_code']
        count = b['doc_count']
        route_key = f"{uri}|{method}"
        
        if route_key not in errors_by_route_status:
            errors_by_route_status[route_key] = {}
        errors_by_route_status[route_key][status_code] = count
    
    # === Step 3: Compute ratios and collect issues that exceed threshold ===
    issues = []  # List of (uri, method, status_code, total_requests, error_count_for_status)
    
    for route_method, status_counts in errors_by_route_status.items():
        uri, method = route_method.split("|")
        total_requests = buckets_total.get(route_method, 0)
        if total_requests == 0:
            continue
        
        # Calculate total error count for this route (sum of all status codes)
        total_errors_all_status = sum(status_counts.values())
        ratio = total_errors_all_status / total_requests
        
        # Only include routes that exceed the threshold
        if ratio >= error_threshold:
            # Add each status code as a separate issue entry
            for status_code, error_count_for_status in status_counts.items():
                issues.append((uri, method, status_code, total_requests, error_count_for_status))

    # === Step 4: Group issues by URI and status_code, then send single alert ===
    if issues:
        # Group by (uri, status_code) and aggregate totals
        grouped_issues = {}
        for uri, method, status_code, total_requests, error_count in issues:
            key = (uri, status_code)
            if key not in grouped_issues:
                grouped_issues[key] = {
                    'uri': uri,
                    'status_code': status_code,
                    'methods': set(),
                    'total_requests': 0,
                    'total_errors': 0
                }
            grouped_issues[key]['methods'].add(method)
            # Aggregate totals across all methods for this URI
            grouped_issues[key]['total_requests'] += total_requests
            grouped_issues[key]['total_errors'] += error_count
        
        # Calculate ratio for each grouped issue
        for issue_data in grouped_issues.values():
            if issue_data['total_requests'] > 0:
                issue_data['ratio'] = issue_data['total_errors'] / issue_data['total_requests']
            else:
                issue_data['ratio'] = 0

        # Sort by total_requests (descending) and build rows
        sorted_issues = sorted(grouped_issues.items(), key=lambda x: x[1]['total_requests'], reverse=True)
        
        rows = []
        for (uri, status_code), issue_data in sorted_issues:
            methods_str = ", ".join(sorted(issue_data['methods']))
            rows.append([
                uri,
                status_code,
                methods_str,
                issue_data['total_requests'],
                issue_data['total_errors'],
                f"{issue_data['ratio']:.2%}"
            ])

        # Build table using utility
        table = build_table(
            title="5xx Errors by Route",
            headers=["URI", "Status Code", "Methods", "Total Requests", "5xx Errors", "Error Rate"],
            rows=rows,
            right_align_last=True
        )

        # Build alert message using utility
        time_window = int((end_time - start_time).total_seconds() / 60)
        alert_msg = build_alert_msg(
            metadata={
                "env": env,
                "application": application,
                "time_window": time_window
            },
            tables=[table]
        )

        start_time_str = start_time.strftime("%Y-%m-%d %H:%M:%S UTC")
        end_time_str = end_time.strftime("%Y-%m-%d %H:%M:%S UTC")
        send_alert(f"High 5xx Error Rate Detected between {start_time_str} and {end_time_str}", alert_msg)

