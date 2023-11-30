import React, { useEffect, useState } from "react";
import { connect } from "react-redux";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import ChartContainer from "../chart-container/chart-container.component";
import Loader from "../../../components/Loader/Loader";
import { getData, loadingStatus } from "utils/loadingUtils";
import { getReportsSelector } from "reduxConfigs/selectors/reports.selector";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { AntText } from "shared-resources/components";
import { useGlobalFilters } from "../../../custom-hooks";
import { isEqual, uniq } from "lodash";
import { useDataTransform } from "../../../custom-hooks";

const JenkinsRestApiContainer = props => {
  const [dataFromApi, setDataFromApi] = useState([]);
  const [reportFromApi, setReportFromApi] = useState({});
  const [fetchData, setFetchData] = useState(true);
  const [fetchReport, setFetchReport] = useState(undefined);

  const filters = props.filters && Object.keys(props.filters).length ? props.filters : {};
  const globalFilters = useGlobalFilters(props.globalFilters);
  const widgetFilters = props.widgetFilters || {};
  const reportType = props.reportType;
  const setAggNameOptions = props.setAggNameOptions;
  const aggNameOptions = props.aggNameOptions;

  const reportData = useDataTransform(reportFromApi, reportType, props.uri, 20, props.filters, null);

  const getFilters = () => {
    let finalFilters = {
      page_size: 1,
      page: 0,
      filter: {
        ...filters,
        ...globalFilters,
        ...widgetFilters,
        // TODO: add back integration ids when BE is fixed
        integration_ids: undefined
      },
      sort: [{ id: "created_at", desc: true }]
    };
    if (finalFilters.filter.hasOwnProperty("across")) {
      const across = finalFilters.filter.across;
      delete finalFilters.filter["across"];
      finalFilters = {
        ...finalFilters,
        across
      };
    }
    return finalFilters;
  };

  useEffect(() => {
    if (fetchData) {
      setDataFromApi([]);
      const id = props.id || "0";
      props.genericList(props.uri, props.method, getFilters(), null, id);
    }
  }, [fetchData]);

  useEffect(() => {
    if (fetchReport !== undefined) {
      setReportFromApi({});
      if (dataFromApi && dataFromApi.length > 0) {
        const id = dataFromApi[0].id;
        props.genericGet(props.uri, id, null);
      }
    }
  }, [fetchReport]);

  useEffect(() => {
    return () => {
      const id = props.id || "0";
      props.restapiClear(props.uri, props.method, id);
      fetchReport !== undefined && props.restapiClear(props.uri, "get", fetchReport);
    };
  }, []);

  useEffect(() => {
    const id = props.id || "0";
    if (id) {
      const { loading: apiLoading, error: apiError } = loadingStatus(props.rest_api, props.uri, props.method, id);
      if (!apiLoading && !apiError && fetchData) {
        const data = getData(props.rest_api, props.uri, props.method, id).records || [];
        if (data) {
          setDataFromApi(data);
        }
        setFetchData(false);
        if (data.length > 0) {
          setFetchReport(data[0].id);
        }
        if (props.reload) {
          props.setReload(false);
        }
      }
    }
    if (fetchReport !== undefined) {
      const { loading: apiLoading, error: apiError } = loadingStatus(props.rest_api, props.uri, "get", fetchReport);
      if (!apiLoading && !apiError) {
        const data = getData(props.rest_api, props.uri, "get", fetchReport);
        setReportFromApi(data);
        setAggNameOptions && setAggNames();
      }
    }
  });

  useEffect(() => {
    setFetchData(true);
  }, [globalFilters]);

  useEffect(() => {
    if (props.reload) {
      setFetchData(true);
      setFetchReport(undefined);
    }
  }, [props.reload]);

  const setAggNames = () => {
    if (reportFromApi && reportFromApi.hasOwnProperty("aggregate_build_results_time_series")) {
      const time_series = reportFromApi.aggregate_build_results_time_series;
      const aggNames = Object.values(time_series).reduce((acc, value) => {
        return [...acc, ...value.reduce((result, agg) => [...result, agg.agg_name], [])];
      }, []);

      const uniqName = uniq(aggNames);
      if (!isEqual(uniqName.sort(), aggNameOptions.sort())) {
        setAggNameOptions(uniqName);
      }
    }
  };

  const getChartPropsAndData = () => {
    return {
      ...props.chartProps,
      ...reportData
    };
  };

  return (
    <>
      <div style={{ height: "100%" }}>
        {fetchData || fetchReport === undefined ? (
          <Loader />
        ) : dataFromApi.length && reportFromApi.id !== undefined ? (
          <>
            <ChartContainer chartType={props.chartType} chartProps={getChartPropsAndData()} />
          </>
        ) : (
          <div style={{ display: "flex", justifyContent: "center" }}>
            <AntText>No Data Available</AntText>
          </div>
        )}
      </div>
    </>
  );
};

const mapStateToProps = state => ({
  rest_api: getReportsSelector(state)
});

const mapDispatchToProps = dispatch => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapGenericToProps(dispatch)
});

export default connect(mapStateToProps, mapDispatchToProps)(JenkinsRestApiContainer);
