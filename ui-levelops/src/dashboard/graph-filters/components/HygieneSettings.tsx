import React from "react";
import "./HygieneSettings.scss";
import { AntSwitchComponent } from "../../../shared-resources/components/ant-switch/ant-switch.component";
import { AntText } from "shared-resources/components";

interface HygieneJiraFilterProps {
  onFilterValueChange: (value: any, type?: any) => void;
  application: string;
  reportType: string;
  filters: any;
}

const HygieneSettingsComponent: React.FC<HygieneJiraFilterProps> = (props: HygieneJiraFilterProps) => {
  const { application, onFilterValueChange, filters } = props;

  return (
    <div className={"hygiene_settings_wrapper"}>
      <AntText strong>Hide Score</AntText>
      <AntSwitchComponent
        checked={filters.hideScore}
        onChange={(value: boolean) => onFilterValueChange(value, "hideScore")}
      />
    </div>
  );
};

export default HygieneSettingsComponent;
