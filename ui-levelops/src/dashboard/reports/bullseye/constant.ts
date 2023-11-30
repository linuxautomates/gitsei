export const BULLSEYE_FILTER_KEY_MAPPING: Record<string, string> = {
  project: "projects",
  job_name: "job_names",
  job_normalized_full_name: "job_normalized_full_names",
  name: "name"
};

export const METRIC_OPTIONS = [
  { label: "Percentage of Covered Branches", value: "condition_percentage_coverage" },
  { label: "Percentage of Uncovered Branches", value: "condition_percentage_uncovered" },
  { label: "Number of Covered Branches", value: "conditions_covered" },
  { label: "Number of Uncovered Branches", value: "conditions_uncovered" }
];
