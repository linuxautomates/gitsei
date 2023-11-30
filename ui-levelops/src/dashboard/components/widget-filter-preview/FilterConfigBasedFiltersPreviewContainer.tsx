import React from "react";
import FilterConfigBasedFilterPreviewComponent from "./FilterConfigBasedFilterPreviewComponent";
import { FilterConfigFiltersPreviewType } from "./types";

interface FilterConfigBasedFiltersPreviewProps {
  filtersConfigs: FilterConfigFiltersPreviewType[];
}
const FilterConfigBasedFiltersPreviewContainer: React.FC<FilterConfigBasedFiltersPreviewProps> = ({
  filtersConfigs
}) => {
  return (
    <div>
      {filtersConfigs?.map(config => (
        <FilterConfigBasedFilterPreviewComponent {...config} />
      ))}
    </div>
  );
};

export default FilterConfigBasedFiltersPreviewContainer;
