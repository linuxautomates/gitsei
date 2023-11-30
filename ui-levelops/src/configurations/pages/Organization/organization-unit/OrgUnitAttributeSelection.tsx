import React from "react";
import { filterFieldType, sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import OrgUnitIntegrationFilterField from "./OrgUnitIntegrationFilterField";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getOrgUnitUtility } from "reduxConfigs/selectors/OrganizationUnitSelectors";

interface OrgUnitAttributeSelectionProps {
  dynamicUsers?: sectionSelectedFilterType[];
  handleRemoveUserAttribute: (index: number) => void;
  handleUserAttributesChanges: (type: filterFieldType, index: number, value: any) => void;
}

const OrgUnitAttributeSelection: React.FC<OrgUnitAttributeSelectionProps> = ({
  dynamicUsers,
  handleRemoveUserAttribute,
  handleUserAttributesChanges
}) => {
  const utilityLoading = useParamSelector(getOrgUnitUtility, { utility: "loading" });
  const userAttributesOptions = useParamSelector(getOrgUnitUtility, { utility: "custom_attributes" });

  return (
    <>
      {(dynamicUsers || []).map((definition, index: number) => (
        <OrgUnitIntegrationFilterField
          apiLoading={utilityLoading}
          apiRecords={userAttributesOptions}
          index={index}
          field={definition}
          fieldLabel={"CONTRIBUTOR :"}
          handleRemoveFilter={handleRemoveUserAttribute}
          handleFieldChange={handleUserAttributesChanges}
        />
      ))}
    </>
  );
};

export default OrgUnitAttributeSelection;
