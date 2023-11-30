import { get, isEqual } from "lodash";
import { DependencyList, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";

import { genericWidgetFilterValuesGet, widgetFilterValuesGet } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { getData, loadingStatus } from "../utils/loadingUtils";
import { JENKINS_AZURE_REPORTS } from "../dashboard/constants/applications/names";
import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";

const ZENDESK_APPS = ["zendesk"];
const JIRA_APPS = ["jira_assignee_time_report", "jira", "jira_stat", WorkflowIntegrationType.IM];
const JIRA_ZENDESK_APPS = [...JIRA_APPS, "jirazendesk", "azure_devops"];

export function useSupportedFilters(
  supportedFilters: any,
  integrationIds: Array<string>,
  application: string,
  deps?: DependencyList,
  removeIntegration?: boolean,
  id: string = "",
  isConfigLoaded?: boolean
) {
  const hasMultiAPIs = Array.isArray(supportedFilters);
  // This skip custom is added to id to cache data at different places in the 2 cases we encounter:
  // Case 1: get data with custom field with some integration_ids
  // Case 2: get data without custom field with same integration_ids
  const uuid = integrationIds.length
    ? integrationIds.sort().join("_") + id + (supportedFilters?.skipFetchCustomData ? "_skip_custom" : "")
    : "0";
  const dispatch = useDispatch();
  const rest_api = useSelector(restapiState);
  const genericSelector = useParamSelector(getGenericRestAPISelector, {
    uri: get(supportedFilters, "uri", ""),
    method: "list",
    uuid
  });

  const [loadingData, setLoading] = useState<boolean>(false);

  const [error, setError] = useState<boolean>(false);

  const [apiData, setApiData] = useState<any[]>([]);

  const [hasNext, setHasNext] = useState<boolean>(false);

  const [fetchData, setFetchData] = useState([...(deps || [])]);

  const forceRefresh = useMemo(() => {
    let refresh = false;
    let data: String[] = [];
    if (!isEqual(deps, fetchData)) {
      return true;
    }
    // for scm_jira_files_report and scm_files_report some filters are not visible
    // thus checked if data is not present (i.e data in an empty object) in the store then force refresh it
    if (hasMultiAPIs) {
      for (let i = 0; i < (supportedFilters || []).length; i++) {
        const filter = supportedFilters[i];
        if (!(Object.keys(get(rest_api, [filter.uri, "list", uuid, "data"], {})).length > 0)) {
          return true;
        }
        data = [
          ...data,
          ...get(rest_api, [filter.uri, "list", uuid, "data"], {}).records.map((record: any) => Object.keys(record)[0])
        ];
        if (!filter.values.every((val: string) => data.includes(val))) {
          return true;
        }
      }
    } else {
      if (!(Object.keys(get(genericSelector, ["data"], {})).length > 0)) {
        return true;
      }
      data = get(genericSelector, "data", {})?.records?.map((record: any) => Object.keys(record)[0]);
      if (data && !supportedFilters.values.every((val: string) => data.includes(val))) {
        return true;
      }
    }
    return refresh;
  }, [rest_api, supportedFilters, ...(deps || [])]);

  useEffect(() => {
    if (!isEqual(deps, fetchData)) {
      setFetchData([...(deps || [])]);
    }
  }, [...(deps || [])]);

  useEffect(() => {
    if (genericSelector.error && !error) {
      setLoading(false);
      setError(true);
    } else {
      setError(false);
    }
  }, [genericSelector.error]);

  // Call API again if filters change.
  useEffect(
    () => {
      if (!forceRefresh) {
        if (hasMultiAPIs) {
          let finalData: any[] = [];
          let hasNext: boolean = false;
          const totalLength = (supportedFilters || []).reduce((acc: any, filter: any) => filter.values.length + acc, 0);
          (supportedFilters || []).forEach((item: any) => {
            const { loading, error } = loadingStatus(rest_api, item.uri, "list", uuid);
            if (!loading && !error) {
              const data = getData(rest_api, item.uri, "list", uuid).records.filter((record: any) => {
                const key = Object.keys(record)[0];
                return (
                  item.values.includes(key) || key.includes("customfield_") || key.includes(AZURE_CUSTOM_FIELD_PREFIX)
                );
              });
              hasNext = get(rest_api, [item.uri, "list", uuid, "data", "_metadata", "has_next"], false);
              if (data && data.length > 0) {
                finalData = [...finalData, ...data];
              }
            }
          });
          if (finalData.length === totalLength) {
            setLoading(false);
            setApiData(finalData);
            setHasNext(hasNext);
          }
        } else {
          let data = (get(genericSelector, "data", {}).records || []).filter((record: any) => {
            const filterKey = Object.keys(record)[0];
            return (
              supportedFilters.values.includes(filterKey) ||
              filterKey.includes("customfield_") ||
              filterKey.includes("Custom")
            );
          });
          const cf = get(genericSelector, "data", {}).custom_fields;
          const cHygienes = get(genericSelector, "data", {}).custom_hygienes;
          if (data && data.length > 0) {
            if (application === IntegrationTypes.JIRA || application === IntegrationTypes.AZURE) {
              data = [...data, { custom_hygienes: cHygienes }];
            }
            if (cf) {
              data = [...data, { custom_fields: cf }];
            }
            setApiData(data);
            setHasNext(get(genericSelector, ["data", "_metadata", "has_next"], false));
          }
          if (
            (application === IntegrationTypes.JIRA ||
              application === IntegrationTypes.JIRA_ASSIGNEE_TIME_REPORT ||
              application === IntegrationTypes.ZENDESK ||
              application === IntegrationTypes.AZURE) &&
            !["jira_zendesk_filter_values", "jira_salesforce_filter_values"].includes(supportedFilters?.uri || "") &&
            cf
          ) {
            setLoading(false);
          } else if (
            (application !== IntegrationTypes.JIRA && application !== IntegrationTypes.JIRA_ASSIGNEE_TIME_REPORT) ||
            ["jira_zendesk_filter_values", "jira_salesforce_filter_values"].includes(supportedFilters?.uri || "")
          ) {
            setLoading(false);
          } else if (
            ["jira", "github", "githubjira"].includes(application) &&
            ["lead_time_filter_values", "scm_jira_commits_count_ba"].includes(supportedFilters?.uri)
          ) {
            setLoading(false);
          }
        }
        return;
      }

      if (apiData && apiData.length > 0) {
        setApiData([]);
      }

      if (supportedFilters && !loadingData) {
        if (
          (![...JIRA_ZENDESK_APPS, "sprint_metrics_single_stat"].includes(application) &&
            ![
              "jira_zendesk_filter_values",
              "jira_salesforce_filter_values",
              "zendesk_filter_values",
              "lead_time_filter_values",
              "scm_issue_management_workitem_values",
              "code_vol_vs_deployment_values",
              "testrails_tests_values"
            ].includes(supportedFilters?.uri || "")) ||
          supportedFilters?.skipFetchCustomData
        ) {
          let additionalPayload = {};
          if (
            (deps && JENKINS_AZURE_REPORTS.includes(deps[0])) ||
            ["cicd_filter_values"].includes(supportedFilters.uri)
          ) {
            additionalPayload = {
              ...additionalPayload,
              cicd_integration_ids: integrationIds
            };
          }
          dispatch(
            genericWidgetFilterValuesGet(supportedFilters, integrationIds, uuid, additionalPayload, removeIntegration)
          );
        } else {
          const filter = {
            fields: supportedFilters?.values || [],
            integration_ids: integrationIds,
            filter: {
              integration_ids: integrationIds,
              ...get(supportedFilters, ["moreFilters"], {})
            }
          };
          dispatch(widgetFilterValuesGet(supportedFilters?.uri || "", filter, null, uuid, isConfigLoaded));
        }
        setLoading(true);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    deps ? [supportedFilters, ...deps] : [supportedFilters]
  );

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (!loadingData || hasMultiAPIs) {
      return;
    }
    const { loading, error } = genericSelector;
    if (!loading && !error && Object.keys(genericSelector.data || {}).length > 0) {
      let data = get(genericSelector, "data", {}).records;
      const cf = get(genericSelector, "data", {}).custom_fields;
      const cHygienes = get(genericSelector, "data", {}).custom_hygienes;
      if (data && data.length > 0) {
        if (["jira", "jira_stat", "azure_devops", WorkflowIntegrationType.IM].includes(application)) {
          data = [...data, { custom_hygienes: cHygienes }];
        }
        if (cf) {
          data = [...data, { custom_fields: cf }];
        }
        setApiData(data);
        setHasNext(get(genericSelector, ["data", "_metadata", "has_next"], false));
      }
      if (
        [...JIRA_APPS, ...ZENDESK_APPS].includes(application) &&
        !["jira_zendesk_filter_values", "jira_salesforce_filter_values"].includes(supportedFilters?.uri || "") &&
        (cf || supportedFilters?.skipFetchCustomData)
      ) {
        setLoading(false);
      } else if (
        ![...JIRA_APPS, ...ZENDESK_APPS].includes(application) ||
        ["jira_zendesk_filter_values", "jira_salesforce_filter_values"].includes(supportedFilters?.uri || "")
      ) {
        setLoading(false);
      }
    }
    if (error) {
      setLoading(false);
      setError(true);
    }
  }, [genericSelector, rest_api]);

  useEffect(() => {
    if (!loadingData || !hasMultiAPIs) {
      return;
    }
    let finalData: any[] = [];
    let hasNext: boolean = false;
    const totalLength = (supportedFilters || []).reduce((acc: any, filter: any) => filter.values.length + acc, 0);
    (supportedFilters || []).forEach((item: any) => {
      const { loading, error } = loadingStatus(rest_api, item.uri, "list", uuid);
      if (!loading && !error) {
        const data = getData(rest_api, item.uri, "list", uuid).records;
        hasNext = get(rest_api, [item.uri, "list", uuid, "data", "_metadata", "has_next"], false);
        if (data && data.length > 0) {
          finalData = [...finalData, ...data];
        }
      }
    });
    if (finalData.length === totalLength) {
      setLoading(false);
      setApiData(finalData);
      setHasNext(hasNext);
    }
  }, [rest_api]);

  return {
    loading: loadingData,
    error,
    apiData,
    has_next: hasNext
  };
}
