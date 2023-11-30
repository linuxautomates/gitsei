import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React from "react";
import UniversalEffortInvestmentProfileFilter from "./UniversalEffortInvestmentProfileFilter";

interface JiraBAWidgetAcrossFiltersProps {
  filters: any;
  onFilterValueChange: (value: any, key: string) => void;
  filterProps: LevelOpsFilter;
}

const JiraBAWidgetAcrossFilters: React.FC<JiraBAWidgetAcrossFiltersProps> = (props: JiraBAWidgetAcrossFiltersProps) => {
  const { filters, onFilterValueChange, filterProps } = props;
  const across = filters["across"];
  return (
    <>
      {across === "ticket_category" && (
        <>
          {
            <UniversalEffortInvestmentProfileFilter
              filterProps={filterProps}
              onFilterValueChange={onFilterValueChange}
            />
          }
        </>
      )}
    </>
  );
};

export default JiraBAWidgetAcrossFilters;
