import { Form } from "antd";
import React from "react";
import { AntSelect } from "shared-resources/components";
import { ITEM_TEST_ID } from "./Constants";
import { stringSortingComparator } from "./sort.helper";
import { statSprintMetricsOptions } from "./sprintFilters.constant";

interface SprintMetricsFiltersProps {
  value: string;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
}
const SprintMetricsFilters: React.FC<SprintMetricsFiltersProps> = ({ onFilterValueChange, value }) => {
  const handleSprintMetricFilterChange = (value: string) => {
    onFilterValueChange(value, "metric");
  };
  return (
    <Form.Item
      key={"stat_sprints_metric_filter"}
      label="Metrics"
      data-filterselectornamekey={`${ITEM_TEST_ID}-stat-sprints-metric`}
      data-filtervaluesnamekey={`${ITEM_TEST_ID}-stat-sprints-metric`}>
      <AntSelect
        dropdownTestingKey={`${ITEM_TEST_ID}-stat-sprints-metric_dropdown`}
        showArrow={true}
        value={value}
        options={statSprintMetricsOptions.sort(stringSortingComparator("label"))}
        onChange={handleSprintMetricFilterChange}
      />
    </Form.Item>
  );
};

export default React.memo(SprintMetricsFilters);
