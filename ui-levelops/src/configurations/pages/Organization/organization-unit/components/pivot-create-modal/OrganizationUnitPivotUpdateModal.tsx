import { Form, notification } from "antd";
import queryString from "query-string";
import { PivotType } from "configurations/configuration-types/OUTypes";
import { PIVOT_LIST_ID } from "configurations/pages/Organization/Constants";
import { get } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { deleteEntities, restapiClear } from "reduxConfigs/actions/restapi";
import { pivotUpdate } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntButton, AntInput, AntModal, AntSwitch, AntText } from "shared-resources/components";
import "./organizationUnitPivotCreateModal.styles.scss";
import { useLocation } from "react-router-dom";
import { CATEGORY_TOGGLE_INFO } from "./constant";
import OrgUnitCategoryNameComponent from "./OrganizationPivotNameComponent";

interface OrganizationUnitPivotUpdateModalProps {
  pivot?: PivotType;
  disablePivotEnableSwitch: boolean;
  setVisibility: (value?: PivotType) => void;
  resetTabs: (pivotId?: string | undefined) => void;
}
const OrganizationUnitPivotUpdateModal: React.FC<OrganizationUnitPivotUpdateModalProps> = ({
  pivot,
  resetTabs,
  disablePivotEnableSwitch,
  setVisibility
}) => {
  const [name, setName] = useState<string>(pivot?.name ?? "");
  const [description, setDescription] = useState<string>(pivot?.description ?? "");
  const [enableForAllUsers, setEnableForAllUsers] = useState<boolean>(pivot?.enabled ?? true);
  const [updatingPivot, setupdatingPivot] = useState<boolean>(false);
  const [validName, setValidName] = useState<boolean>(true);
  const location = useLocation();
  let { ou_workspace_id } = queryString.parse(location.search);
  const pivotUpdateState = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots",
    method: "update",
    uuid: pivot?.id
  });

  const dispatch = useDispatch();

  useEffect(() => {
    if (updatingPivot) {
      const loading = get(pivotUpdateState, ["loading"], true);
      const error = get(pivotUpdateState, ["error"], true);
      if (!loading) {
        const pivotId = get(pivotUpdateState, ["data", "id"]);
        if (!error && pivotId) {
          notification.success({
            message: `Successfully Updated!`
          });
          dispatch(restapiClear("pivots_list", "list", PIVOT_LIST_ID));
        }
        setupdatingPivot(false);
        setVisibility(undefined);
        resetTabs(pivotId);
        dispatch(deleteEntities(["update"], "pivots"));
      }
    }
  }, [pivotUpdateState, updatingPivot, pivot]);

  const handleCancel = () => {
    setVisibility(undefined);
  };

  const handleSave = () => {
    setupdatingPivot(true);
    dispatch(
      pivotUpdate(pivot?.id ?? "", {
        name,
        description,
        is_predefined: false,
        enabled: enableForAllUsers,
        workspace_id: ou_workspace_id
      })
    );
  };

  const renderFooter = useMemo(
    () => [
      <AntButton key="cancel" onClick={(e: any) => handleCancel()}>
        Cancel
      </AntButton>,

      <AntButton key="save" type="primary" onClick={handleSave} disabled={!name || !validName}>
        Save
      </AntButton>
    ],
    [name, description, enableForAllUsers, ou_workspace_id, validName]
  );

  const getNameLabel = useCallback(
    (label: string) => (
      <div>
        {label} <span className="required-field">*</span>
      </div>
    ),
    []
  );

  const handleCategoryValidStatusChange = useCallback((valid: boolean) => {
    setValidName(valid);
  }, []);

  return (
    <AntModal
      title={"EDIT COLLECTION CATEGORY"}
      visible={!!pivot}
      centered={true}
      closable={true}
      onCancel={(e: any) => handleCancel()}
      className="pivot-create-modal"
      footer={renderFooter}>
      <div className="content">
        <div>
          <OrgUnitCategoryNameComponent
            name={name}
            onChange={(name: string) => setName(name)}
            onValidStatusChange={handleCategoryValidStatusChange}
            label={getNameLabel("Name")}
          />
          <Form.Item label="Description" colon={false}>
            <AntInput value={description} onChange={(e: any) => setDescription(e.target.value)} />
          </Form.Item>
          <Form.Item label="ENABLE COLLECTION CATEGORY" colon={false}>
            <AntSwitch
              checked={enableForAllUsers}
              onChange={(v: boolean) => setEnableForAllUsers(v)}
              disabled={disablePivotEnableSwitch}
            />
            <div className="category-toggle-info">{CATEGORY_TOGGLE_INFO}</div>
          </Form.Item>
        </div>
      </div>
    </AntModal>
  );
};

export default OrganizationUnitPivotUpdateModal;
