import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useAppStore } from "contexts/AppStoreContext";
import { useParentProvider } from "contexts/ParentProvider";

export const useDashboardPermissions = () => {
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
          resourceType: ResourceType.SEI_INSIGHTS
        },
        permissions: [
          PermissionIdentifier.CREATE_SEI_INSIGHTS,
          PermissionIdentifier.EDIT_SEI_INSIGHTS,
          PermissionIdentifier.DELETE_SEI_INSIGHTS
        ]
      })
    : [true, true, true];
  return accesses;
};
