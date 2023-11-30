import { debounce, get, isEqual, omit } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { compareFilters } from "utils/commonUtils";
import { v1 as uuidV1 } from "uuid";
import { Select, Spin } from "antd";
import { convertCase } from "utils/stringUtils";
import { SelectProps, SelectValue } from "antd/lib/select";
import { stringSortingComparator } from "../../../dashboard/graph-filters/components/sort.helper";

const { Option } = Select;

interface SelectRestapiProps extends SelectProps {
  uri: string;
  method?: "list" | "bulk";
  searchField: string;
  onChange: (data: any) => void;
  moreFilters?: any;
  morePayload?: any;
  fetchOnMount?: boolean;
  uuid?: string;
  refresh?: number;
  renderOption?: (data: any) => React.ReactNode;
  filterOptionMethod?: (data: any) => any;
  specialKey?: string;
  label_case?: "title_case" | "lower_case" | "upper_case";
  additionalOptions?: Array<any>;
  createOption?: boolean;
  createPrefix?: string;
  sortValues?: boolean;
  showSpinnerWhenLoading?: boolean;
  selectSpinnerClassName?: string;
  transformOptions?: (data: any) => any;
  getPopupContainer?: (triggerNode: HTMLElement) => HTMLElement;
  useOnSelect?: boolean;
  doNotUseOnChange?: boolean;
  removeKeyTypeArr?: string[];
}

const _valueMapping = (val: any) => ({
  key: val.key,
  label: val.label.key || val.label
});

const extraProps = [
  "value",
  "defaultValue",
  "showArrow",
  "showSearch",
  "filterOption",
  "onSearch",
  "onChange",
  "loading",
  "useOnSelect"
];

const SelectRestapi: React.FC<SelectRestapiProps> = (props: SelectRestapiProps) => {
  const {
    moreFilters,
    searchField,
    uuid,
    refresh,
    uri,
    method,
    fetchOnMount,
    morePayload,
    renderOption,
    filterOptionMethod,
    specialKey,
    label_case,
    additionalOptions,
    createPrefix,
    createOption,
    onChange,
    mode,
    labelInValue,
    value,
    defaultValue,
    sortValues,
    showSpinnerWhenLoading,
    selectSpinnerClassName,
    showSearch,
    dropdownClassName,
    useOnSelect,
    doNotUseOnChange,
    removeKeyTypeArr
  } = props;

  const [searchValue, setSearchValue] = useState<string>();

  const moreFiltersRef = useRef<any>(moreFilters || {});
  const uuidRef = useRef<string>(uuid !== undefined ? uuid : uuidV1());
  const refreshRef = useRef<number | undefined>(refresh);

  const dispatch = useDispatch();
  const restState = useParamSelector(getGenericRestAPISelector, { uri, method, uuid: uuidRef.current });

  const loading = useMemo(() => get(restState, "loading", true) || get(restState, "error", true), [restState]);

  const fetchData = useCallback(() => {
    dispatch(
      genericList(
        uri,
        method,
        {
          ...(morePayload || {}),
          filter: { ...moreFiltersRef.current, partial: { [searchField || "name"]: searchValue } }
        },
        null,
        uuidRef.current
      )
    );
  }, [uri, method, morePayload, moreFiltersRef.current, searchValue, uuidRef.current]);

  useEffect(() => {
    if (fetchOnMount && !loading) {
      fetchData();
    }

    return () => {
      if (fetchOnMount) {
        dispatch(restapiClear(uri, method || "list", uuidRef.current));
      }
    };
  }, []);

  useEffect(() => {
    if (!compareFilters(moreFilters, moreFiltersRef.current)) {
      fetchData();
      moreFiltersRef.current = moreFilters;
    }
  }, [moreFilters]);

  useEffect(() => {
    if (!isEqual(refresh, refreshRef.current)) {
      fetchData();
      refreshRef.current = refresh;
    }
  }, [refresh]);

  useEffect(() => fetchData(), [searchValue]);

  const makeOption = useMemo(
    () => (title: any, value: any, label: any) => {
      return (
        <Option title={title} value={value} key={value} label={label}>
          {renderOption ? renderOption(label) : label}
        </Option>
      );
    },
    [renderOption]
  );

  const mapOptions = () => {
    if (loading) return null;
    let list = get(restState, ["data", "records"], []);
    if (filterOptionMethod) {
      list = list.filter((l: any) => filterOptionMethod(l));
    }
    if (sortValues) {
      if (searchField) {
        (list || []).sort(stringSortingComparator(searchField));
      } else {
        list?.sort(stringSortingComparator());
      }
    }
    if (removeKeyTypeArr && removeKeyTypeArr.length > 0) {
      list = list.filter((data: { field_type: string; }) => !removeKeyTypeArr.includes(data.field_type))
    }
    let options = list.map((option: any) => {
      let label = typeof option === "object" ? option[searchField] : option;
      label = convertCase(label, label_case);
      if (typeof option !== "object") {
        return makeOption(label, label, label);
      }
      return makeOption(label, specialKey ? option[specialKey] : option.id, label);
    });

    if ((additionalOptions || []).length !== 0) {
      const { optionsToAppendAtStart, optionsToAppendAtEnd } = (additionalOptions || []).reduce(
        (carry, data) => {
          const label = convertCase(data[searchField], label_case);
          const option = makeOption(label, data.id, label);
          if (searchValue !== "") {
            if (!data.id.includes(searchValue)) {
              return carry;
            }
          }
          if (data?.placement === "start") {
            carry.optionsToAppendAtStart.push(option);
          } else {
            carry.optionsToAppendAtEnd.push(option);
          }
          return carry;
        },
        {
          optionsToAppendAtStart: [],
          optionsToAppendAtEnd: []
        }
      );
      if (optionsToAppendAtStart.length === 0) {
        options.push(...optionsToAppendAtEnd);
      } else {
        options = [...optionsToAppendAtStart, ...options, ...optionsToAppendAtEnd];
      }
    }

    if (createOption && searchValue !== "" && options.length === 0) {
      options.unshift(makeOption(`${searchValue}`, `${createPrefix || "create:"}${searchValue}`, `${searchValue}`));
    }

    return options;
  };

  const onSelectChange = useCallback(
    (_value: any) => {
      let value = _value;

      if (searchValue !== "") {
        setSearchValue("");
      }

      if (!!renderOption) {
        if (!!_value) {
          if (mode === "default") {
            value = _valueMapping(_value);
          } else if (mode === "multiple") {
            value = _value.map(_valueMapping);
          }
        }
      }
      if (mode === "default") {
        if (value === undefined) {
          return onChange(value);
        }
        const entry = (restState?.data?.records || []).filter((option: any) => {
          if (typeof option !== "object") return value.key === option;

          if (specialKey) return value ? option[specialKey] === value || option[specialKey] === value.key : false;

          return value ? option.id === value || option.id === value.key : false;
        });

        if (entry.length > 0) {
          if (typeof entry[0] !== "object") {
            return onChange({
              label: entry[0],
              value: entry[0]
            });
          }
          return onChange({
            label: entry?.[0]?.[searchField],
            value: specialKey ? entry?.[0]?.[specialKey] : entry?.[0]?.id
          });
        } else {
          if (!value) {
            return onChange(value);
          } else {
            return onChange({ label: value.label, value: value.key });
          }
        }
      } else {
        onChange(value);
      }
    },
    [onChange, restState, specialKey, searchField, searchValue]
  );

  const getValue = useMemo(
    () => (labelInValue && mode === "default" ? { key: value || "" } : value || undefined),
    [labelInValue, value, mode]
  );

  const handleValueSelection = () => {
    if (searchValue) {
      setSearchValue("");
    }
  };

  const debounceSearch = useCallback(debounce(setSearchValue, 300), []);

  if (showSpinnerWhenLoading && loading) {
    return (
      <div className={selectSpinnerClassName}>
        <Spin size="small" />
      </div>
    );
  }

  return (
    <Select
      {...(omit(props, extraProps) || {})}
      defaultValue={(labelInValue ? { key: defaultValue || "" } : defaultValue || "") as SelectValue}
      value={getValue as SelectValue}
      showArrow
      showSearch={showSearch}
      filterOption={false}
      dropdownClassName={dropdownClassName}
      loading={loading}
      onSearch={debounceSearch}
      onChange={doNotUseOnChange ? undefined : !useOnSelect ? onChange : onSelectChange}
      onSelect={useOnSelect ? onSelectChange : handleValueSelection}>
      {mapOptions()}
    </Select>
  );
};

SelectRestapi.defaultProps = {
  searchField: "name",
  moreFilters: {},
  morePayload: {},
  method: "list",
  onChange: option => { },
  mode: "default",
  createOption: false,
  createPrefix: "create:",
  additionalOptions: [],
  labelInValue: true,
  specialKey: undefined,
  allowClear: true,
  fetchOnMount: true,
  showSearch: true,
  dropdownClassName: "",
  refresh: 0,
  sortValues: false,
  showSpinnerWhenLoading: false,
  selectSpinnerClassName: "dropdown-spinner"
};

export default SelectRestapi;
