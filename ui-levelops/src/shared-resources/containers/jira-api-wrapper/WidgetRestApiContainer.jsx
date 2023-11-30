import React, { useEffect, useState } from "react";
import ChartContainer from "../chart-container/chart-container.component";
import { ChartType } from '../chart-container/ChartType'
import Loader from "../../../components/Loader/Loader";
import { useApi, useDataTransform, useGlobalFilters } from "../../../custom-hooks";
import { EmptyWidget } from "../../components";

const WidgetRestApiContainer = props => {
  const [fetchData, setFetchData] = useState(0);

  const globalFilters = useGlobalFilters(props.globalFilters);

  const filters = props.filters && Object.keys(props.filters).length ? props.filters : {};

  const widgetFilters = props.widgetFilters || {};

  const uri = props.chartType === ChartType.STATS ? `${props.uri}-stat` : props.uri;

  const getFilters = () => {
    let finalFilters = {
      filter: {
        ...filters,
        ...globalFilters,
        ...widgetFilters
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
    return finalFilters;
  };

  const [apiLoading, apiData] = useApi(
    [
      {
        id: props.id,
        apiName: props.uri,
        apiMethod: props.method,
        filters: getFilters()
      }
    ],
    [fetchData, globalFilters]
  );

  const reportData = useDataTransform(
    apiData && apiData[props.id] ? apiData[props.id] : [],
    props.reportType,
    uri,
    props.maxRecords,
    props.filters,
    props.chartProps.sortBy
  );

  useEffect(() => {
    if (!apiLoading && apiData && props.reload) {
      props.setReload(false);
    }
  }, [apiLoading, apiData]);

  useEffect(() => {
    if (props.reload) {
      setFetchData(state => ++state);
    }
  }, [props.reload]);

  const getChartPropsAndData = () => {
    const filters = getFilters();
    return {
      xUnit: filters && filters.across ? filters.across : "",
      hasClickEvents: props.chartClickEnable,
      onClick: data => props.onChartClick && props.onChartClick(data),
      ...props.chartProps,
      ...reportData,
      id: props.id
    };
  };

  return (
    <div style={{ height: "100%" }}>
      {apiLoading && <Loader />}
      {apiData && Object.keys(apiData).length > 0 && reportData && (
        <ChartContainer
          chartType={props.chartType}
          //chartProps={props.chartType === ChartType.STATS ? chartPropsAndSingleStat() : getChartPropsAndData()}
          chartProps={getChartPropsAndData()}
        />
      )}
      {apiData && Object.keys(apiData).length === 0 && <EmptyWidget />}
    </div>
  );
};

export default WidgetRestApiContainer;
