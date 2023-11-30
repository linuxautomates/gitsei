export type RestMFAEnrollGetState = {
  loading: boolean;
  error: boolean | string;
  code: string;
  qrcode: string;
};

export type RestMFAEnrollPostState = {
  loading: boolean;
  error: boolean | string;
  enrollment_success: boolean;
};

export type RestMFAEnrollState = {
  get: RestMFAEnrollGetState;
  post: RestMFAEnrollPostState;
};
