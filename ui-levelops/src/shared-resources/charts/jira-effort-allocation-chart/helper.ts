import { forEach } from "lodash";
import { newUXColorMapping } from "../chart-themes";

export const NO_SPRINT_DATA = "NO DATA";

// dynamic generating mapping of colors
export const getColorMapping = (ids: string[], defaultColor?: string[]) => {
  const colors = defaultColor ?? ["#8173d6", "#02b5c4", "#98c263", "#ffcb73", "#fb7baf", "#8BBAD9", "#DADDDA"];
  let mapping: any = {};
  forEach(ids, (id: string, index: number) => {
    mapping = {
      ...mapping,
      [id]: id !== NO_SPRINT_DATA ? colors[index] : newUXColorMapping["light_grey"]
    };
  });
  return mapping;
};

// for creating initial category switch state
export const initialDependencySwitchState = (dependencyIds: string[]) => {
  let initialDependencyState: any = {};
  forEach(dependencyIds, id => {
    initialDependencyState = {
      ...initialDependencyState,
      [id]: true
    };
  });
  return initialDependencyState;
};
