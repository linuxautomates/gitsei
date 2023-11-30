import React from "react";
import { cloneDeep, forEach, get } from "lodash";
import { Tag } from "antd";
import { optionType, ReportDrilldownColTransFuncType } from "dashboard/dashboard-types/common-types";
import { AntTooltip } from "shared-resources/components";
import { renderTagText } from "utils/tableUtils";
import { CUSTOM_STORY_POINTS_LABEL } from "dashboard/constants/constants";
import { EffortType, EffortUnitType } from "../../constants/enums/jira-ba-reports.enum";
import { ticketTimeSpentColumnConfig } from "../../pages/dashboard-tickets/configs/jiraTableConfig";

/**
 * This function transforms columns for drilldown specific to reports
 * requirements.For ex. for E.I. Trend Report, we required categories colors
 * for "Ticket Category" column.
 */
export const eIDrilldownColumnTransformFunc: ReportDrilldownColTransFuncType = utilities => {
  const { columns: _columns, categoryColorMapping, tableRecords, filters } = utilities;

  const columns = cloneDeep(_columns);

  const effortType = get(filters, ["widgetMetaData", "effort_type"], EffortType.COMPLETED_EFFORT);
  const uriUnitKey = effortType === EffortType.COMPLETED_EFFORT ? "uri_unit" : "active_work_unit";
  const uriUnit = get(filters, ["query", uriUnitKey]);

  if ([EffortUnitType.TICKET_TIME_SPENT, EffortUnitType.AZURE_TICKET_TIME_SPENT].includes(uriUnit)) {
    const xAxis = get(filters, ["x_axis"], "");
    columns.push(ticketTimeSpentColumnConfig(xAxis));
  }

  return columns.map(column => {
    let newColumn = cloneDeep(column);
    switch (column?.dataIndex) {
      case "workitem_ticket_category":
      case "ticket_category":
        newColumn = {
          ...(column ?? {}),
          render: (item: string) => (
            <AntTooltip key="string-value-type" title={item ?? ""}>
              <Tag color={(categoryColorMapping ?? {})[item ?? ""]} style={{ margin: "5px" }}>
                {(renderTagText(item ?? "") ?? "").toUpperCase()}
              </Tag>
            </AntTooltip>
          )
        };

        return newColumn;
      case "story_points":
        if (tableRecords) {
          let _custom_field_key = "";
          const customFieldsObj = tableRecords.find(
            record => Object.keys(record ?? { dkey: "" })[0] === "custom_fields"
          );
          if (customFieldsObj) {
            forEach(customFieldsObj.custom_fields ?? [], customFilter => {
              if (customFilter?.name === CUSTOM_STORY_POINTS_LABEL) {
                _custom_field_key = customFilter?.key;
              }
            });
          }
          if (!!_custom_field_key) {
            const storyPointsOptionsConfig = tableRecords.find(
              record => Object.keys(record ?? { dkey: "" })[0] === _custom_field_key
            );
            let options: optionType[] = [];
            if (storyPointsOptionsConfig) {
              options = (storyPointsOptionsConfig[_custom_field_key] ?? []).map((record: { key: string }) => ({
                label: record?.key ?? "",
                value: record?.key ?? ""
              }));
            }
            newColumn = {
              ...newColumn,
              filterField: _custom_field_key,
              options
            };
          }
        }
        return newColumn;
      default:
        return column;
    }
  });
};
