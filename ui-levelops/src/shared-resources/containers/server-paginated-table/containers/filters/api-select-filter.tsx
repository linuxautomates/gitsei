import React, { useCallback, useEffect, useState, useMemo } from "react";
import { AntCol } from "../../../../components";
import { SelectRestapi } from "../../../../helpers";
import { FilterHeader } from "./header/filter-header";
import { setFilterState } from "../../helper";
import { FILTER_TYPE } from "../../../../../constants/filters";

interface APISelectFilterProps {
  filter: any;
  onChange: (v: number | undefined | string) => void;
}

export const APISelectFilter: React.FC<APISelectFilterProps> = props => {
  const { filter } = props;

  const [value, setValue] = useState<any>("");

  const style = useMemo(() => ({ width: "100%" }), []);
  const options = useMemo(() => filter.options || [], [filter.options]);

  const morePayload = useMemo(() => {
    let _payload = filter.morePayload || {};
    if (filter.uri === "questionnaires") {
      _payload = {
        ..._payload,
        page: 0,
        page_size: 200
      };
    }
    return _payload;
  }, []);

  const handleChange = useCallback(
    (option: any) => {
      props.onChange(option);
    },
    [props.onChange]
  );

  useEffect(() => {
    const option = filter.selected;
    let val: any = undefined;
    if (option !== undefined) {
      if (option.key !== undefined) {
        val = option.key;
      } else if (option.value !== undefined) {
        val = option.value;
      } else {
        val = option;
      }
    }
    setValue(val);
    setFilterState(value, val, setValue);
  }, [filter]);

  return (
    <AntCol className="gutter-row" span={filter.span ? filter.span : filter.type === "apiSelect" ? 4 : 12}>
      <FilterHeader label={filter.label === "" ? filter.uri : filter.label} />
      <SelectRestapi
        style={style}
        id={`apiselect-${filter.id}`}
        placeholder={filter.label}
        uri={filter.uri}
        searchField={filter.searchField}
        defaultValue={filter.selected || null}
        additionalOptions={options}
        value={value || ""}
        onChange={handleChange}
        moreFilters={filter.morePayload || {}}
        transformOptions={filter.transformOptions}
        morePayload={morePayload}
        createOption={false}
        hasNewRecordsFormat={filter.hasNewRecordsFormat}
        mode={filter.type === FILTER_TYPE.API_MULTI_SELECT ? "multiple" : "default"}
        specialKey={filter.specialKey}
        loadAllData
        dropdownClassName={filter.dropdownClassName || ""}
      />
    </AntCol>
  );
};

export default React.memo(APISelectFilter);
