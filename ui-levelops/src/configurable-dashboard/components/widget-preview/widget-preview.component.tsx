import React, { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { Checkbox, Tooltip } from "antd";
import cx from "classnames";
import { cloneDeep, get, isEqual, unset } from "lodash";

import "./widget-preview.component.scss";
import widgetConstants from "dashboard/constants/widgetConstants";
import { AntCard, AntCol, AntRow, AntText } from "shared-resources/components";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { usePrevious } from "shared-resources/hooks/usePrevious";
import EmptyWidgetPreview from "./custom-preview/EmptyWidgetPreview";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  dashboardWidgetChildrenSelector,
  getDashboard,
  selectedDashboard
} from "reduxConfigs/selectors/dashboardSelector";
import DashboardGraphsContainer from "../../../dashboard/containers/dashboard-graphs.container";
import { CacheWidgetPreview, WidgetFilterContext, WidgetPayloadContext } from "../../../dashboard/pages/context";
import { ChartContainerType, WidgetType } from "../../../dashboard/helpers/helper";
import {
  HYGIENE_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_AZURE_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  PREVIEW_DISABLED,
  scmCicdReportTypes
} from "dashboard/constants/applications/names";
import { useDispatch, useSelector } from "react-redux";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import CompositeWidgetPreview from "./custom-preview/CompositeWidgetPreview";
import DashboardNotesPreviewComponent from "./custom-preview/dashboard-notes-preview/dashboard-notes.component";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import Loader from "components/Loader/Loader";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  jiraStatusFilterValues,
  JIRA_CUSTOM_FIELDS_LIST,
  configsList,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { jiraResolutionTimeReports } from "dashboard/graph-filters/components/Constants";
import { useGlobalFilters } from "../../../custom-hooks";
import { FileReports, ReportsApplicationType } from "../../../dashboard/constants/helper";
import {
  combineAllFilters,
  convertChildKeysToSiblingKeys,
  getWidgetUri,
  sanitizeStages,
  widgetApiFilters
} from "../../../shared-resources/containers/widget-api-wrapper/helper";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";
import { customFieldFiltersSanitize } from "../../../custom-hooks/helpers/zendeskCustomFieldsFiltersTransformer";
import { sprintStatReports } from "../../../dashboard/graph-filters/components/sprintFilters.constant";
import { LEVELOPS_MULTITIME_SERIES_REPORT } from "dashboard/constants/applications/multiTimeSeries.application";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { DEFAULT_SCM_SETTINGS_OPTIONS } from "../../../dashboard/constants/defaultFilterOptions";
import { LEVELOPS_REPORTS } from "../../../dashboard/reports/levelops/constant";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import queryString from "query-string";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { useLocation } from "react-router-dom";
import { IntegrationTypes } from "constants/IntegrationTypes";
interface WidgetPreviewComponentProps {
  widgetId: string;
  dashboardId: string;
  application: string;
  selectedReport: string;
  graphType: string;
  globalFilters: any;
  query: any;
  tableWidgetGraphType: string;
  metadata: any;
  max_records: any;
  custom_hygienes: any;
  weights: any;
  previewDisabled: boolean;
  configurePreview?: boolean;
  previewOnly?: boolean;
}

const WidgetPreviewComponent: React.FC<WidgetPreviewComponentProps> = (props: WidgetPreviewComponentProps) => {
  const { widgetId, dashboardId, configurePreview, graphType } = props;

  const cacheWidgetPreview = useContext(CacheWidgetPreview);
  const dispatch = useDispatch();
  const loction = useLocation();
  const queryParamOU = queryString.parse(loction.search).OU as string;
  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });
  const previewDisabled = useMemo(
    () => (props.previewDisabled === undefined ? false : props.previewDisabled),
    [props.previewDisabled]
  );
  const [showPreview, setShowPreview] = useState<boolean>(!previewDisabled);
  const [localReload, setLocalReload] = useState<number>(1);

  const globalFilters = useGlobalFilters(props.globalFilters);
  const { filters: contextFilters } = useContext(WidgetFilterContext);
  const { payload, setPayload } = useContext(WidgetPayloadContext);

  const previewIsCancelled = useMemo(() => previewDisabled || !showPreview, [previewDisabled, showPreview]);
  const widgetChildren = useParamSelector(dashboardWidgetChildrenSelector, {
    dashboard_id: dashboardId,
    widget_id: widgetId
  });

  const dashboard = useSelector(selectedDashboard);

  const queryWhenCancelled = useRef(props.query);
  const previousQuery = usePrevious(props.query);
  const previousPreviewIsCancelled = usePrevious(previewIsCancelled);
  const previousSelectedReport = usePrevious(props.selectedReport);

  const [excludeFiltersLoading, setExcludeFiltersLoading] = useState(true);
  const [scmGlobalSettingsLoading, setSCMGlobalSettingsLoading] = useState(true);

  const excludeStatusState = useParamSelector(getGenericRestAPISelector, {
    uri: "jira_filter_values",
    method: "list",
    uuid: "exclude_status"
  });

  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });
  const integrationIds = useMemo(() => {
    return get(props.globalFilters, ["integration_ids"], []);
  }, [props.globalFilters?.integration_ids]);

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [integrationIds]
  );

  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });

  const azureFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const jiraFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const zendeskFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const isStat = [
    WidgetType.STATS,
    WidgetType.CONFIGURE_WIDGET_STATS,
    WidgetType.STATS_NOTES,
    WidgetType.GRAPH_NOTES
  ].includes(props.graphType as WidgetType);
  const isComposite = props.graphType === WidgetType.COMPOSITE_GRAPH;
  const isNotes = [WidgetType.STATS_NOTES, WidgetType.GRAPH_NOTES].includes(props.graphType as WidgetType);

  const getWidgetConstant = useCallback(
    (data: any) => get(widgetConstants, [props.selectedReport, data], undefined),
    [props.selectedReport]
  );

  const supportedCustomFields = useMemo(() => {
    const applications = integrations.map((item: any) => item.application);
    let customFields: IntegrationTransformedCFTypes[] = [];
    if (applications.includes("jira") && props.globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(jiraFieldsSelector, "data", [])];
    }

    if (applications.includes("azure_devops") && props.globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(azureFieldsSelector, "data", [])];
    }

    if (applications.includes("zendesk") && props.globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(zendeskFieldsSelector, "data", [])];
    }
    return customFields;
  }, [
    azureFieldsSelector,
    jiraFieldsSelector,
    zendeskFieldsSelector,
    integrations,
    props.globalFilters?.integration_ids?.length
  ]);

  useEffect(() => {
    // handling exclude filters for resolution time report
    let hasResolutionTimeReport = jiraResolutionTimeReports.includes(props.selectedReport as any);
    const isResolutionTrendReport = widgetChildren.find((child: any) => child.type === "resolution_time_report_trends");
    hasResolutionTimeReport = hasResolutionTimeReport || isResolutionTrendReport;
    if (hasResolutionTimeReport) {
      const data = get(excludeStatusState, "data", {});

      if (Object.keys(data).length > 0) {
        setExcludeFiltersLoading(false);
      } else {
        dispatch(
          jiraStatusFilterValues(
            {
              fields: ["status"],
              filter: {
                status_categories: ["Done", "DONE"],
                integration_ids: dashboard?.query?.integration_ids || []
              }
            },
            "exclude_status"
          )
        );
      }
    } else {
      setExcludeFiltersLoading(false);
    }

    // calling global settings API in every because of color scheme
    // checking if we have the data present already
    const data = get(globalSettingsState, "data", {});
    if (Object.keys(data).length > 0) {
      setSCMGlobalSettingsLoading(false);
    } else {
      // id is defined as number in configsList, which shouldn't be the case
      dispatch(configsList({}, GLOBAL_SETTINGS_UUID as any));
    }
  }, []);

  const scmGlobalSettings = useMemo(() => {
    const SCM_GLOBAL_SETTING = globalSettingsState?.data?.records.find(
      (item: any) => item.name === "SCM_GLOBAL_SETTINGS"
    );
    return SCM_GLOBAL_SETTING
      ? typeof SCM_GLOBAL_SETTING?.value === "string"
        ? JSON.parse(SCM_GLOBAL_SETTING?.value)
        : SCM_GLOBAL_SETTING?.value
      : DEFAULT_SCM_SETTINGS_OPTIONS;
  }, [globalSettingsState]);

  useEffect(() => {
    if (scmGlobalSettingsLoading) {
      return;
    }

    const reportType = props.selectedReport;
    const widgetFilters = getWidgetConstant("filters");
    const hiddenFilters = getWidgetConstant("hidden_filters");
    const container = get(widgetConstants, [reportType, "chart_container"], undefined);
    const jiraOrFilters = !get(props.metadata, "disable_or_filters", false) ? dashboard?.jira_or_query : {};
    const uri = getWidgetConstant("uri");
    let filters: any = {};

    if (container === ChartContainerType.HYGIENE_API_WRAPPER) {
      const combinedFilters = combineAllFilters(widgetFilters, props.query, hiddenFilters);

      filters = {
        filter: {
          ...combinedFilters,
          ...globalFilters
        }
      };

      if (filters.filter.hasOwnProperty("across")) {
        const across = filters.filter.across;
        delete filters.filter["across"];
        filters = {
          ...filters,
          across
        };
      }

      filters = updateIssueCreatedAndUpdatedFilters(filters, props.metadata);

      if (props.application === ReportsApplicationType.ZENDESK) {
        filters = customFieldFiltersSanitize(filters, true);
      }

      if (["jira"].includes(props.application) && Object.keys(jiraOrFilters || {}).length > 0) {
        filters = {
          ...filters,
          filter: {
            ...(filters.filter || {}),
            or: jiraOrFilters
          }
        };
      }

      if (["azure_devops"].includes(props.application)) {
        const customFields = get(filters, ["filter", "custom_fields"], {});
        const excludeFields = get(filters, ["filter", "exclude"], {});
        const excludeCustomFields = get(excludeFields, ["custom_fields"], {});
        if (Object.keys(customFields).length > 0) {
          unset(filters, ["filter", "custom_fields"]);
          filters = {
            ...(filters || {}),
            filter: {
              ...(filters?.filter || {}),
              workitem_custom_fields: {
                ...(customFields || {})
              }
            }
          };
        }
        if (Object.keys(excludeFields).length > 0 && excludeFields?.custom_fields) {
          unset(filters, ["filter", "custom_fields"]);
          unset(filters, ["filter", "exclude", "custom_fields"]);
          filters = {
            ...(filters || {}),
            filter: {
              ...(filters?.filter || {}),
              exclude: {
                ...get(filters, ["filter", "exclude"], {}),
                workitem_custom_fields: { ...excludeCustomFields }
              }
            }
          };
        }
      }

      if (HYGIENE_TREND_REPORT.includes(reportType as any)) {
        const interval = filters?.filter?.interval || "month";
        unset(filters, ["filter", "interval"]);
        filters = {
          ...(filters || {}),
          interval
        };
      }

      const azureIterationValues = get(filters, ["filter", "azure_iteration"], undefined);
      if (azureIterationValues) {
        const newAzureIterationValues = azureIterationValues.map((value: any) => {
          if (typeof value === "object") {
            return `${value.parent}\\${value.child}`;
          } else {
            // This is just for backward compatibility with old version that had string values
            return value;
          }
        });

        let key = "workitem_sprint_full_names";
        unset(filters, ["azure_iteration"]);
        filters = {
          ...(filters || {}),
          filter: {
            ...(filters?.filter || {}),
            [key]: newAzureIterationValues
          }
        };
      }
      filters = {
        [widgetId]: { ...filters }
      };
    } else if (container === ChartContainerType.SANKEY_API_WRAPPER) {
      const combinedFilters = combineAllFilters(widgetFilters, props.query, hiddenFilters);

      filters = {
        filter: {
          ...combinedFilters,
          ...globalFilters
        }
      };
      if (filters.filter.hasOwnProperty("across")) {
        const across = filters.filter.across;
        delete filters.filter["across"];
        filters = {
          ...filters,
          across
        };
      }

      filters = updateIssueCreatedAndUpdatedFilters(filters, props.metadata, reportType);

      filters = {
        ...filters,
        filter: {
          ...filters.filter,
          ...(widgetFilters || {}),
          ...(hiddenFilters || {})
        }
      };

      if (
        ["jirazendesk", "jirasalesforce"].includes(props.application) &&
        Object.keys(jiraOrFilters || {}).length > 0
      ) {
        filters = {
          ...filters,
          filter: {
            ...(filters.filter || {}),
            jira_or: jiraOrFilters
          }
        };
      }

      filters = {
        [widgetId]: { ...filters }
      };
    } else if (container === ChartContainerType.PRODUCTS_AGGS_API_WRAPPER) {
      const combinedFilters = combineAllFilters(widgetFilters, props.query, hiddenFilters);
      filters = {
        [widgetId]: {
          page_size: 1,
          page: 0,
          filter: {
            ...combinedFilters,
            ...globalFilters,
            integration_ids: undefined
          },
          sort: [{ id: "created_at", desc: true }]
        }
      };
    } else if ([LEVELOPS_REPORTS.TABLE_REPORT, LEVELOPS_REPORTS.TABLE_STAT_REPORT].includes(reportType as any)) {
      filters = {
        [widgetId]: props.query
      };
    } else {
      if (
        isComposite &&
        !(container === ChartContainerType.BA_WIDGET_API_WRAPPER || sprintStatReports.includes(reportType as any))
      ) {
        (widgetChildren || []).forEach((child: any) => {
          const type = child?.type;
          const childWigetFilters = get(widgetConstants, [type, "filters"], {});
          const childHiddenFilters = get(widgetConstants, [type, "hidden_filters"], {});
          const childURI = get(widgetConstants, [type, "uri"], {});
          const application = get(widgetConstants, [type, "application"], undefined);

          const combinedFilters = combineAllFilters(
            childWigetFilters || {},
            cloneDeep(child.query || {}),
            childHiddenFilters || {}
          );

          let finalFilters: { [x: string]: any } = {
            filter: {
              ...combinedFilters,
              ...globalFilters
            }
          };

          if (JENKINS_AZURE_REPORTS.includes(type)) {
            finalFilters = {
              ...finalFilters,
              filter: {
                ...finalFilters.filter,
                cicd_integration_ids: finalFilters.filter?.integration_ids
              },
              cicd_integration_ids: finalFilters.filter?.integration_ids
            };
          }

          finalFilters = convertChildKeysToSiblingKeys(finalFilters, "filter", ["across", "interval"]);

          // this check is added to fix the bug in old reports that are still sending trend in across
          // @ts-ignore
          if (scmCicdReportTypes.includes(type) && finalFilters.across === "trend") {
            finalFilters["across"] = "job_end";
          }

          finalFilters = updateIssueCreatedAndUpdatedFilters(finalFilters, child.metadata || {}, type, childURI);

          if (application === ReportsApplicationType.ZENDESK) {
            finalFilters = customFieldFiltersSanitize(finalFilters, true);
          }

          if (["jira", "githubjira"].includes(application) && Object.keys(jiraOrFilters || {}).length > 0) {
            const key = application === IntegrationTypes.JIRA ? "or" : "jira_or";
            finalFilters = {
              ...finalFilters,
              filter: {
                ...(finalFilters.filter || {}),
                [key]: jiraOrFilters
              }
            };
          }

          finalFilters.filter = sanitizeStages(excludeStatusState, reportType, finalFilters.filter);
          filters = {
            ...filters,
            [child.id]: { ...finalFilters }
          };
        });
      } else {
        filters = widgetApiFilters({
          widgetFilters,
          filters: props.query,
          hiddenFilters,
          globalFilters,
          reportType,
          contextFilters,
          updateTimeFilters: true,
          application: props.application,
          jiraOrFilters,
          maxRecords: props.max_records,
          filterKey: reportType === FileReports.SCM_JIRA_FILES_REPORT ? "scm_module" : "module",
          widgetId,
          widgetMetaData: props.metadata,
          uri,
          updatedUri: getWidgetUri(reportType, uri, props.query, props.metadata),
          excludeStatusState,
          supportedCustomFields,
          scmGlobalSettings,
          availableIntegrations: integrations,
          dashboardMetaData: dashboard?.metadata ?? {},
          workflowProfile: workspaceOuProfilestate
        });

        filters = {
          [widgetId]: { ...filters }
        };
      }
    }

    if (!isEqual(payload, filters)) {
      setPayload(filters);
    }
  }, [
    props.query,
    globalFilters,
    props.max_records,
    props.metadata,
    widgetChildren,
    props.selectedReport,
    globalSettingsState,
    scmGlobalSettingsLoading
  ]);

  useEffect(() => {
    const loading = get(excludeStatusState, "loading", true);
    if (!loading) {
      setExcludeFiltersLoading(false);
    }
  }, [excludeStatusState]);

  useEffect(() => {
    const loading = get(globalSettingsState, "loading", true);
    if (!loading) {
      setSCMGlobalSettingsLoading(false);
    }
  }, [globalSettingsState]);

  // When preview becomes disabled, store the most recent valid
  // filters. When preview is re-enabled, check if the filters
  // have changed, and if so, trigger the preview to reload.
  useEffect(() => {
    if (previousPreviewIsCancelled !== previewIsCancelled) {
      if (previewIsCancelled) {
        // Preview has been disabled, store current filters.
        if (previewDisabled) {
          // If previewDisabled is true, it means current
          // filters are invalid, so let's save
          // the previous filters, which presumably
          // are valid.
          queryWhenCancelled.current = previousQuery;
        } else {
          queryWhenCancelled.current = props.query;
        }
      } else {
        // Preview has been re-enabled. Check if filters changed.
        const filtersChanged = !isEqual(queryWhenCancelled.current, props.query);
        if (previousSelectedReport && filtersChanged) {
          setLocalReload(n => n + 1);
        }
      }
    }
  }, [previewIsCancelled, props.query, previewDisabled]);

  useEffect(() => {
    setShowPreview(!props.previewDisabled);
  }, [props.previewDisabled]);

  // NOTE: This code must occur below/after the useEffect for props.previewDisabled above.
  //
  // When switching to hygiene report, disable the preview by default.
  // This useEffect doesn't apply until after the first render with the hygiene report,
  // so we need additional code to block the preview on the first render.
  // hence reportJustChangedToDisabledPreviewOne.
  const reportJustChangedToDisabledPreviewOne =
    previousSelectedReport !== props.selectedReport &&
    [JIRA_MANAGEMENT_TICKET_REPORT.HYGIENE_REPORT, ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT].includes(
      props.selectedReport as any
    )
      ? true
      : false;

  useEffect(() => {
    if (previousSelectedReport !== props.selectedReport) {
      if (
        [JIRA_MANAGEMENT_TICKET_REPORT.HYGIENE_REPORT, ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT].includes(
          props.selectedReport as any
        )
      ) {
        setShowPreview(false);
      } else {
        setShowPreview(true);
      }
    }
  }, [props.selectedReport]);

  useEffect(() => {
    const location = window.location;
    // clearing the store for editing widget
    if (
      location &&
      !location.href.split("/").includes("new") &&
      configurePreview &&
      ![WidgetType.CONFIGURE_WIDGET_STATS, WidgetType.CONFIGURE_WIDGET].includes(graphType as any)
    ) {
      if (WidgetType.COMPOSITE_GRAPH === graphType) {
        if (widgetChildren.length === 1) {
          const uri = get(widgetConstants, [widgetChildren[0].type, "uri"], "");
          const method = get(widgetConstants, [widgetChildren[0].type, "method"], "");
          dispatch(restapiClear(uri, method, props.widgetId));
        } else {
          return;
        }
      }

      dispatch(restapiClear(getWidgetConstant("uri"), getWidgetConstant("method"), props.widgetId));
    }
  }, []);

  // TODO : quick hack for now
  useEffect(() => {
    return () => {
      if (isComposite && props.selectedReport === LEVELOPS_MULTITIME_SERIES_REPORT) {
        const uri = get(widgetConstants, [widgetChildren?.[0]?.type, "uri"], "");
        const method = get(widgetConstants, [widgetChildren?.[0]?.type, "method"], "");
        if (uri && method) {
          dispatch(restapiClear(uri, method, widgetChildren[0].id));
        }
      }
    };
  }, []);

  const validSelectedReport = useMemo(
    () => (props.graphType.includes("composite") ? (widgetChildren || []).length > 0 : props.selectedReport),
    [props.graphType, widgetChildren, props.selectedReport]
  );

  const chartType = useMemo(
    () =>
      props.graphType.includes("composite")
        ? ChartType.COMPOSITE
        : props.graphType === "configurewidget"
        ? props.tableWidgetGraphType
        : getWidgetConstant("chart_type"),
    [props.graphType, props.selectedReport, props.tableWidgetGraphType]
  );

  const chartProps = useMemo(() => {
    let propsFromConstant = getWidgetConstant("chart_props");
    const columns = get(propsFromConstant, ["columns"], []);
    if (columns.length) {
      const _columns = columns.map((item: any) => {
        const newItem = { ...item };
        if (newItem?.sorter) {
          unset(newItem, ["sorter"]);
        }
        return newItem;
      });
      propsFromConstant = {
        ...propsFromConstant,
        columns: _columns
      };
    }
    if (!cacheWidgetPreview) {
      return propsFromConstant;
    }
    return {
      ...propsFromConstant,
      chartProps: {
        ...(propsFromConstant?.chartProps || {}),
        margin: {
          ...(propsFromConstant?.chartProps?.margin || {}),
          bottom: 0
        }
      }
    };
  }, [props.selectedReport, cacheWidgetPreview]);

  const setReload = useCallback(() => {}, []);

  const graphContainer = React.useMemo(() => {
    return (
      <DashboardGraphsContainer
        widgetId={`${props.widgetId}-preview`}
        reportType={props.selectedReport}
        applicationType={props.application}
        uri={getWidgetConstant("uri")}
        method={getWidgetConstant("method")}
        chartType={chartType}
        chartProps={chartProps}
        globalFilters={props.globalFilters}
        localFilters={props.query}
        hiddenFilters={getWidgetConstant("hidden_filters")}
        widgetFilters={getWidgetConstant("filters")}
        reload={false}
        setReload={setReload}
        weights={props.weights ?? {}}
        graphType={props.graphType}
        children={widgetChildren.map((child: any) => {
          let newChild = cloneDeep(child);
          newChild.id = `${child.id}-preview`;
          return newChild;
        })}
        maxRecords={props.max_records}
        childrenMaxRecords={widgetChildren.map((child: any) => ({ [child.id]: child.max_records })) || []}
        chartClickEnable={false}
        hideLegend
        customHygienes={props.custom_hygienes}
        widgetMetaData={props.metadata}
        filterApplyReload={localReload}
        previewOnly={props.previewOnly}
        jiraOrFilters={dashboard?.jira_or_query}
        dashboardMetaData={dashboard?._metadata}
      />
    );
  }, [props, widgetChildren, localReload]);

  const renderReport = () => {
    if (isComposite) {
      return <CompositeWidgetPreview />;
    }

    if (!validSelectedReport) {
      return <EmptyWidgetPreview isStat={isStat} />;
    }

    if (previewIsCancelled) {
      return <EmptyWidgetPreview message="Preview not available." isStat={isStat} />;
    }

    if (reportJustChangedToDisabledPreviewOne) {
      return <EmptyWidgetPreview message="Preview not available." isStat={isStat} />;
    }

    if (isNotes) {
      return (
        <DashboardNotesPreviewComponent dashboardId={dashboardId} widgetId={widgetId} previewOnly={props.previewOnly} />
      );
    }

    return graphContainer;
  };

  if (excludeFiltersLoading || scmGlobalSettingsLoading) {
    return <Loader />;
  }

  return (
    <AntRow>
      <AntCol span={24}>
        <AntCard
          className={cx(
            {
              "no-preview": previewIsCancelled,
              "hide-padding-for-preview": cacheWidgetPreview,
              "my-10": !cacheWidgetPreview
            },
            "widget-preview-container"
          )}
          style={{
            overflowY: "unset",
            height: "100%",
            border: "none"
          }}>
          <div
            data-testid={"widget-preview"}
            className={cx({
              "widget-preview": !cacheWidgetPreview,
              "cached-stat-preview": cacheWidgetPreview && isStat,
              "cached-widget-preview": cacheWidgetPreview && !isStat
            })}>
            {renderReport()}
          </div>
        </AntCard>
        {!cacheWidgetPreview && !getWidgetConstant(PREVIEW_DISABLED) && (
          <Tooltip title={previewDisabled ? "Please fill all required fields before previewing." : null}>
            {[JIRA_MANAGEMENT_TICKET_REPORT.HYGIENE_REPORT, ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT].includes(
              props.selectedReport as any
            ) && (
              <Checkbox
                disabled={previewDisabled}
                className="preview-option"
                checked={reportJustChangedToDisabledPreviewOne ? false : showPreview}
                onChange={event => {
                  setShowPreview(event.target.checked);
                }}>
                <AntText className="title">Enable Preview</AntText>
              </Checkbox>
            )}
          </Tooltip>
        )}
      </AntCol>
    </AntRow>
  );
};

export default WidgetPreviewComponent;
