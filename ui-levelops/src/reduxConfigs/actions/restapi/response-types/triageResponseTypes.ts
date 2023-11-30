export interface TriageFilters {
  job_ids?: string[];
  parent_job_ids?: string[];
  results?: string[];
  start_time?: string[] | any | string;
  end_time?: string[] | any | string;
  triage_rule_ids?: string[];
  cicd_user_ids?: string[];
  job_normalized_full_names?: string[];
}

export interface TriageFilterResponse {
  id: string;
  name: string;
  description: string;
  is_default: boolean;
  filter: TriageFilters;
  created_at: string;
  updated_at: string;
  public?: boolean;
}

export interface TriageFilterPayload {
  name: string;
  description: string;
  is_default: boolean;
  filter: TriageFilters;
}

export interface CICDAggsResponse {
  totals: { [key: string]: number };
  key: string;
}

export interface CICDJobAggsResponse {
  id: string;
  name: string;
  aggs: CICDAggsResponse[];
  full_name: string;
}
