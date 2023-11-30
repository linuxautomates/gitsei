import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import { getValueFromTimeRange } from "dashboard/graph-filters/components/helper";
import { RelativeTimeRangePayload } from "model/time/time-range";
import { RelativeTimeRangeUnits } from "shared-resources/components/relative-time-range/constants";

export const transformReportPrevQuery = (widget: any) => {
  const { query } = widget;
  let metaData = widget.metadata;

  if (!query.hasOwnProperty("pr_merged_at")) {
    const _value: RelativeTimeRangePayload = {
      next: { unit: RelativeTimeRangeUnits.TODAY },
      last: { unit: RelativeTimeRangeUnits.DAYS, num: 30 }
    };
    metaData = {
      ...metaData,
      [RANGE_FILTER_CHOICE]: {
        ...(metaData[RANGE_FILTER_CHOICE] || {}),
        pr_merged_at: {
          type: "relative",
          relative: {
            last: {
              num: 30,
              unit: "days"
            },
            next: {
              unit: "today"
            }
          }
        }
      }
    };

    query["pr_merged_at"] = getValueFromTimeRange(_value);

    widget.query = query;
    widget.metadata = metaData;
  }

  return widget;
};
