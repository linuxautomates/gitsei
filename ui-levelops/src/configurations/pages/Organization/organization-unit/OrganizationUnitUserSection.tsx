import React, { useCallback, useState, useEffect } from "react";
import { cloneDeep, forEach } from "lodash";
import { Divider, Icon } from "antd";
import {
  filterFieldType,
  OrgUnitSectionKeys,
  OUUserCreateOptionType,
  sectionSelectedFilterType
} from "configurations/configuration-types/OUTypes";
import "./OrganizationUserSection.styles.scss";
import OrgUnitAttributeSelection from "./OrgUnitAttributeSelection";
import OrgUnitManualSelection from "./OrgUnitManualSelection";
import { AntButton } from "shared-resources/components";

interface OrganizationUnitUserSectionProps {
  userSectionId?: string;
  fromIntegration: boolean;
  csv_users: any;
  users: string[];
  dynamic_user_definitions: sectionSelectedFilterType[];
  handleRemoveUserGroups?: () => void;
  handleUpdateParent: (key: OrgUnitSectionKeys, value: any) => void;
}

const OrganizationUnitUserSection: React.FC<OrganizationUnitUserSectionProps> = ({
  fromIntegration,
  csv_users,
  users,
  dynamic_user_definitions,
  userSectionId,
  handleRemoveUserGroups,
  handleUpdateParent
}) => {
  const [userSelectionType, setUserSelectionType] = useState<OUUserCreateOptionType | undefined>(undefined);
  const [manualSelectionModalShow, setManualSelectionModalShow] = useState<boolean>(false);

  const handleChangeShowModal = (value: boolean) => {
    setManualSelectionModalShow(value);
  };

  useEffect(() => {
    if (dynamic_user_definitions.length > 0 && !userSelectionType) {
      setUserSelectionType("user_attribute");
    }
    if (users.length > 0 && !userSelectionType) {
      setUserSelectionType("manual");
    }
  }, [dynamic_user_definitions, users]);

  const handleChangeUserSelection = (value: OUUserCreateOptionType | undefined) => {
    switch (value) {
      case "user_attribute":
        const dummyUser: sectionSelectedFilterType = { key: "", value: "", param: "" };
        const dynamicDefinations = cloneDeep(dynamic_user_definitions || []);
        dynamicDefinations.push(dummyUser);
        setUserSelectionType(value);
        handleUpdateParent("dynamic_user_definition", dynamicDefinations);
        break;
      case "manual":
        setManualSelectionModalShow(true);
        setUserSelectionType(value);
      default:
        setUserSelectionType(value);
    }
  };

  const handleUserAttributesChanges = useCallback(
    (type: filterFieldType, index: number, value: any) => {
      const dynamicDefinations = cloneDeep(dynamic_user_definitions || []);
      if (dynamicDefinations.length) {
        let nField: sectionSelectedFilterType = cloneDeep(dynamicDefinations[index]);
        (nField as any)[type] = value;
        if (type === "key") {
          nField.param = "";
          nField.value = "";
        }
        if (type === "param") {
          nField.value = "";
        }
        dynamicDefinations[index] = nField;
        handleUpdateParent("dynamic_user_definition", dynamicDefinations);
      }
    },
    [dynamic_user_definitions, handleUpdateParent]
  );

  const handleRemoveUserAttribute = useCallback(
    (index: number) => {
      let nDynamicDefinition: sectionSelectedFilterType[] = [];
      forEach(dynamic_user_definitions, (filter, idx) => {
        if (idx !== index) {
          nDynamicDefinition.push(filter);
        }
      });
      if (nDynamicDefinition.length === 0) {
        handleRemoveUserGroups && handleRemoveUserGroups();
        setUserSelectionType(undefined);
      }
      handleUpdateParent("dynamic_user_definition", nDynamicDefinition);
    },
    [dynamic_user_definitions, userSelectionType]
  );

  return (
    <div className="ou-user-section" key={userSectionId || "0"}>
      {!fromIntegration && (
        <>
          <p className="ou-user-section-title">Contributors</p>
          <Divider />
          <p>
            These user selections apply <b>only</b> to integrations <b>not</b> specified above.
          </p>
        </>
      )}
      {!userSelectionType && !fromIntegration && <p>INCLUDE: All Contributors</p>}
      <OrgUnitAttributeSelection
        dynamicUsers={dynamic_user_definitions || []}
        handleRemoveUserAttribute={handleRemoveUserAttribute}
        handleUserAttributesChanges={handleUserAttributesChanges}
      />
      <OrgUnitManualSelection
        users={users}
        csvUsers={csv_users}
        userSelection={userSelectionType}
        handleVisibilityOfSelectionModal={handleChangeShowModal}
        manualSelectionModalShow={manualSelectionModalShow}
        handleUserGroupRemove={handleRemoveUserGroups}
        handleUpdateGrandParent={handleUpdateParent}
        handleChangeUserSelection={handleChangeUserSelection}
      />
      <div className="user-action-controller">
        <p className="user-add-action">
          {!["manual", "import_csv"].includes(userSelectionType as string) && (
            <span>
              {" "}
              <Icon type="plus-circle" />
              {`Add contributor based on${" "}`}
            </span>
          )}
          {(!userSelectionType || userSelectionType === "user_attribute") && (
            <span className="user-create-option" onClick={(e: any) => handleChangeUserSelection("user_attribute")}>
              Contributor Attribute
            </span>
          )}
          {!userSelectionType && `or${" "}`}
          {!userSelectionType && (
            <span className="user-create-option" onClick={(e: any) => handleChangeUserSelection("manual")}>
              Select Manually
            </span>
          )}
          {!userSelectionType && `or${" "}`}
          {!userSelectionType && (
            <span className="user-create-option" onClick={(e: any) => handleChangeUserSelection("import_csv")}>
              Import from CSV
            </span>
          )}
        </p>
        {fromIntegration && !userSelectionType && (
          <AntButton onClick={(e: any) => handleRemoveUserAttribute(-1)}>
            <Icon type="delete" />
          </AntButton>
        )}
      </div>
    </div>
  );
};

export default OrganizationUnitUserSection;
