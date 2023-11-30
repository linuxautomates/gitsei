import { Button } from "antd";
import moment from "moment";
import React, { useEffect, useMemo, useState } from "react";
import { CustomSelect } from "shared-resources/components";
import { formatDate, getDate, getTimeStamp, getWeekStartEnd } from "utils/dateUtils";

interface WeekSelectionComponentProps {
  dashboardTimeGtValue: string;
  dashboardTimeLtValue: string;
  setSelectedTimeRange(timerange: any): void;
}

const WeekSelectionComponent = (props: WeekSelectionComponentProps) => {
  const { dashboardTimeGtValue, dashboardTimeLtValue, setSelectedTimeRange } = props;

  const [weeks, setWeeks] = useState<Array<{ id: string; label: string }>>([]);
  const [selectedWeek, setSelectedWeek] = useState<string>();
  const [idTimestampMap, setidTimestampMap] = useState<any>();

  useEffect(() => {
    const startDate = getDate(dashboardTimeGtValue);
    const endDate = getDate(dashboardTimeLtValue);
    const startWeekOfStartDate = startDate.subtract(7, "days");
    const weeksArray: Array<{ id: string; label: string }> = [];
    let thisWeek = endDate;
    let map: any = {};
    do {
      const week = getWeekStartEnd(thisWeek);

      const label = `${formatDate(week.start)} - ${formatDate(week.end)}`;
      const timestamp = {
        $gt: `${getTimeStamp(week.start)}`,
        $lt: `${getTimeStamp(week.end)}`
      };
      const id = `${timestamp.$gt}`;
      weeksArray.push({ id, label });
      map[id] = timestamp;

      thisWeek = week.start.subtract(7, "days");
    } while (startDate < thisWeek || (thisWeek < startDate && thisWeek > startWeekOfStartDate));

    setidTimestampMap(map);
    setSelectedWeek(weeksArray[0]["id"]);
    setWeeks(weeksArray);
  }, [dashboardTimeGtValue, dashboardTimeLtValue]);

  const selectedValueIndex = useMemo(() => weeks.findIndex(week => week.id === selectedWeek), [selectedWeek, weeks]);

  const LeftButtonClickHandler = () => setSelectedWeek(weeks[selectedValueIndex - 1].id);
  const RightButtonClickHandler = () => setSelectedWeek(weeks[selectedValueIndex + 1].id);

  useEffect(() => {
    if (selectedWeek) {
      setSelectedTimeRange(idTimestampMap[selectedWeek]);
    }
  }, [selectedWeek]);

  return (
    <>
      <Button
        icon="left"
        onClick={LeftButtonClickHandler}
        disabled={selectedValueIndex === 0}
        size="small"
        className="pr-activity-vertical-auto-margin"
      />
      <CustomSelect
        labelKey="label"
        valueKey="id"
        createOption={false}
        options={weeks}
        mode="default"
        showArrow={true}
        value={selectedWeek}
        truncateOptions={true}
        onChange={setSelectedWeek}
        labelCase={"none"}
        allowClear={false}
        className="margin-auto"
        showSearch={false}
      />
      <Button
        icon="right"
        onClick={RightButtonClickHandler}
        disabled={selectedValueIndex === weeks.length - 1}
        size="small"
        className="pr-activity-vertical-auto-margin"
      />
    </>
  );
};

export default WeekSelectionComponent;
