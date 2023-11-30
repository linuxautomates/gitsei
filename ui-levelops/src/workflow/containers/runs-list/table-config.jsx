import React from "react";
import { AntBadge } from "shared-resources/components";
import { tableCell, updatedAtColumn, actionsColumn } from "utils/tableUtils";
import StringsEn from "locales/StringsEn";

export const tableColumns = [
  {
    title: StringsEn.propel,
    key: "propel_name",
    dataIndex: "propel_name",
    width: "20%",
    filterType: "apiSelect",
    filterField: "runbook_id",
    filterLabel: "Runbook",
    uri: "propels",
    span: 7
  },
  { ...updatedAtColumn("state_changed_at"), width: "20%", align: "center" },
  {
    title: "Trigger",
    key: "trigger_type",
    dataIndex: "trigger_type",
    width: "20%",
    align: "center"
  },
  {
    title: "State",
    key: "state",
    dataIndex: "state",
    filterType: "select",
    filterField: "state",
    options: ["success", "running", "failure"],
    filterLabel: "State",
    align: "center",
    render: (item, record, index) => {
      if (record?.result?.errors?.[0]?.type === "disabled") {
        return <AntBadge status="warning" text="Disabled" />;
      }
      return tableCell("status", item);
    }
  },
  { ...actionsColumn() }
];
