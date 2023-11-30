import { USERLANDINGPAGE, USERROLESUPPER } from "routes/helper/constants";

export type UserWorkspaceSelectionType = {
  [x: string]: {
    workspaceId: string;
    orgUnitIds: string[];
  };
};
export interface MeResponse {
  application_restrictions?: Array<string>;
  id: string;
  first_name: string;
  last_name: string;
  user_type: USERROLESUPPER;
  mfa_enabled: boolean; // <-- new field - the user has successfully enrolled in MFA
  mfa_enforced: boolean;
  managed_ou_ref_ids?: string[];
  mfa_enrollment_end?: number; // <-- new field - datetime untill when the enrollment window will be available (the user can enroll in MFA during login time) - if not present, the global window will apply
  mfa_reset_at?: number; // <-- new field - datetime at which the MFA access was reset
  saml_auth_enabled: boolean;
  password_auth_enabled: boolean;
  email: string;
  landing_page: USERLANDINGPAGE;
  license: string;
  entitlements: Array<string>;
  scopes?: Record<string, any>;
  workspaces?: UserWorkspaceSelectionType;
}
