import React, { isValidElement, useMemo, useState } from "react";
import { connect } from "react-redux";
import { valuesToFilters } from "../../constants/constants";
import widgetConstants from "../../constants/widgetConstants";
import { ServerPaginatedTable } from "../../../shared-resources/containers";
import { AntText } from "../../../shared-resources/components";
import { capitalize, get, isEqual } from "lodash";
import { csvDownloadDrilldownToProps } from "reduxConfigs/maps/csvDownload.map";
import ChildTicketTable from "./ChildTicketsTable";
import * as RestURI from "constants/restUri";
import { csvDrilldownDataTransformer } from "dashboard/helpers/csv-transformers/csvDrilldownDataTransformer";
import { azure_resolution_time, jira_resolution_time } from "./configs/jiraTableConfig";
import {
  AZURE_SPRINT_REPORTS,
  azureLeadTimeIssueReports,
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  JIRA_SPRINT_REPORTS,
  leadTimeReports,
  SPRINT,
  SPRINT_JIRA_ISSUE_KEYS,
  LEAD_TIME_BY_STAGE_REPORTS,
  DORA_REPORTS,
  TESTRAILS_REPORTS
} from "dashboard/constants/applications/names";
import {
  azureRenderVelocityDynamicColumns,
  renderVelocityDynamicColumns,
  renderVelocityStageDynamicColumns
} from "custom-hooks/helpers/leadTime.helper";
import "./dashboard-tickets.scss";
import { CSV_DRILLDOWN_TRANSFORMER } from "dashboard/constants/filter-key.mapping";
import { sprintMetricStatCsvTransformer } from "dashboard/helpers/csv-transformers/sprintMetricStatCSVTransformer";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { ADO_USER_ID_KEYS, USER_ID_KEYS } from "./dashboardTicketsTable.constant";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import cx from "classnames";
import { ColumnProps } from "antd/lib/table";
import widget from "dashboard/components/widgets/widget";
import { ExtraColumnProps, ReportDrilldownColTransFuncType } from "dashboard/dashboard-types/common-types";
interface DashboardTicketsTableProps {
  reportType: string;
  filters: any;
  integrationIds: Array<string>;
  integrationsData: Array<any>;
  filtersData?: Array<any>;
  sortData?: Array<any>;
  customFields?: Array<any>;
  columns: Array<any>;
  supportedFilters: any;
  title: string;
  widgetMetadata?: any;
  uri: string;
  setFilters: (data: any) => void;
  csvDownloadDrilldown: (uri: string, method: string, filters: any, columns: Array<any>) => void;
  ouFilters?: any;
  interval?: any;
  across?: any;
  drillDownColumns?: any;
  allColumns?: any;
  doraProfileIntegrationType?: any;
  doraProfileDeploymentRoute?: string;
  reload?: number;
  setReload?: (data: number) => void;
  widgetId?: string;
  handleToggleFilterChange?: (filters: any) => void;
  doraProfileEvent?: string;
  doraProfileIntegrationApplication?: string;
  testrailsCustomField?: any[];
}

const DashboardTicketsTable: React.FC<DashboardTicketsTableProps> = (props: DashboardTicketsTableProps) => {
  const {
    title,
    uri,
    filters,
    integrationIds,
    setFilters,
    integrationsData,
    filtersData,
    customFields,
    columns,
    supportedFilters,
    reportType,
    ouFilters,
    interval,
    across,
    allColumns,
    doraProfileIntegrationType,
    doraProfileDeploymentRoute,
    reload,
    setReload,
    widgetId,
    widgetMetadata,
    handleToggleFilterChange,
    doraProfileEvent,
    doraProfileIntegrationApplication,
    testrailsCustomField
  } = props;
  const [localFilters, setLocalFilters] = useState({ ...filters });
  const [selectedColumns, setSelectedColumns] = useState(props.drillDownColumns);
  const mapFiltersBeforeCall = get(widgetConstants, [reportType, "mapFiltersBeforeCall"], undefined);

  const getWidgetConstant = (key: string) => {
    return get(widgetConstants, [reportType, key], undefined);
  };

  const getMappedOptions = (key: string) => {
    let _key: any = key;
    if (uri === "jira_zendesk_aggs_list_jira" || uri === "jira_salesforce_aggs_list_jira") {
      _key = `jira_${key}`;
    }
    const data = filtersData!.find((item: any) => Object.keys(item)[0] === _key);

    if (uri === "issue_management_list") {
      const records = get(data, [key], []);
      if (key === "workitem_priority") {
        return records.map((item: any) => ({
          label: get(staticPriorties, item.key, item.key),
          value: item["key"]
        }));
      }
      if (ADO_USER_ID_KEYS.includes(_key)) {
        return records.map((item: any) => ({
          label: (item?.additional_key ?? item?.key ?? "").toUpperCase(),
          value: item["key"]
        }));
      }
      return records.map((item: any) => ({
        label: item.key && item.value ? (item?.value || "").toUpperCase() : (item["key"] || "").toUpperCase(),
        value: item["key"]
      }));
    }

    if (data && Array.isArray(data[_key]) && USER_ID_KEYS.includes(_key)) {
      return data[_key]
        .filter((item: any) => item.key)
        .map((item: any) => ({
          label: (item?.additional_key ?? item?.key ?? "").toUpperCase(),
          value: item["key"]
        }));
    }

    if (data && Array.isArray(data[_key])) {
      return data[_key]
        .filter((item: any) => item.key)
        .map((item: any) => ({
          label: item.key && item.value ? (item?.value || "").toUpperCase() : (item["key"] || "").toUpperCase(),
          value: item["key"]
        }));
    } else {
      return [{ label: " ", value: " " }];
    }
  };

  const getIntegrationOptions = () => {
    return integrationsData!.map((item: any) => ({
      label: `${capitalize(item.application)}-${item.name}`,
      value: item.id
    }));
  };

  const getField = (key: string) => {
    if (uri === "jira_zendesk_aggs_list_jira" || uri === "jira_salesforce_aggs_list_jira") {
      // @ts-ignore
      return valuesToFilters[`jira_${key}`];
    }

    if (
      ["praetorian_issues_report", "ncc_group_vulnerability_report"].includes(reportType) &&
      ["component", "priority"].includes(key)
    ) {
      return key;
    }

    if (
      [
        "cicd_jobs_count_report",
        "cicd_scm_jobs_duration_report",
        "cicd_pipeline_jobs_duration_report",
        "cicd_pipeline_jobs_count_trend_report",
        "cicd_pipeline_jobs_duration_trend_report"
      ].includes(reportType) &&
      ["service"].includes(key)
    ) {
      return "services";
    }

    if (key.includes("customfield_")) {
      return key;
    }
    // @ts-ignore
    return valuesToFilters[key] || key;
  };

  const getDerive = () => {
    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
      ].includes(reportType as JIRA_SPRINT_REPORTS)
    ) {
      return true;
    }
    if (
      [JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT, AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT].includes(
        reportType as JIRA_SPRINT_REPORTS
      )
    ) {
      return false;
    }
    return !["praetorian_issues_report", "ncc_group_vulnerability_report"].includes(reportType);
  };

  const scrollX = useMemo(() => {
    return { x: "fit-content" };
  }, []);

  const extraTableProps = useMemo(() => {
    let props = {};
    const getExtraDrilldownProps = get(widgetConstants, [reportType, "drilldown", "getExtraDrilldownProps"]);
    if (getExtraDrilldownProps) {
      return { ...getExtraDrilldownProps({ widgetId: widgetId || get(filters, ["widget_id"], "") }) };
    }
    const dynamicColumnRenderer = get(widgetConstants, [reportType, "drilldown", "renderDynamicColumns"]);
    const recordsTransformer = get(widgetConstants, [reportType, "drilldown", "transformRecords"]);

    if ([...leadTimeReports, ...azureLeadTimeIssueReports].includes(reportType as any)) {
      const renderColumn =
        dynamicColumnRenderer ??
        (LEAD_TIME_BY_STAGE_REPORTS.includes(reportType as any)
          ? renderVelocityStageDynamicColumns
          : azureLeadTimeIssueReports.includes(reportType as any)
          ? azureRenderVelocityDynamicColumns
          : renderVelocityDynamicColumns);

      props = {
        ...props,
        hasDynamicColumns: true,
        renderDynamicColumns: renderColumn,
        transformRecordsData: recordsTransformer,
        scroll: scrollX,
        shouldDerive: ["velocity_config"]
      };
    }
    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
      ].includes(reportType as JIRA_SPRINT_REPORTS)
    ) {
      props = {
        shouldDerive: get(filters, ["across"], "") === SPRINT ? [SPRINT_JIRA_ISSUE_KEYS] : [],
        report: reportType
      };
    }
    if (["sprint_goal", "azure_sprint_metrics_single_stat", "sprint_metrics_single_stat"].includes(reportType)) {
      props = {
        ...props,
        scroll: scrollX
      };
    }
    props = {
      ...props,
      widgetId: get(filters, ["widget_id"], ""),
      widget: get(filters, ["widget"], ""),
    };

    return props;
  }, [reportType]);

  const renderIntegration = (id: any) => {
    if (integrationsData) {
      const integration = integrationsData.find((item: any) => item.id === id);
      if (integration) {
        return <AntText>{capitalize(integration.name)}</AntText>;
      } else {
        return <AntText>{id}</AntText>;
      }
    } else {
      return <AntText>{id}</AntText>;
    }
  };

  const getSupportedFilterValues = () => {
    if (uri === "jira_zendesk_aggs_list_zendesk" || uri === "jira_salesforce_aggs_list_salesforce") {
      return supportedFilters.values;
    }
    if (uri === "jira_zendesk_aggs_list_jira" || uri === "jira_salesforce_aggs_list_jira") {
      return supportedFilters.values
        .filter((fil: string) => fil.includes("jira_"))
        .map((fil: string) => fil.replace("jira_", ""));
    }
    if (Array.isArray(supportedFilters)) {
      return supportedFilters.reduce((acc, obj) => {
        acc.push(...obj.values);
        return acc;
      }, []);
    } else {
      return supportedFilters?.values || [];
    }
  };

  const getColumns = (columns: any) => {
    if (!filtersData) {
      return columns;
    } else {
      const supportedFilterValues = getSupportedFilterValues();

      let mappedColumns = columns
        .map((filter: any) => {
          if (filter.hygiene_type !== undefined) {
            if (!get(localFilters, ["filter", "hygiene_types"], []).includes(filter.hygiene_type)) {
              return null;
            }
          }

          if (["rollback"].includes(filter.key)) {
            return {
              ...filter,
              filterType: "select",
              filterField: getField(filter.key),
              options: getMappedOptions(filter.key),
              filterLabel: filter.filterLabel,
              span: 7
            };
          }

          if (["azure_iteration", "azure_teams", "azure_areas"].includes(filter?.dataIndex)) {
            return {
              ...(filter || {}),
              moreFilters: {
                integration_ids: integrationIds
              }
            };
          }

          if (uri === "dora_drill_down_report" && ["cicd_instance_name"].includes(filter?.dataIndex)) {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: "instance_name",
              options: getMappedOptions("instance_name")
            };
          }

          if (uri === "dora_drill_down_report" && ["project"].includes(filter?.dataIndex)) {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: "project",
              options: getMappedOptions("project"),
              filterLabel: "Project"
            };
          }

          if (uri === RestURI.MICROSOFT_ISSUES && filter.key === "projects") {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField("project"),
              options: getMappedOptions("project"),
              filterLabel: filter.filterLabel,
              span: 7
            };
          }
          if (uri === RestURI.MICROSOFT_ISSUES && filter.key === "tags") {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField("tag"),
              options: getMappedOptions("tag"),
              filterLabel: filter.filterLabel,
              span: 7
            };
          }

          // adding this new condition so that existing doesn't break and this need less testing
          if (uri === "coverity_defects_list" && supportedFilterValues.includes(filter.key)) {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: filter.filterField || getField(filter.key),
              options: getMappedOptions(filter.key),
              filterLabel: filter.filterLabel,
              span: 7
            };
          }
          if (supportedFilterValues.includes(filter.key)) {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField(filter.key),
              options: getMappedOptions(filter.key),
              filterLabel: filter.filterLabel,
              span: 7
            };
          }
          if (
            filter.key === "status" &&
            !["praetorian_issues_report", "ncc_group_vulnerability_report"].includes(reportType) &&
            !["issue_management_list"].includes(uri)
          ) {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: "job_statuses",
              options: getMappedOptions("job_status"),
              span: 7
            };
          }
          if (filter.key === "labels" && !["issue_management_list"].includes(uri)) {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField("label"),
              options: getMappedOptions("label"),
              span: 7
            };
          }
          if (filter.key === "component_list" && !["issue_management_list"].includes(uri)) {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField(filter.key.split("_")[0]),
              options: getMappedOptions(filter.key.split("_")[0]),
              span: 7
            };
          }
          if (filter.key === "assignee_email") {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField(filter.key.split("_")[0]),
              options: getMappedOptions(filter.key.split("_")[0]),
              span: 7
            };
          }
          if (filter.key === "submitter_email") {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField(filter.key.split("_")[0]),
              options: getMappedOptions(filter.key.split("_")[0]),
              span: 7
            };
          }
          if (filter.key === "requester_email") {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: getField(filter.key.split("_")[0]),
              options: getMappedOptions(filter.key.split("_")[0]),
              span: 7
            };
          }
          if (filter.key === "integration_id") {
            return {
              ...filter,
              filterType: "multiSelect",
              filterField: "integration_ids",
              options: getIntegrationOptions(),
              render: (item: any, record: any, index: any) => renderIntegration(item),
              span: 7
            };
          }

          return filter;
        })
        .filter((f: any) => f !== null);

      if (
        (getWidgetConstant("application") === "jira" ||
          getWidgetConstant("application") === "any" ||
          getWidgetConstant("application") === "testrails") &&
        ![
          ...leadTimeReports,
          jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
          TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_REPORT,
          TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_TRENDS_REPORT,
          TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT,
          TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_FORECAST_TRENDS_REPORT
        ].includes(reportType as any)
      ) {
        customFields &&
          customFields.forEach(cf => {
            let extra: any = {
              hidden: true
            };
            if (
              [
                JIRA_MANAGEMENT_TICKET_REPORT.TICKETS_REPORT,
                JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_REPORT,
                JIRA_MANAGEMENT_TICKET_REPORT.BACKLOG_TREND_REPORT,
                JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT,
                ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
                ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
                ISSUE_MANAGEMENT_REPORTS.BACKLOG_TREND_REPORT,
                DORA_REPORTS.DEPLOYMENT_FREQUENCY_REPORT
              ].includes(reportType as any)
            ) {
              const across = get(filters, ["across"], "");
              const stacks = get(filters, ["filter", "stacks"], []);

              if (across === cf.key || stacks.includes(cf.key)) {
                extra = {
                  width: "10%",
                  hidden: false,
                  ellipsis: true,
                  render: (value: any, record: any, index: any) => get(record, ["custom_fields", cf.key], "")
                };
              }
            }

            mappedColumns.push({
              title: cf.name,
              key: cf.key,
              filterLabel: cf.name,
              filterType: "multiSelect",
              filterField: cf.key,
              options: getMappedOptions(cf.key),
              prefixPath: "custom_fields",
              span: 7,
              ...(extra || {})
            });
          });
      }
      // if (["response_time_report", "response_time_report_trends"].includes(reportType)) {
      //   mappedColumns.push(jira_response_time);
      // }
      // if (["resolution_time_report", "resolution_time_report_trends"].includes(reportType)) {
      //   mappedColumns.push(jira_resolution_time);
      // }
      if (["resolution_time_report", "resolution_time_report_trends"].includes(reportType)) {
        mappedColumns.push(jira_resolution_time);
      }

      if (
        [
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_TREND_REPORT,
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT
        ].includes(reportType as any)
      ) {
        mappedColumns.push(azure_resolution_time);
      }
      const columnsResult = mappedColumns.map((col: any) => ({
        ...col,
        showExcludeSwitch: getWidgetConstant("application") === "jira",
        showCheckboxes: getWidgetConstant("application") === "jira"
      }));

      return columnsResult;
    }
  };

  const onFilterChange = (data: any) => {
    // let dirty = false;
    // if (
    //   data.hasOwnProperty("late_attachments") ||
    //   data.hasOwnProperty("idle") ||
    //   data.hasOwnProperty("poor_description")
    // ) {
    //   const keys1 = Object.keys(localFilters.filter || {});
    //   const keys2 = Object.keys(data || {});
    //   const diff = keys2.filter(key => !keys1.includes(key));
    //   if (diff.includes("late_attachments") || diff.includes("idle") || diff.includes("poor_description")) {
    //     //data.hygiene_filters = hygieneFilters;
    //     notification.info({
    //       message: "Hygiene Filter needed",
    //       description: "Please select associated hygiene type for this filter to take effect"
    //     });
    //   }
    // }
    setLocalFilters({
      ...localFilters,
      filter: { ...data }
    });
    if (data && !isEqual(data.integration_ids, integrationIds)) {
      setFilters(data);
    }
  };

  const renderExpandedRow = (row: any) => {
    let childTableColumns = columns;
    if (selectedColumns) {
      const savedColumns = selectedColumns.map(
        (drilldownColumn: any) =>
          columns.find((column: any) => column.dataIndex === drilldownColumn) ||
          allColumns.find((column: any) => column.dataIndex === drilldownColumn)
      );
      childTableColumns = getColumns(savedColumns);
    }
    return (
      <ChildTicketTable
        {...props}
        columns={childTableColumns}
        ticketId={uri === "issue_management_list" ? row.workitem_id : row.key}
      />
    );
  };

  const expandProps = {} as any;
  const supportExpandRow = get(getWidgetConstant("drilldown"), ["supportExpandRow"]);
  if (["jira_tickets", "issue_management_list"].includes(uri) && supportExpandRow !== false) {
    // show expand feature just for only jira_issues/list endpoint.
    expandProps["expandedRowRender"] = renderExpandedRow;
  }

  const getJsxHeaders = () => {
    const columns = getColumns(props.columns);
    let jsxHeaders: any = [];
    columns.forEach((col: any) => {
      if (isValidElement(col?.title) && !col?.hidden) {
        let jsxTitle = col?.titleForCSV;
        jsxHeaders.push({
          title: jsxTitle ? jsxTitle : capitalize(col?.dataIndex?.replace(/_/g, " ")),
          key: col?.dataIndex
        });
      }
    });
    return jsxHeaders;
  };

  const getCSVDataTransformer = () => {
    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
      ].includes(reportType as JIRA_SPRINT_REPORTS) &&
      get(filters, ["across"], "") !== SPRINT
    ) {
      return sprintMetricStatCsvTransformer;
    }
    return getWidgetConstant(CSV_DRILLDOWN_TRANSFORMER) || csvDrilldownDataTransformer;
  };

  const getDrilldownFooter = () => {
    const drilldownfooter = get(widgetConstants, [reportType, "drilldownFooter"], undefined);
    if (drilldownfooter) {
      const component = drilldownfooter({ filters });
      if (component) {
        return React.createElement(component, {});
      }
    }
    return drilldownfooter;
  };

  const hideFilterButton = () => {
    const getHideFilterButton = get(widgetConstants, [reportType, "hideFilterButton"], undefined);
    if (getHideFilterButton) {
      return getHideFilterButton();
    }
    return false;
  };

  const configureDrilldownDynamicColumns = (columns: (ExtraColumnProps & ColumnProps<any>)[]) => {
    /** transforming columns as per report requirements */
    const drilldownConfig = getWidgetConstant("drilldown");
    const drilldownDynamicColumnTransformer: ReportDrilldownColTransFuncType | undefined = get(drilldownConfig, [
      "drilldownDynamicColumnTransformer"
    ]);

    if (drilldownDynamicColumnTransformer) {
      return drilldownDynamicColumnTransformer({
        columns,
        filters: {
          metadata: widgetMetadata
        }
      });
    }
    return columns;
  };

  return (
    <ServerPaginatedTable
      uuid="jira-tickets"
      className={cx("dashboard-tickets-table", { "customised-scroll": extraTableProps?.customisedScroll })}
      title={title}
      uri={uri}
      columns={getColumns(columns)}
      moreFilters={filters?.filter || {}}
      ouFilters={ouFilters}
      across={across}
      interval={interval}
      hasSearch={false}
      sort={props.sortData || []}
      hasFilters={!!filtersData}
      onFiltersChange={onFilterChange}
      rowKey={uri === "issue_management_list" ? "workitem_id" : "key"}
      derive={getDerive()}
      downloadCSV={{
        tableDataTransformer: getCSVDataTransformer(),
        jsxHeaders: getJsxHeaders()
      }}
      {...expandProps}
      {...extraTableProps}
      showCustomFilters
      report={reportType}
      selectedDrilldownColumns={selectedColumns}
      drilldown={true}
      drilldownFooter={getDrilldownFooter()}
      setSelectedColumns={setSelectedColumns}
      allColumns={getColumns(allColumns)}
      mapFiltersBeforeCall={mapFiltersBeforeCall}
      hideFilterButton={hideFilterButton()}
      doraProfileIntegrationType={doraProfileIntegrationType}
      doraProfileDeploymentRoute={doraProfileDeploymentRoute}
      reload={reload}
      doraProfileEvent={doraProfileEvent}
      doraProfileIntegrationApplication={doraProfileIntegrationApplication}
      configureDynamicColumns={configureDrilldownDynamicColumns}
      testrailsCustomField={testrailsCustomField}
    />
  );
};

// @ts-ignore
export default connect(null, csvDownloadDrilldownToProps)(DashboardTicketsTable);
