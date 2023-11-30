import * as React from "react";
import { Button } from "antd";
import { AntText, SvgIcon } from "../../../shared-resources/components";
import { useHasEntitlements } from "./../../../custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "./../../../custom-hooks/constants";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";
import { useParams } from "react-router-dom";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";
import { getIsStandaloneApp } from "helper/helper";

interface NoDefaultDashProps {
  history: any;
}

export const NoDefaultDashboard: React.FC<NoDefaultDashProps> = props => {
  const entDashboard = useHasEntitlements([Entitlement.DASHBOARDS, Entitlement.ALL_FEATURES], EntitlementCheckType.OR);
  const projectParams = useParams<ProjectPathProps>();
  const [createAccess] = useDashboardPermissions();

  const hasAccess = getIsStandaloneApp() ? entDashboard : createAccess;
  return (
    <div className="flex direction-column justify-center align-center">
      <SvgIcon style={{ width: "350px", height: "350px" }} icon={"emptyDashboard"} />
      <AntText style={{ fontSize: "20px", color: "#595959" }}>There are no Insights yet</AntText>
      <AntText style={{ fontSize: "38px", color: "#2d68dd" }}>Start an insight and...</AntText>
      <AntText style={{ fontSize: "16px", color: "#595959", width: "490px", textAlign: "center" }}>
        Get data driven insights on engineering process velocity, effort investment, customer insights, product quality,
        security and more...
      </AntText>
      {hasAccess && (
        <Button
          style={{ marginTop: "2.5rem" }}
          type="primary"
          onClick={() => {
            props.history.push(`${getDashboardsPage(projectParams)}/create?default=true`);
          }}>
          Add Insight
        </Button>
      )}
    </div>
  );
};
