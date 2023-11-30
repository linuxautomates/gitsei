import React, { useCallback, useState } from "react";
import { AntCol, AntInput } from "../../../../components";
import { FilterHeader } from "./header/filter-header";

interface SearchFilterProps {
  filter: any;
  onChange: (v: number | undefined) => void;
}

export const SearchFilter: React.FC<SearchFilterProps> = props => {
  const { filter } = props;

  const [value, setValue] = useState<number | undefined>(filter.selected);

  const handleChange = useCallback(
    (e: any) => {
      const { value } = e.target;
      setValue(value);
      props.onChange(value);
    },
    [props.onChange]
  );

  return (
    <AntCol className="gutter-row" span={filter.span ? filter.span : 4}>
      <FilterHeader label={filter.label} />
      <AntInput
        id={`search-${filter.id}`}
        placeholder={filter.label}
        onChange={handleChange}
        value={value || ""}
        name={filter.id}
      />
    </AntCol>
  );
};

export default React.memo(SearchFilter);
