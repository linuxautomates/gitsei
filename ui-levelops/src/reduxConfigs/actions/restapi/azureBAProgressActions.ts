import { AZURE_PROGRESS_REPORT, AZURE_PROGRAM_PROGRESS_REPORT } from "../actionTypes";

export const azureBAProgressReport = (uri: string, method: string, filters: any, id = "0", extra?: any) => ({
  type: AZURE_PROGRESS_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra
});

export const azureBAProgramProgressReport = (uri: string, method: string, filters: any, id = "0", extra?: any) => ({
  type: AZURE_PROGRAM_PROGRESS_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra
});
