import * as actions from "../actionTypes";

export const signatureLogsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "signature_logs",
  function: "getSignatureLogs",
  method: "list"
});

export const signatureLogsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: "signature_logs",
  function: "getSignatureLog",
  method: "get"
});

export const signaturesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "signatures",
  function: "getSignatures",
  method: "list"
});
