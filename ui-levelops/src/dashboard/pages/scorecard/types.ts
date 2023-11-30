import { basicMappingType } from "dashboard/dashboard-types/common-types";

export interface drilldownColumn {
  SCM_COMMITS: Array<string>;
  SCM_PRS: Array<string>;
  JIRA_ISSUES: Array<string>;
  WORKITEM_ISSUES: Array<string>;
  SCM_CONTRIBUTION: Array<string>;
  SCM_CONTRIBUTIONS: Array<string>;
}
export interface drilldownTitleType {
  DRILLDOWN_TITLE: string;
}
export type drilldownKeyType = keyof drilldownColumn;

export type drilldownKeyValue = {
  [Key in keyof drilldownColumn]?: drilldownColumn[Key];
};

type CombinedType = drilldownKeyValue & drilldownTitleType;

export type devProdDrilldownTypes = basicMappingType<CombinedType>;


export interface Org {
  ou_ref_id: string | number;
  ou_name: string;
}
export interface AssociatedOU {
  associated_ous: Array<Org>;
}