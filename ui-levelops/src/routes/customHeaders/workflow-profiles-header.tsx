import { Button, Tooltip } from "antd";
import { ADD_PROFILE } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import CreateOldNewProfileModal from "./createOldNewProfileModal/CreateOldNewProfileModal";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

const WorkflowProfilesHeader = () => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const history = useHistory();
  const entWorkflowProfile = useHasEntitlements(Entitlement.SETTING_WORKFLOW);
  const entWorkflowProfileCountExceed = useHasEntitlements(
    Entitlement.SETTING_WORKFLOW_PROFILE_COUNT_3,
    EntitlementCheckType.AND
  );

  const handleAddProfile = () => {
    setShowModal(true);
  };

  const handleModalClick = (profileType: "old" | "new") => {
    history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}?configId=new&profileType=${profileType}`);
  };

  const harnessPermission = useConfigScreenPermissions();
  const oldReadOnly = getRBACPermission(PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : !harnessPermission[0];

  if (isReadOnly) {
    return <></>;
  }

  return (
    <>
      <Tooltip title={!entWorkflowProfile || entWorkflowProfileCountExceed ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
        <Button
          type="primary"
          onClick={handleAddProfile}
          disabled={!entWorkflowProfile || entWorkflowProfileCountExceed}
          icon="plus-circle"
          style={{ marginTop: "auto" }}>
          {ADD_PROFILE}
        </Button>
      </Tooltip>
      {showModal && (
        <CreateOldNewProfileModal setVisibility={setShowModal} handleClickProceedButton={handleModalClick} />
      )}
    </>
  );
};

export default WorkflowProfilesHeader;
