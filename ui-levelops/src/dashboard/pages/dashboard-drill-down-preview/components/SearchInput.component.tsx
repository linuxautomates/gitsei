import { AntInputComponent as AntInput } from "shared-resources/components/ant-input/ant-input.component";
import * as React from "react";
import { useEffect, useState } from "react";

interface SearchInputProps {
  loading?: boolean;
  value?: string;
  onChange: (value: string) => void;
}

export const SearchInput: React.FC<SearchInputProps> = props => {
  const [value, setValue] = useState<string | undefined>(undefined);
  useEffect(() => {
    if (props.value !== value) {
      setValue(props.value);
    }
  }, [props.value]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <AntInput
      type="search"
      loading={props.loading}
      id="dash-search-input"
      placeholder="Search"
      onChange={(e: any) => {
        setValue(e.target.value);
        props.onChange(e.target.value);
      }}
      value={value}
      name="search-input"
      autoComplete="off"
    />
  );
};
