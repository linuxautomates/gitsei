import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useAppStore } from "contexts/AppStoreContext";
import { useParentProvider } from "contexts/ParentProvider";

export const useCollectionPermissions = () => {
  const { accountInfo, selectedProject } = useAppStore();
  const {
    hooks: { usePermission }
  } = useParentProvider();
  const { identifier: accountIdentifier = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};
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
        permissions: [
          PermissionIdentifier.CREATE_SEI_COLLECTIONS,
          PermissionIdentifier.EDIT_SEI_COLLECTIONS,
          PermissionIdentifier.DELETE_SEI_COLLECTIONS
        ]
      })
    : [true, true, true];
  return accesses;
};
