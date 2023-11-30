import React, { useCallback, useMemo } from "react";
import DropdownWithTagSelectionComponent from "shared-resources/components/dropdown-with-tag-select/DropdownWIthTagSelection";
import { optionType } from "dashboard/dashboard-types/common-types";
import CustomDropdownWithTagSelect from "shared-resources/components/custom-dropdown-with-tag-select/CustomDropdownWithTagSelect";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { useSelector } from "react-redux";

interface OUSelectorDropdownContainerProps {
  showDropdown: boolean;
  dropdownValue: string[];
  onFilerValueChange: (value: any, key: string) => void;
}

export const OU_DASHBOARD_LIST_ID = "ou_dashboard_list_id";

const OUSelectorDropdownContainer: React.FC<OUSelectorDropdownContainerProps> = ({
  showDropdown,
  dropdownValue,
  onFilerValueChange
}) => {
  const isTrialUser = useSelector(isSelfOnboardingUser);

  const handleFilterChange = useCallback(
    (selectedValue: optionType) => {
      onFilerValueChange(selectedValue?.value ? [selectedValue?.value] : [], "ou_ids");
    },
    [onFilerValueChange]
  );

  const getValueForOUSelector = useMemo(() => {
    if (!dropdownValue?.length) return "";
    return dropdownValue[0];
  }, [dropdownValue]);

  const OULabel = useMemo(() => {
    if (isTrialUser) return "";
    else return "Collection";
  }, []);

  return (
    <CustomDropdownWithTagSelect
      label={OULabel}
      showDropdown={showDropdown}
      dropdownValue={getValueForOUSelector}
      onFilerValueChange={handleFilterChange}
      dropdownConfig={{
        uri: "organization_unit_management",
        placeholder: "Select Collection...",
        searchField: "name",
        showSearch: true,
        uuid: OU_DASHBOARD_LIST_ID,
        allowClear: true,
        showSpinnerWhenLoading: true
      }}
    />
  );
};

export default OUSelectorDropdownContainer;
