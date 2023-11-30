import { Form } from "antd";
import { ERROR, SUCCESS } from "constants/formWarnings";
import { get } from "lodash";
import React, { ReactNode, useMemo, useState } from "react";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntInput } from "shared-resources/components";
import { PIVOT_NAME_EXIST_WARNING, PIVOT_NAME_TOO_LARGE } from "./constant";
import { ALLOWED_ORG_UNIT_NAME_LENGTH, PIVOT_LIST_ID } from "configurations/pages/Organization/Constants";
import { PivotType } from "configurations/configuration-types/OUTypes";

interface OrgUnitCategoryNameProps {
  name: string;
  onValidStatusChange: (value: boolean) => void;
  onChange: (args: any) => void;
  label?: ReactNode;
}

const OrgUnitCategoryNameComponent: React.FC<OrgUnitCategoryNameProps> = ({
  name,
  onChange,
  onValidStatusChange,
  label
}) => {
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [nameTooLarge, setNameTooLarge] = useState(name?.length > ALLOWED_ORG_UNIT_NAME_LENGTH);

  const pivotsListState = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots_list",
    method: "list",
    uuid: PIVOT_LIST_ID
  });

  const pivots: Array<PivotType> = useMemo(() => get(pivotsListState, ["data", "records"], []), [pivotsListState]);

  const checkName = (name: string) => {
    const ouCategory: PivotType | undefined = pivots.find(
      category => category?.name?.toLowerCase() === name?.toLowerCase()
    );
    setNameExist(!!ouCategory);
  };

  const handleInputChange = (e: any) => {
    onChange(e.target.value);
    const name: string = (e.target.value ?? "").trim();
    if (name?.length <= ALLOWED_ORG_UNIT_NAME_LENGTH) {
      if (nameTooLarge) {
        setNameTooLarge(false);
      }
      checkName(name);
    } else {
      setNameTooLarge(true);
    }
  };

  const getValidStatus = useMemo(() => {
    if (nameTooLarge || nameExist) return ERROR;

    if (!nameExist) {
      return SUCCESS;
    }
  }, [nameExist, nameTooLarge]);

  const helpText = useMemo(() => {
    let help = nameExist ? PIVOT_NAME_EXIST_WARNING : nameTooLarge ? PIVOT_NAME_TOO_LARGE : "";

    let validName = true;
    if (help) {
      validName = false;
    }
    onValidStatusChange(validName);
    return help;
  }, [nameExist, nameTooLarge]);

  return (
    <Form.Item
      label={label ?? "Name"}
      required={!label}
      className="ou-category-name-container"
      colon={false}
      hasFeedback={name ? true : false}
      key={"org-unit-name"}
      validateStatus={getValidStatus}
      help={helpText}>
      <AntInput value={name} onChange={handleInputChange} />
    </Form.Item>
  );
};

export default OrgUnitCategoryNameComponent;
