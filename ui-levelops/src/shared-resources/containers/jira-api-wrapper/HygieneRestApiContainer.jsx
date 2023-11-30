import React, { useEffect, useState, useContext, useRef, useMemo } from "react";
import { connect } from "react-redux";
import { get, isEqual, unset, cloneDeep } from "lodash";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import ChartContainer from "../chart-container/chart-container.component";
import { ChartType } from '../chart-container/ChartType'
import Loader from "components/Loader/Loader";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { mapHygieneScoresDispatchToProps } from "reduxConfigs/maps/hygieneScores.map";
import { mapFormDispatchToPros, mapFormStateToProps } from "reduxConfigs/maps/formMap";
import { useDataTransform, useGlobalFilters } from "custom-hooks";
import { CacheWidgetPreview, WidgetLoadingContext } from "dashboard/pages/context";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";
import {
  combineAllFilters,
  getSupportedApplications,
  sanitizeCustomDateFilters,
  updateTimeFiltersValue
} from "../widget-api-wrapper/helper";
import { EmptyWidget } from "../../components";
import { customFieldFiltersSanitize } from "custom-hooks/helpers/zendeskCustomFieldsFiltersTransformer";
import { ReportsApplicationType } from "dashboard/constants/helper";
import widgetConstants from "dashboard/constants/widgetConstants";
import {
  azureIterationSupportableReports,
  HYGIENE_TREND_REPORT
} from "dashboard/constants/applications/names";
import { uniq } from "lodash";
import { ACTIVE_SPRINT_TYPE_FILTER_KEY } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
import { sanitizeObject } from "utils/commonUtils";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  JIRA_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import queryString from "query-string";
import { useLocation } from "react-router-dom";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { transformAzureWidgetQueryForCustomFields } from "dashboard/helpers/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const HygieneRestApiContainer = props => {
  const [dataFromApi, setDataFromApi] = useState({});
  const [hygieneMapping, setHygieneMapping] = useState({});
  const [fetchData, setFetchData] = useState(true);
  const { setWidgetLoading } = useContext(WidgetLoadingContext);
  const reportRef = useRef();
  const metadataRef = useRef();
  const filterApplyReloadRef = useRef({});
  const filtersRef = useRef({});
  const cacheWidgetPreview = useContext(CacheWidgetPreview);
  const integrationIds = useMemo(() => {
    return get(props.globalFilters, ["integration_ids"], []);
  }, [props.globalFilters?.integration_ids]);

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [integrationIds]
  );
  const jiraFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const azureFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const zendeskFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: props.globalFilters?.integration_ids
  });

  const application = useMemo(
    () => get(widgetConstants, [props.reportType, "application"], undefined),
    [props.reportType]
  );

  const fetchDataAgain = () => {
    if (cacheWidgetPreview) {
      return;
    }

    const id = props.id || "0";
    props.formClear(`hygiene_score_${id}`);
    setFetchData(true);
  };

  const isSupportedCusotmFieldsLoading = useMemo(() => {
    const applications = (integrations || [])?.map(item => item.application);
    if (applications.includes("jira")) {
      return get(jiraFieldsSelector, "loading", true);
    }

    if (applications.includes("azure_devops")) {
      return get(azureFieldsSelector, "loading", true);
    }

    if (applications.includes("zendesk")) {
      return get(zendeskFieldsSelector, "loading", true);
    }
    if (!applications.includes("zendesk") && !applications.includes("azure_devops") && !applications.includes("jira")) {
      return false;
    }
    return true;
  });

  const supportedCustomFields = useMemo(() => {
    const applications = (integrations || []).map(item => item.application);
    let customFields = [];
    if (applications.includes("jira")) {
      customFields = [...customFields, ...get(jiraFieldsSelector, "data", [])];
    }

    if (applications.includes("azure_devops")) {
      customFields = [...customFields, ...get(azureFieldsSelector, "data", [])];
    }

    if (applications.includes("zendesk")) {
      customFields = [...customFields, ...get(zendeskFieldsSelector, "data", [])];
    }

    return customFields;
  }, [azureFieldsSelector, jiraFieldsSelector, zendeskFieldsSelector]);

  const filters = props.filters && Object.keys(props.filters).length ? props.filters : {};
  const globalFilters = useGlobalFilters(props.globalFilters);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU;
  const widgetFilters = props.widgetFilters || {};
  const reportData = useDataTransform(dataFromApi, props.reportType, props.uri, 20, props.filters, null);

  const getFilters = (updateTimeFilters = true) => {
    const combinedFilters = combineAllFilters(widgetFilters, filters, props.hiddenFilters);

    let finalFilters = cloneDeep({
      filter: {
        ...combinedFilters,
        ...props.globalFilters
      }
    });

    if (finalFilters.filter.hasOwnProperty("across")) {
      const across = finalFilters.filter.across;
      delete finalFilters.filter["across"];
      finalFilters = {
        ...finalFilters,
        across
      };
    }

    if (updateTimeFilters) {
      finalFilters = updateIssueCreatedAndUpdatedFilters(finalFilters, props.widgetMetaData);
    }

    if (application === ReportsApplicationType.ZENDESK) {
      finalFilters = customFieldFiltersSanitize(finalFilters, true);
    }

    if (["jira"].includes(application) && Object.keys(props.jiraOrFilters || {}).length > 0) {
      finalFilters = {
        ...finalFilters,
        filter: {
          ...(finalFilters.filter || {}),
          or: props.jiraOrFilters
        }
      };
    }

    if (["azure_devops"].includes(application)) {
      const customFields = get(finalFilters, ["filter", "custom_fields"], {});
      const excludeFields = get(finalFilters, ["filter", "exclude"], {});
      const excludeCustomFields = get(excludeFields, ["custom_fields"], {});
      if (Object.keys(customFields).length > 0) {
        unset(finalFilters, ["filter", "custom_fields"]);
        finalFilters = {
          ...(finalFilters || {}),
          filter: {
            ...(finalFilters?.filter || {}),
            workitem_custom_fields: {
              ...(customFields || {})
            }
          }
        };
      }
      if (Object.keys(excludeFields).length > 0 && excludeFields?.custom_fields) {
        unset(finalFilters, ["filter", "custom_fields"]);
        unset(finalFilters, ["filter", "exclude", "custom_fields"]);
        finalFilters = {
          ...(finalFilters || {}),
          filter: {
            ...(finalFilters?.filter || {}),
            exclude: {
              ...get(finalFilters, ["filter", "exclude"], {}),
              workitem_custom_fields: { ...excludeCustomFields }
            }
          }
        };
      }
    }

    if (HYGIENE_TREND_REPORT.includes(props.reportType)) {
      const interval = finalFilters?.filter?.interval || "month";
      unset(finalFilters, ["filter", "interval"]);
      finalFilters = {
        ...(finalFilters || {}),
        interval
      };
    }

    const excludeAzureIterationValues = get(finalFilters, ["filter", "exclude", "azure_iteration"], undefined);
    if (application === IntegrationTypes.AZURE && excludeAzureIterationValues) {
      const newExcludeAzureIterationValues = excludeAzureIterationValues.map(value => {
        if (typeof value === "object") {
          return `${value.parent}\\${value.child}`;
        } else {
          // This is just for backward compatibility with old version that had string values
          return value;
        }
      });
      let key = "workitem_sprint_full_names";
      unset(finalFilters, ["filter", "exclude", "azure_iteration"]);
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          exclude: {
            ...finalFilters?.filter?.exclude,
            [key]: newExcludeAzureIterationValues
          }
        }
      };
    }

    const partialAzureIterationValue = get(finalFilters, ["filter", "partial_match", "azure_iteration"], undefined);
    if (application === IntegrationTypes.AZURE && partialAzureIterationValue) {
      let key = "workitem_sprint_full_names";
      unset(finalFilters, ["filter", "partial_match", "azure_iteration"]);
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          partial_match: {
            ...finalFilters?.filter?.partial_match,
            [key]: partialAzureIterationValue
          }
        }
      };
    }

    const azureIterationValues = get(finalFilters, ["filter", "azure_iteration"], undefined);
    if (azureIterationValues) {
      const newAzureIterationValues = azureIterationValues.map(value => {
        if (typeof value === "object") {
          return `${value.parent}\\${value.child}`;
        } else {
          // This is just for backward compatibility with old version that had string values
          return value;
        }
      });
      let key = "workitem_sprint_full_names";
      unset(finalFilters, ["filter", "azure_iteration"]);
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          [key]: newAzureIterationValues
        }
      };
    }

    const azureCodeAreaValues = get(finalFilters, ["filter", "workitem_attributes", "code_area"], undefined);
    if (azureCodeAreaValues) {
      const newAzureCodeAreaValues = azureCodeAreaValues?.map(value => {
        if (typeof value === "object") {
          return `${value?.child}`;
        } else {
          // This is just for backward compatibility with old version that had string values
          return value;
        }
      });
      let key = "workitem_attributes";
      finalFilters = {
        ...finalFilters,
        filter: {
          ...(finalFilters?.filter || {}),
          [key]: {
            ...finalFilters?.filter?.workitem_attributes,
            ["code_area"]: newAzureCodeAreaValues
          }
        }
      };
    }

    // default key for sprint_states in ApiManyOptions is jira_sprint_states
    // so changing it to sprint_states
    if (application === IntegrationTypes.JIRA && finalFilters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY]) {
      finalFilters = {
        ...finalFilters,
        filter: { ...finalFilters.filter, sprint_states: finalFilters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY] }
      };
      unset(finalFilters, ["filter", ACTIVE_SPRINT_TYPE_FILTER_KEY]);
    }

    const ou_ids = queryParamOU ? [queryParamOU] : get(props.dashboardMetaData, "ou_ids", []);

    if (
      ou_ids.length &&
      ["azure_devops", "jira", "jenkins", "github", "githubjira", "jenkinsgithub"].includes(application)
    ) {
      let combinedOUFilters = {
        ...get(props.dashboardMetaData, "ou_user_filter_designation", {}),
        ...sanitizeObject(get(props.widgetMetaData, "ou_user_filter_designation", {}))
      };

      const supportedApplications = getSupportedApplications(props.reportType);
      Object.keys(combinedOUFilters).forEach(key => {
        if (!supportedApplications.includes(key)) {
          delete combinedOUFilters?.[key];
        }
      });

      if (["jira", "azure_devops", "githubjira"].includes(application)) {
        let sprint = undefined;

        if (azureIterationSupportableReports.includes(props.reportType)) {
          sprint = "sprint_report";
        } else {
          const sprintCustomField = supportedCustomFields.find(item =>
            (item.name || "").toLowerCase().includes("sprint")
          );
          if (sprintCustomField) {
            sprint = sprintCustomField?.field_key;
          }
        }
        combinedOUFilters = {
          ...combinedOUFilters,
          sprint: [sprint]
        };
      }
      if (
        combinedOUFilters?.hasOwnProperty("sprint") &&
        (!combinedOUFilters?.sprint || !combinedOUFilters?.sprint?.[0])
      ) {
        delete combinedOUFilters?.sprint;
      }
      finalFilters = {
        ...finalFilters,
        ou_ids,
        ou_user_filter_designation: Object.keys(combinedOUFilters).length ? combinedOUFilters : undefined
      };
    }

    finalFilters = sanitizeCustomDateFilters(finalFilters, supportedCustomFields);

    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...updateTimeFiltersValue(props.dashboardMetaData, props.widgetMetaData, finalFilters?.filter)
      }
    };

    if (application === IntegrationTypes.AZURE) {
      finalFilters = transformAzureWidgetQueryForCustomFields(finalFilters, supportedCustomFields);
    }

    const widgetFiltersTransformer = get(widgetConstants, [props.reportType, "widget_filter_transform"]);
    if (widgetFiltersTransformer) {
      finalFilters = widgetFiltersTransformer(finalFilters);
    }

    return finalFilters;
  };

  useEffect(() => {
    filtersRef.current = getFilters();
    reportRef.current = props.reportType;
    metadataRef.current = props.widgetMetaData;
    filterApplyReloadRef.current = props.filterApplyReload;
  }, []);

  useEffect(() => {
    const allFilters = getFilters(false);
    if (!isEqual(allFilters, filtersRef.current) && !fetchData) {
      filtersRef.current = allFilters;
      fetchDataAgain();
    }
  }, [filters, widgetFilters, props.hiddenFilters, props.dashboardMetaData, queryParamOU, fetchData]);

  useEffect(() => {
    if (!isEqual(props.reportType, reportRef.current) && !fetchData) {
      reportRef.current = props.reportType;
      fetchDataAgain();
    }
  }, [props.reportType]);

  useEffect(() => {
    const oldDashboardTimeRange = get(metadataRef.current, ["dashBoard_time_keys"], {});
    const newDashboardTimeRange = get(props.widgetMetaData, ["dashBoard_time_keys"], {});
    const dashbaordTimeEnabledDisabledChanged = !isEqual(oldDashboardTimeRange, newDashboardTimeRange);
    if (!isEqual(props.widgetMetaData, metadataRef.current) && !fetchData && !dashbaordTimeEnabledDisabledChanged) {
      metadataRef.current = props.widgetMetaData;
      fetchDataAgain();
    }
  }, [props.widgetMetaData]);

  useEffect(() => {
    if (!isEqual(props.filterApplyReload, filterApplyReloadRef.current) && !fetchData) {
      filterApplyReloadRef.current = props.filterApplyReload;
      fetchDataAgain();
    }
  }, [props.filterApplyReload]);

  useEffect(() => {
    setWidgetLoading(props.id, fetchData);
    if (fetchData && !isSupportedCusotmFieldsLoading) {
      setDataFromApi([]);
      setHygieneMapping({});
      const id = props.id || "0";
      const weights = props.weights || {};
      const report = props.reportType;
      const metadata = props.widgetMetaData;
      if (props.chartType === ChartType.SCORE) {
        if (props.reportType === "azure_hygiene_report")
          props.azureHygieneReport(report, id, getFilters(), weights, props.customHygienes || []);
        else props.hygieneReport(report, id, getFilters(), weights, props.customHygienes || []);
      } else {
        if (props.reportType === "azure_hygiene_report_trends")
          props.azureHygieneTrend(report, id, getFilters(), weights, props.customHygienes || []);
        else props.hygieneTrend(report, id, getFilters(), weights, props.customHygienes || [], metadata);
      }
    }
  }, [fetchData, isSupportedCusotmFieldsLoading]);

  useEffect(() => {
    const id = props.id || "0";
    if (id) {
      const hygiene_form = `hygiene_score_${id}`;
      if (props.form_state[hygiene_form]) {
        if (HYGIENE_TREND_REPORT.includes(props.reportType)) {
          const hygieneData = props.form_state[hygiene_form];
          setDataFromApi(hygieneData?.data);
          setHygieneMapping(hygieneData?.hygieneMapping);
        } else {
          setDataFromApi(props.form_state[hygiene_form]);
        }
        setFetchData(false);
        if (props.reload) {
          props.setReload(false);
        }
      }
    }
  }, [props.id, props.reload, props.form_state]);

  useEffect(() => {
    if (props.reload) {
      fetchDataAgain();
    }
  }, [props.reload]);

  useEffect(() => {
    return () => {
      const id = props.id || "0";
      props.formClear(`hygiene_score_${id}`);
    };
  }, []);

  const getChartPropsAndData = () => {
    let chartProps = { ...props.chartProps };

    if (HYGIENE_TREND_REPORT.includes(props.reportType)) {
      const visualization = get(props.filters, ["visualization"], "stacked_area");
      if (visualization === "stacked_bar") {
        const keys = (reportData?.data || []).reduce((acc, next) => {
          return uniq([...acc, ...Object.keys(next).filter(key => !["name", "key"].includes(key))]);
        }, []);

        const updatedBarProps = keys.map(key => ({
          name: key,
          dataKey: key
        }));

        chartProps = {
          ...chartProps,
          stacked: true,
          barProps: updatedBarProps
        };
      }
    }
    return {
      id: props.id,
      hasClickEvents: props.chartClickEnable,
      onClick: data => props.onChartClick && props.onChartClick(data),
      ...reportData,
      ...chartProps,
      previewOnly: props.previewOnly,
      hideLegend: props.hideLegend,
      hideScore: filters.hideScore,
      reportType: props.reportType,
      hygieneMapping: hygieneMapping,
      widgetMetaData: props.widgetMetaData
    };
  };

  const getChartType = () => {
    if (HYGIENE_TREND_REPORT.includes(props.reportType)) {
      const visualization = get(props.filters, ["visualization"], "stacked_area");
      if (visualization === "stacked_bar") {
        return ChartType.HYGIENE_BAR_CHART;
      } else {
        return ChartType.HYGIENE_AREA_CHART;
      }
    }
    return props.chartType;
  };

  return (
    <div style={{ height: "100%" }}>
      {fetchData && <Loader />}
      {!fetchData && dataFromApi && Object.keys(dataFromApi).length > 0 && (
        <ChartContainer chartType={getChartType()} chartProps={getChartPropsAndData()} />
      )}
      {!fetchData && Object.keys(dataFromApi).length === 0 && <EmptyWidget />}
    </div>
  );
};

const mapStateToProps = state => ({
  ...mapFormStateToProps(state)
});

const mapDispatchToProps = dispatch => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapGenericToProps(dispatch),
  ...mapHygieneScoresDispatchToProps(dispatch),
  ...mapFormDispatchToPros(dispatch)
});

export default connect(mapStateToProps, mapDispatchToProps)(HygieneRestApiContainer);
