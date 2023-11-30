import React, { useEffect, useMemo, useState } from "react";
import { Modal, Spin } from "antd";
import { AntButton, AntIcon } from "shared-resources/components";
import { useDispatch } from "react-redux";
import "./integration-delete-smart-button.style.scss";
import {
  clearOrgUnitsListForIntegration,
  listOrgUnitsForIntegration
} from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { orgUnitsForIntegrationGetDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { get } from "lodash";
import { DELETE_INTEGRATION } from "../constant";
import { OrgUnits } from "./types.helper";
import DeleteModal from "./delete-modal";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface IntegrationDeleteSmartButtonProps {
  action: any;
}

const IntegrationDeleteSmartButton: React.FC<IntegrationDeleteSmartButtonProps> = props => {
  const { action } = props;
  const { id } = action;
  const [ouLoading, setOULoading] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [orgUnits, setOrgUnits] = useState<OrgUnits>([]);

  const dispatch = useDispatch();
  const permissions = useConfigScreenPermissions();
  const hasDeleteAccess = permissions[2];

  const orgUnitsForIntegrationState = useParamSelector(orgUnitsForIntegrationGetDataSelect, {
    id
  });

  const actionId = useMemo(() => {
    return `${DELETE_INTEGRATION}_${action.id}`;
  }, [action]);

  const clearState = () => {
    setShowConfirmModal(false);
    dispatch(clearOrgUnitsListForIntegration());
  };

  const handleOk = () => {
    action.onClickEvent(action.id);
    clearState();
  };

  const confirmPopupContent = useMemo(() => {
    if (ouLoading) {
      return <Spin />;
    }
    if (orgUnits?.length > 0) {
      return (
        <>
          <p>This integration is being used in the following collections configuration:</p>
          <div className="org-list">
            <ul>
              {orgUnits.map(unit => (
                <li>{unit.ou_name}</li>
              ))}
            </ul>
          </div>
          <p>Do you want to proceed with deletion?</p>
        </>
      );
    }
    return null;
  }, [ouLoading, orgUnits]);

  const confirmationModal = useMemo(() => {
    return (
      <DeleteModal
        showConfirmModal={showConfirmModal}
        handleOk={handleOk}
        clearState={clearState}
        ouLoading={ouLoading}
        confirmPopupContent={confirmPopupContent}
      />
    );
  }, [orgUnits, ouLoading, showConfirmModal]);

  useEffect(() => {
    const loading = get(orgUnitsForIntegrationState, "loading", true);
    const error = get(orgUnitsForIntegrationState, "error", true);
    if (!loading && !error) {
      const orgUnits = get(orgUnitsForIntegrationState, ["data"], []);
      setOrgUnits(orgUnits);
      setOULoading(false);
    }
    if (error) {
      setOULoading(false);
    }
  }, [orgUnitsForIntegrationState]);

  const onDeleteButtonClick = () => {
    setOULoading(true);
    dispatch(listOrgUnitsForIntegration(id, {}, `${actionId}_COMPLETE`));
    setShowConfirmModal(true);
  };

  const hasAccess = useMemo(() => window.isStandaloneApp || hasDeleteAccess, [hasDeleteAccess]);
  return (
    <>
      <AntButton className="ant-btn-outline mx-5" icon="delete" onClick={onDeleteButtonClick} disabled={!hasAccess} />
      {confirmationModal}
    </>
  );
};

export default IntegrationDeleteSmartButton;
