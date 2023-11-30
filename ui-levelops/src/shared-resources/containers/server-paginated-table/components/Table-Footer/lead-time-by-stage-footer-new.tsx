import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { get } from "lodash";
import React, { useMemo, useState } from "react";
import LeadTimeLegend from "shared-resources/charts/components/lead-time-stage-legend/LeadTimeLegend";
import "./Lead-time-stage-footer.style.scss";

interface LeadTimeByStageFooterProps {
  filters: basicMappingType<any>;
  setFilters: (param: basicMappingType<any>) => void;
}
const LeadTimeByStageFooterNew: React.FC<LeadTimeByStageFooterProps> = props => {
  const { filters, setFilters } = props;
  const defaultLegendValue = useMemo(() => {
    const ratings = get(filters, ["filter", "ratings"], []);
    const checkBoxValue = !ratings.includes("missing");
    const defaultValue = { good: checkBoxValue, needs_attention: checkBoxValue, slow: checkBoxValue };

    if (ratings.length) {
      const leftValues = { good: false, needs_attention: false, slow: false };
      return {
        ...leftValues,
        ...ratings.reduce((acc: any, next: any) => {
          return { ...acc, [next]: true };
        }, {})
      };
    }
    return defaultValue;
  }, [filters]);
  const [legend, setLegend] = useState<any>(defaultLegendValue);

  const contextFilterHandler = (newFilters: any) => {
    let ratings = Object.keys(newFilters).filter((key: string) => newFilters[key]);
    setLegend(newFilters);
    setFilters({ ...filters, filter: { ...filters.filter, ratings } });
  };

  return (
    <div className="lead-time-stage-footer">
      <LeadTimeLegend hideMissing={true} filters={legend} setFilters={contextFilterHandler} />
    </div>
  );
};

export default LeadTimeByStageFooterNew;
