import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useAppStore } from "contexts/AppStoreContext";
import { useParentProvider } from "contexts/ParentProvider";

export const useHasViewConfigPermission = () => {
  const { accountInfo } = useAppStore();
  const {
    hooks: { usePermission }
  } = useParentProvider();
  const { identifier: accountIdentifier = "" } = accountInfo || {};
  const [hasViewAccess] = usePermission
    ? usePermission({
        resourceScope: {
          accountIdentifier
        },
        resource: {
          resourceType: ResourceType.SEI_CONFIGURATION_SETTINGS
        },
        permissions: [PermissionIdentifier.VIEW_SEI_COLLECTIONS]
      })
    : [false];
  return hasViewAccess;
};
