import { CICDEvents, IMFilter } from "classes/RestWorkflowProfile";
import { get } from "lodash";
import { Integration } from "model/entities/Integration";
import { all, call, put, select, take, takeLatest } from "redux-saga/effects";
import { CACHED_INTEGRATION_READ, workflowProfileActions } from "reduxConfigs/actions/actionTypes";
import { genericList, restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { FILTER_VALUES_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { getUserOrFilterValue } from "dashboard/graph-filters/containers/Dora/helper";
import { getOUFiltersSaga } from "../dashboards/getOUFiltersSaga";
import { DORA_REPORT_TO_KEY_MAPPING } from "dashboard/graph-filters/components/helper";
import { cachedIntegrationEffectSaga } from "../integrations/cachedIntegrationSaga";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { FilterType, InfoDataType } from "dashboard/graph-filters/containers/Dora/typing";

type PartialFilterType = { [key: string]: string | string[] };

const getCRFCount = (apiData: InfoDataType[][]) => {
  return apiData.reduce((count: number, itm) => {
    count += itm?.length;
    return count;
  }, 0);
};
const getFailureRateData = (data: InfoDataType[][]) => {
  return data?.map((data, index: number) => {
    let key = "failed";
    if (index === 1) {
      key = "total";
    }
    return { [key]: data?.map(dta => ({ ...dta, className: "indent-left" })) };
  });
};
const transformFilterData = (filters: FilterType[]) => {
  return filters?.map(item => {
    const value = getUserOrFilterValue(item);
    return {
      key: "FILTER",
      value: value,
      className: "indent-top"
    };
  });
};

const getPartialData = (addedFilter: PartialFilterType, label: string) => {
  let type = "contains";
  let value = addedFilter?.$contains;
  if (addedFilter?.$begins) {
    type = "STARTS";
    value = addedFilter?.$begins;
  }
  return { value: [value], label: label, type } as FilterType;
};

const getSCMFiltersData = (filters: { [key: string]: PartialFilterType }) => {
  const data = Object.keys(filters).map((key: string) => {
    return getPartialData(filters[key], key);
  });
  return transformFilterData(data);
};
function* getIMFiltersData(
  workflowFilters: IMFilter,
  integrations: Integration[],
  index: number = 0
): Generator<any, any, any> {
  const data = integrations?.map((integration: Integration) => {
    return {
      id: integration?.id,
      type: integration?.application,
      name: integration?.name,
      filters: workflowFilters?.filter
    };
  });
  const uri = "profile_filter";
  const method = "list";
  const uuid = `filters_${index}`;
  yield call(getOUFiltersSaga, { uri: uri, method: method, uuid: uuid, data: data });
  const profileFilters = yield select(getGenericUUIDSelector, {
    uri,
    method,
    uuid
  });
  const filters = get(profileFilters, ["data", "0", "filters"], []);

  return transformFilterData(filters);
}
function* getCICDFiltersData(event: CICDEvents, integrationIds: string[], index: number = 0): Generator<any, any, any> {
  const uri = "jenkins_jobs_filter_values";
  const filterKey = "job_normalized_full_name";
  const complete = `COMPLETED_CICD_VALUES`;
  const filters = {
    fields: [filterKey],
    filter: {
      integration_ids: integrationIds
    },
    integration_ids: integrationIds
  };
  if (index === 0) {
    yield put(genericList(uri, "list", filters, complete as any, FILTER_VALUES_LIST_ID));
  }
  yield take(complete);
  const apiValues = yield select(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: FILTER_VALUES_LIST_ID
  });
  const path = ["data", "records", "0", filterKey];
  const response: { cicd_job_id: string; key: string }[] = get(apiValues, path, []);
  const values = response?.filter(dta => event?.values?.includes(dta?.cicd_job_id));
  const data = [
    {
      key: "EVENT",
      value: values.length === 0 ? 'All jobs included' : values.map(val => val?.key).join(" , "),
      className: "indent-top"
    }
  ];
  if (event?.params && Object?.keys(event?.params)?.length) {
    const paramKey = Object?.keys(event?.params);
    const values = paramKey?.map((param: string) => {
      return { key: "PARAM", value: `${param} Equals ${event?.params?.[param]?.join(",")}`, className: "indent-top" };
    });
    return [...data, ...values];
  }
  return data;
}

export function* getWorkflowProfileFilterSaga(action: {
  type: string;
  uri: string;
  method: string;
  reportType: string;
  uuid: string;
}): Generator<any, any, any> {
  try {
    yield put(restapiLoading(true, action?.uri, action?.method, action?.uuid));

    const selectedOUState = yield select(getSelectedOU);
    const workflowProfile = yield select(workflowProfileDetailSelector, { queryParamOU: selectedOUState.id });
    const reportNameKey = DORA_REPORT_TO_KEY_MAPPING[action.reportType];
    const profileName = get(workflowProfile, ["name"], "");
    const selectedWorkflow = get(workflowProfile, [reportNameKey], {});
    const workflowFilters = get(selectedWorkflow, ["filters", reportNameKey], {});
    const failedDeployment = get(selectedWorkflow, ["filters", "failed_deployment"], {});
    const totalDeployment = get(selectedWorkflow, ["filters", "total_deployment"], {});
    const integrationIds = selectedWorkflow?.integration_ids?.map((id: any) => id?.toString()) || [
      selectedWorkflow?.integration_id?.toString()
    ];
    yield call(cachedIntegrationEffectSaga, {
      type: CACHED_INTEGRATION_READ,
      payload: { method: "list", integrationIds: integrationIds }
    });
    const profileIntegrations: Integration[] = yield select(cachedIntegrationsListSelector, {
      integration_ids: integrationIds
    });

    let filtersData;
    let moreCount = 0;

    if (workflowFilters?.scm_filters) {
      filtersData = yield call(getSCMFiltersData, workflowFilters?.scm_filters);
    }
    if (workflowFilters?.event) {
      filtersData = yield call(getCICDFiltersData, workflowFilters?.event, integrationIds);
    }
    if (workflowFilters?.filter) {
      const filter = yield call(getIMFiltersData, workflowFilters, profileIntegrations);
      filtersData = [...(filtersData || []), ...(filter || [])];
    }
    moreCount = filtersData?.length;
    if (failedDeployment?.scm_filters) {
      let apiCallArry = [failedDeployment?.scm_filters];
      if (totalDeployment?.scm_filters) {
        apiCallArry = [failedDeployment?.scm_filters, totalDeployment?.scm_filters];
      }
      const apiData = yield all(
        apiCallArry?.map((filters: { [key: string]: PartialFilterType }) => call(getSCMFiltersData, filters))
      );
      moreCount = yield call(getCRFCount, apiData);
      filtersData = getFailureRateData(apiData);
    }

    if (failedDeployment?.event) {
      let apiCallArry = [failedDeployment?.event];
      if (totalDeployment?.event) {
        apiCallArry = [failedDeployment?.event, totalDeployment?.event];
      }
      const apiData = yield all(
        apiCallArry?.map((item: CICDEvents, index: number) => call(getCICDFiltersData, item, integrationIds, index))
      );
      moreCount = yield call(getCRFCount, apiData);
      filtersData = getFailureRateData(apiData);
    }
    if (failedDeployment?.filter) {
      let apiCallArry = [failedDeployment];
      if (totalDeployment?.filter) {
        apiCallArry = [failedDeployment, totalDeployment];
      }
      const apiData = yield all(
        apiCallArry?.map((item: IMFilter, index: number) => call(getIMFiltersData, item, profileIntegrations, index))
      );
      const filterCount = yield call(getCRFCount, apiData);
      const filter = getFailureRateData(apiData);
      filtersData = [...(filtersData || []), ...(filter || [])];
      moreCount = moreCount + filterCount;
    }

    const data = [
      {
        key: "NAME",
        value: profileName,
        className: "indent-top"
      },
      {
        key: "INTEGRATIONS",
        value: profileIntegrations.map((integration: Integration) => integration.name)?.join(", "),
        className: "indent-top"
      }
    ];
    yield put(restapiLoading(false, action?.uri, action?.method, action?.uuid));
    yield put(restapiError(false, action?.uri, action?.method, action?.uuid));
    yield put(
      restapiData({ records: [...data, ...(filtersData ?? [])], moreCount }, action?.uri, action?.method, action?.uuid)
    );
  } catch (e) {
    yield put(restapiError(true, action?.uri, action?.method, action?.uuid));
  } finally {
    yield put(restapiLoading(false, action?.uri, action?.method, action?.uuid));
  }
}
export function* getWorkflowProfileFilterWatcherSaga() {
  yield takeLatest(workflowProfileActions.WORKFLOW_PROFILE_FILTERS, getWorkflowProfileFilterSaga);
}
