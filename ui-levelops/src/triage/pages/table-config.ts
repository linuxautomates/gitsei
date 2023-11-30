// @ts-ignore
import { actionsColumn, updatedAtColumn, nameColumn } from "utils/tableUtils";
import { getBaseUrl, TRIAGE_ROUTES } from "../../constants/routePaths";
import { baseColumnConfig } from "../../utils/base-table-config";

export const tableColumns = () => [
  { ...nameColumn(`${getBaseUrl()}${TRIAGE_ROUTES.EDIT}?rule`), width: "20%" },
  updatedAtColumn("created_at"),
  baseColumnConfig("Application", "application"),
  {
    ...baseColumnConfig("Regexes", "regexes"),
    render: (value: string[], record: any, index: number) => {
      return value.length;
    }
  },
  baseColumnConfig("Owner", "owner"),
  { ...actionsColumn() }
];
