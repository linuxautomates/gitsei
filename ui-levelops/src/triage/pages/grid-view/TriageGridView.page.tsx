import React, { useCallback, useEffect, useMemo, useState } from "react";
import { v1 as uuid } from "uuid";
import { Col, Row, notification } from "antd";
import { defaultTriageGridViewColumns } from "./grid-table-config";
import { convertEpochToDate } from "../../../utils/dateUtils";

import { AntTag, AntText, Tooltip } from "../../../shared-resources/components";
import { getBaseUrl, TRIAGE_ROUTES } from "../../../constants/routePaths";

import "./TriageGridView.scss";
import { buildQueryParamsFromObject, parseQueryParamsIntoKeys } from "../../../utils/queryUtils";
import { connect, useDispatch } from "react-redux";
import { csvDownloadDrilldownToProps } from "reduxConfigs/maps/csvDownload.map";
import FailedJobCellComponent from "./status-cell/failed-job-cell.component";
import { IssuesDrawerComponent } from "./issues-drawer/issues-drawer.component";
import { StatusCountCell } from "./status-cell";
import RestApiPaginatedTableUrlWrapper from "../../../shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { CSVDownloadSagaType } from "dashboard/helpers/helper";
import { TRIAGE_RESULTS_PAGE_FILTERS, TRIAGE_TABS } from "../../../constants/triageParams";
import Loader from "components/Loader/Loader";
import { get } from "lodash";
import {
  TRIAGE_ABORTED_STATUS_KEYS,
  TRIAGE_FAILED_STATUS_KEYS,
  TRIAGE_SUCCESS_STATUS_KEYS,
  TRIAGE_UUID
} from "./grid-view-constant";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  listTriageFilter,
  getTriageFilter,
  updateTriageFilter
} from "reduxConfigs/actions/restapi/triageFilter.actions";
import moment from "moment";
import { DATE_RANGE_FILTER_FORMAT } from "../../../shared-resources/containers/server-paginated-table/containers/filters/constants";
import queryString from "query-string";
import { TriageFilterDropDown } from "./triage-filter-dropdown/TriageFilterDropDown";
import { sanitizeTimeFilter, checkArrayKeys } from "./triage-filter-dropdown/helper";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { TriageFilterDropDownList } from "../../../model/triage/triage";
import { CICDJobAggsResponse, TriageFilters } from "reduxConfigs/actions/restapi/response-types/triageResponseTypes";
import { triageGridViewFilterConfigs } from "./triage-filter-configs";
import { Link } from "react-router-dom";

interface TriageGridViewPageProps {
  history: any;
  location: any;
  csvDownloadTriageGridView: (uri: string, method: string, filters: any, columns: Array<any>) => void;
  setTriageFilters: (filters: any) => void;
}

const TriageGridViewPage: React.FC<TriageGridViewPageProps> = ({ history, location, ...props }) => {
  const dispatch = useDispatch();

  const [selectedJob, setSelectedJob] = useState<any>(undefined);
  const [moreFilters, setMoreFilters] = useState({});
  const [showIssuesDrawer, setIssuesDrawerVisibility] = useState(false);
  const [loadingSavedFilters, setLoadingSavedFilters] = useState(true);
  const [savingFilters, setSavingFilters] = useState<boolean>(false);
  const [selectedTriageFilter, setSelectedTriageFilter] = useState<string | undefined>(undefined);
  const [filterDescription, setFilterDescription] = useState<string>("");
  const [defaultFiltersLoading, setDefaultFiltersLoading] = useState<boolean>(true);
  const [isDefaultTriageFilter, setIsDefaultTriageFilter] = useState<boolean>(false);
  const [noDefaultFilterFound, setNoDefaultFilterFound] = useState<boolean>(false);
  const [createFilterWithData, setCreateFilterWithData] = useState<TriageFilters>({});
  const [updateInitialFilters, setUpdateInitialFilters] = useState<boolean>(false);

  const filterSaveState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "update",
    uuid: selectedTriageFilter
  });

  const defaultTriageFilterState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "list",
    uuid: "default"
  });

  const selectedTriageFilterState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_filters",
    method: "get",
    uuid: selectedTriageFilter
  });

  const queryParamsToParse = useMemo(
    () => [
      "product_ids",
      "results",
      "job_ids",
      "start_time",
      "end_time",
      "cicd_user_ids",
      "parent_job_ids",
      "triage_rule_ids",
      "job_normalized_full_names",
      "instance_names"
    ],
    []
  );

  const scrollX = useMemo(() => {
    return { x: "fit-content", y: 500 };
  }, []);

  useEffect(() => {
    if (savingFilters) {
      const loading = get(filterSaveState, "loading", true);
      const error = get(filterSaveState, "error", true);
      if (!loading && !error) {
        setSavingFilters(false);
        setUpdateInitialFilters(false);
        notification.success({ message: "Filters saved successfully!" });
      }
    }
  }, [filterSaveState]);

  useEffect(() => {
    const parsedQuery = queryString.parse(location.search);
    if (parsedQuery.filter_name && parsedQuery.filter_name !== selectedTriageFilter) {
      const filterName = parsedQuery.filter_name as string;
      setSelectedTriageFilter(filterName);
      setDefaultFiltersLoading(false);
      setLoadingSavedFilters(true);
      dispatch(getTriageFilter(filterName));
    } else {
      const existingData = get(defaultTriageFilterState, ["data", "records", 0, "filter"], {});
      setLoadingSavedFilters(false);

      if (!Object.keys(existingData).length) {
        dispatch(listTriageFilter({ filter: { is_default: true } }, "default"));
      } else {
        setDefaultFiltersLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    const queryFilters = parseQueryParamsIntoKeys(location.search, [
      "product_ids",
      "results",
      "job_ids",
      "start_time",
      "end_time",
      "cicd_user_ids",
      "parent_job_ids"
    ]);
    if (queryFilters) {
      const { product_ids, start_time, end_time } = queryFilters;
      if (product_ids) {
        queryFilters["product_ids"] = product_ids.map((id: string) => ({ key: id }));
      }
      if (start_time && end_time) {
        delete queryFilters.end_time;
        queryFilters["start_time"] = {
          $gt: start_time[0], // $gt is not passed as array in api call
          $lt: end_time[0] // $lt is not passed as array in api call
        };
      }
      setMoreFilters(queryFilters);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (loadingSavedFilters) {
      const loading = get(selectedTriageFilterState, "loading", true);
      const error = get(selectedTriageFilterState, "error", true);

      if (!loading && !error) {
        let data = get(selectedTriageFilterState, ["data", "filter"], {});
        const description = get(selectedTriageFilterState, ["data", "description"], "");

        data = sanitizeTimeFilter(data);

        setLoadingSavedFilters(false);
        data.page = 1;

        setFilterDescription(description);

        history.push({
          search:
            "?" + buildQueryParamsFromObject({ ...data, tab: "triage_grid_view", filter_name: selectedTriageFilter })
        });

        // for now I can set them equal to moreFilters
        setMoreFilters(data);
        props.setTriageFilters(data);
        setIsDefaultTriageFilter(get(selectedTriageFilterState, ["data", "is_default"], false));
      }
    }
  }, [selectedTriageFilterState]);

  useEffect(() => {
    if (defaultFiltersLoading) {
      const loading = get(defaultTriageFilterState, "loading", true);
      const error = get(defaultTriageFilterState, "error", true);

      if (!loading && !error) {
        const records = get(defaultTriageFilterState, ["data", "records"], []);
        if (records.length === 0) {
          if (!noDefaultFilterFound) {
            dispatch(listTriageFilter({}, "default"));
            setNoDefaultFilterFound(true);
            return;
          } else {
            setDefaultFiltersLoading(false);
            history.push({
              search: "?" + buildQueryParamsFromObject({ tab: "triage_grid_view" })
            });
            return;
          }
        }

        let data = get(defaultTriageFilterState, ["data", "records", 0, "filter"], {});
        data = sanitizeTimeFilter(data);
        const filterName = get(defaultTriageFilterState, ["data", "records", 0, "name"], undefined);
        const description = get(defaultTriageFilterState, ["data", "records", 0, "description"], "");

        setFilterDescription(description);
        setDefaultFiltersLoading(false);
        data.page = 1;

        history.push({
          search: "?" + buildQueryParamsFromObject({ ...data, tab: "triage_grid_view" })
        });

        // for now I can set them equal to moreFilters
        setMoreFilters(data);
        props.setTriageFilters(data);
        setSelectedTriageFilter(filterName);
        setIsDefaultTriageFilter(!noDefaultFilterFound);
      }
    }
  }, [defaultTriageFilterState]);

  const mappedColumns = useMemo(() => {
    return defaultTriageGridViewColumns.map((column: any) => {
      if (column.hasOwnProperty("valueName")) {
        if (column.key === "name") {
          return {
            ...column,
            title: <span className={"pl-10 grid-job-path-label"}>Job Path</span>,
            options: [{ label: " ", value: " " }],
            children: [
              {
                ...column.children[0],
                children: [
                  {
                    ...column.children[0].children[0],
                    title: <span className={"pl-10"}>Daily Totals</span>,
                    render: (item: string, record: CICDJobAggsResponse) => {
                      return (
                        <AntText className="pl-10">
                          <Link className="name-cell ellipsis" to={handleJobNameClick(record.name)}>
                            <Tooltip tooltip={record.full_name}>{record.full_name}</Tooltip>
                          </Link>
                        </AntText>
                      );
                    }
                  }
                ]
              }
            ]
          };
        }
        return {
          ...column,
          options: [{ label: " ", value: " " }]
        };
      }
      return column;
    });
  }, []);

  const handleJobNameClick = (jobName: string) => {
    const queryParams = buildQueryParamsFromObject({
      job_names: [jobName]
    });
    return `${getBaseUrl()}${TRIAGE_ROUTES.RESULTS}?${queryParams}`;
  };

  const handleStatusCountClick = (jobName: string, result: string, date: string | undefined) => {
    const queryFilters = parseQueryParamsIntoKeys(location.search, [
      "product_ids",
      "results",
      "job_ids",
      "start_time",
      "end_time",
      "cicd_user_ids",
      "parent_job_ids",
      "job_normalized_full_names",
      "instance_names"
    ]);

    const triageResultPageFilters: TRIAGE_RESULTS_PAGE_FILTERS = {
      job_names: [jobName],
      job_statuses: [result],
      tab: "triage_results",
      parent_cicd_job_ids: queryFilters!.parent_job_ids || []
    };

    if (date) {
      const dateString = moment.unix(parseInt(date)).format(DATE_RANGE_FILTER_FORMAT);
      triageResultPageFilters.start_time = dateString;
      triageResultPageFilters.end_time = dateString;
    }

    const queryParams = buildQueryParamsFromObject(triageResultPageFilters);
    return `${getBaseUrl()}${TRIAGE_ROUTES.RESULTS}?${queryParams}`;
  };

  const handleDynamicColumnsConfiguration = useCallback((columns: any) => {
    if (!columns) {
      return columns;
    }
    const columnCount = columns.length;
    if (columnCount < 5) {
      return columns;
    }

    return columns.map((column: any) => {
      column.children.map((child: any) => {
        return child.children.map((_child: any) => {
          _child.width = 100;
          return _child;
        });
      });
      return column;
    });
  }, []);

  const handleParsedQueryParams = useCallback((filters: any) => {
    if (filters) {
      const {
        product_ids,
        start_time,
        end_time,
        triage_rule_ids,
        job_normalized_full_names,
        cicd_user_ids,
        instance_names
      } = filters;

      if (product_ids) {
        filters["product_ids"] = product_ids.map((id: string) => ({ key: id }));
      }
      if (triage_rule_ids) {
        filters["triage_rule_ids"] = triage_rule_ids;
      }
      if (start_time && end_time) {
        delete filters.end_time;
        filters["start_time"] = {
          $gt: start_time[0], // $gt is not passed as array in api call
          $lt: end_time[0] // $lt is not passed as array in api call
        };
      }
      if (job_normalized_full_names) {
        filters["job_normalized_full_names"] = job_normalized_full_names;
      }
      if (cicd_user_ids) {
        filters["cicd_user_ids"] = cicd_user_ids;
      }
      if (instance_names) {
        filters["instance_names"] = instance_names;
      }
    }
    return filters;
  }, []);

  const transformRecords = useCallback((records: any[]) => {
    return records.map((record: any) => {
      if (record && record.aggs && record.aggs.length) {
        record.aggs.map(({ key, totals }: any) => {
          record[`dynamic_column_aggs_${key}`] = totals;
        });
      }
      return record;
    });
  }, []);

  const handleInfoClick = useCallback((record: any, date: any, statusKey?: string) => {
    setIssuesDrawerVisibility(true);
    setSelectedJob({
      name: record.name,
      status: statusKey || "FAILURE",
      job_id: record.id,
      date: date ? [date, (parseInt(date) + 86399).toString()] : undefined
    });
  }, []);

  const renderDynamicColumns = useCallback((key: string, extraData?: any) => {
    if (!key.includes("dynamic_column_aggs")) {
      return;
    }
    const time = key.replace("dynamic_column_aggs_", "");
    const defaultArguments = {
      dataIndex: key,
      align: "center"
    };
    const item = extraData[time];

    const FAILED_STATUS = checkArrayKeys(TRIAGE_FAILED_STATUS_KEYS, Object.keys(extraData[time]));
    const SUCCESS_STATUS = checkArrayKeys(TRIAGE_SUCCESS_STATUS_KEYS, Object.keys(extraData[time]));
    const ABORT_STATUS = checkArrayKeys(TRIAGE_ABORTED_STATUS_KEYS, Object.keys(extraData[time]));

    return {
      title: convertEpochToDate(time, "LL", true) + "(GMT)",
      dataIndex: key,
      key: key,
      children: [
        {
          ...defaultArguments,
          key: uuid(),
          title: "Success",
          children: [
            {
              ...defaultArguments,
              key: uuid(),
              title: item && SUCCESS_STATUS ? <AntTag color="green">{item[SUCCESS_STATUS]}</AntTag> : null,
              render: (item: any, record: any, index: number) => {
                if (!item || !SUCCESS_STATUS || !item[SUCCESS_STATUS]) {
                  return null;
                }
                const date = key ? key.replace("dynamic_column_aggs_", "") : undefined;
                return (
                  <StatusCountCell
                    color="green"
                    count={item ? item[SUCCESS_STATUS] || 0 : 0}
                    href={handleStatusCountClick(record.name, SUCCESS_STATUS, date)}
                  />
                );
              }
            }
          ]
        },
        {
          ...defaultArguments,
          key: uuid(),
          title: "Failed",
          children: [
            {
              ...defaultArguments,
              key: uuid(),
              title: item && FAILED_STATUS ? <AntTag color="red">{item[FAILED_STATUS]}</AntTag> : null,
              render: (item: any, record: any, index: number) => {
                if (!item || !FAILED_STATUS || !item[FAILED_STATUS]) {
                  return null;
                }
                const date = key ? key.replace("dynamic_column_aggs_", "") : undefined;
                return (
                  <FailedJobCellComponent
                    record={record}
                    date={date}
                    color="red"
                    href={handleStatusCountClick(record.name, FAILED_STATUS, date)}
                    count={item ? item[FAILED_STATUS] || 0 : 0}
                    onInfoClick={handleInfoClick}
                    statusKey={FAILED_STATUS}
                  />
                );
              }
            }
          ]
        },
        {
          ...defaultArguments,
          key: uuid(),
          title: "Aborted",
          children: [
            {
              ...defaultArguments,
              key: uuid(),
              title: item && ABORT_STATUS ? <AntTag color="grey">{item[ABORT_STATUS] || null}</AntTag> : null,
              render: (item: any, record: any, index: number) => {
                if (!item || !ABORT_STATUS || !item[ABORT_STATUS]) {
                  return null;
                }
                const date = key ? key.replace("dynamic_column_aggs_", "") : undefined;
                return (
                  <StatusCountCell
                    color="grey"
                    count={item ? item[ABORT_STATUS] || 0 : 0}
                    href={handleStatusCountClick(record.name, ABORT_STATUS, date)}
                  />
                );
              }
            }
          ]
        }
      ]
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const queryParamsFromFilters = useCallback((filters: any, tab = TRIAGE_TABS.TRIAGE_GRID_VIEW) => {
    if (!filters) {
      return {
        tab
      };
    }
    const {
      product_ids,
      results,
      cicd_user_ids,
      job_ids,
      parent_job_ids,
      start_time,
      triage_rule_ids,
      job_normalized_full_names,
      instance_names
    } = filters;
    const st = start_time ? start_time.$gt : "";
    const end_time = start_time ? start_time.$lt : "";
    return {
      product_ids: product_ids && product_ids.map((p: any) => p.key),
      results,
      cicd_user_ids,
      job_ids,
      parent_job_ids,
      triage_rule_ids: triage_rule_ids && triage_rule_ids.filter((p: any) => !!p),
      start_time: st,
      end_time,
      tab,
      job_normalized_full_names,
      instance_names
    };
  }, []);

  const handleDrawerOnCLose = useCallback(() => {
    setIssuesDrawerVisibility(false);
  }, []);

  const handleFilterSave = () => {
    const filter: { [key: string]: any } | undefined = parseQueryParamsIntoKeys(location.search, queryParamsToParse);
    filter && delete filter.tab;
    filter && delete filter.page;
    if (selectedTriageFilter) {
      dispatch(
        updateTriageFilter(selectedTriageFilter, {
          filter,
          description: filterDescription,
          is_default: isDefaultTriageFilter
        })
      );
      props.setTriageFilters(filter);
      setSavingFilters(true);
      setUpdateInitialFilters(true);
    } else {
      setCreateFilterWithData(filter as TriageFilters);
    }
  };

  const handleItemClick = useCallback((name: string, item: any) => {
    setUpdateInitialFilters(true);
    const params = buildQueryParamsFromObject({
      tab: TRIAGE_TABS.TRIAGE_GRID_VIEW,
      filter_name: name,
      ...(item?.data?.filter || {})
    });
    history.push({
      search: "?" + params
    });
    setSelectedTriageFilter(name);
    setIsDefaultTriageFilter(!!item?.data?.is_default);
    setFilterDescription(item?.data?.description);
  }, []);

  const handleCreateItem = useCallback(
    (name: string, item: any, action: string) => {
      if (action === "create") {
        const params = buildQueryParamsFromObject({
          tab: TRIAGE_TABS.TRIAGE_GRID_VIEW,
          filter_name: name,
          ...(item.filter || {})
        });
        history.push({
          search: "?" + params
        });
        setSelectedTriageFilter(name);
        setIsDefaultTriageFilter(!!item.is_default);
        setFilterDescription(item?.description);
        if (Object.keys(createFilterWithData || {}).length > 0) {
          setCreateFilterWithData({});
          setSavingFilters(false);
          setUpdateInitialFilters(false);
        }
        setUpdateInitialFilters(true);
      }
    },
    [createFilterWithData]
  );

  const handleCloneItem = useCallback((item: any) => {
    const filter_name = item.name;
    const filters = item.data.filter;

    const params = buildQueryParamsFromObject({ tab: TRIAGE_TABS.TRIAGE_GRID_VIEW, filter_name, ...filters });
    history.push({
      search: "?" + params
    });
    setSelectedTriageFilter(filter_name);
    setIsDefaultTriageFilter(!!item?.is_default);
    setFilterDescription(item?.description);
  }, []);

  const clearDefaultData = () => {
    dispatch(restapiClear("triage_filters", "list", "default"));
  };

  const handleDefaultChange = useCallback(
    (name: string) => {
      setIsDefaultTriageFilter(name === selectedTriageFilter);
      clearDefaultData();
    },
    [selectedTriageFilter]
  );

  const handleEditItem = useCallback((data: any) => {
    setFilterDescription(data.description);
    setIsDefaultTriageFilter(data.is_default);
  }, []);

  const handleItemDelete = useCallback(
    (name: string, filterList: TriageFilterDropDownList[]) => {
      clearDefaultData();
      let _id = "";
      let _description = "";
      let _default = false;

      if (name === selectedTriageFilter) {
        const filter = filterList.find(filter => filter.id !== selectedTriageFilter);
        if (filter) {
          const { id, data } = filter;
          const params = buildQueryParamsFromObject({
            tab: TRIAGE_TABS.TRIAGE_GRID_VIEW,
            filter_name: id,
            ...data.filter
          });
          history.push({
            search: "?" + params
          });
          _id = id;
          _description = filter.description;
          _default = filter.default;
        } else {
          const params = buildQueryParamsFromObject({
            tab: TRIAGE_TABS.TRIAGE_GRID_VIEW
          });
          history.push({
            search: "?" + params
          });
        }

        setSelectedTriageFilter(_id);
        setFilterDescription(_description);
        setIsDefaultTriageFilter(_default);
      }
    },
    [selectedTriageFilter]
  );

  const doCreateAction = useCallback(() => {
    setSavingFilters(true);
    setUpdateInitialFilters(true);
  }, []);

  const titleComponent = useMemo(
    () => (
      <TriageFilterDropDown
        filterName={selectedTriageFilter as string}
        filterDescription={filterDescription}
        isDefault={isDefaultTriageFilter}
        handleItemClick={handleItemClick}
        handleCreateItem={handleCreateItem}
        handleCloneItem={handleCloneItem}
        handleDefaultChange={handleDefaultChange}
        handleEditItem={handleEditItem}
        handleItemDelete={handleItemDelete}
        createFilterWithData={createFilterWithData}
        doCreateAction={doCreateAction}
      />
    ),
    [selectedTriageFilter, isDefaultTriageFilter, filterDescription, createFilterWithData, handleDefaultChange]
  );

  if (loadingSavedFilters || defaultFiltersLoading) {
    return <Loader />;
  }

  return (
    <>
      <Row type="flex" justify="start" className="triage-grid-view-container">
        <Col span={24}>
          <RestApiPaginatedTableUrlWrapper
            pageName="triageGridViewList"
            uri="cicd_job_aggs"
            method="list"
            columns={mappedColumns}
            hasFilters
            hasDynamicColumns
            renderDynamicColumns={renderDynamicColumns}
            configureDynamicColumns={handleDynamicColumnsConfiguration}
            bordered
            hasSearch={true}
            uuid={TRIAGE_UUID}
            scroll={scrollX}
            filters={moreFilters}
            buildQueryParamsFromFilters={queryParamsFromFilters}
            query_params_to_parse={queryParamsToParse}
            onQueryParamsParsed={handleParsedQueryParams}
            transformRecords={transformRecords}
            downloadCSV={{ type: CSVDownloadSagaType.TRIAGE_CSV_DOWNLOAD }}
            savingFilters={savingFilters}
            handleFilterSave={handleFilterSave}
            disableSaveFilterButton={false}
            componentTitle={titleComponent}
            updateInitialFilters={updateInitialFilters}
            setUpdateInitialFilters={setUpdateInitialFilters}
            generalSearchField={"job_full_name"}
            useRestTableAddFilters={true}
            restTableFilterConfigs={triageGridViewFilterConfigs}
          />
        </Col>
      </Row>
      <IssuesDrawerComponent visible={showIssuesDrawer} onClose={handleDrawerOnCLose} filters={selectedJob} />
    </>
  );
};

// @ts-ignore
export default connect(null, csvDownloadDrilldownToProps)(TriageGridViewPage);
