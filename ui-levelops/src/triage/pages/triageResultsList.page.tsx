import React, { useCallback, useEffect, useMemo, useState } from "react";

import { useSupportedFilters } from "custom-hooks/useSupportedFilters";
import { jenkinsPipelineJobSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { tableColumns } from "./results-table-config";
import { parseQueryParamsIntoKeys } from "../../utils/queryUtils";
import RestApiPaginatedTableUrlWrapper from "shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { TRIAGE_RESULT_PARAM_KEYS, TRIAGE_RESULTS_PAGE_FILTERS, TRIAGE_TABS } from "../../constants/triageParams";
import { useDispatch } from "react-redux";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { WebRoutes } from "../../routes/WebRoutes";

interface StoreProps {
  location: any;
  history: any;
}

interface TriageResultsListPageProps extends StoreProps {}

const listUUID = "triage-results-list";

const TriageResultsListPage: React.FC<TriageResultsListPageProps> = (props: TriageResultsListPageProps) => {
  const dispatch = useDispatch();
  const [moreFilters, setMoreFilters] = useState({});

  const { location } = props;

  const listOFAllParams = [
    TRIAGE_RESULT_PARAM_KEYS.PRODUCT_IDS,
    TRIAGE_RESULT_PARAM_KEYS.JOB_STATUSES,
    TRIAGE_RESULT_PARAM_KEYS.JOB_NAMES,
    TRIAGE_RESULT_PARAM_KEYS.CICD_USER_IDS,
    TRIAGE_RESULT_PARAM_KEYS.PARENT_CICD_USER_IDS,
    TRIAGE_RESULT_PARAM_KEYS.START_TIME,
    TRIAGE_RESULT_PARAM_KEYS.END_TIME,
    TRIAGE_RESULT_PARAM_KEYS.INSTANCE_NAMES,
    TRIAGE_RESULT_PARAM_KEYS.JOB_PATH
  ];

  const memoizedParamsList = useMemo(() => listOFAllParams, []);

  function transformFilters(filters: TRIAGE_RESULTS_PAGE_FILTERS) {
    const { product_ids, start_time, end_time } = filters;

    if (product_ids) {
      filters.product_ids = product_ids.map((id: string) => ({ key: id }));
    }
    if (start_time && end_time) {
      delete filters.end_time;
      filters.start_time = {
        $gt: start_time[0],
        $lt: end_time[0]
      };
    }
    return filters;
  }

  useEffect(() => {
    const queryFilters: TRIAGE_RESULTS_PAGE_FILTERS = parseQueryParamsIntoKeys(location.search, listOFAllParams);
    queryFilters && setMoreFilters(transformFilters(queryFilters));
  }, []);

  useEffect(() => {
    const settings = {
      title: "Triage Results",
      bread_crumbs: [
        {
          label: "Triage Grid View",
          path: WebRoutes.triage.root()
        },
        {
          label: "Triage Results",
          path: WebRoutes.triage.results()
        }
      ],
      bread_crumbs_position: "before",
      withBackButton: true,
      showBottomSeparator: true
    };
    dispatch(setPageSettings(location.pathname, settings));
  }, []);

  const handleParsedQueryParams = useCallback((filters: TRIAGE_RESULTS_PAGE_FILTERS) => {
    if (filters) {
      transformFilters(filters);
    }
    return filters;
  }, []);

  const queryParamsFromFilters = useCallback(
    (filters: TRIAGE_RESULTS_PAGE_FILTERS, tab = TRIAGE_TABS.TRIAGE_RESULTS) => {
      if (!filters) {
        return {
          tab
        };
      }
      const {
        product_ids,
        job_statuses,
        job_names,
        cicd_user_ids,
        parent_cicd_job_ids,
        start_time,
        instance_names,
        job_normalized_full_names
      } = filters;
      const st = start_time ? start_time.$gt : "";
      const end_time = start_time ? start_time.$lt : "";
      return {
        product_ids: product_ids && product_ids.map((p: any) => p.key),
        job_statuses,
        job_names,
        cicd_user_ids,
        parent_cicd_job_ids,
        start_time: st,
        end_time,
        tab,
        instance_names,
        job_normalized_full_names
      };
    },
    []
  );

  const { apiData: filtersData } = useSupportedFilters(jenkinsPipelineJobSupportedFilters, ["1"], "jenkins", [], true);

  const getMappedOptions = (key: string) => {
    const data = filtersData!.find((item: any) => Object.keys(item)[0] === key);
    if (data) {
      return data[key].map((item: any) => ({
        label: (item["key"] || "").toUpperCase(),
        value: item["key"]
      }));
    } else {
      return [{ label: " ", value: " " }];
    }
  };

  const mappedColumns = tableColumns().map((column: any) => {
    if (column.hasOwnProperty("valueName")) {
      return {
        ...column,
        options: getMappedOptions(column.valueName)
      };
    }
    return column;
  });

  return (
    <>
      <RestApiPaginatedTableUrlWrapper
        pageName={"triageResultsList"}
        uri={"jenkins_pipeline_triage_runs"}
        method={"list"}
        uuid={listUUID}
        columns={mappedColumns}
        hasSearch
        generalSearchField={"job_name"}
        filters={moreFilters}
        buildQueryParamsFromFilters={queryParamsFromFilters}
        onQueryParamsParsed={handleParsedQueryParams}
        query_params_to_parse={memoizedParamsList}
      />
    </>
  );
};

export default TriageResultsListPage;
