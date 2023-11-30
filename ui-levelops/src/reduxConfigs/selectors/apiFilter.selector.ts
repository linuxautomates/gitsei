import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getURI = createParameterSelector((params: any) => params.uri);
const getMethod = createParameterSelector((params: any) => params.method);
const getId = createParameterSelector((params: any) => params.id);

export const apiFilterSelector = createSelector(
  restapiState,
  getURI,
  getMethod,
  getId,
  (state: any, uri: string, method: string, id: string = "0") => get(state, [uri, method, id], {})
);
