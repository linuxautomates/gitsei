import { get, isEqual, set } from "lodash";
import { DependencyList, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { azureIterationSupportableReports } from "../dashboard/constants/applications/names";
import { jiraSprintFilterList } from "reduxConfigs/actions/restapi/jiraSprintActions";
import { IntegrationTypes } from "constants/IntegrationTypes";

const SPRINT_REPORT_URI = "jira_sprint_filters";
const ISSUE_MANAGEMENT_SPRINT_REPORT_URI = "issue_management_sprint_filters";

export function useSprintReportFilters(application: string, filters: any, deps?: DependencyList) {
  const uuid = filters?.integration_ids?.length ? filters.integration_ids.sort().join("_") : "0";
  const uri = application === IntegrationTypes.AZURE ? ISSUE_MANAGEMENT_SPRINT_REPORT_URI : SPRINT_REPORT_URI;
  const dispatch = useDispatch();
  const rest_api = useSelector(restapiState);
  const genericSelector = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid
  });

  const [loadingData, setLoading] = useState<boolean>(false);

  const [error, setError] = useState<boolean>(false);

  const [sprintApiData, setSprintApiData] = useState<any[]>([]);

  const [fetchData, setFetchData] = useState([...(deps || [])]);

  const forceRefresh = useMemo(() => {
    let refresh = false;

    if (!isEqual(deps, fetchData)) {
      return true;
    }

    if (!(Object.keys(get(genericSelector, ["data"], {})).length > 0)) {
      return true;
    }

    return refresh;
  }, [rest_api, ...(deps || []), filters]);

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

  useEffect(
    () => {
      if (!loadingData && !forceRefresh && azureIterationSupportableReports.includes(deps && (deps[0] as any))) {
        let data = get(genericSelector, "data", {}).records;
        setSprintApiData(data);
        setLoading(false);
        setError(false);
        return;
      }

      if (sprintApiData && sprintApiData.length > 0) {
        setSprintApiData([]);
      }

      if (!loadingData && azureIterationSupportableReports.includes(deps && (deps[0] as any))) {
        let filter = {
          filter: {
            integration_ids: filters.integration_ids || []
          }
        };
        if (filters?.completed_at) {
          set(filter, ["filter", "completed_at"], filters?.completed_at);
        }
        dispatch(jiraSprintFilterList(uri, "list", filter, uuid));
        setLoading(true);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [...(deps || []), filters]
  );

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (!loadingData) {
      return;
    }

    const { loading, error } = genericSelector;

    if (!loading && !error && Object.keys(genericSelector.data || {}).length > 0) {
      let data = get(genericSelector, "data", {}).records;
      setSprintApiData(data);
      setLoading(false);
      setError(false);
    }

    if (error) {
      setLoading(false);
      setError(true);
    }
  }, [genericSelector, rest_api]);

  return {
    loading: loadingData,
    error,
    sprintApiData
  };
}
