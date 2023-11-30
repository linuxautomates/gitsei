import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { getIsStandaloneApp } from "helper/helper";

export const getIsDisabled = (
  item: any,
  disabled?: boolean,
  reverseState?: orgUnitJSONType[],
  allowedOUs?: string[]
) => {
  let isDisabled = true;
  if (reverseState) {
    const dashboardOU = reverseState?.find((obj: any) => obj.id === item.key);
    if ((disabled !== undefined && disabled !== false) || dashboardOU) {
      isDisabled = dashboardOU ? false : true;
    } else if (disabled === false) {
      isDisabled = false;
    }
  } else {
    isDisabled = false;
  }

  if (getIsStandaloneApp()) {
    if (allowedOUs?.length && !allowedOUs.includes(item.id)) isDisabled = true;
  } else {
    isDisabled = isDisabled || !item.access_response?.view;
  }
  return isDisabled;
};
