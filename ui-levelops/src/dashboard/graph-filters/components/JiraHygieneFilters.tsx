import React from "react";
import { AntInput } from "shared-resources/components";
import { v1 as uuid } from "uuid";
import { Form } from "antd";
import { defaultHygineTrendsFilters } from "dashboard/constants/hygiene.constants";

interface JiraHygieneFiltersProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any) => void;
}

const JiraHygieneFilters: React.FC<JiraHygieneFiltersProps> = (props: JiraHygieneFiltersProps) => {
  const { filters, onFilterValueChange } = props;
  return (
    <>
      <Form.Item key="jira_hygiene_poor_description_filter" label={"Poor Description Length (number of characters)"}>
        <AntInput
          type="number"
          defaultValue={filters.poor_description || 10}
          style={{ width: "100%" }}
          onChange={(e: number) => onFilterValueChange(e, "poor_description")}
        />
      </Form.Item>
      <Form.Item key="jira_hygiene_idle_length_filter" label={"Idle Length (Days)"}>
        <AntInput
          type="number"
          defaultValue={filters.idle || 30}
          style={{ width: "100%" }}
          onChange={(e: number) => onFilterValueChange(e, "idle")}
        />
      </Form.Item>
    </>
  );
};

export default JiraHygieneFilters;
