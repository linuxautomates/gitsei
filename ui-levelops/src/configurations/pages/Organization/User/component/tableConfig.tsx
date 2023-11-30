import React from "react";
import { AntText } from "shared-resources/components";
import { timeColumn } from "../../../../../dashboard/pages/dashboard-tickets/configs/common-table-columns";
import { baseColumnConfig } from "../../../../../utils/base-table-config";
import { actionsColumn } from "../../../../../utils/tableUtils";

export const tableColumns = [
  {
    ...baseColumnConfig("Version", "version", { width: 100 }),
    render: (item: any, record: any) => {
      return <AntText>Version {item}</AntText>;
    }
  },
  timeColumn("TimeStamp", "created_at", { sorter: false, width: 100 }),
  { ...actionsColumn(), align: "right" }
];
