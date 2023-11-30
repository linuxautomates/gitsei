export const MFA_SETTINGS = "MFA Settings :";
export const ENROLLMENT_PERIOD_TEXT = "Enrollment Period";
export const ENROLLMENT_PERIOD_DESCRIPTION =
  "Enrollment period allows users without MFA to configure\n MFA after login. Once the enrollment period expires users\n without MFA will be locked out. SEI admin can \nreset enrollment window for individual users on User \n Profile page.";
export const enrollmentPeriodOptions = [
  { label: "24 Hours", value: 1 },
  { label: "48 hours", value: 2 },
  { label: "3 days", value: 3 },
  { label: "A week", value: 7 }
];
export const SEND_EMAIL_TEXT = "Send email notification to users who haven't yet configured MFA";

export const scmGlobalCodeChangeSizeUnits = [
  { label: "Lines Of Code", value: "lines" },
  { label: "Files", value: "files" }
];
