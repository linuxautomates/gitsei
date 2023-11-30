import React, { useContext, useEffect, useMemo, useRef, useCallback, useState } from "react";
import queryString from "query-string";
import ChartContainer from "../chart-container/chart-container.component";
import { useLocation, useHistory } from "react-router-dom";
import { RestWidget } from "classes/RestDashboards";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get, isEmpty, isEqual } from "lodash";
import { getDateRangeEpochToString } from "utils/dateUtils";
import { useDispatch } from "react-redux";
import { getWidgetaDataAction } from "reduxConfigs/actions/restapi/widgetAPIActions";
import { getWidgetDataSelector } from "reduxConfigs/selectors/widgetAPISelector";
import Loader from "components/Loader/Loader";
import {
  workflowProfileDetailSelector,
  workflowProfileDetailStateSelector
} from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { DORA_REPORTS, GET_GRAPH_FILTERS } from "dashboard/constants/applications/names";
import { getBaseUrl, getWorkflowProfilePage, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import {
  INCOMPLETE_ASSOCIATE_PROFILE_MESSAGE_BEFORE,
  LOADING_ERROR_MESSAGE,
  INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_ERROR_MESSAGE_AFTER,
  INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_ERROR_MESSAGE_BEFORE,
  INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_FUNCTION_CALL,
  INCOMPLETE_ASSOCIATE_PROFILE_MESSAGE_AFTER,
  INVALID_INT_FILTER_MESSAGE,
  INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE,
  INCOMPLETE_ASSOCIATE_PROFILE_REASONS,
  INVALID_INT_FILTER_REASONS
} from "./constants";
import DoraWrapperMessage from "./DoraWrapperMessage";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";
import { WidgetFilterContext, WidgetLoadingContext } from "dashboard/pages/context";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { WORKFLOW_PROFILE_MENU } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";
import {
  extractProfileFilter,
  etractOrgFilters,
  hasCommonString,
  findIntegrationInOrg,
  changeCustomFieldPrefix,
  extractProfileType,
  hasCommonCicdFilter,
  extractProfileEventValue,
  getDoraProfileIntegrationApplication,
  updateRollback,
  getDateTime,
  extractProfileCalculationField,
  hasAppliedFilterOnWidgetForJira
} from "./helper";
import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { transformAzureWidgetQueryForCustomFields } from "dashboard/helpers/helper";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  JIRA_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { DORA_HARNESS_STACK_DROPDOWN_PIPELINES } from "shared-resources/charts/constant";
import { ChartType } from "../chart-container/ChartType";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";

interface DoraAPIWrapperProps {
  id: string;
  globalFilters: any;
  filterApplyReload?: number;
  onChartClick?: (data: any, filters?: any) => void;
  chartClickEnable?: boolean;
  application: string;
  dashboardMetaData: any;
  chartType: ChartType;
  widgetMetaData?: basicMappingType<string>;
  dashboardId: string | undefined;
  uri: string;
}

const DoraAPIWrapper: React.FC<DoraAPIWrapperProps> = ({
  id,
  dashboardMetaData,
  onChartClick,
  chartClickEnable,
  chartType,
  widgetMetaData,
  globalFilters,
  dashboardId,
  uri
}) => {
  const history = useHistory();
  const dispatch = useDispatch();
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: id });
  const reportType = widget?.type;
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const widgetConfig = useMemo(() => get(widgetConstants, [reportType], {}), [reportType]);
  const reportChartProps = useMemo(() => get(widgetConfig, ["chart_props"], {}), [widgetConfig]);
  const widgetDataState = useParamSelector(getWidgetDataSelector, { widgetId: id });
  const { filters: contextFilters } = useContext(WidgetFilterContext);
  const { setWidgetLoading } = useContext(WidgetLoadingContext);
  const filtersRef = useRef<any>({});
  const integrationIds = useMemo(() => {
    return get(globalFilters, ["integration_ids"], []);
  }, [globalFilters]);

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [globalFilters]
  );

  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const workflowProfileLoadingState = useParamSelector(workflowProfileDetailStateSelector, {
    queryParamOU: queryParamOU
  });
  const workflowProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: queryParamOU });
  const orgUnit = useParamSelector(getSelectedOU);

  // COMMNET THIS CODE BECAUSE IN THIS SPRINT WE ARE ONLY SUPPORT PIPELINE DATA SO NO NEED OF THIS , BUT IN FUTURE THIS WILL COME & IT'S RELETED FUNCTION ALSO
  // const [dropdownFilterTypeValue, setdropdownFilterTypeValue] = useState<string>(widget.metadata.harness_profile_stacks || DORA_HARNESS_STACK_DROPDOWN_SERVICES)
  const [dropdownFilterTypeValue, setdropdownFilterTypeValue] = useState<string>(DORA_HARNESS_STACK_DROPDOWN_PIPELINES);

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

  const showDoraGrading = widget?.metadata?.hasOwnProperty("show_dora_grading")
    ? widget.metadata.show_dora_grading
    : true;

  const doraProfileIntegrationApplication = useMemo(
    () => getDoraProfileIntegrationApplication(reportType, workflowProfile),
    [reportType, workflowProfile]
  );

  const widgetLevelFilter = useMemo(() => {
    if (doraProfileIntegrationApplication === IntegrationTypes.HARNESSNG) {
      return updateRollback(widget?.query);
    }
    return widget?.query;
  }, [widget]);

  const dateRangeValue = useMemo(() => {
    return getDateTime(dashboardMetaData, widgetMetaData, widget?.query?.time_range);
  }, [dashboardMetaData]);
  const statDescInterval = useMemo(() => getDateRangeEpochToString(dateRangeValue), [dateRangeValue]);

  const supportedCustomFields = useMemo(() => {
    const applications = (integrations || []).map((item: any) => item.application);
    let customFields: IntegrationTransformedCFTypes[] = [];
    if (applications.includes("jira") && globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(jiraFieldsSelector, "data", [])];
    }

    if (applications.includes("azure_devops") && globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(azureFieldsSelector, "data", [])];
    }

    if (applications.includes("zendesk") && globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(zendeskFieldsSelector, "data", [])];
    }

    return customFields;
  }, [azureFieldsSelector, jiraFieldsSelector, zendeskFieldsSelector, integrations, globalFilters?.integration_ids]);

  const doraProfileIntegrationTypeCheck = useMemo(() => {
    const getdoraProfileType = get(widgetConstants, [reportType as string, "getDoraProfileIntegrationType"], undefined);
    let currentIntegrationType;
    if (getdoraProfileType && workflowProfile) {
      currentIntegrationType = getdoraProfileType({
        integrations,
        workspaceOuProfilestate: workflowProfile
      });
    }

    delete widget?.query["across"];
    const KeysToNeglect = get(widgetConstants, [reportType, "keysToNeglect"], []);
    if (
      widget?.metadata.integration_type &&
      currentIntegrationType &&
      currentIntegrationType !== widget?.metadata.integration_type &&
      Object.keys(widget?.query).filter((key: string) => !["time_range", ...KeysToNeglect].includes(key)).length > 0
    ) {
      return false;
    } else {
      return true;
    }
  }, [workflowProfile, reportType, integrations, widget]);

  const profileType = useMemo(() => {
    const getProfileType = get(widgetConstants, [reportType as string, "getDoraProfileIntegrationType"], undefined);
    if (getProfileType && workflowProfile) {
      return getProfileType({
        integrations,
        workspaceOuProfilestate: workflowProfile,
        doraImprovementFlow: true
      });
    }
    return undefined;
  }, [workflowProfile, integrations]);

  const profileIntId = useMemo(() => {
    const getdoraProfileIntegrationId = get(
      widgetConstants,
      [reportType as string, "getDoraProfileIntegrationId"],
      undefined
    );
    if (getdoraProfileIntegrationId && workflowProfile) {
      return getdoraProfileIntegrationId({
        workspaceOuProfilestate: workflowProfile
      });
    }
    return "";
  }, [reportType, workflowProfile]);

  const doraProfileIntegrationIdCheck = useMemo(() => {
    if (profileIntId && orgUnit.sections?.length > 0) {
      return !!findIntegrationInOrg(orgUnit, profileIntId);
    } else {
      return true;
    }
  }, [orgUnit, profileIntId]);

  const filters = useMemo(() => {
    const time_range = getDateTime(dashboardMetaData, widgetMetaData, widget?.query?.time_range);
    const doraProfileIntegrationApplication = getDoraProfileIntegrationApplication(reportType, workflowProfile);
    let widgetFilter = widget?.query;
    if (doraProfileIntegrationApplication === IntegrationTypes.HARNESSNG) {
      widgetFilter = updateRollback(widget?.query);
    }
    let finalFilters = {
      filter: {
        ...(widgetFilter || {}),
        time_range
      },
      ou_ids: [queryParamOU]
    };
    if (doraProfileIntegrationApplication === IntegrationTypes.HARNESSNG) {
      finalFilters = {
        ...finalFilters,
        ...{ stacks: [dropdownFilterTypeValue] }
      };
    }
    const timeRangeFilter = get(widgetMetaData, "range_filter_choice", {});
    if (!isEmpty(timeRangeFilter)) {
      finalFilters = updateIssueCreatedAndUpdatedFilters(
        finalFilters,
        widgetMetaData,
        widget.type,
        uri
      );
    }

    const getGraphFilters = getWidgetConstant(reportType, GET_GRAPH_FILTERS);
    if (getGraphFilters) {
      finalFilters = getGraphFilters({
        filters: finalFilters,
        contextfilters: contextFilters,
        id,
        dashboardMetaData,
        widgetMetaData,
        reportType,
        widgetQuery: widget?.query,
        integrationIds,
        workflowProfile,
        availableIntegrations: integrations
      });
    }
    if (profileType === IM_ADO) {
      finalFilters = changeCustomFieldPrefix(finalFilters, reportType);
      finalFilters = transformAzureWidgetQueryForCustomFields(finalFilters, supportedCustomFields as any);
    }
    return finalFilters;
  }, [widget?.query, reportType, contextFilters, id, workflowProfile, dropdownFilterTypeValue, dashboardMetaData]);

  const isFilterConflict = useMemo(() => {
    if (workflowProfile && widget?.query && orgUnit) {
      const workflowProfileObj = new RestWorkflowProfile(workflowProfile);
      const profileFilter = extractProfileFilter(workflowProfileObj, reportType);
      const orgFilter = etractOrgFilters(profileIntId, orgUnit);
      const widgetFilters = Object.keys(widget.query);
      const profileType = extractProfileType(workflowProfileObj, reportType);
      const profileEventValue = extractProfileEventValue(workflowProfileObj, reportType);
      const profileCalculationFiled = extractProfileCalculationField(workflowProfileObj, reportType);

      return (
        hasCommonString(profileFilter, orgFilter) ||
        hasCommonString(orgFilter, widgetFilters) ||
        hasCommonString(widgetFilters, profileFilter) ||
        hasCommonCicdFilter(orgFilter, profileType, profileEventValue) ||
        hasAppliedFilterOnWidgetForJira(
          profileType,
          doraProfileIntegrationApplication,
          profileCalculationFiled,
          widgetFilters
        )
      );
    }
    return false;
  }, [workflowProfile, widget, orgUnit, reportType, profileIntId, doraProfileIntegrationApplication]);

  useEffect(() => {
    if (!doraProfileIntegrationIdCheck || !doraProfileIntegrationTypeCheck || isFilterConflict || !workflowProfile) {
      setWidgetLoading(id, false);
    } else {
      setWidgetLoading(id, widgetDataState.isLoading);
    }
  }, [
    widgetDataState,
    id,
    setWidgetLoading,
    isFilterConflict,
    doraProfileIntegrationIdCheck,
    doraProfileIntegrationTypeCheck,
    workflowProfile
  ]);

  useEffect(() => {
    if (
      workflowProfile &&
      doraProfileIntegrationIdCheck &&
      !workflowProfileLoadingState.isLoading &&
      !isEqual(filtersRef.current, filters) &&
      doraProfileIntegrationTypeCheck &&
      !isFilterConflict
    ) {
      filtersRef.current = filters;
      const getApiAction = getWidgetConstant(reportType, STORE_ACTION);
      if (getApiAction) {
        dispatch(getApiAction(reportType, id, filters));
      } else {
        dispatch(getWidgetaDataAction(reportType, id, filters));
      }
    }
  }, [
    filters,
    doraProfileIntegrationTypeCheck,
    doraProfileIntegrationIdCheck,
    isFilterConflict,
    reportType,
    dispatch,
    id
  ]);

  const onClickHandler = (data: any) => {
    if (onChartClick) {
      let finalFilters = {
        ...widgetLevelFilter,
        ...data,
        integration_ids: integrationIds
      };
      if (doraProfileIntegrationApplication === IntegrationTypes.HARNESSNG) {
        finalFilters = {
          ...finalFilters,
          ...{ stacks: [dropdownFilterTypeValue] }
        };
      }
      onChartClick(finalFilters);
    }
  };

  const handleRedirectToProfileDetailPage = useCallback(() => {
    history.push(
      `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}?configId=${workflowProfile.id}&tabComponent=${WORKFLOW_PROFILE_MENU.CHANGE_FAILURE_RATE
      }`
    );
  }, [history, workflowProfile]);

  const transformedData = useMemo(() => {
    const apiData = get(widgetDataState, ["data"], []);
    const transFunc = get(widgetConstants, [reportType, "transformFunction"], undefined);
    if (transFunc) {
      return transFunc({ reportType, apiData, metadata: widget?.metadata })?.data || [];
    }
    return apiData;
  }, [widgetDataState, widget]);

  const widgetChartProps = useMemo(() => {
    const chartPropsFunc = get(widgetConstants, [reportType, "getChartProps"], undefined);
    if (chartPropsFunc) {
      return chartPropsFunc({ widgetMetaData: widget?.metadata });
    }
    return {};
  }, [reportType, widget]);

  const updatedChartProps = {
    ...reportChartProps,
    apiData: widgetDataState.data,
    trendData: {},
    dateRange: dateRangeValue,
    onDoraClick: onClickHandler,
    hasClickEvents: chartClickEnable,
    statDescInterval,
    showDoraGrading,
    reportType: reportType,
    id: id,
    widgetMetaData: widgetMetaData,
    dateRangeValue: dateRangeValue,
    workflowProfile: workflowProfile,
    filters,
    data: transformedData,
    onClick: onClickHandler,
    // onFilterDropdownClick: onFilterDropdownClick,
    dropdownFilterTypeValue: dropdownFilterTypeValue,
    getProfileIntegrationApplication: doraProfileIntegrationApplication,
    ...widgetChartProps
  };

  if (workflowProfileLoadingState.isLoading) {
    return <Loader />;
  }

  if (!workflowProfileLoadingState.isLoading && !workflowProfile) {
    return (
      <DoraWrapperMessage
        link={getWorkflowProfilePage()}
        message={INCOMPLETE_ASSOCIATE_PROFILE_MESSAGE_BEFORE}
        messageAfterLinkToAppend={INCOMPLETE_ASSOCIATE_PROFILE_MESSAGE_AFTER}
        widgetTitle={widget.name}
        reasons={INCOMPLETE_ASSOCIATE_PROFILE_REASONS}
      />
    );
  }

  if (!doraProfileIntegrationIdCheck || !doraProfileIntegrationTypeCheck || isFilterConflict) {
    return (
      <DoraWrapperMessage
        message={INVALID_INT_FILTER_MESSAGE}
        widgetTitle={widget.name}
        reasons={INVALID_INT_FILTER_REASONS}
      />
    );
  }

  if (widgetDataState.isLoading && ![DORA_REPORTS.LEADTIME_CHANGES].includes(reportType)) return <Loader />;
  if (widgetDataState.error && ![DORA_REPORTS.LEADTIME_CHANGES].includes(reportType)) {
    if (
      [DORA_REPORTS.CHANGE_FAILURE_RATE].includes(reportType) &&
      widgetDataState.error?.response?.data?.message?.startsWith(INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE)
    ) {
      return (
        <DoraWrapperMessage
          link={INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_FUNCTION_CALL}
          message={INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_ERROR_MESSAGE_BEFORE}
          messageAfterLinkToAppend={INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_ERROR_MESSAGE_AFTER}
          redirectWidgetEdit={handleRedirectToProfileDetailPage}
          widgetTitle={widget.name}
        />
      );
    }
    return <DoraWrapperMessage message={LOADING_ERROR_MESSAGE} widgetTitle={widget.name} />;
  }

  return <ChartContainer chartType={chartType as any} chartProps={updatedChartProps} />;
};

export default DoraAPIWrapper;
