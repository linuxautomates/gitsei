import { Tag } from "antd";
import { useAPIFilter } from "custom-hooks/useAPIFilter";
import { cloneDeep, isEqual, upperCase } from "lodash";
import { ApiDropDownData } from "model/filters/levelopsFilters";
import React, { useEffect, useState } from "react";
import { AntText } from "shared-resources/components";

interface FilterConfigBasedFilterPreviewProps {
  key: string;
  label: string;
  value: string[];
  valueKey: string;
  labelKey: string;
  filterMetaData: ApiDropDownData;
}
const FilterConfigBasedFilterPreviewComponent: React.FC<FilterConfigBasedFilterPreviewProps> = ({
  label,
  key,
  value,
  valueKey,
  labelKey,
  filterMetaData
}) => {
  const [transformedValues, setTransformedValues] = useState<string[]>(value);
  const { data: filtersValuesData, loading } = useAPIFilter(filterMetaData);
  useEffect(() => {
    if (!loading && filtersValuesData && isEqual(transformedValues, value)) {
      const records = cloneDeep(filtersValuesData);
      if (records.length) {
        const newValues = value?.reduce((acc: string[], next: string) => {
          const corRec = records.find(rec => rec?.[valueKey] === next);
          if (corRec) {
            return [...acc, corRec[labelKey]];
          }
          return acc;
        }, []);
        setTransformedValues(newValues);
      }
    }
  }, [filtersValuesData, transformedValues, value, loading]);

  return (
    <div className="widget-filter" key={key}>
      <AntText className={"widget-filter_label"}>{upperCase(label)}</AntText>
      <div>
        {transformedValues?.map((filter_val: any) => {
          return <Tag key={filter_val} className="widget-filter_tags">{`${filter_val}`}</Tag>;
        })}
      </div>
    </div>
  );
};

export default FilterConfigBasedFilterPreviewComponent;
