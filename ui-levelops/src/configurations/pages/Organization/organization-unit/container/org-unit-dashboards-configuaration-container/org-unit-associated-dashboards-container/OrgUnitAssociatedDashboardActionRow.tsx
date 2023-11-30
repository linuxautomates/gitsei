import { orgUnitBasicInfoType, OUDashboardType } from "configurations/configuration-types/OUTypes";
import { cloneDeep, get } from "lodash";
import React, { useCallback } from "react";
import { AntTag, AntIcon, TooltipWithTruncatedText } from "shared-resources/components";

interface OrgUnitAssociatedDashboardActionRowProps {
  associatedDashboards: Array<OUDashboardType>;
  curDashboards: OUDashboardType;
  isDefault: boolean;
  isInherited: boolean;
  handleOUChanges: (key: orgUnitBasicInfoType, value: any) => void;
}
const OrgUnitAssociatedDashboardActionRow: React.FC<OrgUnitAssociatedDashboardActionRowProps> = ({
  associatedDashboards,
  curDashboards,
  handleOUChanges,
  isDefault,
  isInherited
}) => {
  const handleSetDefaultDashboard = useCallback(
    (id: string) => {
      handleOUChanges("defaultDashboardId", id);
    },
    [handleOUChanges]
  );

  const handleMoveUp = useCallback(
    (id: string) => {
      const newDashboards = cloneDeep(associatedDashboards);
      const movedDashboardIdx = newDashboards.findIndex(dash => dash.dashboard_id === id);
      if (movedDashboardIdx > 0) {
        const tDash = newDashboards[movedDashboardIdx - 1];
        newDashboards[movedDashboardIdx - 1] = newDashboards[movedDashboardIdx];
        newDashboards[movedDashboardIdx] = tDash;
      }
      handleOUChanges("dashboards", newDashboards);
    },
    [handleOUChanges, associatedDashboards]
  );

  const handleMoveDown = useCallback(
    (id: string) => {
      const newDashboards = cloneDeep(associatedDashboards);
      const movedDashboardIdx = newDashboards.findIndex(dash => dash.dashboard_id === id);
      if (movedDashboardIdx < associatedDashboards.length - 1) {
        const tDash = newDashboards[movedDashboardIdx + 1];
        newDashboards[movedDashboardIdx + 1] = newDashboards[movedDashboardIdx];
        newDashboards[movedDashboardIdx] = tDash;
      }
      handleOUChanges("dashboards", newDashboards);
    },
    [associatedDashboards, handleOUChanges]
  );

  const handleMoveToTop = useCallback(
    (id: string) => {
      const newDashboards = cloneDeep(associatedDashboards);
      const updatedDashboards = [];
      const movedDashboardIdx = newDashboards.findIndex(dash => dash.dashboard_id === id);
      updatedDashboards.push(newDashboards[movedDashboardIdx]);
      for (let i = 0; i < newDashboards.length; i++) {
        if (i !== movedDashboardIdx) {
          updatedDashboards.push(newDashboards[i]);
        }
      }
      handleOUChanges("dashboards", updatedDashboards);
    },
    [associatedDashboards, handleOUChanges]
  );

  return (
    <div className="flex align-center justify-space-between">
      <span>
        <TooltipWithTruncatedText
          title={get(curDashboards, ["display_name"], curDashboards.name)}
          allowedTextLength={20}
        />
        {isDefault && <AntTag className="ml-40">Default</AntTag>}
      </span>
      <div className="org-dashboard-row-hover-actions">
        {!isDefault && (
          <AntTag onClick={(e: any) => handleSetDefaultDashboard(curDashboards.dashboard_id)}>Set as Default</AntTag>
        )}
      </div>
      {!isInherited && (
        <div className="org-dashboard-row-hover-actions">
          <AntTag onClick={(e: any) => handleMoveToTop(curDashboards.dashboard_id)}>Move To Top</AntTag>
          <AntIcon
            type="arrow-down"
            onClick={(e: any) => handleMoveDown(curDashboards.dashboard_id)}
            className="mr-10"></AntIcon>
          <AntIcon type="arrow-up" onClick={(e: any) => handleMoveUp(curDashboards.dashboard_id)}></AntIcon>
        </div>
      )}
    </div>
  );
};

export default OrgUnitAssociatedDashboardActionRow;
