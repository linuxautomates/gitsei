import React, { useCallback, useEffect, useState, useMemo } from "react";
import { AntCol, AntSelect, CustomFormItemLabel } from "../../../../components";
import { setFilterState } from "../../helper";
import { FILTER_TYPE } from "../../../../../constants/filters";

interface SelectFilterProps {
  filter: any;
  onOptionSelect: (v: string) => void;
  onSwitchValueChange: (v: boolean) => void;
}

export const SelectFilter: React.FC<SelectFilterProps> = props => {
  const { filter } = props;
  const [selected, setSelected] = useState([]);

  const [search_value, setSearchValue] = useState("");

  useEffect(() => {
    const value = filter.selected;
    let val: any = undefined;
    if (value !== undefined) {
      if (value.key !== undefined) {
        val = value.key;
      } else {
        val = value;
      }
    }
    setSelected(val);
    setFilterState(selected, val, setSelected);
  }, [filter]);

  const handleChange = useCallback(
    (value: any) => {
      let val = undefined;
      if (value !== undefined) {
        if (value.key !== undefined) {
          val = value.key;
        } else {
          val = value;
        }
      }
      // setSelected(val);
      props.onOptionSelect(val);
    },
    [props.onOptionSelect]
  );

  const handleSearchChange = useCallback((value: string) => {
    setSearchValue(value);
  }, []);

  const optionLength = (filter.options || []).length;
  let filteredOptions = filter.options;
  const maxLength = Math.min(20, optionLength);
  if (optionLength > 20) {
    filteredOptions = (filter.options || [])
      .filter((opt: any) => {
        return (
          (opt.value || "").includes(search_value) ||
          (opt.label || "").toLowerCase().includes((search_value || "").toLowerCase())
        );
      })
      .slice(0, maxLength);
  }
  //done because we have case where we have more than 20 options and the selected option has index > 20
  const filterKeys = filteredOptions.map((option: any) => option.value);
  if (filter.type === FILTER_TYPE.MULTI_SELECT) {
    (selected || []).forEach((item: any) => {
      const option = filter.options.find((option: any) => option.value === item);
      if (option && !filterKeys.includes(item)) {
        filteredOptions.push(option);
      }
    });
  }
  if (filter.type === FILTER_TYPE.SELECT) {
    const option = filter.options.find((option: any) => option.value === selected);
    if (option && !filterKeys.includes(selected)) {
      filteredOptions.push(option);
    }
  }

  const style = useMemo(() => ({ width: "100%" }), []);

  return (
    <AntCol className="gutter-row" span={filter.span ? filter.span : filter.type === "select" ? 4 : 12}>
      <CustomFormItemLabel
        label={filter.label}
        withSwitch={{
          showSwitchText: filter.showExcludeSwitch,
          showSwitch: filter.showExcludeSwitch,
          switchValue: filter.excludeSwitchValue,
          onSwitchValueChange: props.onSwitchValueChange
        }}
      />
      <AntSelect
        style={style}
        id={`select-${filter.id}`}
        placeholder={filter.label}
        onSearch={handleSearchChange}
        options={[...filteredOptions]}
        filterOption={optionLength < 20}
        defaultValue={filter.selected}
        onBlur={(e: any) => handleSearchChange("")}
        value={selected}
        onChange={handleChange}
        allowClear={true}
        mode={filter.type === FILTER_TYPE.MULTI_SELECT ? "multiple" : "default"}
      />
    </AntCol>
  );
};

export default React.memo(SelectFilter);
