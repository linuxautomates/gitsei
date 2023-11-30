import { tableColumnsDevProductivity } from "./dev-productivity-score-report/tableConfig";
import { tableColumnsOrgUnit } from "./org-unit-score-report/tableConfig";
import { WebRoutes } from "../../../routes/WebRoutes";
import { timeInterval } from "../../constants/devProductivity.constant";
import { ProjectPathProps } from "@harness/microfrontends/dist/modules/10-common/interfaces/RouteInterfaces";

export const filters = {};
export const defaultQuery = {
  interval: "LAST_QUARTER"
};
export const defaultSort = { id: "score", desc: true };

export const chartPropsOrgUnit = {
  columns: tableColumnsOrgUnit,
  modifiedColumnInfo: { key: "org_name", title: "Name" },
  showNameButton: false,
  onRowClick: (
    params: ProjectPathProps,
    record: any,
    index: number,
    event: Event,
    interval: string | null,
    ou_id: Array<string>,
    locationPathName?: string
  ) => {
    if (record.org_id && ou_id?.[0]) {
      window.open(
        `${locationPathName || ""}${WebRoutes.dashboard.devProductivityDashboard(
          params,
          ou_id?.[0],
          record.org_id,
          interval?.toLowerCase()
        )}`
      );
    }
  }
};

export const chartPropsDevProductivity = {
  columns: tableColumnsDevProductivity,
  modifiedColumnInfo: { key: "full_name", title: "Name" },
  showNameButton: true,
  onRowClick: (
    params: ProjectPathProps,
    record: any,
    index: number,
    event: Event,
    interval: string | null,
    ou_id?: string | undefined,
    locationPathName?: string
  ) => {
   
    if (record.org_user_id && index != 0) {
      window.open(
        `${locationPathName || ""}${WebRoutes.dashboard.scorecard(
          params,
          record.org_user_id,
          interval?.toLowerCase(),
          "ou_user_ids",
          ou_id
        )}`
      );
    }
  }
};
