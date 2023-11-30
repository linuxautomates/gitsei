import { PermeableMetrics, PERMEABLE_METRICS_USERROLES_MAPPING } from "constants/userRolesPermission.constant";
import { USERROLES } from "routes/helper/constants";
import LocalStoreService from "services/localStoreService";

/** this function helps to determine whether a current user is given permission for a given metric or not */
export const getRBACPermission: (permissionMetric: PermeableMetrics) => boolean = (
  permissionMetric: PermeableMetrics
) => {
  const ls = new LocalStoreService();
  const rbac = ls.getUserRbac();
  const allowedRBACs = PERMEABLE_METRICS_USERROLES_MAPPING[permissionMetric];
  if (rbac) {
    return allowedRBACs.includes(rbac?.toLowerCase() as USERROLES);
  }
  return false;
};
