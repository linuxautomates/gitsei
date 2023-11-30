import { Checkbox, Form } from "antd";
import React, { useMemo } from "react";
import { CustomSelect } from "shared-resources/components";
import { ITEM_TEST_ID } from "../Constants";
import { buildMetricOptions, getOptionKey } from "./options.constant";

interface BullseyeMetricFilterProps {
  reportType: string;
  filters: any;
  onFilterChange: (value: any, key: string) => void;
}

const BullseyeMetricFilter: React.FC<BullseyeMetricFilterProps> = (props: BullseyeMetricFilterProps) => {
  const { reportType, filters, onFilterChange } = props;

  const optionsKey = useMemo(() => getOptionKey(reportType), [reportType]);

  const options = useMemo(() => buildMetricOptions(optionsKey as any), [optionsKey]);

  return (
    <>
      <Form.Item
        label="Metric"
        key={`${ITEM_TEST_ID}-bullseye-metric`}
        data-filterselectornamekey={`${ITEM_TEST_ID}-bullseye-metric`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-bullseye-metric`}>
        <div className="flex justify-content-between align-center">
          <CustomSelect
            dataFilterNameDropdownKey={`${ITEM_TEST_ID}-bullseye-metric_dropdown`}
            labelKey="label"
            valueKey="value"
            mode={reportType.includes("trend") ? "default" : "multiple"}
            value={filters?.metric || options[0].value}
            labelCase="none"
            sortOptions
            onChange={(value: any) => onFilterChange(value, "metric")}
            createOption={false}
            options={options}
            style={{ width: "75%" }}
          />
          {!reportType.includes("trend") && (
            <Checkbox
              style={{ width: "20%" }}
              checked={filters?.stacked_metrics}
              onChange={e => onFilterChange(e.target.checked, "stacked_metrics")}>
              Stacked
            </Checkbox>
          )}
        </div>
      </Form.Item>
    </>
  );
};

export default React.memo(BullseyeMetricFilter);
