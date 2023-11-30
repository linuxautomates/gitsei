import { Form, TimePicker } from "antd";
import { PAGERDUTY_REPORT } from "dashboard/constants/applications/names";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import moment from "moment";
import React, { useMemo, useState } from "react";
import { ITEM_TEST_ID } from "../Constants";

interface PagerdutyOfficeHoursProps {
  filterProps: LevelOpsFilter;
  report?: string;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
}

const PagerdutyOfficeHoursComponent: React.FC<PagerdutyOfficeHoursProps> = ({
  filterProps,
  report,
  onFilterValueChange
}) => {
  const { beKey, allFilters: filters } = filterProps;
  const [officeHours, setOfficeHours] = useState<any>(filters.office_hours);

  const timeSelected = (type: string, time: any, timeString: any) => {
    setOfficeHours((state: any) => ({ ...state, [type]: timeString }));

    const _officeHours = { ...officeHours, [type]: timeString };

    if (report === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      if (
        _officeHours?.$to &&
        _officeHours?.$from &&
        (filters?.office_hours?.$to !== _officeHours?.$to || filters?.office_hours?.$from !== _officeHours?.$from)
      ) {
        onFilterValueChange(_officeHours, beKey);
      }

      if (Object.keys(_officeHours).length > 1 && (!_officeHours?.$to || !_officeHours?.$from)) {
        onFilterValueChange({}, beKey);
      }
    } else {
      if (
        _officeHours?.to &&
        _officeHours?.from &&
        (filters?.office_hours?.to !== _officeHours?.to || filters?.office_hours?.from !== _officeHours?.from)
      ) {
        onFilterValueChange(_officeHours, beKey);
      }

      if (Object.keys(_officeHours).length > 1 && (!_officeHours?.to || !_officeHours?.from)) {
        onFilterValueChange({}, beKey);
      }
    }
  };

  const officeHoursFiltersConfig = useMemo(() => {
    return report === PAGERDUTY_REPORT.RESPONSE_REPORTS
      ? {
          fromKey: "$from",
          toKey: "$to",
          valueFrom: officeHours?.$from ? moment(officeHours?.$from, "HH:mm") : undefined,
          valueTo: officeHours?.$to ? moment(officeHours?.$to, "HH:mm") : undefined
        }
      : {
          fromKey: "from",
          toKey: "to",
          valueFrom: officeHours?.from ? moment(officeHours?.from, "HH:mm") : undefined,
          valueTo: officeHours?.to ? moment(officeHours?.to, "HH:mm") : undefined
        };
  }, [report, officeHours]);

  return (
    <div>
      <Form.Item key={`${ITEM_TEST_ID}-pagerduty_office_hours`} label={"Office hours"}>
        <TimePicker
          value={officeHoursFiltersConfig.valueFrom}
          style={{ width: "48%", marginRight: "4%" }}
          format="HH:mm"
          placeholder={"Select from"}
          onChange={(time: any, timeString: any) => timeSelected(officeHoursFiltersConfig.fromKey, time, timeString)}
        />
        <TimePicker
          value={officeHoursFiltersConfig.valueTo}
          style={{ width: "48%" }}
          format="HH:mm"
          placeholder={"Select to"}
          onChange={(time: any, timeString: any) => timeSelected(officeHoursFiltersConfig.toKey, time, timeString)}
        />
      </Form.Item>
    </div>
  );
};

export default PagerdutyOfficeHoursComponent;
