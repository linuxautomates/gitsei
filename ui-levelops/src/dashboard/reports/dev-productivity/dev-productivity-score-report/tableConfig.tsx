import { Icon, Popover, Tooltip } from "antd";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";
import ScoreCardInfoIconPopoverDetails from "dashboard/pages/scorecard/components/ScoreCardInfoIconPopoverDetails";
import { get } from "lodash";
import React from "react";
import { AntIcon } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { getTimeForTrellisProfile } from "utils/dateUtils";
import { basicMappingType } from "../../../dashboard-types/common-types";
import { displayFirstRow, displayValues, lastUpdatedAt } from "../tableConfigHelper";

export const tableColumnsDevProductivity = [
  {
    ...baseColumnConfig("Name", "full_name", { width: "12%" }),
    render: (item: any, record: any) =>
      typeof item === "object" && typeof item !== null ? (
        <span style={{ color: "#2967dd", filter: "blur(4px)", cursor: "pointer" }}>{item.name}</span>
      ) : (
        <span style={{ color: "#2967dd", cursor: "pointer" }}>{item}</span>
      )
  },
  {
    ...baseColumnConfig("Overall", "score", { sorter: true }),
    render: (item: any, record: basicMappingType<any>, index: number) =>
      index === 0 ? displayFirstRow(item) : displayValues(item)
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
          ...baseColumnConfig(section?.name || "", key, { ellipsis: false }),
          title: (
            <>
              {get(TRELLIS_SECTION_MAPPING, [section?.name || ""], section?.name || "")}{" "}
              <Popover
                title=""
                content={<ScoreCardInfoIconPopoverDetails section={section} isUseQueryParamsOU={true} />}>
                <AntIcon type="info-circle" className="ml-5" />
              </Popover>
            </>
          ),
          render: (item: any, record: basicMappingType<any>, index: number) =>
            index === 0 ? displayFirstRow(item) : displayValues(item)
        };
      });
  }
  return [];
};
