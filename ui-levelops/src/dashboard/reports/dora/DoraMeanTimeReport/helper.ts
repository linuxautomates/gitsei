import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { LeadTimeByStageReportFiltersConfig } from "dashboard/reports/azure/lead-time-by-stage-report/filter.config";
import { IssueLeadTimeByStageReportFiltersConfig } from "dashboard/reports/jira/lead-time-by-stage-report/filters.config";
import { PrLeadTimeByStageReportFiltersConfig } from "dashboard/reports/scm/pr-lead-time-by-stage/filter.config";
import { get, unset } from "lodash";
import { getFiltereredIntegrationIds, removedUnusedFilterConfig } from "../helper";
import { updateTimeFiltersValue } from "shared-resources/containers/widget-api-wrapper/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { changeAzureAreaKey, changeIterationKey } from "shared-resources/containers/dora-api-container/helper";

const deriveEvent = (workspaceProfile: any) => {
  const startingEventIsCommitCreated = get(workspaceProfile, [
    "mean_time_to_restore",
    "starting_event_is_commit_created"
  ]);
  const startingEventIsGenericEvent = get(workspaceProfile, [
    "mean_time_to_restore",
    "starting_event_is_generic_event"
  ]);
  return startingEventIsCommitCreated ? "commit_created" : startingEventIsGenericEvent ? "api_event" : "ticket_created";
};

export const getFilterConfig = (params: any) => {
  const IMApplication = get(params.workspaceProfile, ["mean_time_to_restore", "issue_management_integrations", 0]);
  const event = deriveEvent(params.workspaceProfile);
  switch (event) {
    case "ticket_created":
    case "api_event":
      if (IMApplication === IntegrationTypes.AZURE) {
        return removedUnusedFilterConfig(LeadTimeByStageReportFiltersConfig);
      }
      return removedUnusedFilterConfig(IssueLeadTimeByStageReportFiltersConfig);
    case "commit_created":
      return removedUnusedFilterConfig(PrLeadTimeByStageReportFiltersConfig);
    default:
      return [];
  }
};

export const getDoraProfileIntegrationType = (params: any) => {
  const { workspaceOuProfilestate } = params;
  const event = deriveEvent(workspaceOuProfilestate);
  const IMApplication = get(workspaceOuProfilestate, ["mean_time_to_restore", "issue_management_integrations", 0]);
  switch (event) {
    case "commit_created":
      return "SCM";
    case "api_event":
    case "ticket_created":
      if (IMApplication !== IntegrationTypes.JIRA) {
        return IM_ADO;
      }
      return "IM";
  }
};

export const getDoraProfileEvent = (params: any) => {
  const { workspaceOuProfilestate } = params;
  return deriveEvent(workspaceOuProfilestate);
};

export const getChartProps = (params: any) => {
  const { widgetMetaData } = params;
  const hideStages = get(widgetMetaData, ["hide_stages"], undefined);
  const metrics = get(widgetMetaData, ["metrics"], "mean");
  return {
    hideKeys: hideStages,
    dataKey: metrics,
    showStaticLegends: true
  };
};
export const defaultQuery = (params: any) => {
  const { profileType } = params;
  const limit_to_only_applicable_data = true;
  const ratings = ["good", "slow", "needs_attention"];
  const initialQuery = {
    limit_to_only_applicable_data,
    ratings
  };
  switch (profileType) {
    case "IM":
      return {
        ...initialQuery,
        work_items_type: IntegrationTypes.JIRA,
        calculation: "ticket_velocity"
      };
    case IM_ADO:
      return {
        ...initialQuery,
        work_items_type: "work_item",
        calculation: "ticket_velocity"
      };
    case "SCM":
      return {
        ...initialQuery,
        limit_to_only_applicable_data: false,
        calculation: "pr_velocity"
      };
  }
};

export const getFilters = (props: any) => {
  const {
    filters,
    contextfilters,
    id,
    integrationIds,
    workflowProfile,
    availableIntegrations,
    dashboardMetaData,
    widgetMetaData,
    reportType
  } = props;
  const newFilters = { ...filters };
  const integrationType = getDoraProfileIntegrationType({ workspaceOuProfilestate: workflowProfile });
  let filteredintegrationIds = integrationIds;
  const availableApplications: string[] = availableIntegrations.map((integration: any) => integration?.application);
  if (
    availableApplications?.includes(IntegrationTypes.JIRA) &&
    availableApplications?.includes(IntegrationTypes.AZURE) &&
    availableIntegrations?.length > 0
  ) {
    filteredintegrationIds = getFiltereredIntegrationIds(availableIntegrations, integrationType, integrationIds);
  }
  const _defaultQuery = defaultQuery({ profileType: integrationType });
  if (integrationType === "SCM" && newFilters?.filter?.hasOwnProperty("work_items_type")) {
    unset(newFilters, ["filter", "work_items_type"]);
  }
  unset(newFilters, ["filter", "time_range"]);
  let finalFilters = {
    ...newFilters,
    filter: {
      ...newFilters?.filter,
      ...(contextfilters?.[id] || {}),
      integration_ids: filteredintegrationIds,
      ...(_defaultQuery || {}),
      ...updateTimeFiltersValue(dashboardMetaData, widgetMetaData, { ...newFilters?.filter })
    },
    across: "velocity",
    widget_id: id
  };

  if (integrationType === IM_ADO) {
    finalFilters = changeIterationKey(finalFilters);
    finalFilters = changeAzureAreaKey(finalFilters);
  }
  return finalFilters;
};

export const getDrilldownTitle = (params: any) => {
  const { drillDownProps } = params;
  return get(drillDownProps, ["x_axis", "histogram_stage_name"], "Total Time");
};

export const conditionalUriMethod = (params: any) => {
  const { workspaceProfile } = params;
  const integrationType = getDoraProfileIntegrationType({ workspaceOuProfilestate: workspaceProfile });
  switch (integrationType) {
    case "SCM":
      return "lead_time_filter_values";
    case "IM":
      return "lead_time_filter_values";
    case "IM_ADO":
      return "issue_management_workitem_values";
    default:
      return "lead_time_filter_values";
  }
};

export const getDoraProfileIntegrationApplication = (params: any) => {
  const { workspaceOuProfilestate } = params;
  const profileType = getDoraProfileIntegrationType({ workspaceOuProfilestate });
  switch (profileType) {
    case "IM":
      return IntegrationTypes.JIRA;
    case "IM_ADO":
      return IntegrationTypes.AZURE;
    case "SCM":
      return IntegrationTypes.GITHUB;
  }
};

export const getDoraMeanTimeData = (params: any) => {
  const { workspaceOuProfilestate } = params;
  return workspaceOuProfilestate.mean_time_to_restore;
};
