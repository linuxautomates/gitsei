import React, { useEffect, useState, useContext, useRef } from "react";
import { connect } from "react-redux";
import { isEqual } from "lodash";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import ChartContainer from "../chart-container/chart-container.component";
import Loader from "../../../components/Loader/Loader";
import { getData, loadingStatus } from "utils/loadingUtils";
import { getReportsSelector } from "reduxConfigs/selectors/reports.selector";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { Col, Row, Statistic } from "antd";
import { useDataTransform, useGlobalFilters } from "../../../custom-hooks";
import { toTitleCase } from "../../../utils/stringUtils";
import { CacheWidgetPreview, WidgetLoadingContext } from "../../../dashboard/pages/context";
import { combineAllFilters, updateTimeFiltersValue } from "../widget-api-wrapper/helper";
import EmptyWidgetComponent from "../../components/empty-widget/empty-widget.component";

const pagerDutyReports = ["pagerduty_incident_report", "pagerduty_alert_report"];

const ProductAggRestApiContainer = props => {
  const [dataFromApi, setDataFromApi] = useState([]);
  const [reportFromApi, setReportFromApi] = useState({});
  const [fetchData, setFetchData] = useState(true);
  const [fetchReport, setFetchReport] = useState(undefined);
  const { setWidgetLoading } = useContext(WidgetLoadingContext);

  const filters = props.filters && Object.keys(props.filters).length ? props.filters : {};
  const globalFilters = useGlobalFilters(props.globalFilters);
  const widgetFilters = props.widgetFilters || {};
  const reportType = props.reportType;

  const cacheWidgetPreview = useContext(CacheWidgetPreview);

  const filtersRef = useRef({});
  const reportRef = useRef();

  useEffect(() => {
    filtersRef.current = getFilters();
    reportRef.current = props.reportType;
  }, []);

  useEffect(() => {
    const allFilters = getFilters();
    if (!isEqual(allFilters, filtersRef.current) && !fetchData) {
      filtersRef.current = allFilters;
      fetchDataAgain();
    }
  }, [filters, widgetFilters, props.hiddenFilters]);

  useEffect(() => {
    if (!isEqual(props.reportType, reportRef.current) && !fetchData) {
      reportRef.current = props.reportType;
      fetchDataAgain();
    }
  }, [props.reportType]);

  const fetchDataAgain = (forceRefresh = false) => {
    if (cacheWidgetPreview && !forceRefresh) {
      return;
    }
    setFetchData(true);
  };

  const reportData = useDataTransform(reportFromApi, props.reportType, props.uri, 20, filters, null);

  const getFilters = () => {
    const combinedFilters = combineAllFilters(widgetFilters, filters, props.hiddenFilters);
    let finalFilters = {
      page_size: 1,
      page: 0,
      filter: {
        ...combinedFilters,
        ...props.globalFilters,
        // TODO: add back integration ids when BE is fixed
        integration_ids: undefined
      },
      sort: [{ id: "created_at", desc: true }]
    };

    return finalFilters;
  };

  useEffect(() => {
    setWidgetLoading(props.id, fetchData);
    if (fetchData) {
      setDataFromApi([]);
      const id = props.id || "0";
      props.genericList(props.uri, props.method, getFilters(), null, id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchData]);

  useEffect(() => {
    console.log("effect for fetching report");
    if (fetchReport !== undefined) {
      console.log("fetchreport is defined now");
      setReportFromApi({});
      if (dataFromApi && dataFromApi.length > 0) {
        const id = dataFromApi[0].id;
        props.genericGet(props.uri, id, null);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchReport]);

  useEffect(() => {
    return () => {
      const id = props.id || "0";
      props.restapiClear(props.uri, props.method, id);
      fetchReport !== undefined && props.restapiClear(props.uri, "get", fetchReport);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    const id = props.id || "0";
    if (id) {
      const { loading: apiLoading, error: apiError } = loadingStatus(props.rest_api, props.uri, props.method, id);
      if (!apiLoading && !apiError && fetchData) {
        console.log("fetched list data");
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
      }
    }
  });

  useEffect(() => {
    fetchDataAgain();
  }, [globalFilters]);

  useEffect(() => {
    if (props.reload) {
      fetchDataAgain();
      setFetchReport(undefined);
    }
  }, [props.reload]);

  const getChartPropsAndData = () => {
    return {
      id: props.id,
      ...props.chartProps,
      hasClickEvents: false,
      hideLegend: props.hideLegend,
      ...reportData
    };
  };

  const getAggs = () => {
    const chartData = getChartPropsAndData();
    const aggs = chartData.aggs || {};
    return (
      <Row gutter={[10, 10]} type={"flex"} justify={"start"}>
        {Object.keys(aggs).map(agg => {
          return (
            <Col span={4}>
              <Statistic
                title={
                  <span style={{ fontSize: "14px", fontWeight: "500", color: "var(--graph-label)" }}>
                    {toTitleCase(agg || "")}
                  </span>
                }
                value={aggs[agg]}
                valueStyle={{ fontSize: "24px", fontWeight: "600", color: "var(--graph-label)" }}
              />
            </Col>
          );
        })}
      </Row>
    );
  };

  return (
    <>
      <div style={{ height: "100%" }}>
        {fetchData ? (
          <Loader />
        ) : dataFromApi.length && reportFromApi.id !== undefined ? (
          <>
            {
              // TODO: More random stats here
              pagerDutyReports.includes(reportType) && getAggs()
            }
            {pagerDutyReports.includes(reportType) && (
              // 73px is height of stats
              <div style={{ height: "calc(100% - 73px)" }}>
                <ChartContainer chartType={props.chartType} chartProps={getChartPropsAndData()} />
              </div>
            )}
            {!pagerDutyReports.includes(reportType) && (
              <ChartContainer chartType={props.chartType} chartProps={getChartPropsAndData()} />
            )}
          </>
        ) : (
          <EmptyWidgetComponent />
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

export default connect(mapStateToProps, mapDispatchToProps)(ProductAggRestApiContainer);
