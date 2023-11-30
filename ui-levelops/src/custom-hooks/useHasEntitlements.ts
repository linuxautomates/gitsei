import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { userEntitlements } from "reduxConfigs/selectors/entitlements.selector";
import { Entitlement, EntitlementCheckType } from "./constants";
import { checkEntitlements } from "./helpers/entitlements.helper";

export function useHasEntitlements(
  entitlement: Entitlement | Array<Entitlement>,
  checkType: EntitlementCheckType = EntitlementCheckType.OR
) {
  const [hasEntitlement, setHasEntitlement] = useState(false);
  const entitlementsState = useSelector(userEntitlements);

  useEffect(() => {
    setHasEntitlement(checkEntitlements(entitlementsState, entitlement, checkType));
  }, [entitlementsState, entitlement, checkType]);
  return hasEntitlement;
}
