import { IDrawerProps } from "@blueprintjs/core";
import { noop } from "lodash";

export const SELECT_TYPE_DRAWER_OPTIONS = {
  onClose: noop
} as IDrawerProps;

export enum CONNECTION_TYPE {
  JIRA_CLOUD = "jira_cloud",
  JIRA_SELF_MANAGED = "jira_selfManaged",
  GITHUB_CLOUD = "github_cloud",
  GITHUB_SELF_MANAGED = "github_selfManaged"
}
