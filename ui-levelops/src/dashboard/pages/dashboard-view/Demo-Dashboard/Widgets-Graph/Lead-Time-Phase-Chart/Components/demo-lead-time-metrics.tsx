import { demoLeadTimeMetricOptions } from "dashboard/graph-filters/components/Constants";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import React, { useState } from "react";
import { _widgetUpdateCall } from "reduxConfigs/actions/restapi";
import { AntSelect } from "shared-resources/components";

const DemoLeadTimeStageDurationMetric: React.FC = () => {
  const [selectedValue, setSelectedValue] = useState<any>("mean");
  const handleStageDurationMetricChange = (value: string) => {
    setSelectedValue(value);
  };

  return (
    <AntSelect
      className="stage_duration_filter_select"
      showArrow={true}
      value={selectedValue}
      options={demoLeadTimeMetricOptions.sort(stringSortingComparator("label"))}
      mode={"single"}
      onChange={handleStageDurationMetricChange}
    />
  );
};

export default DemoLeadTimeStageDurationMetric;
