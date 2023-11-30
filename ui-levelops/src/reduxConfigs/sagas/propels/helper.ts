import { cloneDeep, get, set } from "lodash";
import { ProjectToWorkspaceMappingKeys, TriggerFieldType, TriggerType } from "../saga-types/propels.types";
import { PROJECT_WORKSPACE_MAPPING } from "./constant";

export const getModifiedTrigger = (trigger: TriggerType) => {
  const nTrigger = cloneDeep(trigger);
  const nfields = cloneDeep(trigger.fields);

  if (Object.keys(nfields).length) {
    const productsConfig: TriggerFieldType = get(nfields, ["products"]);
    if (productsConfig) {
      const nProductsConfig = cloneDeep(productsConfig);
      nProductsConfig.display_name = PROJECT_WORKSPACE_MAPPING[ProjectToWorkspaceMappingKeys.NAME];
      nProductsConfig.description = PROJECT_WORKSPACE_MAPPING[ProjectToWorkspaceMappingKeys.DESCRIPTION];
      nProductsConfig.dynamic_resource_name = PROJECT_WORKSPACE_MAPPING[ProjectToWorkspaceMappingKeys.URI];
      set(nfields, ["products"], nProductsConfig);
    }
  }

  nTrigger.fields = nfields;
  return nTrigger;
};
