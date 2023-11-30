import React, { useCallback } from "react";
import queryParser from "query-string";
import DropdownWithTagSelectionComponent from "shared-resources/components/dropdown-with-tag-select/DropdownWIthTagSelection";
import { optionType } from "dashboard/dashboard-types/common-types";
import { useHistory, useLocation } from "react-router-dom";

const ENGINEER_SELECT_ID = "ENGINEER_SELECT_ID";

interface EngineerDropdownContainerProps {
  onFilterValueChange: (value: any, key: string) => void;
}

const EngineerDropdownContainer: React.FC<EngineerDropdownContainerProps> = ({ onFilterValueChange }) => {
  const location = useLocation();
  const history = useHistory();
  const parsed = queryParser.parse(location.search);

  const handleFilterChange = useCallback(
    (selectedValue: optionType) => {
      history.push({
        search:
          "?" +
          new URLSearchParams({
            ...(parsed || {}),
            user_id: selectedValue.value,
            user_id_type: "ou_user_id"
          }).toString()
      });
    },
    [onFilterValueChange]
  );

  return (
    <DropdownWithTagSelectionComponent
      label="Engineer :"
      showDropdown={true}
      dropdownValue={parsed?.user_id_type === "integration_user_ids" ? undefined : parsed?.user_id}
      onFilerValueChange={handleFilterChange}
      dropdownConfig={{
        uri: "org_users",
        placeholder: "Select Engineer...",
        searchField: "full_name",
        mode: "tags",
        showSearch: false,
        specialKey: "org_uuid",
        filterOptionMethod: value => !!value.org_uuid,
        uuid: ENGINEER_SELECT_ID,
        allowClear: false,
        showSpinnerWhenLoading: true,
        dropdownClassName: "org-unit-dropdown"
      }}
    />
  );
};

export default EngineerDropdownContainer;
