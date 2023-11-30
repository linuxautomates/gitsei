import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AddWidgetFilterContainerProps } from "./AddWidgetFilter/AddWidgetFilter.container";
import UniversalInputTimeRangeWrapper from "./GenericFilterComponents/UniversalInputTimeRangeWrapper";

interface CodingDaysCountFilterProps extends AddWidgetFilterContainerProps {
  filterProps: LevelOpsFilter;
  handleRemoveFilter: (key: string) => void;
}

const CodingDaysCountFilterWrapperComponent: React.FC<CodingDaysCountFilterProps> = (
  props: CodingDaysCountFilterProps
) => {
  const { filterProps } = props;
  const renderFilter = useMemo(() => {
    const isAcrossIsAuthor = get(filterProps, ["allFilters", "across"], "");
    if (isAcrossIsAuthor != "author") return null;
    return React.createElement(UniversalInputTimeRangeWrapper, props);
  }, [props]);
  return renderFilter;
};

export default CodingDaysCountFilterWrapperComponent;
