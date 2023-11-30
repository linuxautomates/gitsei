import { RangePickerValue } from "antd/lib/date-picker/interface";
import { getMappedRangeValue, MappedRangeType } from "./getMappedRangeValue";
import { getMaxRangeUnix } from "./getMaxRangeUnix";
import { TimeRangeLimit } from "shared-resources/components/range-picker/CustomRangePickerTypes";

export const createOnRangePickerChange = (
  maxRange: TimeRangeLimit | undefined,
  onChange: (mappedRange: MappedRangeType | undefined) => any,
  value_type?: "string" | "number"
) => {
  const maxRangeUnix = getMaxRangeUnix(maxRange);

  return (dates: RangePickerValue) => {
    const mappedRangeValue = getMappedRangeValue(dates, value_type);

    // mappedRangeValue may be undefined
    if (mappedRangeValue && mappedRangeValue.$lt && mappedRangeValue.$gt) {
      // Validate Range if needed.
      let isValid = true;
      const rangeLength = +mappedRangeValue.$lt - +mappedRangeValue.$gt;
      if (maxRangeUnix) {
        if (rangeLength > maxRangeUnix) {
          isValid = false;
        }
      }

      if (isValid) {
        onChange(mappedRangeValue);
      }
    } else {
      // setRangePickerIsOpen(false)
      onChange(mappedRangeValue);
    }
  };
};
