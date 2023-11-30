import FormItem from "antd/lib/form/FormItem";
import debounce from "lodash/debounce";
import * as React from "react";
import { useCallback, useEffect, useState } from "react";
import { AntInputComponent as AntInput } from "../ant-input/ant-input.component";
import NewCustomFormItemLabel from "../custom-form-item-label/NewCustomFormItemLabel";
import { withDeleteProps } from "../custom-form-item-label/CustomFormItemLabel";

interface InputRangeFilterProps {
  greaterThanKey?: string;
  lessThanKey?: string;
  onChange: (data: any) => void;
  value: any;
  label?: string;
  formClass?: string;
  withDelete?: withDeleteProps;
}

const InputRangeFilterComponent: React.FC<InputRangeFilterProps> = ({
  greaterThanKey = "$gt",
  lessThanKey = "$lt",
  value,
  label,
  onChange,
  formClass,
  withDelete
}) => {
  const [gtValue, setGTValue] = useState("");
  const [ltValue, setLTValue] = useState("");

  const debounceOnChange = useCallback(
    debounce((key: string, _value: any, value: any) => {
      const v = { ...value };
      if (!_value) {
        delete v[key];
      } else {
        v[key] = _value;
      }
      onChange(Object.keys(v).length ? v : undefined);
    }, 300),
    [onChange]
  );

  useEffect(() => {
    const greaterThanValue = value?.[greaterThanKey] ? value[greaterThanKey] : "";
    const lessThanValue = value?.[lessThanKey] ? value[lessThanKey] : "";

    if (greaterThanValue !== gtValue) {
      setGTValue(greaterThanValue);
    }

    if (lessThanValue !== ltValue) {
      setLTValue(lessThanValue);
    }
  }, [value]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <FormItem
      key={`input-range-key-value_${label}`}
      label={<NewCustomFormItemLabel label={label || ""} withDelete={withDelete} />}
      className={formClass}>
      <div style={{ display: "flex" }}>
        <AntInput
          value={gtValue}
          onChange={(e: any) => {
            setGTValue(e.target.value);
            debounceOnChange(greaterThanKey, e.target.value, value);
          }}
          placeholder="Greater Than"
        />
        <AntInput
          value={ltValue}
          onChange={(e: any) => {
            setLTValue(e.target.value);
            debounceOnChange(lessThanKey, e.target.value, value);
          }}
          placeholder="Less Than"
        />
      </div>
    </FormItem>
  );
};

export default InputRangeFilterComponent;
