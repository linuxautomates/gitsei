import React, { useCallback, useState } from "react";
import { debounce, omit } from "lodash";
import cx from "classnames";

import "./SearchBox.scss";
import { AntInput } from "../../components";
import { SearchProps } from "antd/lib/input";
import DashboardViewContainer from "../../../dashboard/pages/dashboard-view/dashboard-view.container";

interface SearchBoxProps extends SearchProps {
  onDebouncedChange?: (value: string) => void;
  debounceTime?: number;
}

const SearchBox: React.FC<SearchBoxProps> = (props: SearchBoxProps) => {
  const { value, debounceTime } = props;
  const [searchQuery, setSearchQuery] = useState<any>(props.value);

  const debouncedSearch = useCallback(
    debounce((value: string) => props.onDebouncedChange && props.onDebouncedChange(value), debounceTime ?? 250),
    [debounceTime] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const handleSearch = useCallback(
    (e: any) => {
      const { value: _value } = e.target;
      if (value !== _value) {
        setSearchQuery(_value);
        props.onChange && props.onChange(_value);
      }
      debouncedSearch(_value);
    },
    [value, props.onChange] // eslint-disable-line react-hooks/exhaustive-deps
  );

  return (
    <AntInput
      className={cx("search-box", props.className)}
      type="search"
      onChange={handleSearch}
      value={searchQuery}
      {...omit(props, "value", "onDebouncedChange", "debounceTime", "className", "search")}
    />
  );
};

export default React.memo(SearchBox);
