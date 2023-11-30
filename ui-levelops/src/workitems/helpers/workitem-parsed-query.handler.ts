export const queryParamsToParse: string[] = [
  "product_ids",
  "updated_at",
  "updated_end",
  "reporter",
  "assignee_user_ids",
  "tag_ids",
  "status",
  "priority",
  "unassigned",
  "created_after"
];

export const handleParsedQueryParams = (filters: any) => {
  if (filters) {
    const {
      product_ids,
      updated_at,
      reporter,
      assignee_user_ids,
      tag_ids,
      status,
      priority,
      updated_end,
      unassigned,
      created_after
    } = filters;
    if (created_after && created_after.length > 0) {
      filters["created_after"] = parseInt(created_after[0]);
    }
    if (unassigned && unassigned.length > 0) {
      filters["unassigned"] = unassigned[0] === "true";
    }
    if (product_ids && product_ids.length) {
      filters["product_ids"] = product_ids.map((id: any) => ({ key: id }));
    }
    if (assignee_user_ids && assignee_user_ids.length) {
      filters["assignee_user_ids"] = assignee_user_ids.map((id: any) => ({ key: id }));
    }
    if (status && status.length > 0) {
      filters["status"] = status[0];
    }
    if (reporter && reporter.length) {
      filters["reporter"] = reporter[0];
    }
    if (tag_ids && tag_ids.length) {
      filters["tag_ids"] = tag_ids.map((id: any) => ({ key: id }));
    }
    if (priority && priority.length) {
      filters["priority"] = priority[0];
    }
    if (updated_at && updated_end) {
      delete filters.updated_end;
      filters["updated_at"] = {
        $gt: updated_at[0],
        $lt: updated_end[0]
      };
    }
  }
  return filters;
};

export const queryParamsFromFilters = (filters: any, tab: string, moreFilters?: any) => {
  if (!filters) {
    if (!moreFilters) {
      return {
        tab
      };
    }
    filters = moreFilters;
  }
  const {
    product_ids,
    updated_at,
    updated_end,
    reporter,
    assignee_user_ids,
    tag_ids,
    status,
    priority,
    unassigned,
    created_after,
    filter_name
  } = filters;
  const updated_st = updated_at?.$gt ? updated_at.$gt : updated_at || "";
  const updatedEnd = updated_end ? updated_end : updated_at?.$lt || "";
  return {
    created_after: created_after !== undefined ? created_after.toString() : undefined,
    unassigned: unassigned !== undefined ? "true" : undefined,
    updated_at: updated_st,
    updated_end: updatedEnd,
    product_ids: product_ids && (product_ids || []).map((p: any) => p.key || p),
    assignee_user_ids: assignee_user_ids && (assignee_user_ids || []).map((p: any) => p.key),
    reporter,
    tag_ids: tag_ids && (tag_ids || []).map((p: any) => p.key),
    status,
    priority,
    tab,
    filter_name
  };
};
