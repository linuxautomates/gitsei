import { TIME_INTERVAL_TYPES } from "constants/time.constants";
import { getIntervalString } from "utils/dateUtils";
import { rawStatsDaysColumns } from "dashboard/reports/dev-productivity/rawStatsTable.config";
import { convertToDays } from "utils/timeUtils";

export const transformGraphData = (data: any, columnDataIndex: string, interval: string) =>
  data.records.map((record: any) => {
    const { additional_key } = record;
    let key = getIntervalString(
      interval === TIME_INTERVAL_TYPES.BI_WEEK || interval === TIME_INTERVAL_TYPES.WEEK ? record?.key : additional_key,
      interval as TIME_INTERVAL_TYPES
    );
    let value = null;
    if (!key) {
      key = additional_key;
    }
    const sections = record.report[0].report.section_responses;
    for (let section of sections) {
      for (let feature of section.feature_responses) {
        if (feature.name === columnDataIndex) {
          value =
            rawStatsDaysColumns.includes(columnDataIndex) && feature.count
              ? convertToDays(feature.count)
              : feature.count;
          break;
        }
      }
      if (value) {
        break;
      }
    }

    return {
      key,
      value,
      additional_key
    };
  });
