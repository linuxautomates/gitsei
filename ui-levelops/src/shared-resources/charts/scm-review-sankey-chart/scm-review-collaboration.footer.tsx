import { basicMappingType } from "dashboard/dashboard-types/common-types";
import React from "react";
import NewLegendComponent from "../jira-effort-allocation-chart/components/LegendComponent/EffortInvestmentLegend";
import { collabStateColorMapping } from "./constant";

interface SCMReviewCollborationFooterProps {
  setLegendMapping: any;
  legendMapping: basicMappingType<boolean>;
}

const SCMReviewCollborationFooterComponent = (props: SCMReviewCollborationFooterProps) => {
  const { setLegendMapping, legendMapping } = props;
  return (
    <div className={"flex justify-space-between w-100"}>
      <NewLegendComponent setFilters={setLegendMapping} filters={legendMapping} colors={collabStateColorMapping} />
    </div>
  );
};

export default SCMReviewCollborationFooterComponent;
