import { DependencyList, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { get, isEqual } from "lodash";

import { exactReportsState } from "reduxConfigs/selectors/reports.selector";
import { getData, getErrorCode, loadingStatus } from "../utils/loadingUtils";
import {
  genericGet,
  genericList,
  restapiClear,
  levelopsWidgets,
  jiraSalesforceZendeskStagesWidgets,
  jiraSalesforceZendeskC2FWidgets
} from "reduxConfigs/actions/restapi";
import widgetConstants from "dashboard/constants/widgetConstants";
import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";
import { jiraSprintListReport } from "reduxConfigs/actions/restapi/jiraSprintActions";
import { getAssigneeTimeReport } from "reduxConfigs/actions/restapi/assigneeTimeReportAction";
import { trellisScoreReport } from "reduxConfigs/actions/restapi/trellisReportActions";
import { IntegrationTypes } from "constants/IntegrationTypes";

export type apiCall = {
  id: string;
  apiName: string;
  apiMethod: string;
  reportType: string;
  filters?: any;
  isWidget?: boolean;
  onUnmountClearData?: boolean;
};

export function useApi(apis: Array<apiCall>, deps: DependencyList) {
  const dispatch = useDispatch();

  const rest_api = useSelector(state => exactReportsState(state, apis));
  const [rest_api_entities, setRestApiEntities] = useState({});
  const [loading, setLoading] = useState<boolean>(false);
  const [loaded, setLoaded] = useState<{}>(true);
  const [errorCode, setErrorCode] = useState<number>(0);
  const [apiData, setApiData] = useState<{ [x: string]: any } | undefined>(undefined);
  const [apisLoading, setApisLoading] = useState<{ [x: string]: boolean }>({});
  const [fetchData, setFetchData] = useState([...deps]);
  const [apisMetaData, setApisMetaData] = useState<{ [x: string]: any } | undefined>(undefined);

  // moving this here, widgetConstants is undefined outside the useApi
  const levelOpsWidgetsUris = Object.keys(widgetConstants || {})
    .map(rKey => get(widgetConstants, [rKey], undefined))
    .filter(report => report?.application === IntegrationTypes.LEVELOPS)
    .map(report => report?.uri);

  const forceRefresh = useMemo(() => {
    let refresh = false;
    if (!isEqual(deps, fetchData)) {
      return true;
    }
    apis.forEach(api => {
      if (!get(rest_api, [api.apiName, api.apiMethod, api.id, "data"], undefined)) {
        refresh = true;
        return;
      }
    });
    return refresh;
  }, [rest_api, apis, ...deps]);
  useEffect(() => {
    return () => {
      apis.forEach((api: apiCall) => {
        if (api?.onUnmountClearData) {
          // @ts-ignore
          dispatch(restapiClear(api.apiName, api.apiMethod, api.id));
        }
      });
    };
  }, []);

  const getApiData = (apis: Array<apiCall>) => {
    let loadings = apisLoading;
    let _loaded = true;
    let newapiData: any = {};
    let newApisMetaData: any = {};
    let errorCode = 0;
    apis.forEach((api: apiCall) => {
      const { loading: apiLoading, error: apiError } = loadingStatus(rest_api, api.apiName, api.apiMethod, api.id);
      loadings[api.id] = apiLoading;
      if (!apiLoading && !apiError) {
        let data;
        if (api.apiMethod === "list") {
          data = getData(rest_api, api.apiName, api.apiMethod, api.id).records;
        } else {
          data = getData(rest_api, api.apiName, api.apiMethod, api.id);
        }
        newapiData[api.id] = data;
        newApisMetaData[api.id] = getData(rest_api, api.apiName, api.apiMethod, api.id)?._metadata || {};
        loadings = {
          ...loadings,
          [api.id]: apiLoading
        };
      } else if (!apiLoading && apiError) {
        errorCode = getErrorCode(rest_api, api.apiName, api.apiMethod, api.id);
        newapiData[api.id] = undefined;
        loadings = {
          ...loadings,
          [api.id]: false
        };
      }
      if (apiLoading) {
        _loaded = false;
      }
    });

    return [loadings, newapiData, newApisMetaData, _loaded, errorCode];
  };

  useEffect(() => {
    if (!isEqual(deps, fetchData)) {
      setFetchData([...deps]);
    }
  }, [...deps]);

  useEffect(() => {
    let newEntities = {};
    apis.forEach(api => {
      const entity = get(rest_api, [api.apiName, api.apiMethod, api.id]);
      newEntities = {
        ...newEntities,
        [api.apiName]: { ...entity }
      };
    });

    if (!isEqual(newEntities, rest_api_entities)) {
      setRestApiEntities({
        ...newEntities
      });
    }
  }, [rest_api]);

  useEffect(() => {
    forceRefresh &&
      apis.forEach((api: apiCall) => {
        // @ts-ignore
        dispatch(restapiClear(api.apiName, api.apiMethod, api.id));
      });
  }, [forceRefresh]);

  useEffect(() => {
    if (apiData && Object.keys(apiData).length > 0) {
      setApiData(undefined);
    }
    if (!forceRefresh) {
      const [loading, apiData, apisMetaData, loaded, errorCode] = getApiData(apis);
      setApiData(apiData);
      setApisLoading(loading);
      setApisMetaData(apisMetaData);
      setLoaded(loaded);
      setErrorCode(errorCode);
    } else {
      let loadings = apisLoading;
      apis.forEach((api: apiCall) => {
        if (api.apiMethod === "list") {
          if (!!get(widgetConstants, [api.reportType, STORE_ACTION], undefined)) {
            // this is called when there is store action provided
            const apiCallAction = get(widgetConstants, [api.reportType, STORE_ACTION], undefined);
            dispatch(apiCallAction(api.apiName, api.apiMethod, api.filters, null, api.id));
          } else if (levelOpsWidgetsUris.includes(api.apiName)) {
            dispatch(levelopsWidgets(api.apiName, api.apiMethod, api.filters, null, api.id));
          } else if (["jira_zendesk_aggs_list_zendesk", "jira_salesforce_aggs_list_salesforce"].includes(api.apiName)) {
            dispatch(jiraSalesforceZendeskStagesWidgets(api.apiName, api.apiMethod, api.filters, null, api.id));
          } else if (
            ["jira_zendesk_resolved_tickets_trend", "jira_salesforce_resolved_tickets_trend"].includes(api.apiName)
          ) {
            dispatch(jiraSalesforceZendeskC2FWidgets(api.apiName, api.apiMethod, api.filters, null, api.id));
          } else if (api.apiName.includes("jira_sprint_report")) {
            dispatch(jiraSprintListReport(api.filters, api.id));
          } else if (api.apiName.includes("dev_productivity_score_report")) {
            dispatch(trellisScoreReport(api.id, api.filters));
          } else if (api.reportType === "assignee_time_report") {
            dispatch(getAssigneeTimeReport(api.id, api.filters));
          } else {
            const showNotfication = get(widgetConstants, [api.reportType, "show_notification_on_error"], true);
            dispatch(
              genericList(
                api.apiName,
                api.apiMethod,
                api.filters,
                null,
                api.id,
                true,
                {},
                api.isWidget,
                showNotfication
              )
            );
          }
        } else if (api.apiMethod === "get") {
          dispatch(genericGet(api.apiName, api.id));
        }
        loadings = {
          ...loadings,
          [api.id]: true
        };
      });
      setLoaded(false);
      setApisLoading(loadings);
    }
  }, [...deps]);

  useEffect(() => {
    if (loading) {
      const [loadings, newApiData, apisMetaData, loaded, errorCode] = getApiData(apis);
      setApiData(newApiData);
      setApisLoading(loadings);
      setApisMetaData(apisMetaData);
      setLoaded(loaded);
      setErrorCode(errorCode);
    } else {
      if (!apiData) {
        setLoading(true);
      }
    }
  }, [rest_api]);

  useEffect(() => {
    if (apisLoading && Object.keys(apisLoading).length > 0 && apis.length === Object.keys(apisLoading).length) {
      //filtering apisLoading for true values. If that filtered array has length === 0
      // means there is no apisLoading left and now we can set the global loading to false
      if (Object.values(apisLoading).filter(value => value).length === 0) {
        setLoading(false);
      }
    }
  }, [apisLoading]);

  const calculatedLoading = Object.keys(apisLoading).reduce((acc, obj) => {
    acc = apisLoading[obj] || acc;
    return acc;
  }, false);

  return [calculatedLoading, apiData, apisMetaData, loaded, errorCode];
}
