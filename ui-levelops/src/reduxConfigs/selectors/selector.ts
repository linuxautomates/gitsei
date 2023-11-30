import { useSelector } from "react-redux";

export function useParamSelector(selector: any, ...params: any) {
  return useSelector((state: any) => selector(state, ...params));
}

export function createParameterSelector(selector: any) {
  return (_: any, params: any) => selector(params);
}
