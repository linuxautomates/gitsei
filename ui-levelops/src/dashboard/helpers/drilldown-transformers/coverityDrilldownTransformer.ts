import { genericDrilldownTransformer } from "./genericDrilldownTransformer";
import moment from "moment";
import { DateFormats } from "../../../utils/dateUtils";

export const coverityDrillDownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);

  if (acrossValue === "snapshot_created") {
    const date = filters.filter["snapshot_created"];
    const xaxisTimestamp = moment.utc(date, DateFormats.DAY).unix();

    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        cov_defect_snapshot_created_range: {
          $gt: xaxisTimestamp.toString(),
          $lt: (xaxisTimestamp + 86399).toString()
        }
      }
    };

    delete filters.filter["snapshot_created"];
  }
  return { acrossValue, filters };
};
