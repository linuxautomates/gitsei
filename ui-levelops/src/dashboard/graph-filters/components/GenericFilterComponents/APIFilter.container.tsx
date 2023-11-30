import { useAPIFilter } from "custom-hooks/useAPIFilter";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AddWidgetFilterContainerProps } from "../AddWidgetFilter/AddWidgetFilter.container";

interface APIFilterContainerProps extends AddWidgetFilterContainerProps {
  filterProps: LevelOpsFilter;
  handleRemoveFilter: (key: string) => void;
  activePopKey?: string;
  handleActivePopkey: (key: string | undefined) => void;
}

const APIFilterContainer: React.FC<APIFilterContainerProps> = (props: APIFilterContainerProps) => {
  const { filterProps } = props;
  const { filterMetaData } = filterProps;

  const restData = useAPIFilter(filterMetaData as ApiDropDownData);

  const renderFilter = useMemo(() => {
    return React.createElement(props.filterProps.renderComponent, {
      ...props,
      filterProps: {
        ...props.filterProps,
        filterMetaData: { ...props.filterProps.filterMetaData, apiConfig: { ...restData } }
      }
    });
  }, [props, restData]);

  return renderFilter;
};

export default APIFilterContainer;
