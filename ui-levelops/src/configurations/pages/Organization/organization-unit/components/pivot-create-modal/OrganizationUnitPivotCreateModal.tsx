import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Form, Icon, notification } from "antd";
import queryString from "query-string";
import { PIVOT_LIST_ID } from "configurations/pages/Organization/Constants";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import { deleteEntities, restapiClear } from "reduxConfigs/actions/restapi";
import { pivotCreate } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntButton, AntInput, AntModal, AntSwitch, AntText } from "shared-resources/components";
import OrgUnitNameComponent from "../OrgUnitNameComponent";
import "./organizationUnitPivotCreateModal.styles.scss";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import { useLocation } from "react-router-dom";
import { CATEGORY_TOGGLE_INFO } from "./constant";
import OrgUnitCategoryNameComponent from "./OrganizationPivotNameComponent";

interface OrganizationUnitPivotCreateModalProps {
  setVisibility: (value: boolean) => void;
  resetTabs: (pivotId?: string | undefined) => void;
}

const OrganizationUnitPivotCreateModal: React.FC<OrganizationUnitPivotCreateModalProps> = ({
  resetTabs,
  setVisibility
}) => {
  const [proceedToCreatePivot, setproceedToCreatePivot] = useState<boolean>(false);
  const [name, setName] = useState<string>("");
  const [description, setDescription] = useState<string>("");
  const [enableForAllUsers, setEnableForAllUsers] = useState<boolean>(true);
  const [creatingPivot, setCreatingPivot] = useState<boolean>(false);
  const [validCategoryName, setValidCategoryName] = useState<boolean>(true);
  const [rootOUName, setRootOUName] = useState<string>("");
  const [rootOUNameValidStatus, setRootOUNameValidStatus] = useState<boolean>(true);
  const location = useLocation();
  let { ou_workspace_id } = queryString.parse(location.search);

  const pivotCreateState = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots",
    method: "create",
    uuid: "0"
  });

  const dispatch = useDispatch();

  useEffect(() => {
    if (creatingPivot) {
      const loading = get(pivotCreateState, ["loading"], true);
      const error = get(pivotCreateState, ["error"], true);
      if (!loading) {
        const pivotId = get(pivotCreateState, ["data", "id"]);
        if (!error && pivotId) {
          notification.success({
            message: "Successfully created a custom collection!"
          });
          dispatch(restapiClear("pivots_list", "list", PIVOT_LIST_ID));
        }
        setCreatingPivot(false);
        setVisibility(false);
        resetTabs(pivotId);
        dispatch(deleteEntities(["create"], "pivots"));
      }
    }
  }, [pivotCreateState, creatingPivot]);

  const handleCancel = () => {
    setVisibility(false);
  };

  const handleClickProceed = () => {
    setproceedToCreatePivot(true);
  };

  const handleSave = () => {
    setCreatingPivot(true);
    dispatch(
      pivotCreate(
        sanitizeObjectCompletely({
          name,
          description,
          is_predefined: false,
          enabled: enableForAllUsers,
          root_ou_name: rootOUName,
          workspace_id: ou_workspace_id
        })
      )
    );
  };

  const handleOUNameChange = useCallback((value: string) => setRootOUName(value), []);
  const handleOUValidStatusChange = useCallback((value: boolean) => setRootOUNameValidStatus(value), []);
  const handleCategoryValidStatusChange = useCallback((valid: boolean) => {
    setValidCategoryName(valid);
  }, []);

  const getNameLabel = useCallback(
    (label: string) => (
      <div>
        {label} <span className="required-field">*</span>
      </div>
    ),
    []
  );

  const renderFooter = useMemo(
    () => [
      <AntButton key="cancel" onClick={(e: any) => handleCancel()}>
        Cancel
      </AntButton>,
      proceedToCreatePivot ? (
        <AntButton
          key="save"
          type="primary"
          onClick={handleSave}
          disabled={!name || !rootOUName || !rootOUNameValidStatus || !validCategoryName}>
          Save
        </AntButton>
      ) : (
        <AntButton key="save" type="primary" onClick={handleClickProceed}>
          Proceed
        </AntButton>
      )
    ],
    [
      name,
      description,
      enableForAllUsers,
      proceedToCreatePivot,
      rootOUNameValidStatus,
      rootOUName,
      ou_workspace_id,
      validCategoryName
    ]
  );

  return (
    <AntModal
      title={"ADD COLLECTION CATEGORY"}
      visible={true}
      centered={true}
      closable={true}
      onCancel={(e: any) => handleCancel()}
      className="pivot-create-modal"
      footer={renderFooter}>
      <div className="content">
        {proceedToCreatePivot ? (
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
              <AntSwitch checked={enableForAllUsers} onChange={(v: boolean) => setEnableForAllUsers(v)} />
              <div className="category-toggle-info">{CATEGORY_TOGGLE_INFO}</div>
            </Form.Item>
            <OrgUnitNameComponent
              onChange={handleOUNameChange}
              onValidStatusChange={handleOUValidStatusChange}
              name={rootOUName}
              label={getNameLabel("Root Collection Name")}
            />
          </div>
        ) : (
          <div className="warning">
            <div className="title">
              <Icon type="warning" />
              Warning
            </div>
            <AntText>Custom collection types are an advanced feature.</AntText>
            <br />
            <br />
            <AntText>Contact a SEI Success Architect to ensure proper configuration.</AntText>
            <br />
            <br />
            Are you sure you want to proceed?
          </div>
        )}
      </div>
    </AntModal>
  );
};

export default OrganizationUnitPivotCreateModal;
