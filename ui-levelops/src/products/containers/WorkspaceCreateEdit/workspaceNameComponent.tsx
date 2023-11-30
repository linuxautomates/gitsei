import { Form } from "antd";
import { checkTemplateNameExists } from "configurations/helpers/checkTemplateNameExits";
import { VALIDATING, ERROR, SUCCESS } from "constants/formWarnings";
import { debounce, get } from "lodash";
import React, { ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { workspaceRead } from "reduxConfigs/actions/workspaceActions";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericWorkSpaceUUIDSelector } from "reduxConfigs/selectors/workspace/workspace.selector";
import { AntInput } from "shared-resources/components";

interface WorkspaceNameProps {
  name: string;
  onChange: (args: any) => void;
  label?: ReactNode;
  nameExist: boolean | undefined;
  setNameExist: (value: any) => void;
  isNameValid: boolean;
  setIsNameValid: (value: boolean) => void;
  nameFieldRef: any;
}
const WorkspaceNameComponent: React.FC<WorkspaceNameProps> = ({
  name,
  onChange,
  label,
  nameExist,
  setNameExist,
  isNameValid,
  setIsNameValid,
  nameFieldRef
}) => {
  const [nameChecking, setNameChecking] = useState(false);
  const workspaceNameState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "list",
    uuid: "name_exist"
  });

  const dispatch = useDispatch();

  useEffect(() => {
    if (nameChecking) {
      const loading = get(workspaceNameState, ["loading"], true);
      const error = get(workspaceNameState, ["loading"], true);

      if (!loading && !error) {
        const data = get(workspaceNameState, ["data", "records"], []).map((d: { name: string }) => ({
          name: d.name
        }));
        const checkName = checkTemplateNameExists(name?.trim() ?? "", data);
        if (checkName && nameFieldRef.current && name?.trim()?.toLowerCase() === nameFieldRef.current?.toLowerCase()) {
          setNameExist(false);
        } else {
          setNameExist(checkName);
        }
        setNameChecking(false);
      }
    }
  }, [workspaceNameState, name]);

  const checkName = (name: string) => {
    const filter = {
      filter: {
        partial: {
          name: name?.trim()
        }
      }
    };
    dispatch(workspaceRead("name_exist", "list", filter));
    setNameChecking(true);
  };

  const checkDebouncedName = useCallback(debounce(checkName, 500), []);

  const handleInputChange = (e: any) => {
    setIsNameValid(true);
    let validRegexNameForWorkspaceName = new RegExp(/^[a-zA-Z0-9_-]*[a-zA-Z0-9_ -]*[a-zA-Z0-9_-]$/);
    onChange(e.target.value);
    if (!validRegexNameForWorkspaceName.test(e.target.value)) {
      setIsNameValid(false);
    }
    if (isNameValid) {
      checkDebouncedName(e.target.value);
    }
  };

  const getValidStatus = useMemo(() => {
    if (nameChecking) {
      return VALIDATING;
    }
    if (!isNameValid || (!nameChecking && nameExist)) {
      return ERROR;
    }
    if (!nameChecking && !nameExist) {
      return SUCCESS;
    }
  }, [nameExist, nameChecking, isNameValid]);

  const helpText = useMemo(() => {
    let help =
      (!isNameValid ? "Project name must contain only characters 'a-z A-Z 0-9 -_' or space." : "") ||
      (nameExist && !nameChecking ? "A project with this name already exists." : "");
    let validName = true;
    if (help) {
      validName = false;
    }
    return help;
  }, [nameChecking, nameExist, isNameValid]);

  return (
    <Form.Item
      label={label ?? "Name"}
      required={true}
      colon={false}
      hasFeedback={name ? true : false}
      key={"workspace-name"}
      validateStatus={getValidStatus}
      help={helpText}>
      <AntInput value={name} onChange={handleInputChange} />
    </Form.Item>
  );
};

export default WorkspaceNameComponent;
