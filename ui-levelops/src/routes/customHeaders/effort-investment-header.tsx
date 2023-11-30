import { Button, Tooltip } from "antd";
import {
  ADD_PROFILE,
  NEW_SCHEME_ID
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { EIConfigurationTabs } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import React from "react";
import { useHistory } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";

const EffortInvestmentHeader = () => {
  const history = useHistory();
  const entEffortInvestment = useHasEntitlements(Entitlement.SETTING_EFFORT_INVESTMENT);
  const entEffortInvestmentCountExceed = useHasEntitlements(
    Entitlement.SETTING_EFFORT_INVESTMENT_PROFILE_COUNT_3,
    EntitlementCheckType.AND
  );

  const handleAddProfile = () => {
    history.push(WebRoutes.ticket_categorization.scheme.edit(NEW_SCHEME_ID, EIConfigurationTabs.BASIC_INFO));
  };

  const harnessPermission = useConfigScreenPermissions();
  const oldReadOnly = getRBACPermission(PermeableMetrics.EFFORT_INVESTMENT_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : !harnessPermission[0];

  if (isReadOnly) {
    return <></>;
  }

  return (
    <Tooltip title={!entEffortInvestment || entEffortInvestmentCountExceed ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
      <Button
        type="primary"
        onClick={handleAddProfile}
        disabled={!entEffortInvestment || entEffortInvestmentCountExceed}
        icon="plus-circle"
        style={{ marginTop: "auto" }}>
        {ADD_PROFILE}
      </Button>
    </Tooltip>
  );
};

export default EffortInvestmentHeader;
