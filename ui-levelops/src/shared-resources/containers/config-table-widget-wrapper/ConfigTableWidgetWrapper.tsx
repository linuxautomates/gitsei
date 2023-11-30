import Loader from "components/Loader/Loader";
import { DEFAULT_MAX_STACKED_ENTRIES } from "dashboard/constants/constants";
import { WidgetLoadingContext } from "dashboard/pages/context";
import { get, isArray, unset } from "lodash";
import React, { useContext, useEffect, useMemo, useState } from "react";
import { AntText } from "shared-resources/components";
import ChartContainer from "../chart-container/chart-container.component";
import {
  configureWidgetGroupByTransform,
  configureWidgetSingleStatTransform,
  configWidgetDataTransform
} from "./helper";
import { getValueFromTimeRange } from "../../../dashboard/graph-filters/components/helper";
import { updateTimeFiltersValue } from "../widget-api-wrapper/helper";
import moment from "moment";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import { useDispatch, useSelector } from "react-redux";
import { configTablesGet, genericList } from "reduxConfigs/actions/restapi";
import { configTablesGetState } from "reduxConfigs/selectors/restapiSelector";
import { convertFromTableSchema } from "configuration-tables/helper";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ChartType } from "../chart-container/ChartType";
import { MAX_STACK_ENTRIES } from "dashboard/reports/jira/issues-report/constants";

interface ConfigTableWidgetWrapperProps {
  id: string;
  widgetMetaData?: any;
  localFilters?: any;
  maxRecords?: number;
  chartProps?: any;
  hasClickEvents?: boolean;
  onChartClick?: (data: any, filters?: any, stackFilters?: string[]) => void;
  hideLegend?: boolean;
  dashboardMetaData?: any;
  reportType?: string;
}

const defaultTimeRange = {
  type: "absolute",
  relative: {
    next: {
      unit: "days"
    },
    last: {
      unit: "days"
    }
  }
};

const getDaysCount = (value: { $gt: string; $lt: string }) => {
  const diff = parseInt(value.$lt) - parseInt(value.$gt);
  return Math.round(diff / 86400);
};

const ConfigTableWidgetWrapper: React.FC<ConfigTableWidgetWrapperProps> = props => {
  const { widgetMetaData, localFilters, id, chartProps, hasClickEvents, hideLegend, dashboardMetaData, reportType } =
    props;
  const tableId = get(widgetMetaData, ["tableId"], "");
  const dispatch = useDispatch();
  const getRestState = useSelector(configTablesGetState);
  const [apiId, setApiId] = useState<string>("");
  const [apiData, setApiData] = useState<any>();
  const [apiLoading, setApiLoading] = useState<boolean>(false);
  const [apiLoaded, setApiLoaded] = useState<boolean>(false);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;

  const orgUnitChildrenState = useParamSelector(getGenericRestAPISelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: queryParamOU
  });

  const ouChildrenIds: string[] = useMemo(() => {
    const children: orgUnitJSONType[] = get(orgUnitChildrenState, ["data", "records"], []);
    return [queryParamOU, ...children.map(child => child.id ?? "")];
  }, [orgUnitChildrenState, queryParamOU]);

  useEffect(() => {
    if (get(orgUnitChildrenState, ["data", "records"], undefined) === undefined) {
      dispatch(
        genericList(
          "organization_unit_management",
          "list",
          {
            filter: {
              parent_ref_id: queryParamOU
            }
          },
          null,
          queryParamOU
        )
      );
    }
  }, []);
  const getTableData = () => {
    if (tableId) {
      let getId = `${tableId}?expand=schema,rows,history`;
      dispatch(configTablesGet(getId));
      setApiData(undefined);
      setApiId(getId as string);
      setApiLoading(true);
      setApiLoaded(false);
    }
  };

  useEffect(() => {
    getTableData();
  }, [widgetMetaData, queryParamOU]);

  useEffect(() => {
    if (apiLoading) {
      const restGet = get(getRestState, [apiId], {});
      const loading = get(restGet, ["loading"], true);
      const error = get(restGet, ["error"], true);
      if (!loading) {
        if (!error) {
          const data = get(restGet, ["data"], {});
          setApiData(convertFromTableSchema(data));
          setApiLoaded(true);
        } else {
          setApiData({});
        }
        setApiLoading(false);
      }
    }
  }, [getRestState, apiLoading]);

  const [mappedData, setMappedData] = useState<any>({});

  const { setWidgetLoading } = useContext(WidgetLoadingContext);

  useEffect(() => {
    setWidgetLoading(id, !apiLoaded);
  }, [apiLoaded]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (apiData && Object.keys(apiData).length > 0) {
      const widgetType = get(widgetMetaData, ["widget_type"], "");
      const groupBy = get(widgetMetaData, ["groupBy"], false);
      const stackBy = get(widgetMetaData, ["stackBy"], undefined);
      const xAxisSort = get(widgetMetaData, ["sort_xaxis"], undefined);

      const columns = get(apiData, ["columns"], []);
      let rows = get(apiData, ["rows"], []);
      const rangeFilterChoice = get(widgetMetaData, ["range_filter_choice"], {});
      const timeFilterKeys = Object.keys(localFilters).filter((key: string) => {
        const col = columns.find((col: any) => col.id === key);
        if (col) {
          return col.inputType === "date";
        }
        return false;
      });

      const timeFilters = timeFilterKeys.reduce((acc: any, next: string) => {
        const filterData = localFilters[next];

        if (isArray(filterData)) {
          return { ...acc, [next]: moment(filterData?.[0]).utc().unix() };
        }

        let _rangeChoice: any = get(rangeFilterChoice, [next], defaultTimeRange);

        if (!Object.keys(rangeFilterChoice).length) {
          // no key present in metadata, treat filter as slicing
          _rangeChoice = "slicing";
        }

        // checking for existing filter
        if (typeof _rangeChoice === "string" && filterData) {
          _rangeChoice = {
            type: _rangeChoice === "absolute" ? "absolute" : "relative",
            absolute: filterData,
            relative: {
              next: { num: 0, unit: "days" },
              last: { num: getDaysCount(filterData), unit: "days" }
            }
          };
        }

        return {
          ...acc,
          [next]: _rangeChoice.type === "relative" ? getValueFromTimeRange(_rangeChoice.relative, true) : filterData
        };
      }, {});

      const updatedTimeFilters = updateTimeFiltersValue(dashboardMetaData, widgetMetaData, timeFilters);
      const ouIds = queryParamOU ? [queryParamOU] : get(dashboardMetaData, "ou_ids", []);
      const ouColumn = columns.find((col: any) => col?.title?.toLowerCase() === "ou_id");
      rows = rows.filter((item: any) => {
        const keys = Object.keys(item);

        if (ouIds.length && ouColumn && !ouIds.includes(item?.[ouColumn.dataIndex])) {
          return false;
        }

        for (let i = 0; i < timeFilterKeys.length; i++) {
          let key = timeFilterKeys[i];
          const filterValue = updatedTimeFilters?.[key];
          const col = columns.find((col: any) => col.id === key);
          if (col) {
            key = col.dataIndex;
          }

          const value = moment.utc(item?.[key]).unix();

          if (keys.includes(key)) {
            if (typeof filterValue === "number") {
              return value === filterValue;
            } else {
              return value >= parseInt(filterValue?.$gt) && value <= parseInt(filterValue?.$lt);
            }
          }
        }
        return true;
      });

      const filters = Object.keys(localFilters)
        .filter((key: string) => !timeFilterKeys.includes(key))
        .reduce((acc: any, next: any) => {
          return {
            ...acc,
            [next]: localFilters?.[next]
          };
        }, {});

      // removing conflicting ou_id filter i.e prioritizing dashboard ou filter over widget query ou_id filter.
      if (ouIds?.length && ouColumn) {
        unset(filters, [ouColumn.id]);
      }

      const dataForTransform = {
        columns: columns,
        rows: rows,
        xAxis: get(widgetMetaData, ["xAxis"], ""),
        yAxis: get(widgetMetaData, ["yAxis"], []),
        yUnit: get(widgetMetaData, ["yUnit"], ""),
        showBaseLines: get(widgetMetaData, ["show_baseline"], false),
        showTrendLines: get(widgetMetaData, ["show_trend_line"], false),
        filters: filters,
        stackBy,
        xAxisSort,
        groupBy,
        maxStackedEntries: MAX_STACK_ENTRIES,
        maxRecords: props.maxRecords,
        ouIds,
        ouColumn
      };

      let data: any = groupBy
        ? configureWidgetGroupByTransform(dataForTransform, chartProps)
        : widgetType.includes("stat")
        ? configureWidgetSingleStatTransform(dataForTransform)
        : configWidgetDataTransform(dataForTransform, chartProps);

      setMappedData((prev: any) => ({ ...prev, ...data }));
    }
  }, [apiLoading, apiData, localFilters, widgetMetaData, props.maxRecords, dashboardMetaData, queryParamOU]);

  const isDataValid = () => {
    const widgetType = get(widgetMetaData, ["widget_type"], "");
    const groupBy = get(widgetMetaData, ["groupBy"], false);
    const xAxis = get(widgetMetaData, ["xAxis"], "");
    const yAxis = get(widgetMetaData, ["yAxis"], []);

    return groupBy
      ? !!(xAxis.length && mappedData?.data?.length)
      : widgetType.includes("stat")
      ? !!yAxis.length
      : !!(xAxis.length && yAxis.length && mappedData?.data?.length);
  };

  const hasDonutChart = useMemo(
    () => !!get(widgetMetaData, ["yAxis"], []).find((axis: any) => axis.value === "donut"),
    [widgetMetaData]
  );

  const getPropsAndData = () => {
    const widgetType = get(widgetMetaData, ["widget_type"], "");
    if (widgetType.includes("stat")) {
      return {
        ...(mappedData || {}),
        tableId,
        hasClickEvents
      };
    }
    let data = mappedData?.data || [];
    if (hasDonutChart) {
      const dataKey = get(mappedData, ["chart_props", "areaProps"], [])?.[0]?.dataKey;
      if (dataKey) {
        data = (mappedData?.data || [])
          .map((item: any) => {
            const _split = dataKey.split("_");
            const label = _split.slice(0, _split.length - 1).join(" ");
            const tooltipLabel = `${label} ${item?.[dataKey]}`;
            return {
              ...item,
              value: item?.[dataKey],
              tooltip_label: tooltipLabel
            };
          })
          .filter((item: any) => item.value !== undefined);
      }
    }
    return {
      id,
      tableId,
      hasClickEvents,
      hideLegend,
      data: data || [],
      trendLineData: mappedData.trendLineData ?? {},
      baseLinesDataPoints: mappedData?.baseLinesDataPoints ?? [],
      baseLineMap: mappedData?.baseLineMap ?? {},
      trendLineKey: mappedData?.trendLineKey,
      onClick: (data: any, stackFilters: string[]) => props?.onChartClick?.(data, null, stackFilters),
      reportType,
      displayValueOnBar: true, // considering it as true for now. For any specific case please handle here accordingly
      showValueOnBarStacks: true,
      displayValue: true,
      ...(mappedData.chart_props || {})
    };
  };

  const getChartType = () => {
    const widgetType = get(widgetMetaData, ["widget_type"], "");
    const groupBy = get(widgetMetaData, ["groupBy"], false);
    if (hasDonutChart) {
      return ChartType.DONUT;
    }
    return groupBy
      ? ChartType.COMPOSITE_TABLE_CHART
      : widgetType.includes("stat")
      ? ChartType.STATS
      : ChartType.COMPOSITE;
  };

  return (
    <>
      {apiLoading && <Loader />}
      {!apiLoading && isDataValid() && apiData && Object.keys(apiData).length > 0 && (
        //@ts-ignore
        <ChartContainer chartType={getChartType()} chartProps={getPropsAndData()} />
      )}
      {!apiLoading && (!isDataValid() || (apiData && Object.keys(apiData).length === 0)) && (
        <div style={{ display: "flex", height: "100%", alignItems: "center", justifyContent: "center" }}>
          <AntText type={"secondary"}>No Data</AntText>
        </div>
      )}
    </>
  );
};

export default ConfigTableWidgetWrapper;
