import { sankeyChartColors, lineChartColors } from "shared-resources/charts/chart-themes";

export const NODE_ZENDESK = "Zendesk";
export const NODE_OPEN = "Open";
export const NODE_CLOSED = "Closed";
export const NODE_ESCALATED = "Escalated";
export const NODE_PRS = "PRs";
export const NODE_CLOSED_WO_FIX = "Closed w/o fix";

export enum JiraZendeskNodeType {
  JIRA = "jira",
  ZENDESK = "zendesk",
  ZENDESK_LIST = "zendesk_list",
  COMMIT = "commit_zendesk"
}

export enum JiraSalesforceNodeType {
  JIRA = "jira",
  SALESFORCE = "salesforce",
  SALESFORCE_LIST = "salesforce_list",
  COMMIT = "commit_salesforce"
}

export const getProps = (value: any, max: number, scaleHeight: number, index: number) => ({
  pathProps: {
    fill: "none",
    stroke: sankeyChartColors[index],
    strokeWidth: Math.max((value / max) * scaleHeight, 0.02 * scaleHeight)
  },
  nodeProps: {
    height: (value / max) * scaleHeight,
    fill: lineChartColors[index]
  },
  textProps: {
    textAnchor: "start",
    fontSize: "16px",
    fontWeight: 500,
    x: 5
  }
});
