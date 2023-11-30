import React from "react";
import { getNameInitials } from "utils/stringUtils";
import { ColumnProps } from "antd/lib/table";
import { capitalize } from "lodash";
import TooltipWithTruncatedTextComponent from "shared-resources/components/tooltip-with-truncated-text/TooltipWithTruncatedTextComponent";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";

export const engineerTableConfig: ColumnProps<any>[] = [
  {
    title: "Engineer",
    dataIndex: "engineer",
    key: "engineer",
    sorter: stringSortingComparator("engineer"),
    sortDirections: ["descend", "ascend"],
    fixed: "left",
    width: 400,
    className: "engineer-header",
    render: (item: string) => {
      const name = (item || "").replaceAll(/_/g, " ");
      return (
        <div className="engineer-name">
          <TooltipWithTruncatedTextComponent
            title={name}
            allowedTextLength={32}
            textClassName="avatar-text"
          />
        </div>
      );
    }
  },
  {
    title: "Allocation Summary",
    dataIndex: "allocation_summary",
    key: "effort-bar",
    fixed: "left",
    width: 300
  },
  {
    title: "Allocation Details",
    dataIndex: "category_columns",
    key: "category_columns",
    children: [],
    className: "last-header"
  }
];
