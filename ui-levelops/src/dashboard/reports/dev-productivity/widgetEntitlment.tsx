import { useEffect, useState } from "react";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";

export function widgetEntitlement() {
  const TRELLIS_BY_JOB_ROLES = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);
  return { TRELLIS_BY_JOB_ROLES };
}
