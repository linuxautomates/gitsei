import React, { useCallback, useState, useMemo } from "react";
import { AntCol } from "../../../../components";
import { InputNumber } from "antd";

interface InputFilterProps {
  filter: any;
  onChange: (v: number | undefined) => void;
}

export const InputFilter: React.FC<InputFilterProps> = props => {
  const { filter } = props;

  const [value, setValue] = useState<number | undefined>(filter.selected);

  const handleChange = useCallback(
    value => {
      setValue(value);
      props.onChange(value);
    },
    [props.onChange]
  );

  const memo = useMemo(() => ({ width: "100%" }), []);

  return (
    <AntCol className="gutter-row" span={filter.span ? filter.span : 4}>
      <h5>{filter.label}</h5>
      <InputNumber
        style={memo}
        id={`input-${filter.id}`}
        placeholder={filter.label}
        onChange={handleChange}
        value={value}
        name={filter.id}
      />
    </AntCol>
  );
};

export default React.memo(InputFilter);
