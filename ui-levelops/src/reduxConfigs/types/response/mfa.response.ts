export interface MFAResponse {
  mfa_required?: boolean;
  mfa_enrollment?: boolean;
  token?: string;
}
