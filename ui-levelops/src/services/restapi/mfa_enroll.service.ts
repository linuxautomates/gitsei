import { MFA_ENROLL } from "constants/restUri";
import BackendService from "services/backendService";

export class MFAEnrollService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.post = this.post.bind(this);
  }

  get() {
    return this.restInstance.get(MFA_ENROLL, this.options);
  }

  post(otp: string) {
    const options = {
      ...this.options,
      headers: { ...(this.options as any)?.["headers"], "Content-Type": "application/json" }
    };
    return this.restInstance.post(MFA_ENROLL, { otp }, options);
  }
}
