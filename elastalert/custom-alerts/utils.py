"""
Utility functions for alert rules.
"""


def build_table(title, headers, rows, right_align_last=False):
    """
    Build a markdown table with any number of columns.
    
    Args:
        title: Table title (markdown header)
        headers: List of column headers
        rows: List of rows, where each row is a list/tuple of values
        right_align_last: If True, right-align the last column
    
    Returns:
        Markdown formatted table string
    """
    if not headers:
        return ""
    
    num_cols = len(headers)
    lines = [f"### {title}"]
    
    # Build header row
    header_row = "| " + " | ".join(headers) + " |"
    lines.append(header_row)
    
    # Build separator row
    if right_align_last and num_cols > 1:
        # Right-align last column, left-align others
        separator_parts = ["---"] * (num_cols - 1) + ["---:|"]
        separator = "|" + "|".join(separator_parts) + "|"
    else:
        # Left-align all columns
        separator = "|" + "|".join(["---"] * num_cols) + "|"
    lines.append(separator)
    
    # Build data rows
    for row in rows:
        # Ensure row has the same number of columns as headers
        row_values = list(row)[:num_cols]
        if len(row_values) < num_cols:
            row_values.extend([""] * (num_cols - len(row_values)))
        
        # Format each cell
        formatted_cells = []
        for val in row_values:
            val_str = str(val) if val is not None else "(missing)"
            formatted_cells.append(f"`{val_str}`")
        
        lines.append("| " + " | ".join(formatted_cells) + " |")
    
    return "\n".join(lines)


def build_alert_msg(metadata, info_lines=None, tables=None):
    """
    Build a complete alert message with metadata, info lines, and tables.
    
    Args:
        metadata: Dict with keys like 'env', 'application', 'time_window', etc.
        info_lines: List of additional info lines to include
        tables: List of table strings (from build_table)
    
    Returns:
        Complete alert message string
    """
    alert_lines = []
    
    # Add metadata
    if metadata.get("env"):
        alert_lines.append(f"**Environment**: `{metadata['env']}`")
    if metadata.get("application"):
        alert_lines.append(f"**Application**: `{metadata['application']}`")
    if metadata.get("time_window"):
        alert_lines.append(f"**Time Window**: {metadata['time_window']} minutes")
    
    # Add additional info lines
    if info_lines:
        if alert_lines:  # Add separator if we have metadata
            alert_lines.append("")
        alert_lines.extend(info_lines)
    
    # Add tables
    if tables:
        if alert_lines:  # Add separator if we have previous content
            alert_lines.append("")
        alert_lines.extend(tables)
    
    return "\n".join(alert_lines)

