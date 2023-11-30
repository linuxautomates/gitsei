import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useAppStore } from "contexts/AppStoreContext";
import { useParentProvider } from "contexts/ParentProvider";

export const useConfigScreenPermissions = () => {
  const { accountInfo } = useAppStore();
  const {
    hooks: { usePermission }
  } = useParentProvider();
  const { identifier: accountIdentifier = "" } = accountInfo || {};
  const accesses = usePermission
    ? usePermission({
        resourceScope: {
          accountIdentifier
        },
        resource: {
          resourceType: ResourceType.SEI_CONFIGURATION_SETTINGS
        },
        permissions: [
          PermissionIdentifier.CREATE_SEI_CONFIGURATIONSETTINGS,
          PermissionIdentifier.EDIT_SEI_CONFIGURATIONSETTINGS,
          PermissionIdentifier.DELETE_SEI_CONFIGURATIONSETTINGS
        ]
      })
    : [true, true, true];
  return accesses;
};
