export interface SigninStatusType {
  token?: string;
  email?: string;
  company?: string;
  signinError?: string;
  isSSOEnabled?: boolean;
  isMultiTenant?: boolean;
  isValidEmail?: boolean;
  first_name?: string;
  isMFARequired?: boolean;
  password?: string;
  isMFAEnrollmentRequired?: boolean;
  troublingInSignIn?: boolean;
  firstTimeSSO?: boolean;
}
