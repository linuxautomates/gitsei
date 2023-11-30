import widgetConstants from "dashboard/constants/widgetConstants";
import { cloneDeep, get } from "lodash";
import React, { useContext, useEffect, useMemo, useState } from "react";
import queryString from "query-string";
import { useDispatch } from "react-redux";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { convertFromTableSchema } from "configuration-tables/helper";
import { WidgetLoadingContext } from "dashboard/pages/context";
import ChartContainer from "../chart-container/chart-container.component";
import { ChartType } from "../chart-container/ChartType";
import Loader from "components/Loader/Loader";
import { AntText } from "shared-resources/components";
import { unsetKeysFromObject } from "utils/commonUtils";
import {
  IGNORE_TABLE_FILTERS,
  TableFiltersBEKeys
} from "configurable-dashboard/components/configure-widget/configuration/table/constant";
import { useLocation } from "react-router-dom";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { updateTableFilters } from "./helper";

interface TableWidgetApiWrapperProps {
  widgetId: string;
  widgetMetaData: { tableId?: string };
  filters?: any;
  maxRecords?: number;
  reportType: string;
  dashboardMetaData: any;
}
const TableWidgetApiWrapper: React.FC<TableWidgetApiWrapperProps> = ({
  widgetMetaData,
  widgetId,
  filters,
  reportType,
  dashboardMetaData
}) => {
  const [apiData, setApiData] = useState<any>();
  const [apiLoading, setApiLoading] = useState<boolean>(false);
  const [apiLoaded, setApiLoaded] = useState<boolean | undefined>(undefined);
  const tableId = `${get(widgetMetaData, ["tableId"], "")}?expand=schema,rows,history`;
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const { setWidgetLoading } = useContext(WidgetLoadingContext);
  const tableDataState = useParamSelector(getGenericRestAPISelector, {
    uri: "config_tables",
    method: "get",
    uuid: tableId
  });

  const orgUnitChildrenState = useParamSelector(getGenericRestAPISelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: queryParamOU
  });

  const ouChildrenIds: string[] = useMemo(() => {
    const children: orgUnitJSONType[] = get(orgUnitChildrenState, ["data", "records"], []);
    return [queryParamOU, ...children.map(child => child.id ?? "")];
  }, [orgUnitChildrenState, queryParamOU]);

  const dispatch = useDispatch();

  const getTableData = () => {
    if (tableId) {
      const reportAction = get(widgetConstants, [reportType, "STORE_ACTION"]);
      if (reportAction) {
        dispatch(reportAction({ tableId, ou_id: queryParamOU }));
        setApiData(undefined);
        setApiLoaded(false);
        setApiLoading(true);
      }
    }
  };

  useEffect(() => {
    getTableData();
  }, [tableId, queryParamOU]);

  useEffect(() => {
    if (apiLoaded !== undefined) setWidgetLoading(widgetId, !apiLoaded);
  }, [apiLoaded]);

  useEffect(() => {
    if (apiLoading) {
      const loading = get(tableDataState, ["loading"], true);
      const error = get(tableDataState, ["error"], true);
      if (!loading) {
        if (!error) {
          const data = get(tableDataState, ["data"], {});
          setApiData(convertFromTableSchema(data));
        } else {
          setApiData({});
        }
        setApiLoading(false);
        setApiLoaded(true);
      }
    }
  }, [tableDataState, apiLoading]);

  const getChartType = useMemo(
    () => get(widgetConstants, [reportType, "chart_type"], ChartType.LEVELOPS_TABLE_CHART),
    [reportType]
  );

  const getPropsAndData = useMemo(() => {
    const nTableFilters = cloneDeep(filters);
    unsetKeysFromObject(IGNORE_TABLE_FILTERS, nTableFilters);
    return {
      data: apiData,
      tableId: get(widgetMetaData, ["tableId"], ""),
      columns: get(filters, ["columns"], []),
      validOUIds: ouChildrenIds,
      showOUSpecificData: get(filters, [TableFiltersBEKeys.SHOW_VALUES_OF_SELECTED_OU], false),
      tableFilters: updateTableFilters(nTableFilters, get(apiData, ["columns"], []), widgetMetaData, dashboardMetaData)
    };
  }, [apiData, filters, ouChildrenIds, widgetMetaData, dashboardMetaData]);

  const ouLoading = useMemo(() => get(orgUnitChildrenState, ["loading"], false), [orgUnitChildrenState]);

  return (
    <>
      {(apiLoading || ouLoading) && <Loader />}
      {!apiLoading && apiData && Object.keys(apiData).length > 0 && (
        //@ts-ignore
        <ChartContainer chartType={getChartType} chartProps={getPropsAndData} />
      )}
      {!apiLoading && apiData && Object.keys(apiData).length === 0 && (
        <div className="flex h-100 align-center w-100 justify-center">
          <AntText type={"secondary"}>No Data</AntText>
        </div>
      )}
    </>
  );
};

export default TableWidgetApiWrapper;
