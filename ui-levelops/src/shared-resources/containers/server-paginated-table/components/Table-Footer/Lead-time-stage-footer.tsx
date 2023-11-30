import React from "react";
import LeadTimeByStageFooterNew from "./lead-time-by-stage-footer-new";
import "./Lead-time-stage-footer.style.scss";

interface LeadTimeByStageFooterProps {
  filters?: Record<any, any>;
  setFilters?: () => void;
}
export const LeadTimeByStageFooter: React.FC<LeadTimeByStageFooterProps> = props => {
  const { filters = {}, setFilters = () => null } = props;
  return <LeadTimeByStageFooterNew filters={filters} setFilters={setFilters} />;
};

export default LeadTimeByStageFooter;
