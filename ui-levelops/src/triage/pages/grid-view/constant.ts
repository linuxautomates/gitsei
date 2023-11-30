export const TRIAGE_FILTERS_MAPPING: Record<string, { value: string; label: string }> = {
  job_ids: { value: "cicd_job_id", label: "Name" },
  results: { value: "job_status", label: "Status" },
  parent_job_ids: { value: "cicd_job_id", label: "Parent Job name" },
  cicd_user_ids: { value: "cicd_user_id", label: "CI/CD User" },
  job_normalized_full_names: { value: "job_normalized_full_name", label: "Job Normalized Full Names" },
  instance_names: { value: "instance_name", label: "Instance Name" }
};
