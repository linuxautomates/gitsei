import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const SLA_MODULE_DATA = "sla_module_data";

const getMethod = createParameterSelector((params: any) => params.method);
const getId = createParameterSelector((params: any) => params.id);
const getUri = createParameterSelector((params: any) => params.uri);

export const slaParamSelector = createSelector(
  restapiState,
  getUri,
  getMethod,
  getId,
  (data: any, uri: string, method: any, id: any) => {
    return get(data, [uri, method, id], {});
  }
);
