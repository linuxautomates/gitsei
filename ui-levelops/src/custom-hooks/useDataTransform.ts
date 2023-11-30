import { DependencyList, useEffect, useState } from "react";
import { get, orderBy } from "lodash";
import { getProps, JiraSalesforceNodeType, JiraZendeskNodeType } from "./helpers/sankey.helper";
import { findSumInArray } from "../utils/commonUtils";
import { toTitleCase } from "../utils/stringUtils";
import { convertEpochToDate, DateFormats } from "../utils/dateUtils";

export function useDataTransform(
  apiData: any,
  reportType: string,
  uri: string,
  records: number,
  filters: any,
  sortBy: any,
  deps?: DependencyList
) {
  const [chartData, setChartData] = useState(null);

  useEffect(() => {
    let data;
    // first see if it is a product agg or not in a super switch where default is this data thingie
    switch (reportType) {
      case "pagerduty_response_report":
        const trendData =
          apiData && apiData.hasOwnProperty("time_series")
            ? apiData!.time_series
                .sort((a: any, b: any) => a.from - b.from)
                .map((record: any) => ({
                  name: convertEpochToDate(record.from, DateFormats.DAY_MONTH, true),
                  by_alert_resolved: Math.round(get(record, ["by_alert_resolved", "medium"], 0) / 3600),
                  by_incident_resolved: Math.round(get(record, ["by_incident_resolved", "medium"], 0) / 3600),
                  by_incident_acknowledged: Math.round(get(record, ["by_incident_acknowledged", "medium"], 0) / 3600)
                }))
            : [];
        // @ts-ignore
        data = { data: trendData, aggs: {} };
        break;
      case "pagerduty_incident_report":
        // @ts-ignore
        const ItrendData =
          apiData && apiData.hasOwnProperty("time_series")
            ? apiData.time_series
                .sort((a: any, b: any) => a.from - b.from)
                .map((record: any) => ({
                  name: convertEpochToDate(record.from, DateFormats.DAY_MONTH, true),
                  ...Object.keys(get(record, ["by_incident_urgency"], {})).reduce((acc, obj) => {
                    // @ts-ignore
                    acc[`by_incident_urgency_${obj}`] = record.by_incident_urgency[obj];
                    return acc;
                  }, {})
                }))
            : [];
        const urgencyData = Object.keys(apiData.agg_incidents_by_urgency || {}).reduce((acc, obj) => {
          // @ts-ignore
          acc[`urgent ${obj}`] = apiData.agg_incidents_by_urgency[obj];
          return acc;
        }, {});
        data = { data: ItrendData, aggs: urgencyData };
        break;
      case "pagerduty_alert_report":
        // @ts-ignore
        const AtrendData =
          apiData && apiData.hasOwnProperty("time_series")
            ? apiData.time_series
                .sort((a: any, b: any) => a.from - b.from)
                .map((record: any) => ({
                  name: convertEpochToDate(record.from, DateFormats.DAY_MONTH, true),
                  ...Object.keys(get(record, ["by_alert_severity"], {})).reduce((acc, obj) => {
                    // @ts-ignore
                    acc[`by_alert_severity_${obj}`] = record.by_alert_severity[obj];
                    return acc;
                  }, {})
                }))
            : [];
        const alertsData = Object.keys(apiData.agg_alerts_by_severity || {})
          .slice(0, 3)
          .reduce((acc, obj) => {
            // @ts-ignore
            acc[obj] = apiData.agg_alerts_by_severity[obj];
            return acc;
          }, {});
        data = { data: AtrendData, aggs: alertsData };
        break;
      case "hygiene_report":
      case "azure_hygiene_report":
      case "zendesk_hygiene_report":
      case "salesforce_hygiene_report":
        const score = Object.keys(apiData).length
          ? Object.keys(apiData).reduce((acc, hygiene) => {
              // @ts-ignore
              acc = acc + apiData[hygiene].score;
              return acc;
            }, 0)
          : {};

        // @ts-ignore
        const breakdown = Object.keys(apiData).map((item: any) => {
          const stack_data = apiData[item]["stack_data"];
          let stackedData = {};
          let stackedTicketsTotal = 0;
          if (stack_data) {
            let stackedTicketsOtherTotal = Object.keys(stack_data)
              .sort((a: string, b: string) => stack_data[b] - stack_data[a])
              .slice(10, Object.keys(stack_data).length)
              .reduce((acc: number, obj: any) => {
                acc = acc + stack_data[obj];
                return acc;
              }, 0);

            stackedData = Object.keys(stack_data)
              .sort((a: string, b: string) => stack_data[b] - stack_data[a])
              .slice(0, 10)
              .reduce((acc: any, obj: any) => {
                acc[obj] = stack_data[obj];
                stackedTicketsTotal += stack_data[obj];
                return acc;
              }, {});

            const missingTickets =
              stackedTicketsOtherTotal +
              Math.max(apiData[item]["total_tickets"] - (stackedTicketsTotal + stackedTicketsOtherTotal), 0);

            if (missingTickets > 0) {
              stackedData = {
                ...stackedData,
                Other: missingTickets
              };
            }

            return {
              hygiene: item,
              ...apiData[item],
              stack_data: { ...stackedData }
            };
          }
          return {
            hygiene: item,
            ...apiData[item]
          };
        });
        // @ts-ignore
        data = { score: score, breakdown: breakdown };
        break;
      case "azure_hygiene_report_trends":
      case "hygiene_report_trends":
      case "zendesk_hygiene_report_trends":
        const arrayData = Object.keys(apiData).reduce((acc, obj) => {
          // @ts-ignore
          acc.push(apiData[obj]);
          return acc;
        }, []);
        //data = { data: apiData };
        data = { data: arrayData };
        break;
      case "jira_zendesk_report":
      case "jira_salesforce_report":
        if (
          apiData &&
          (apiData.zendesk_total_data || apiData.salesforce_total_data) &&
          apiData.jira_pr_data &&
          apiData.jira_no_pr_data &&
          apiData.scm_commits_data
        ) {
          let type = "zendesk";
          if (reportType === "jira_salesforce_report") {
            type = "salesforce";
          }
          // This method picks the top {limitingLength} item in list
          // and sums the remaining items in list as {limitingLength + 1} item
          // It returns original array if the item count in list <= {limitingLength}
          const _transformData = (
            data: { key: string; total_tickets?: number; total_cases?: number; filterKeys?: string[] }[],
            key: string,
            limitingLength = 3
          ) => {
            let _transformedData;
            const sortedRawData = orderBy(data || [], [key], ["desc"]);

            if (sortedRawData.length <= limitingLength) {
              _transformedData = sortedRawData;
            } else {
              const _remainingDataSum = findSumInArray(sortedRawData.slice(3), key);

              _transformedData = [
                ...sortedRawData.slice(0, 3),
                {
                  key: "OTHERS",
                  [key]: _remainingDataSum,
                  filterKeys: sortedRawData.slice(3).map(data => data.key)
                }
              ];
            }

            return _transformedData;
          };

          const typeData = _transformData(
            type === "zendesk" ? apiData.zendesk_total_data : apiData.salesforce_total_data,
            type === "zendesk" ? "total_tickets" : "total_cases"
          );

          const prData = _transformData(apiData.jira_pr_data, "total_tickets");
          const noPRData = _transformData(apiData.jira_no_pr_data, "total_tickets");
          const commitsData = _transformData(apiData.scm_commits_data, "total_tickets");

          const typeSum = findSumInArray(typeData, type === "zendesk" ? "total_tickets" : "total_cases");
          const prSum = findSumInArray(prData, "total_tickets");
          const noPRSum = findSumInArray(noPRData, "total_tickets");

          const escalatedSum = apiData.zendesk_escalated_count;
          const totalSum = typeSum;

          const scaleHeight = 100;

          const transformedZendesk = typeData.map(d => ({
            label: `${toTitleCase(d.key)} : ${d.total_tickets}`,
            name: {
              type: JiraZendeskNodeType.ZENDESK,
              filter: {
                zendesk_statuses: d.key !== "OTHERS" ? [d.key] : d.filterKeys
              }
            },
            ...getProps(d.total_tickets, typeSum, scaleHeight, 1)
          }));

          const transformedSalesforce = typeData.map((d: any) => ({
            label: `${toTitleCase(d.key)} : ${d.total_cases}`,
            name: {
              type: JiraSalesforceNodeType.SALESFORCE,
              filter: {
                salesforce_statuses: d.key !== "OTHERS" ? [d.key] : d.filterKeys
              }
            },
            ...getProps(d.total_cases, typeSum, scaleHeight, 1)
          }));

          const transformedNoPR = noPRData.map(d => ({
            label: `${toTitleCase(d.key)} : ${d.total_tickets}`,
            name: {
              type: JiraZendeskNodeType.JIRA,
              filter: {
                jira_statuses: [d.key],
                jira_has_commit: false,
                jira_get_commit: false
              }
            },
            ...getProps(d.total_tickets, typeSum, scaleHeight, 3)
          }));

          const transformedPR = [];

          // This iteration is adding commits node to each PR nodes...
          for (let i = 0; i < prData.length; i++) {
            const _prDatum = prData[i];
            const _commitDatum = commitsData.find(item => item.key === _prDatum.key);

            transformedPR.push({
              label: `${toTitleCase(_prDatum.key)} : ${_prDatum.total_tickets}`,
              name: {
                type: JiraZendeskNodeType.JIRA,
                filter: {
                  jira_statuses: [_prDatum.key],
                  jira_has_commit: true,
                  jira_get_commit: false
                }
              },
              ...getProps(_prDatum.total_tickets, totalSum, scaleHeight, 3),
              children: [
                {
                  label: `# Commits: ${_commitDatum?.total_tickets || 0}`,
                  name: {
                    type: type === "zendesk" ? JiraZendeskNodeType.COMMIT : JiraSalesforceNodeType.COMMIT,
                    filter: {
                      jira_statuses: [_prDatum.key],
                      jira_has_commit: true,
                      jira_get_commit: true
                    }
                  },
                  ...getProps(_prDatum.total_tickets, totalSum, scaleHeight, 4)
                }
              ]
            });
          }

          let graphData: any;

          if (!!totalSum) {
            graphData = {
              label: `${type === "zendesk" ? "Zendesk" : "Salesforce"} : ${totalSum}`,
              name: {
                type: type === "zendesk" ? JiraZendeskNodeType.ZENDESK : JiraSalesforceNodeType.SALESFORCE,
                filter: {}
              },
              ...getProps(totalSum, totalSum, scaleHeight, 0),
              children: type === "zendesk" ? [...transformedZendesk] : [...transformedSalesforce]
            };

            if (!!escalatedSum) {
              let escalatedChildren: any[] = [];

              if (!!prSum) {
                escalatedChildren = [
                  {
                    label: `PR : ${prSum}`,
                    name: {
                      type: JiraZendeskNodeType.JIRA,
                      filter: {
                        jira_has_commit: true,
                        jira_get_commit: false
                      }
                    },
                    ...getProps(prSum, totalSum, scaleHeight, 2),
                    children: transformedPR
                  }
                ];
              }

              if (!!noPRSum) {
                escalatedChildren = [
                  ...escalatedChildren,
                  {
                    label: `No PR : ${noPRSum}`,
                    name: {
                      type: JiraZendeskNodeType.JIRA,
                      filter: {
                        jira_has_commit: false,
                        jira_get_commit: false
                      }
                    },
                    ...getProps(noPRSum, totalSum, scaleHeight, 2),
                    children: transformedNoPR
                  }
                ];
              }

              graphData = {
                ...graphData,
                children: [
                  ...graphData.children,
                  {
                    label: `Escalated : ${escalatedSum}`,
                    name: {
                      type:
                        type === "zendesk" ? JiraZendeskNodeType.ZENDESK_LIST : JiraSalesforceNodeType.SALESFORCE_LIST,
                      filter: {}
                    },
                    ...getProps(escalatedSum, totalSum, scaleHeight, 1),
                    children: escalatedChildren
                  }
                ]
              };
            }
          }
          data = {
            data: graphData
          };
        }
        break;
      default:
        break;
    }

    // @ts-ignore
    setChartData(data);
  }, [apiData, filters, reportType, deps]);

  useEffect(() => {
    // console.log("data transform");
  }, [filters]);

  return chartData;
}
