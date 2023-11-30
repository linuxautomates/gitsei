import { Form } from "antd";
import { checkTemplateNameExists } from "configurations/helpers/checkTemplateNameExits";
import queryString from "query-string";
import { VALIDATING, ERROR, SUCCESS } from "constants/formWarnings";
import { debounce, get, uniqueId } from "lodash";
import React, { ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntInput } from "shared-resources/components";
import { useLocation } from "react-router-dom";
import {
  ALLOWED_ORG_UNIT_NAME_LENGTH,
  OU_NAME_EXISTS_WARNING,
  OU_NAME_INVALID_CHARS_EXISTS_WARNING,
  OU_NAME_TOO_LARGE_TEXT
} from "../../Constants";
import { stringContainsInvalidChars } from "utils/stringUtils";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";

interface OrgUnitNameProps {
  name: string;
  onValidStatusChange: (value: boolean) => void;
  onChange: (args: any) => void;
  label?: ReactNode;
  draftOrgUnit?: RestOrganizationUnit;
}
const OrgUnitNameComponent: React.FC<OrgUnitNameProps> = ({
  name,
  onChange,
  onValidStatusChange,
  label,
  draftOrgUnit
}) => {
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [nameChecking, setNameChecking] = useState(false);
  const [nameTooLarge, setNameTooLarge] = useState(name?.length > ALLOWED_ORG_UNIT_NAME_LENGTH);
  const [nameCheckId, setNameCheckId] = useState<undefined | string>();
  const [nameHasInvalidChars, setNameHasInvalidChars] = useState<boolean>(stringContainsInvalidChars(name));
  const location = useLocation();
  let { ou_workspace_id } = queryString.parse(location.search);

  const orgUnitListState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_filter_values",
    method: "list",
    uuid: nameCheckId
  });

  const dispatch = useDispatch();

  const isConfigReadonly = useHasConfigReadOnlyPermission();
  const oldReadOnly = getRBACPermission(PermeableMetrics.ORG_UNIT_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isConfigReadonly;

  useEffect(() => {
    if (nameChecking && nameCheckId) {
      const loading = get(orgUnitListState, ["loading"], true);
      const error = get(orgUnitListState, ["loading"], true);

      if (!loading && !error) {
        const data = get(orgUnitListState, ["data", "records", 0, "name", "records"], []).map((d: { key: string }) => ({
          name: d.key
        }));

        const checkName = checkTemplateNameExists((name ?? "").trim(), data);
        setNameChecking(false);
        setNameCheckId(undefined);
        setNameExist(checkName);
      }
    }
  }, [orgUnitListState, name]);

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

  const handleInputChange = (e: any) => {
    const name: string = e.target.value;
    onChange(name);
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
    onValidStatusChange(validName);
    return help;
  }, [nameChecking, nameExist, nameTooLarge, nameHasInvalidChars]);

  return (
    <Form.Item
      label={label ?? "Name"}
      required={!label}
      className="org-unit-name-container"
      colon={false}
      hasFeedback={name ? true : false}
      key={"org-unit-name"}
      validateStatus={getValidStatus}
      help={helpText}>
      <AntInput value={name} onChange={handleInputChange} disabled={isReadOnly} />
    </Form.Item>
  );
};

export default OrgUnitNameComponent;
