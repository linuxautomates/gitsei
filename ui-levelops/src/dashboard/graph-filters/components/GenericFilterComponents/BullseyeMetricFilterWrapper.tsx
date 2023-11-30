import { Checkbox, Form } from "antd";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { CustomSelect } from "shared-resources/components";
import { buildMetricOptions, getOptionKey } from "../bullseye-filters/options.constant";
import { ITEM_TEST_ID } from "../Constants";

interface BullseyeMetricFilterProps {
  filters: any;
  onFilterValueChange: (value: any, key: string) => void;
  filterProps: LevelOpsFilter;
}

const BullseyeMetricFilterWrapper: React.FC<BullseyeMetricFilterProps> = (props: BullseyeMetricFilterProps) => {
  const { filters, onFilterValueChange, filterProps } = props;
  const { label, beKey, filterMetaData, labelCase } = filterProps;
  const { selectMode, reportType } = filterMetaData as DropDownData;
  const optionsKey = useMemo(() => getOptionKey(reportType as any), [reportType]);

  const options = useMemo(() => buildMetricOptions(optionsKey as any), [optionsKey]);

  return (
    <>
      <Form.Item
        label={label}
        key={`${ITEM_TEST_ID}-bullseye-metric`}
        data-filterselectornamekey={`${ITEM_TEST_ID}-bullseye-metric`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-bullseye-metric`}>
        <div className="flex justify-content-between align-center">
          <CustomSelect
            dataFilterNameDropdownKey={`${ITEM_TEST_ID}-bullseye-metric_dropdown`}
            labelKey="label"
            valueKey="value"
            mode={selectMode}
            value={filters?.metric || options[0].value}
            labelCase={labelCase as any}
            sortOptions
            onChange={(value: any) => onFilterValueChange(value, beKey)}
            createOption={false}
            options={options}
            style={{ width: "75%" }}
          />
          {reportType && !reportType.includes("trend") && (
            <Checkbox
              style={{ width: "20%" }}
              checked={filters?.stacked_metrics}
              onChange={e => onFilterValueChange(e.target.checked, "stacked_metrics")}>
              Stacked
            </Checkbox>
          )}
        </div>
      </Form.Item>
    </>
  );
};

export default BullseyeMetricFilterWrapper;
