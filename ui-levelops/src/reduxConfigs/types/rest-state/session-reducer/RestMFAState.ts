export type RestMFAState = {
  loading: boolean;
  error: boolean | string;
  mfa_required: boolean;
  mfa_enrollment: boolean;
};
