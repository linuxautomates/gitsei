import { generateHygieneConfigs } from "dashboard/reports/jira/hygiene-report/filters.config";
import { unset } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AntText } from "../../../../../shared-resources/components";

interface HygieneWeightFilterProps {
  filterProps: LevelOpsFilter;
  onWeightChange: (value: any, type?: any) => void;
  weightError: string;
  widgetWeights: any;
  handleRemoveFilter: (key: string) => void;
}

const HygieneWeightFilterContainer: React.FC<HygieneWeightFilterProps> = (props: HygieneWeightFilterProps) => {
  const { weightError, filterProps } = props;
  const { filterMetaData, allFilters } = filterProps;

  const { customHygienes, options } = filterMetaData as DropDownData;

  const mappedOptions = useMemo(() => {
    if (typeof options === "function") return options({ allFilters });
    return options;
  }, [options, allFilters]);

  const allHygienesConfigs = useMemo(
    () => generateHygieneConfigs([...(customHygienes ?? []), ...mappedOptions]),
    [customHygienes, mappedOptions]
  );

  const renderFilter = useMemo(() => {
    const mappedProps: Omit<HygieneWeightFilterProps, "filterProps"> = { ...props };
    unset(mappedProps, "filterProps");
    return allHygienesConfigs.map((config: LevelOpsFilter) =>
      React.createElement(config.renderComponent, {
        filterProps: {
          ...config,
          filterMetaData: { ...props.filterProps.filterMetaData }
        },
        ...mappedProps
      })
    );
  }, [props, allHygienesConfigs]);

  return (
    <>
      {renderFilter}
      <AntText className={"f-12"} type={"danger"}>
        {weightError}
      </AntText>
    </>
  );
};

export default HygieneWeightFilterContainer;
