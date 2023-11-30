import {
  GET_DEV_PROD_CENTRAL_PROFILE,
  GET_DEV_PROD_PROFILE_LIST,
  PUT_DEV_PROD_CENTRAL_PROFILE,
  PUT_DEV_PROD_PROFILE
} from "./actionTypes";

export const getDevProdParentProfileList = (payload: any, id: string) => ({
  type: GET_DEV_PROD_PROFILE_LIST,
  payload,
  id
});

export const putDevProdParentProfile = (payload: any, id: string, location: any) => ({
  type: PUT_DEV_PROD_PROFILE,
  payload,
  id,
  location
});

export const getDevProdCentralProfile = (payload: any, id: string) => ({
  type: GET_DEV_PROD_CENTRAL_PROFILE,
  payload,
  id
});

export const putDevProdCentralParentProfile = (payload: any, id: string) => ({
  type: PUT_DEV_PROD_CENTRAL_PROFILE,
  payload,
  id
});
