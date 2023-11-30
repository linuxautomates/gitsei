export enum SIGNIN_STEPS {
  EMAIL = "email",
  COMPANY = "company",
  PASSWORD = "password",
  MFA = "mfa",
  MFA_ENROLLMENT = "mfa_enrollment"
}

export const SSO_ERROR_DESCRIPTIONS = {
  sso_not_configured:
    "Your SEI administrator has not configured Single Sign-On (SSO). Contact your administrator for assistance.",
  invalid_saml_response:
    "The response from your Single Sign-On provider did not return a valid user. Please contact SEI Support for assistance.",
  unsuccessful_saml_response:
    "Your Single Sign-On provider could not authenticate your credentials successfully. Please check your SSO configuration.",
  sso_disabled_for_user:
    "You do not have permission to use Single Sign-On. Contact your SEI administrator to obtain permission.",
  unparsable_saml_response:
    "The response from your Single Sign-On provider could not be processed. Please contact SEI Support for assistance.",
  missing_company: "The company name is invalid or missing. Try again with a valid company name."
};

export const FIRST_TIME_SSO =
  "Since you are signing in for the first time, please enter your company name. We won't ask you this again.";
