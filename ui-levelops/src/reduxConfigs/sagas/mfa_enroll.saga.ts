import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest } from "redux-saga/effects";
import { SESSION_MFA_ENROLL_GET, SESSION_MFA_ENROLL_POST } from "reduxConfigs/actions/actionTypes";
import { sessionMFAEnroll } from "reduxConfigs/actions/restapi/mfa.action";
import { MFAEnrollPostActionType } from "reduxConfigs/types/actions/mfa.action";
import { MFAEnrollService } from "services/restapi/mfa_enroll.service";

export const parse = (data: string) => {
  const separator = data.trim().split("\n")[0].trim();
  const parts = data
    .split(separator)
    .map(part => part.trim())
    .filter(part => part !== "" && part !== "--");

  return parts.map(part => {
    const arr = part.split("\n");
    let slice = 3;
    arr.forEach(part => {
      if (part.includes("qrcode")) {
        slice = 4;
      }
    });
    return arr.slice(slice, arr.length + 1).join();
  });
};

function* mfaEnrollGetEffectSaga() {
  try {
    yield put(sessionMFAEnroll("get", { loading: true }));
    const mfaService = new MFAEnrollService();
    const enrollment: { data: any } = yield call(mfaService.get);
    const data = parse(enrollment.data);
    const qrcode: string = `data:image/png;base64,${data[1]}`;
    const code = JSON.parse(data[0])?.secret || "";
    yield put(sessionMFAEnroll("get", { loading: false, error: false, code, qrcode }));
  } catch (e) {
    console.error("[MFA ENROLL GET ERROR]", e);

    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.MFA_ENROLL,
        data: { e }
      }
    });

    yield put(
      sessionMFAEnroll("get", { loading: false, error: (e as any)?.response?.data?.message || "SOMETHING WENT WRONG" })
    );
  }
}

function* mfaEnrollPostEffectSaga(action: MFAEnrollPostActionType) {
  try {
    yield put(sessionMFAEnroll("post", { loading: true }));
    const mfaService = new MFAEnrollService();
    yield call(mfaService.post, action.otp);
    yield put(sessionMFAEnroll("post", { loading: false, enrollment_success: true }));
  } catch (e) {
    console.error("[MFA ENROLL POST ERROR]", e);
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.MFA_ENROLL,
        data: { e, action }
      }
    });
    yield put(
      sessionMFAEnroll("post", { loading: false, error: (e as any)?.response?.data?.error || "SOMETHING WENT WRONG" })
    );
  }
}

export function* mfaEnrollGetWatcherSaga() {
  yield takeLatest([SESSION_MFA_ENROLL_GET], mfaEnrollGetEffectSaga);
}

export function* mfaEnrollPostWatcherSaga() {
  //@ts-ignore
  yield takeLatest([SESSION_MFA_ENROLL_POST], mfaEnrollPostEffectSaga);
}
