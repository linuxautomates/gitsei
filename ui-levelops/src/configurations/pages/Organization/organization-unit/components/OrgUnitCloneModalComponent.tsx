import { Form } from "antd";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { checkTemplateNameExists } from "configurations/helpers/checkTemplateNameExits";
import { VALIDATING, ERROR, SUCCESS } from "constants/formWarnings";
import { uniqueId, debounce, get, cloneDeep } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import queryString from "query-string";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { OrganizationUnitClone } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntButton, AntInput, AntModal } from "shared-resources/components";
import { useLocation } from "react-router-dom";
import {
  ALLOWED_ORG_UNIT_NAME_LENGTH,
  OU_NAME_EXISTS_WARNING,
  OU_NAME_INVALID_CHARS_EXISTS_WARNING,
  OU_NAME_TOO_LARGE_TEXT
} from "../../Constants";
import { stringContainsInvalidChars } from "utils/stringUtils";

interface OrgUnitCloneModalProps {
  orgUnit?: orgUnitJSONType;
  cloneId: string;
  setVisiblitiyOff: (clonning?: boolean) => void;
}

const OrgUnitCloneModalComponent: React.FC<OrgUnitCloneModalProps> = ({ orgUnit, setVisiblitiyOff, cloneId }) => {
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [nameChecking, setNameChecking] = useState(false);
  const [nameCheckId, setNameCheckId] = useState<undefined | string>();
  const [newName, setNewName] = useState<string | undefined>();
  const [nameTooLarge, setNameTooLarge] = useState<boolean>(false);
  const [nameHasInvalidChars, setNameHasInvalidChars] = useState<boolean>(false);
  const [validStatus, setValidStatus] = useState<boolean>(false);
  const location = useLocation();
  let { ou_workspace_id } = queryString.parse(location.search);

  const dispatch = useDispatch();

  const orgUnitListState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_filter_values",
    method: "list",
    uuid: nameCheckId
  });

  useEffect(() => {
    if (nameChecking && nameCheckId) {
      const loading = get(orgUnitListState, ["loading"], true);
      const error = get(orgUnitListState, ["loading"], true);

      if (!loading && !error) {
        const data = get(orgUnitListState, ["data", "records", 0, "name", "records"], []).map((d: { key: string }) => ({
          name: d.key
        }));
        const checkName = checkTemplateNameExists((newName ?? "").trim(), data);
        setNameChecking(false);
        setNameCheckId(undefined);
        setNameExist(checkName);
      }
    }
  }, [orgUnitListState, newName]);

  const checkName = (name: string) => {
    const filter = {
      fields: ["name"],
      filter: {
        partial: {
          name: (name ?? "").trim()
        },
        workspace_id: [ou_workspace_id]
      }
    };
    const id = uniqueId();
    dispatch(genericList("organization_unit_filter_values", "list", filter, null, id));
    setNameCheckId(id);
    setNameChecking(true);
  };

  const checkDebouncedName = useCallback(debounce(checkName, 500), []);

  const getValidStatus = useMemo(() => {
    if (nameTooLarge || nameHasInvalidChars) return ERROR;

    if (nameChecking) {
      return VALIDATING;
    }
    if (!nameChecking && nameExist) {
      return ERROR;
    }
    if (!nameChecking && !nameExist) {
      return SUCCESS;
    }
  }, [nameExist, nameChecking, nameTooLarge, nameHasInvalidChars]);

  const helpText = useMemo(() => {
    let help = nameTooLarge
      ? OU_NAME_TOO_LARGE_TEXT
      : nameHasInvalidChars
      ? OU_NAME_INVALID_CHARS_EXISTS_WARNING
      : nameExist && !nameChecking
      ? OU_NAME_EXISTS_WARNING
      : "";

    let validName = true;
    if (help) {
      validName = false;
    }
    setValidStatus(validName);
    return help;
  }, [nameChecking, nameExist, nameTooLarge, nameHasInvalidChars]);

  const handleInputChange = (e: any) => {
    const name = e.target.value;
    setNewName(name);
    const invalidCharsExists = stringContainsInvalidChars(name);

    if (name?.length <= ALLOWED_ORG_UNIT_NAME_LENGTH && nameTooLarge) {
      setNameTooLarge(false);
    }

    if (!invalidCharsExists && nameHasInvalidChars) {
      setNameHasInvalidChars(false);
    }

    if (name?.length <= ALLOWED_ORG_UNIT_NAME_LENGTH && !invalidCharsExists) {
      checkDebouncedName(name);
    } else {
      if (invalidCharsExists) setNameHasInvalidChars(true);
      if (name?.length > ALLOWED_ORG_UNIT_NAME_LENGTH) setNameTooLarge(true);
    }
  };

  const handleSave = () => {
    if (orgUnit) {
      const orgUnitToClone = cloneDeep(orgUnit);
      orgUnitToClone.name = (newName ?? "").trim();
      dispatch(OrganizationUnitClone(orgUnitToClone, cloneId));
      setVisiblitiyOff(true);
    } else setVisiblitiyOff(false);
  };

  const renderFooter = useMemo(
    () => [
      <AntButton key="cancel" onClick={(e: any) => setVisiblitiyOff(false)}>
        Cancel
      </AntButton>,

      <AntButton key="save" type="primary" onClick={handleSave} disabled={!newName || !validStatus}>
        Clone
      </AntButton>
    ],
    [newName, validStatus]
  );

  const getNameLabel = useMemo(
    () => (
      <div>
        Name <span className="required-field">*</span>
      </div>
    ),
    []
  );

  return (
    <AntModal
      title="Clone Collection"
      visible={!!orgUnit}
      centered={true}
      closable={true}
      onCancel={(e: any) => setVisiblitiyOff(false)}
      className="clone-org-unit-modal"
      footer={renderFooter}>
      <Form.Item
        label={getNameLabel}
        colon={false}
        hasFeedback={newName ? true : false}
        key={"organization_name"}
        validateStatus={getValidStatus}
        help={helpText}>
        <AntInput value={newName} onChange={handleInputChange} placeHolder="Name" />
      </Form.Item>
    </AntModal>
  );
};

export default OrgUnitCloneModalComponent;
