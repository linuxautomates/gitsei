import React, { useCallback, useState } from "react";

import "./WidgetActionBar.scss";
import SearchBox from "../../../../shared-resources/components/search-box/SearchBox";
import WidgetLibraryFilterBtn from "./WidgetLibraryFilterBtn";
import WidgetLibrarySortButton from "./WidgetLibrarySortButton";
import { useDispatch } from "react-redux";
import { updateWidgetLibrarySearchQuery } from "reduxConfigs/actions/widgetLibraryActions";

import debounce from "lodash/debounce";

interface WidgetActionBarProps {
  hideCategoryFilter?: boolean;
}

const WidgetActionBar: React.FC<WidgetActionBarProps> = ({ hideCategoryFilter }) => {
  const dispatch = useDispatch();

  const [searchQuery, setSearchQuery] = useState<string>("");
  const debounceUpdate = useCallback(
    debounce((value: string) => {
      dispatch(updateWidgetLibrarySearchQuery(value));
    }, 500),
    []
  );

  const handleDebouncedChange = useCallback((searchQuery: string) => {
    setSearchQuery(searchQuery);
    debounceUpdate(searchQuery);
  }, []);

  return (
    <div className="widget-action-bar">
      <div className="d-flex justify-space-between">
        <SearchBox
          className="library-search-box"
          placeholder="Search Widgets"
          value={searchQuery}
          onDebouncedChange={handleDebouncedChange}
        />
        <WidgetLibraryFilterBtn hideCategoryFilter={hideCategoryFilter} />
        <WidgetLibrarySortButton />
      </div>
    </div>
  );
};

export default WidgetActionBar;
