import { OUDashboardList } from "configurations/configuration-types/OUTypes";
import { PREDEFINED_DASHBOARDS } from "configurations/pages/Organization/Constants";
import React from "react";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { PREDEFINED_ROOT_DASHBOARD } from "views/Pages/landing-page/constant";
import { iconMapping } from "./constants";
import "./PredefinedDashboard.scss";

interface PredefinedDashboardProps {
  selectedDasboard: OUDashboardList | undefined;
  predefinedDashboard: Array<OUDashboardList>;
  showSeperator?: boolean;
  onClickDashboard: (val: Record<string, any>) => void;
}

const PredefinedDashboard: React.FC<PredefinedDashboardProps> = props => {
  const { selectedDasboard, predefinedDashboard, showSeperator, onClickDashboard } = props;
  const isSelected = (val: string) => {
    return selectedDasboard?.name?.toString()?.toLowerCase() === val ? "ou-item-selected" : " ";
  };
  return (
    <div className="predefined-dashboard">
      {predefinedDashboard.map((record, index: number) => {
        const name = record?.name?.toString()?.toLowerCase();
        if (PREDEFINED_DASHBOARDS.some(str => str.toLowerCase() === name)) {
          return (
            <span
              onClick={() => {
                onClickDashboard(record);
              }}
              key={`${record.name}-${index}`}
              className={`ou-item ${isSelected(name)}`}>
              <SvgIconComponent icon={`${iconMapping[name]}`} />
              <span>{record.name}</span>
              {showSeperator && <span className="seperator">|</span>}
            </span>
          );
        }
      })}
    </div>
  );
};

export default React.memo(PredefinedDashboard);
