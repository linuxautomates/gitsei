import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ScmCommonPRFilter } from "../scm-common-filter";
import { PrMergedAtFilterConfig } from "../scm-specific-filter-config.constant";

export const DeploymentFrequencySingleStatFiltersConfig: LevelOpsFilter[] = [
  ...ScmCommonPRFilter.filter((item: LevelOpsFilter) => item.id !== "pr_merged_at"),
  { ...PrMergedAtFilterConfig, required: true, deleteSupport: false }
];
