import { CustomRangePickerProps } from "shared-resources/components/range-picker/CustomRangePicker";
import { getIsTrendReport } from "./getIsTrendReport";

export const MAX_RANGE = { length: 90, units: "days" } as const;

export function getMaxRangeFromReportType(report_type: string) {
  const isTrendReport = getIsTrendReport(report_type);

  const maxRange: CustomRangePickerProps["maxRange"] = isTrendReport ? MAX_RANGE : undefined;

  return maxRange;
}
