import { getGroupByRootFolderKey } from "configurable-dashboard/helpers/helper";
import { JiraSalesforceNodeType, JiraZendeskNodeType } from "custom-hooks/helpers/sankey.helper";
import widgetConstants from "dashboard/constants/widgetConstants";
import { JiraSalesforceReports, JiraZendeskReports } from "dashboard/constants/helper";
import { get } from "lodash";
import { combineAllFilters } from "shared-resources/containers/widget-api-wrapper/helper";
import { renameKey, sanitizeObject } from "utils/commonUtils";
import { transformFilters } from "utils/dashboardFilterUtils";

export const jiraZendeskSalesforceDrilldownTransformer = (data: any) => {
  const { drillDownProps, widget, dashboardQuery, metaData } = data;
  let { across, ...remainData } = widget.query;
  const widgetFilter = get(widgetConstants, [widget.type, "filters"], {});
  if (widgetFilter.across) {
    across = widgetFilter.across;
  }
  const hiddenFilters = get(widgetConstants, [widget.type, "hidden_filters"], {});

  const initialFilters = combineAllFilters(remainData, widgetFilter, hiddenFilters);

  let filters = {
    filter: {
      ...(initialFilters || {}),
      ...(dashboardQuery || {})
    },
    across
  };
  const passedFilters = get(drillDownProps, [drillDownProps.application, "filter"], {});

  let _filters = {
    ...filters.filter,
    ...passedFilters
  };

  if (["jira_zendesk_report"].includes(widget.type)) {
    // Renaming "jira_issue_created_at" to "created_at"
    // to prevent it from being stripped out in next step.
    _filters = renameKey(_filters, "jira_issue_created_at", "created_at");
  }

  const type = get(drillDownProps, [drillDownProps.application, "type"]);
  if (type === JiraZendeskNodeType.ZENDESK) {
    _filters = transformFilters(_filters, "zendesk_");
  }

  if (type === JiraSalesforceNodeType.SALESFORCE) {
    _filters = transformFilters(_filters, "salesforce_");
  }

  if (typeof drillDownProps.x_axis === "string" && !get(metaData, [getGroupByRootFolderKey(widget.type)], undefined)) {
    _filters = {
      ...(_filters || {}),
      repo_ids: [drillDownProps?.x_axis]
    };
  }

  if (
    [JiraZendeskReports.JIRA_ZENDESK_FILES_REPORT, JiraSalesforceReports.JIRA_SALESFORCE_FILES_REPORT].includes(
      widget.type
    ) &&
    get(metaData, [getGroupByRootFolderKey(widget.type)], undefined)
  ) {
    _filters = {
      ...(_filters || {}),
      module: drillDownProps.x_axis
    };
  }

  filters = {
    ...filters,
    filter: sanitizeObject(_filters)
  };

  return { acrossValue: across, filters };
};
