import { legendColorMapping } from "custom-hooks/helpers/leadTime.helper";
import React from "react";
import { toTitleCase } from "utils/stringUtils";

interface LeadTimeStaticLegendProps {
  filterKeys: string[];
}

const LeadTimeStaticLegend: React.FC<LeadTimeStaticLegendProps> = (props: LeadTimeStaticLegendProps) => {
  const { filterKeys } = props;
  return (
    <div className="flex align-center justify-space-between">
      {filterKeys.map(key => (
        <div className="flex align-center mr-10">
          <div style={{ backgroundColor: legendColorMapping?.[key] }} className="static-legend-square" />
          <span>{toTitleCase(key)}</span>
        </div>
      ))}
    </div>
  );
};

export default LeadTimeStaticLegend;
