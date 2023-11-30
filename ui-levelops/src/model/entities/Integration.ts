import { Entity } from "./entity";

export type AuthenticationTypes = "oauth" | "none" | "api_key" | "multiple_api_keys";
export type IntegrationMetadataKeys =
  | "is_push_based"
  | "fetch_commits"
  | "fetch_prs"
  | "fetch_issues"
  | "fetch_projects"
  | "fetch_commit_files"
  | "fetch_action_logs"
  | "repos"
  | "subtype";

export interface Integration extends Entity {
  append_metadata: boolean;
  application: string;
  authentication: AuthenticationTypes;
  created_at: number;
  description: string;
  metadata: Record<IntegrationMetadataKeys, boolean | string>;
  name: string;
  satellite: boolean;
  status: string;
  tags: Array<string>;
  updated_at: number;
  url: string;
}

export enum AzureIntegrationSubType {
  WI = "wi",
  SCM = "scm",
  CICD = "cicd"
}

export enum AzureApplicationSubType {
  BOARDS = "azure_devops_boards",
  PIPELINES = "azure_devops_pipelines",
  REPOS = "azure_devops_repos"
}
