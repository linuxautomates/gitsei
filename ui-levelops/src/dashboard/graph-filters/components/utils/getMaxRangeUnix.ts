import { TimeRangeLimit } from "shared-resources/components/range-picker/CustomRangePickerTypes";
import moment from "moment";

export function getMaxRangeUnix(maxRange: TimeRangeLimit | undefined) {
  const maxRangeUnix = maxRange ? moment.duration(maxRange.length, maxRange.units).asSeconds() : undefined;

  return maxRangeUnix;
}
