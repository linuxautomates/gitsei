import { get } from "lodash";

export const getSCMPrsTimeAcrossValue = (args: any) => {
  const filters = args?.allFilters;
  let across = filters?.across;
  if (["pr_closed", "pr_created"].includes(across)) {
    const interval = get(filters, ["interval"]);
    if (interval) across = `${across}_${interval}`;
  }
  return across;
};
