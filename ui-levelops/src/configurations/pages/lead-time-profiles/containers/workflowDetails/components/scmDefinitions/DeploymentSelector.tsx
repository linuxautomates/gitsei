import React from "react";
import SwitchButtonControl from "../switch-button-control/SwitchButtonControl";

interface DeploymentSelectorProps {
  calculationRoute: "pr" | "commit",
  setCalculationRoute: (depType: string) => void;
}

const DeploymentSelector: React.FC<DeploymentSelectorProps> = ({
  calculationRoute,
  setCalculationRoute
}) => {
  const options = [{
    key: "pr",
    label: "PR",
    icon: "branch"
  }, {
    key: "commit",
    label: "Commit",
    icon: "gitCommit"
  }];

  return (
    <SwitchButtonControl
      value={calculationRoute}
      onChange={setCalculationRoute}
      options={options} />
  );
}

export default DeploymentSelector;