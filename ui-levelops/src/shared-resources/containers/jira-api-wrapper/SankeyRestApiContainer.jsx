import widgetConstants from "dashboard/constants/widgetConstants";
import { cloneDeep, get, isEqual } from "lodash";
import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import { connect } from "react-redux";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  JIRA_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import Loader from "../../../components/Loader/Loader";
import { useDataTransform, useGlobalFilters } from "../../../custom-hooks";
import { updateIssueCreatedAndUpdatedFilters } from "../../../dashboard/graph-filters/components/updateFilter.helper";
import { CacheWidgetPreview, WidgetLoadingContext } from "../../../dashboard/pages/context";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { getData } from "../../../utils/loadingUtils";
import { EmptyWidget } from "../../components";
import ChartContainer from "../chart-container/chart-container.component";
import {
  combineAllFilters,
  mapWidgetMetadataForCompare,
  sanitizeCustomDateFilters,
  updateTimeFiltersValue
} from "../widget-api-wrapper/helper";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import queryString from "query-string";
import { useLocation } from "react-router-dom";
// add mapping for sankey charts
const mapApplicationTypeToApiCall = {
  jira_zendesk_report: "jiraZendeskGet",
  jira_salesforce_report: "jiraSalesforceGet"
};

const SankeyRestApiContainer = props => {
  const [dataFromApi, setDataFromApi] = useState([]);
  const [fetchData, setFetchData] = useState(0);
  const [loading, setLoading] = useState(false);
  const [rearrangeWidgetState, setRearrangeWidgetState] = useState(props.previewOnly);

  const { setWidgetLoading } = useContext(WidgetLoadingContext);
  const filters = props.filters && Object.keys(props.filters).length ? props.filters : {};
  const globalFilters = useGlobalFilters(props.globalFilters);
  const widgetFilters = props.widgetFilters || {};
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU;
  const filtersRef = useRef({});
  const reportRef = useRef();
  const metadataRef = useRef();
  const filterApplyReloadRef = useRef({});
  const integrationIds = useMemo(() => {
    return get(props.globalFilters, ["integration_ids"], []);
  }, [props.globalFilters?.integration_ids]);

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: props.globalFilters?.integration_ids
  });

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

  const cacheWidgetPreview = useContext(CacheWidgetPreview);

  const supportedCustomFields = useMemo(() => {
    const applications = (integrations || []).map(item => item.application);

    if (applications.includes("jira")) {
      if (["jira_zendesk_report", "jira_salesforce_report"].includes(props.reportType)) {
        return get(jiraFieldsSelector, "data", []).map(item => {
          return {
            ...item,
            field_key: `jira_${item.field_key}`
          };
        });
      }
      return get(jiraFieldsSelector, "data", []);
    }

    if (applications.includes("azure_devops")) {
      return get(azureFieldsSelector, "data", []);
    }

    if (applications.includes("zendesk")) {
      return get(zendeskFieldsSelector, "data", []);
    }

    return [];
  }, [azureFieldsSelector, jiraFieldsSelector, zendeskFieldsSelector, props.reportType]);

  const widgetMetaToCompare = useMemo(() => {
    return mapWidgetMetadataForCompare(props.widgetMetaData);
  }, [props.widgetMetaData]);

  const fetchDataAgain = (forceRefresh = false) => {
    if (cacheWidgetPreview && !forceRefresh && !rearrangeWidgetState) {
      return;
    }
    if (rearrangeWidgetState) {
      setRearrangeWidgetState(false);
    }
    setFetchData(prev => prev + 1);
  };

  useEffect(() => {
    filtersRef.current = getFilters();
    reportRef.current = props.reportType;
    metadataRef.current = widgetMetaToCompare;
    filterApplyReloadRef.current = props.filterApplyReload;
  }, []);

  useEffect(() => {
    const allFilters = getFilters(false);
    if (!isEqual(allFilters, filtersRef.current) && !loading) {
      filtersRef.current = allFilters;
      fetchDataAgain();
    }
  }, [filters, widgetFilters, props.hiddenFilters, props.dashboardMetaData, queryParamOU]);

  useEffect(() => {
    if (!isEqual(props.reportType, reportRef.current) && !loading) {
      reportRef.current = props.reportType;
      fetchDataAgain();
    }
  }, [props.reportType]);

  useEffect(() => {
    if (!isEqual(widgetMetaToCompare, metadataRef.current) && !loading) {
      metadataRef.current = widgetMetaToCompare;
      fetchDataAgain();
    }
  }, [props.widgetMetaData]);

  useEffect(() => {
    if (!isEqual(props.filterApplyReload, filterApplyReloadRef.current) && !loading) {
      filterApplyReloadRef.current = props.filterApplyReload;
      fetchDataAgain();
    }
  }, [props.filterApplyReload]);

  const reportData = useDataTransform(dataFromApi, props.reportType, props.uri, 20, props.filters, null);

  const getFilters = (updateTimeFilters = true) => {
    const combinedFilters = combineAllFilters(widgetFilters, cloneDeep(filters), props.hiddenFilters);

    let finalFilters = {
      filter: {
        ...combinedFilters,
        ...props.globalFilters
      }
    };
    if (finalFilters.filter.hasOwnProperty("across")) {
      const across = finalFilters.filter.across;
      delete finalFilters.filter["across"];
      finalFilters = {
        ...finalFilters,
        across
      };
    }

    if (updateTimeFilters) {
      finalFilters = updateIssueCreatedAndUpdatedFilters(finalFilters, props.widgetMetaData, props.reportType);
    }

    const constFiltersData = get(widgetConstants, [props.reportType, "filters"], {});
    const constHiddenFilters = get(widgetConstants, [props.reportType, "hidden_filters"], {});
    finalFilters = {
      ...finalFilters,
      filter: {
        ...finalFilters.filter,
        ...(constFiltersData || {}),
        ...(constHiddenFilters || {})
      }
    };

    if (
      ["jirazendesk", "jirasalesforce"].includes(props.application) &&
      Object.keys(props.jiraOrFilters || {}).length > 0
    ) {
      finalFilters = {
        ...finalFilters,
        filter: {
          ...(finalFilters.filter || {}),
          jira_or: props.jiraOrFilters
        }
      };
    }
    finalFilters = sanitizeCustomDateFilters(finalFilters, supportedCustomFields);
    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...updateTimeFiltersValue(props.dashboardMetaData, props.widgetMetaData, finalFilters?.filter)
      }
    };

    return finalFilters;
  };

  useEffect(() => {
    if (fetchData) {
      //setDataFromApi([]);
      props[mapApplicationTypeToApiCall[props.reportType]](props.uri, getFilters(), props.id);
      setLoading(true);
      setWidgetLoading(props.id, true);
    }
  }, [fetchData]);

  useEffect(() => {
    const uri = `${props.uri}_combined`;
    //const { loading, error } = loadingStatus(props.rest_api, props.uri, props.method, "0");
    const loading = get(props.rest_api, [uri, props.method, props.id, "loading"], true);
    const error = get(props.rest_api, [uri, props.method, props.id, "error"], false);
    if (!loading && !error) {
      const data = getData(props.rest_api, uri, props.method, props.id);
      if (data) {
        setDataFromApi(data);
        setLoading(false);
        setWidgetLoading(props.id, false);
      }
    }
  });

  useEffect(() => {
    if (props.reload) {
      setLoading(true);
      setWidgetLoading(props.id, true);
      fetchDataAgain();
    }
  }, [props.reload]);

  useEffect(() => {
    if (!loading && props.reload) {
      props.setReload && props.setReload(false);
    }
  }, [loading, dataFromApi]);

  const getChartPropsAndData = () => {
    return {
      id: props.id,
      hasClickEvents: props.chartClickEnable,
      onClick: data => props.onChartClick && props.onChartClick(data),
      ...reportData,
      ...props.chartProps,
      previewOnly: props.previewOnly
    };
  };

  return (
    <div style={{ height: "100%" }}>
      {loading && <Loader />}
      {!loading && dataFromApi && Object.keys(dataFromApi).length > 0 && (
        <ChartContainer chartType={props.chartType} chartProps={getChartPropsAndData()} />
      )}
      {!loading && Object.keys(dataFromApi).length === 0 && <EmptyWidget />}
    </div>
  );
};

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(SankeyRestApiContainer);
