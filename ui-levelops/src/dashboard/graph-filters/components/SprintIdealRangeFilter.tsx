import { Form } from "antd";
import { capitalize, debounce, get } from "lodash";
import React, { useCallback } from "react";
import { AntInput } from "shared-resources/components";
import { IDEAL_FILTER_MAX, IDEAL_FILTER_MIN, IDEAL_RANGE_FILTER_KEY } from "./sprintFilters.constant";
import "./sprintIdealRangeFilter.styles.scss";

interface SprintIdealRangeFilterProps {
  idealFilters: any;
  onFilterValueChange?: (value: any, type: any, reportType?: String) => void;
}

const SprintIdealRangeFilter: React.FC<SprintIdealRangeFilterProps> = ({ onFilterValueChange, idealFilters }) => {
  const handleIdealFilterValueChange = debounce(
    useCallback(
      (value: any, dataKey: string) => {
        const adjacentKey = dataKey === IDEAL_FILTER_MIN ? IDEAL_FILTER_MAX : IDEAL_FILTER_MIN;
        const adjacentValue = get(idealFilters, [adjacentKey], "");
        const dataObject: any = {};
        dataObject[dataKey] = typeof value === "string" ? "" : value;
        dataObject[adjacentKey] = adjacentValue;
        if (typeof value === "number" || typeof value === "string") {
          onFilterValueChange && onFilterValueChange(dataObject, IDEAL_RANGE_FILTER_KEY);
        }
      },
      [idealFilters]
    ),
    150
  );

  return (
    <Form.Item label="Ideal Range">
      <div className="ideal-range-container">
        <AntInput
          className="ideal-min"
          type="number"
          min={0}
          placeholder={capitalize(IDEAL_FILTER_MIN)}
          value={get(idealFilters, [IDEAL_FILTER_MIN], "")}
          onChange={(value: number) => handleIdealFilterValueChange(value, IDEAL_FILTER_MIN)}
        />
        <AntInput
          type="number"
          className="ideal-max"
          min={0}
          placeholder={capitalize(IDEAL_FILTER_MAX)}
          value={get(idealFilters, [IDEAL_FILTER_MAX], "")}
          onChange={(value: any) => handleIdealFilterValueChange(value, IDEAL_FILTER_MAX)}
        />
      </div>
    </Form.Item>
  );
};

export default React.memo(SprintIdealRangeFilter);
