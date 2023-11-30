import { Button, Select } from "antd";
import React, { useEffect, useState } from "react";
import { formatDate, getDate, getTimeStamp, getWeekStartEnd } from "utils/dateUtils";

interface WeekSelectionComponentProps {
  dashboardTimeGtValue: string;
  dashboardTimeLtValue: string;
  setSelectedTimeRange(timerange: any): void;
}

const DemoWeekSelectionComponent = (props: WeekSelectionComponentProps) => {
  const { dashboardTimeGtValue, dashboardTimeLtValue } = props;

  const [weeks, setWeeks] = useState<Array<any>>([]);
  const [selectedWeek, setSelectedWeek] = useState<string>("Aug 01, 2022 - Aug 07, 2022");
  const Option = Select.Option;

  useEffect(() => {
    const startDate = getDate(dashboardTimeGtValue);
    const endDate = getDate(dashboardTimeLtValue);
    const weeksArray: Array<any> = [];
    let thisWeek = startDate;
    let map: any = {};
    let count = 0;
    do {
      const week = getWeekStartEnd(thisWeek);
      const label = `${formatDate(week.start)} - ${formatDate(week.end)}`;
      const timestamp = {
        $gt: `${getTimeStamp(week.start)}`,
        $lt: `${getTimeStamp(week.end)}`
      };
      const id = `${timestamp.$gt}`;
      if (count > 0)
        weeksArray.push(
          <Option key={id} disabled={true}>
            {label}
          </Option>
        );
      map[id] = timestamp;

      thisWeek = week.start.add(7, "days");
      count++;
    } while (endDate > thisWeek);
    setWeeks(weeksArray);
  }, [dashboardTimeGtValue, dashboardTimeLtValue]);

  return (
    <>
      <Button icon="left" disabled={true} size="small" className="pr-activity-vertical-auto-margin" />
      <Select defaultValue={selectedWeek} onChange={setSelectedWeek}>
        {weeks}
      </Select>
      <Button icon="right" disabled={true} size="small" className="pr-activity-vertical-auto-margin" />
    </>
  );
};

export default DemoWeekSelectionComponent;
