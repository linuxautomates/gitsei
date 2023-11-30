import { forEach } from "lodash";
import { EICategoryTypes } from "dashboard/dashboard-types/BAReports.types";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { colorPickerColors } from "../constants/ticket-categorization.constants";

/** this creates a mapping between colors and it's current available state
 *  true => available
 *  false => not available
 *  intially all are available
 */
export const getProfileCategoriesColorMapping = () => {
  return colorPickerColors.reduce((acc: basicMappingType<boolean>, next: string) => {
    acc[next] = true;
    return acc;
  }, {});
};

/** this toggles those colors which are already used by the categories. */
export const getInitialColorMapping = (categories: EICategoryTypes[]) => {
  let colorMapping = getProfileCategoriesColorMapping();
  const categoriesColors = (categories ?? []).map(category => category?.color ?? "");
  forEach(categoriesColors, color => {
    if (colorMapping.hasOwnProperty(color)) {
      colorMapping[color] = false;
    }
  });

  return colorMapping;
};
