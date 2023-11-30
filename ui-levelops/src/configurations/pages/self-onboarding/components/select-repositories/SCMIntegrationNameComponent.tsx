import { Form } from "antd";
import { checkTemplateNameExists } from "configurations/helpers/checkTemplateNameExits";
import { VALIDATING, ERROR, SUCCESS } from "constants/formWarnings";
import { debounce, get, uniqueId } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { integrationsList } from "reduxConfigs/actions/restapi";
import { integrationListSelector } from "reduxConfigs/selectors/integrationSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntInput } from "shared-resources/components";

interface IntegrationNameProps {
  name: string;
  setNameStatus: (args: any) => void;
  onChange: (args: any) => void;
}
const IntegrationNameComponent: React.FC<IntegrationNameProps> = ({ name, onChange, setNameStatus }) => {
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [nameChecking, setNameChecking] = useState(false);
  const [nameCheckId, setNameCheckId] = useState<undefined | string>();

  const integrationListState = useParamSelector(integrationListSelector, {
    integration_key: nameCheckId
  });

  const dispatch = useDispatch();

  useEffect(() => {
    if (nameChecking && nameCheckId) {
      const loading = get(integrationListState, ["loading"], true);
      const error = get(integrationListState, ["loading"], true);

      if (!loading && !error) {
        const data = get(integrationListState, ["data", "records"], []);
        const checkName = checkTemplateNameExists(name, data);
        setNameChecking(false);
        setNameCheckId(undefined);
        setNameExist(checkName);
      }
    }
  }, [integrationListState]);

  const checkName = (name: string) => {
    const filter = {
      partial: {
        name
      }
    };
    const id = uniqueId();
    dispatch(integrationsList({ filter }, null, id));
    setNameCheckId(id);
    setNameChecking(true);
  };
  const checkDebouncedName = useCallback(debounce(checkName, 500), []);

  const handleInputChange = (e: any) => {
    onChange(e.target.value);
    checkDebouncedName(e.target.value);
  };

  const getValidStatus = useMemo(() => {
    if (nameChecking) {
      return VALIDATING;
    }
    if (!nameChecking && nameExist) {
      return ERROR;
    }
    if (!nameChecking && !nameExist) {
      return SUCCESS;
    }
  }, [nameExist, nameChecking]);

  const renderIntegrationNameLabel = useMemo(
    () => (
      <p className="select-repo-title">
        Integration Name
        <span style={{ color: "red", fontSize: "18px" }} className="ml-5">
          *
        </span>
      </p>
    ),
    []
  );

  const helpText = useMemo(() => {
    let help = nameExist && !nameChecking ? "This integration name already exist" : "";
    let validName = true;
    if (help) {
      validName = false;
    }
    setNameStatus(validName);
    return help;
  }, [nameChecking, nameExist]);

  return (
    <Form.Item
      label={renderIntegrationNameLabel}
      className="integration-name-container"
      colon={false}
      hasFeedback={name ? true : false}
      key={"integration_name"}
      validateStatus={getValidStatus}
      help={helpText}>
      <AntInput value={name} onChange={handleInputChange} />
    </Form.Item>
  );
};

export default IntegrationNameComponent;
