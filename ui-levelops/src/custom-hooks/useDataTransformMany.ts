import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { allTimeFilterKeys } from "dashboard/graph-filters/components/helper";
import { get, reduce, set } from "lodash";
import { DependencyList, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import widgetConstants, { getWidgetConstant } from "../dashboard/constants/widgetConstants";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { exactIssuesState } from "reduxConfigs/selectors/reports.selector";
import { multiplexData } from "./helpers";
import { ALLOW_KEY_FOR_STACKS } from "../dashboard/constants/filter-key.mapping";
import { mapMultiTimeSeriesData } from "./helpers/multiTimeSeriesReport.helper";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";

export function useDataTransformMany(
  apiCallsData: any,
  callData: any,
  deps?: DependencyList,
  customeDateTimeKeysFields?: Array<string>
) {
  const dispatch = useDispatch();
  const [chartData, setChartData] = useState({});
  const [apisLoading, setApisLoading] = useState<{ [x: string]: boolean }>({});
  const [fetchMoreInfo, setFetchMoreInfo] = useState(false);

  const rest_api = useSelector(state =>
    exactIssuesState(
      state,
      apiCallsData.map((call: { id: any }) => call.id)
    )
  );

  // This is used to update the tooltip in the widget preview when you change
  // the name of a data series in a composite widget.
  const compositeWidgetChildNames = (apiCallsData || [])
    .map((child: any) => {
      return typeof child.childName === "string" ? child.childName || "" : "";
    })
    .join(",");

  useEffect(() => {
    let newChartData = {};
    let moreInfo = false;
    let loadings = apisLoading;
    apiCallsData.forEach((call: any) => {
      let data: { [x: string]: any };
      // first see if it is a product agg or not in a super switch where default is this data thingie
      const reportType = call.reportType;
      const apiData = callData ? callData[call.id] : undefined;
      const records = call.maxRecords;
      const uri = call.apiName;
      const sortBy = call.sortBy;
      const filters = call.localFilters;
      const statUri = call.apiStatUri;
      const widgetFilters = call.filters || {};
      const widgetName = call.childName ? call.childName : undefined;
      const metadata = { ...(call.metadata || {}), ...(call.childMetaData || {}) };
      const isMultiTimeSeriesReport = call.isMultiTimeSeriesReport;
      const dashMeta = { ...(call.dashboardMetaData || {}) };
      const supportedCustomFields = call?.supportedCustomFields || [];

      const transFunc = get(widgetConstants, [reportType, "transformFunction"], undefined);
      const timeFilterKeys = allTimeFilterKeys.concat(customeDateTimeKeysFields || []);
      if (transFunc) {
        const transData = transFunc({
          reportType,
          apiData,
          metadata,
          records,
          filters,
          widgetFilters,
          statUri,
          uri,
          sortBy,
          isMultiTimeSeriesReport,
          dashMeta,
          supportedCustomFields,
          timeFilterKeys
        });
        data = {
          ...transData,
          widgetName
        };
      } else data = {};

      if (get(filters, ["across"], "") === "teams") {
        let records = get(data, ["data"], []);
        if (records.length) {
          records = records.map((record: { name: string }) => {
            const teamsValue: string[] = (record?.name || "").split("/");
            const team = teamsValue.length ? teamsValue[2] : "";
            return {
              ...(record || {}),
              name: team
            };
          });
          set(data, ["data"], records);
        }
      }

      if ((widgetFilters?.stacks || []).length) {
        const updatedKeys = reduce(
          (data as any)?.data || [],
          (acc: any, next: any) => {
            return {
              ...acc,
              ...next
            };
          },
          {}
        );
        delete updatedKeys.count;
        delete updatedKeys.name;
        const keyAllowedForCurStacks = getWidgetConstant(reportType, ALLOW_KEY_FOR_STACKS);
        let ignoreKeys = ["name", "id", "additional_key", "timestamp"];
        if (!!keyAllowedForCurStacks) {
          ignoreKeys.push("key");
        }
        data["stackKeys"] = Object.keys(updatedKeys).filter(key => !ignoreKeys.includes(key));
      }

      Object.keys(data || {}).forEach(key => {
        if (data[key] === undefined) {
          delete data[key];
        }
      });

      if (
        ![
          jiraBAReportTypes.JIRA_PROGRESS_REPORT,
          azureBAReportTypes.AZURE_ISSUES_PROGRESS_REPORT,
          azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT
        ].includes(reportType) &&
        filters &&
        filters.across &&
        filters.across === "epic" &&
        apiData &&
        !loadings[call.id]
      ) {
        const epicFilter = { filter: { keys: apiData.map((d: { key: any }) => d.key) } };
        dispatch(genericList("jira_tickets", "list", epicFilter, null, call.id));
        loadings[call.id] = true;
        moreInfo = true;
      }

      // @ts-ignore
      newChartData[call.id] = data;
    });
    if (apiCallsData?.[0].isMultiTimeSeriesReport) {
      const apiCall = apiCallsData?.[0];
      const interval = get(apiCall, ["filters", "interval"], "day");
      const weekDateFormat = get(apiCall, ["childMetaData", "weekdate_format"], undefined);
      const options = { weekDateFormat };
      setChartData(mapMultiTimeSeriesData(newChartData, interval, options));
    } else {
      // @ts-ignore
      setChartData(multiplexData(newChartData, apiCallsData));
    }
    if (moreInfo) {
      console.log("setting more info to true");
      setFetchMoreInfo(moreInfo);
    }
    setApisLoading(loadings);
  }, [compositeWidgetChildNames, callData, deps]);

  useEffect(() => {
    if (fetchMoreInfo) {
      let loadings = apisLoading;
      // @ts-ignore
      let newData = chartData;
      Object.keys(apisLoading).forEach(id => {
        // for now just set it to false
        if (loadings[id]) {
          // @ts-ignore
          const loading = rest_api[id].loading;
          // @ts-ignore
          const error = rest_api[id].error;

          if (loading !== undefined && !loading && !error) {
            // @ts-ignore
            const data = get(rest_api, [id, "data", "records"], []);
            // @ts-ignore
            const updatedData = (newData.data || []).map((record: { name: any }) => {
              const recordData = data.find((rec: { key: any }) => rec.key === record.name);

              if (!!recordData) {
                let keysForTooltip = ["summary", "name", "key"],
                  tooltipKey;
                for (let _key of keysForTooltip) {
                  if (_key in recordData && !!recordData[_key]) {
                    tooltipKey = _key;
                    break;
                  }
                }

                if (!!tooltipKey) {
                  return {
                    ...record,
                    toolTip: recordData[tooltipKey]
                  };
                }
              }

              // No key found for tooltip, return as is
              return record;
            });
            // @ts-ignore
            newData.data = updatedData;
            loadings[id] = false;
          }
        }
      });
      // set chart data if there has been any changes
      setApisLoading(loadings);
      /* 
          TODO :  Hack for now , not setting for now
          the state still updates with the new data because of deep cloning
       */
      // setChartData(newData);
      if (Object.values(loadings).filter(value => value).length === 0) {
        setFetchMoreInfo(false);
        // @ts-ignore
        Object.keys(loadings).forEach((l: any) => dispatch(restapiClear("jira_tickets", "list", l)));
      }
    }
  }, [rest_api]);

  return chartData;
}

export function useMataDataTransformMany(
  apiCallsData: any,
  callData: any,
  apisMetaData?: any,
) {
  return apiCallsData.map((data: { id: string | number; }) => {
    return apisMetaData ? apisMetaData[data.id] : undefined;
  })
}