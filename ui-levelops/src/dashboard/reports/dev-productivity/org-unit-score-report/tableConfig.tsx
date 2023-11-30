import { baseColumnConfig } from "utils/base-table-config";
import React from "react";
import { displayValues, lastUpdatedAt } from "../tableConfigHelper";
import { get } from "lodash";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";
import { Icon, Tooltip } from "antd";
import { getTimeForTrellisProfile } from "utils/dateUtils";

export const tableColumnsOrgUnit = [
  {
    ...baseColumnConfig("Name", "org_name", { width: "12%" }),
    render: (item: any, record: any) => lastUpdatedAt(item, record?.result_time)
  },
  {
    ...baseColumnConfig("Overall", "score", { sorter: true }),
    render: displayValues
  }
];

export const getDynamicColumns = (data: { [key: string]: any }[] | any) => {
  const record = data && data[0];
  if (record && record?.section_responses) {
    return (record?.section_responses || [])
      .filter((section: { [enabled: string]: any }) => section.enabled)
      .map((section: { [key: string]: any }) => {
        let key = section?.name.split(" ").join("_");
        key = `${key}_score`;
        return {
          ...baseColumnConfig(get(TRELLIS_SECTION_MAPPING, [section?.name], section?.name || ""), key),
          render: displayValues
        };
      });
  }
  return [];
};
