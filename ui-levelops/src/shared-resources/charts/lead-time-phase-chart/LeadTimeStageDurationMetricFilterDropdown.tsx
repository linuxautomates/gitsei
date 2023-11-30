import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { leadTimeMetricOptions, newLeadTimeMetricOptions } from "dashboard/graph-filters/components/Constants";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import React from "react";
import { useDispatch } from "react-redux";
import { remoteWidgetUpdate, _widgetUpdateCall } from "reduxConfigs/actions/restapi";
import { AntSelect } from "shared-resources/components";

interface StageDurationMetricFilterProps {
  selectedMetrics: string;
  widgetId: string;
}
const LeadTimeStageDurationMetricFilterDropdown: React.FC<StageDurationMetricFilterProps> = ({
  selectedMetrics,
  widgetId
}) => {
  const dispatch = useDispatch();
  const handleStageDurationMetricChange = (value: string) => {
    dispatch(
      remoteWidgetUpdate(widgetId, {
        metadata: {
          metrics: value
        }
      })
    );
  };

  let newProps: any = {
    options: newLeadTimeMetricOptions.sort(stringSortingComparator("label"))
  };

  return (
    <>
      Show:
      <AntSelect
        {...newProps}
        className="stage_duration_filter_select"
        showArrow={true}
        value={selectedMetrics}
        mode={"single"}
        onChange={handleStageDurationMetricChange}
      />
    </>
  );
};

export default LeadTimeStageDurationMetricFilterDropdown;
