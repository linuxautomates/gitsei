import { Button, Tooltip } from "antd";
import {
  ADD_PROFILE,
  NEW_SCHEME_ID
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { Entitlement, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import React from "react";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import { WebRoutes } from "routes/WebRoutes";

const TrellisScoreProfilesHeader = () => {
  const history = useHistory();
  const entDevProd = useHasEntitlements(Entitlement.SETTING_DEV_PRODUCTIVITY);
  const trellisProfileListState: TrellisProfilesListState = useSelector(trellisProfileListSelector);

  const handleAddProfile = () => {
    history.push(WebRoutes.trellis_profile.scheme.edit(NEW_SCHEME_ID));
  };

  const harnessPermission = useConfigScreenPermissions();
  const oldReadOnly = getRBACPermission(PermeableMetrics.TRELLIS_PROFILE_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : !harnessPermission[0];

  if (isReadOnly) {
    return <></>;
  }

  const disabled = trellisProfileListState.requireIntegrations || !entDevProd;

  return (
    <Tooltip title={disabled ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
      <Button
        type="primary"
        disabled={disabled}
        onClick={handleAddProfile}
        icon="plus-circle"
        style={{ marginTop: "auto" }}>
        {ADD_PROFILE}
      </Button>
    </Tooltip>
  );
};

export default TrellisScoreProfilesHeader;
