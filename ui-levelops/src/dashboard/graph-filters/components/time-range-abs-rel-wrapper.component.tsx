import { Form, Popconfirm, Switch } from "antd";
import { get } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { setRequiredFieldError } from "reduxConfigs/actions/requiredField";
import { AbsoluteTimeRange, TimeRangeAbsoluteRelativePayload } from "../../../model/time/time-range";
import { AntIcon, AntText, TimeRangeAbsoluteRelative } from "../../../shared-resources/components";
import { TimeRangeLimit } from "../../../shared-resources/components/range-picker/CustomRangePickerTypes";
import {
  RelativeTimeRangeUnits,
  TimeRangeFilterType
} from "../../../shared-resources/components/relative-time-range/constants";
import { RANGE_FILTER_CHOICE } from "../../constants/filter-key.mapping";
import {
  getPartialRelativeValueFromTimeRange,
  getRelativeValueFromTimeRange,
  getValueFromTimeRange,
  rangeMap
} from "./helper";
import "./time-range-abs-rel-wrapper.component.scss";
import { VALUE_EXCEED_THE_MAXIMUM_VALUE } from "./Constants";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";

interface TimeRangeAbsoluteRelativeWrapperProps {
  label: string;
  metaData: any;
  filterKey: string;
  onFilterValueChange: (value: TimeRangeAbsoluteRelativePayload, key: string) => void;
  filters: any;
  onTypeChange: (key: string, value: { type: string; relative: any }) => void;
  maxRange?: TimeRangeLimit;
  onDelete?: (value: any) => void;
  useMapping?: boolean;
  onMetadataChange?: (key: any, value: any) => void;
  dashboardMetaData?: any;
  className?: string;
  required?: boolean;
}

const initialData = {
  type: "relative",
  relative: {
    last: {
      unit: "days"
    },
    next: {
      unit: "today"
    }
  },
  absolute: {
    $lt: undefined,
    $gt: undefined
  }
} as TimeRangeAbsoluteRelativePayload;

export const TimeRangeAbsoluteRelativeWrapperComponent: React.FC<TimeRangeAbsoluteRelativeWrapperProps> = ({
  label,
  metaData,
  onFilterValueChange,
  filters,
  onTypeChange,
  onDelete,
  useMapping = true,
  filterKey,
  onMetadataChange,
  dashboardMetaData,
  className,
  required = false
}) => {
  const dispatch = useDispatch();
  // key used to save the relative value of the filter
  const _filterKey = useMapping ? get(rangeMap, filterKey, filterKey) : filterKey;

  const [data, setData] = useState<TimeRangeAbsoluteRelativePayload>(initialData);
  const [valueExceedMaximum, setValueExceedMaximum] = useState<boolean>(false);

  const getDaysCount = (value: { $gt: string; $lt: string }) => {
    const diff = parseInt(value.$lt) - parseInt(value.$gt);
    return Math.round(diff / 86400);
  };

  useEffect(() => {
    const metaDataValue: any = get(metaData, [RANGE_FILTER_CHOICE, _filterKey], {});
    // filters in not added in the deps so that we can have initial value for it
    const filterValue = get(filters, filterKey, undefined);

    const metaDataType = typeof metaDataValue;

    if (metaDataType === "object") {
      if (Object.keys(metaDataValue).length > 0) {
        setData({
          ...metaDataValue,
          absolute:
            metaDataValue.type === "relative" ? getValueFromTimeRange(metaDataValue.relative, true, true) : filterValue
        });
      } else {
        // to handle existing relative filter
        const filterValue = get(filters, filterKey, {});

        if (Object.keys(filterValue).length > 0) {
          // find the difference in days and set it
          const numOfDays = getDaysCount(filterValue);
          if (numOfDays > 0) {
            setData(_data => ({
              ..._data,
              relative: { ..._data.relative, last: { num: numOfDays, unit: RelativeTimeRangeUnits.DAYS } },
              absolute: Object.keys(filterValue).reduce(
                (acc: any, key: string) => ({ ...acc, [key]: parseInt(filterValue[key]) }),
                {}
              )
            }));
          } else {
            setData(_data => ({ ..._data, absolute: filterValue }));
          }
        }
      }
    } else if (metaDataType === "string" && filterValue) {
      // to handle existing absolute filters
      if (metaDataValue === "absolute" && Object.keys(filterValue).length > 0) {
        setData(_data => ({ ..._data, type: TimeRangeFilterType.ABSOLUTE, absolute: filterValue }));
      }

      if (metaDataValue === "slicing" && Object.keys(filterValue).length > 0) {
        const numOfDays = getDaysCount(filterValue);

        if (numOfDays > 0) {
          setData(_data => ({
            ..._data,
            relative: { ..._data.relative, last: { num: numOfDays, unit: RelativeTimeRangeUnits.DAYS } },
            absolute: getValueFromTimeRange(
              { ..._data.relative, last: { num: numOfDays, unit: RelativeTimeRangeUnits.DAYS } },
              true
            ) as AbsoluteTimeRange
          }));
        }
      }
    }
  }, [metaData]);

  const handleTimeRangeChange = async (data: TimeRangeAbsoluteRelativePayload) => {
    const _data = { ...data };
    const validValue = getValueFromTimeRange(data.relative, true, true);
    if (validValue && (parseInt(validValue.$gt as any) < 0 || parseInt(validValue.$lt as any) < 0)) {
      setValueExceedMaximum(true);
      setData(data);
      return;
    } else {
      if (valueExceedMaximum) {
        setValueExceedMaximum(false);
      }
    }
    let error: boolean | AbsoluteTimeRange = false;
    if (data.type === TimeRangeFilterType.RELATIVE) {
      const isRelativeValid = getValueFromTimeRange(data.relative, true, false) as AbsoluteTimeRange;
      if (required && !isRelativeValid) {
        error = true;
        await dispatch(
          setRequiredFieldError({ is_required_error_field: true, required_field_msg: `${label} is required` })
        );
      } else {
        await dispatch(setRequiredFieldError({ is_required_error_field: false, required_field_msg: "" }));
      }
      _data.absolute = isRelativeValid;
    } else {
      _data.relative = getPartialRelativeValueFromTimeRange(data.absolute) || initialData.relative;
    }
    if (!error) {
      onFilterValueChange(_data, filterKey);
    }
  };

  const _onTypeChange = (value: TimeRangeFilterType) => {
    let relative: any = data.relative;
    if (data.absolute && (parseInt(data.absolute.$gt as any) < 0 || parseInt(data.absolute.$lt as any) < 0)) {
      setValueExceedMaximum(true);
      return;
    } else {
      if (valueExceedMaximum) {
        setValueExceedMaximum(false);
      }
    }
    // converting absolute time into relative when the filter type is changed
    if (value === TimeRangeFilterType.RELATIVE) {
      const updatedRelative = getRelativeValueFromTimeRange(data.absolute);
      if (updatedRelative) {
        relative = updatedRelative;
      } else {
        const metaDataValue: any = get(metaData, [RANGE_FILTER_CHOICE, _filterKey], undefined);
        onFilterValueChange({ ...(metaDataValue || initialData), absolute: data.absolute, type: value }, filterKey);
        return;
      }
    } else {
      if (!relative.last.num && relative.last.num !== 0) {
        relative = { ...relative, next: { unit: "days" } };
      }
    }
    onTypeChange(_filterKey, { relative, type: value });
  };

  const _onClear = () => {
    onFilterValueChange(
      {
        type: data.type,
        relative: { last: { unit: "days" }, next: { unit: "today" } },
        absolute: undefined
      } as any,
      filterKey
    );
  };

  const deleteIconStyle = useMemo(
    () => ({
      fontSize: "15px"
    }),
    []
  );

  const formLabelStyle = useMemo(() => ({ marginTop: "18px" }), []);
  const useDashboardLabelStyle = useMemo(() => ({ color: "#8a94a5" }), []);
  const useDashboardCheckValue = useMemo(
    () => get(metaData, ["dashBoard_time_keys", filterKey, "use_dashboard_time"], false),
    [metaData, filterKey]
  );

  const _label = useMemo(
    () => (
      <div className="flex w-100 align-center justify-space-between">
        <span style={{ color: "var(--form-item-label)" }}>
          {label}
          {required && <span style={{ color: "red", fontSize: "18px" }}>*</span>}
        </span>
        <div className={"flex"}>
          {isDashboardTimerangeEnabled(dashboardMetaData) && (
            <div className="use-dashboard-time-switch-container">
              <Switch
                title={"Use Insights Time"}
                onChange={(value: any) =>
                  onMetadataChange?.(
                    { ...get(metaData, ["dashBoard_time_keys"], {}), [filterKey]: { use_dashboard_time: value } },
                    "dashBoard_time_keys"
                  )
                }
                checked={get(metaData, ["dashBoard_time_keys", filterKey, "use_dashboard_time"], false)}
              />
              <AntText style={useDashboardLabelStyle} className="use-dashboard-time-switch-container__switch-text">
                Use Insights Time
              </AntText>
            </div>
          )}
          {onDelete && (
            <Popconfirm
              title={"Are you sure you want to delete this filter?"}
              okText={"Yes"}
              onConfirm={() => onDelete?.(filterKey)}>
              <AntIcon
                className={`${isDashboardTimerangeEnabled(dashboardMetaData) ? "ml-20" : ""}`}
                style={deleteIconStyle}
                type={"delete"}
              />
            </Popconfirm>
          )}
        </div>
      </div>
    ),
    [onDelete, label, metaData, onMetadataChange, dashboardMetaData]
  );

  return (
    <Form.Item label={_label} style={formLabelStyle} className={className}>
      {!useDashboardCheckValue && (
        <TimeRangeAbsoluteRelative
          data={data}
          onChange={handleTimeRangeChange}
          onTypeChange={_onTypeChange}
          onClear={_onClear}
          required={required}
        />
      )}
      {valueExceedMaximum && <AntText style={{ color: "red" }}>{VALUE_EXCEED_THE_MAXIMUM_VALUE}</AntText>}
    </Form.Item>
  );
};
