import { Entitlement, EntitlementCheckType, EntitlementTypes } from "custom-hooks/constants";

let StoredEntitlements: Array<string> = [];
export const setStoredEntitlements = (entitlements: Array<EntitlementTypes>) => (StoredEntitlements = entitlements);
export const getStoredEntitlements = () => StoredEntitlements;
export const hasStoredEntitlement = (entitlement: EntitlementTypes) => getStoredEntitlements().includes(entitlement);

export const checkEntitlements = (
  entitlementsState: Array<string>,
  entitlement: Entitlement | Array<Entitlement>,
  checkType: EntitlementCheckType = EntitlementCheckType.OR
) => {
  const entitlementList: Array<string> = typeof entitlement === "string" ? [entitlement] : entitlement;
  switch (checkType) {
    case EntitlementCheckType.OR: {
      if (entitlementsState.includes(Entitlement.ALL_FEATURES)) {
        return true;
      }
      return entitlementList.some((value: any) => {
        return entitlementsState.includes(value);
      });
    }
    case EntitlementCheckType.AND: {
      return entitlementList.every((value: any) => {
        return entitlementsState.includes(value);
      });
    }
    default:
      return false;
  }
};
