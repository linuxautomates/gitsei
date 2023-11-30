import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useAppStore } from "contexts/AppStoreContext";
import { useParentProvider } from "contexts/ParentProvider";

export const useViewCollectionPermission = () => {
  const { accountInfo, selectedProject } = useAppStore();
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};
  const {
    hooks: { usePermission }
  } = useParentProvider();
  const { identifier: accountIdentifier = "" } = accountInfo || {};
  const accesses = usePermission
    ? usePermission({
        resourceScope: {
          accountIdentifier,
          projectIdentifier,
          orgIdentifier
        },
        resource: {
          resourceType: ResourceType.SEI_COLLECTIONS
        },
        permissions: [PermissionIdentifier.VIEW_SEI_COLLECTIONS]
      })
    : [true];
  return accesses[0];
};
