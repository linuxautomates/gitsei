import {
  SESSION_MFA,
  SESSION_MFA_CLEAR,
  SESSION_MFA_ENROLL,
  SESSION_MFA_ENROLL_CLEAR,
  SESSION_MFA_ENROLL_GET,
  SESSION_MFA_ENROLL_POST,
  SESSION_MFA_GET
} from "reduxConfigs/actions/actionTypes";

export const sessionMFA = (data: { loading?: boolean; error?: any; mfa_required?: boolean }) => ({
  type: SESSION_MFA,
  payload: data
});

export const sessionMFAClear = () => ({
  type: SESSION_MFA_CLEAR
});

export const sessionGetMFA = (data: { company: string; email: string; password: string }) => ({
  type: SESSION_MFA_GET,
  ...data
});

export const sessionMFAEnroll = (
  method: "get" | "post",
  data: { loading?: boolean; error?: any; code?: string; qrcode?: string; enrollment_success?: boolean }
) => ({
  type: SESSION_MFA_ENROLL,
  method,
  payload: data
});

export const sessionMFAEnrollGet = () => ({
  type: SESSION_MFA_ENROLL_GET
});

export const sessionMFAEnrollPost = (data: { otp: string }) => ({
  type: SESSION_MFA_ENROLL_POST,
  ...data
});

export const sessionMFAEnrollClear = (method?: "get" | "post") => ({
  type: SESSION_MFA_ENROLL_CLEAR,
  method
});
