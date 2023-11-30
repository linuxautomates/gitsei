import { SELF_ONBOARDING_INTEGRATION_FORM } from "configurations/pages/self-onboarding/constants";
import { get } from "lodash";
import { createSelector } from "reselect";
import { getFormStore } from "./formSelector";

export const selfOnBoardingFormSelector = createSelector(getFormStore, (data: any) => {
  return get(data, [SELF_ONBOARDING_INTEGRATION_FORM], {});
});
