import { RESTAPI_READ } from "../actionTypes";

const uri = "report_docs";

export const getReportDocs = (id: string) => ({
  type: RESTAPI_READ,
  id,
  method: "get",
  uri
});

export const listReportDocs = (
  filters: { filter: { applications?: string[]; categories?: string[] } },
  complete: string | null = null,
  id = "0"
) => ({
  type: RESTAPI_READ,
  data: filters,
  method: "list",
  uri,
  complete,
  id
});
