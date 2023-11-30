import * as actions from "../actionTypes";

export interface APIFilterActionType {
  uri: string;
  method?: string;
  id: string;
  payload: Record<string, any>;
}

export const apiFilterAction = (data: APIFilterActionType) => ({
  type: actions.GET_API_FILTER_DATA,
  uri: data.uri,
  method: data.method ?? "list",
  payload: data.payload,
  id: data.id
});
