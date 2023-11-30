import { trellisProfileActions } from "../actionTypes";

const uri: string = "dev_productivity_profile";

export const trellisProfilesListReadAction = (filters: any = {}, forceReload = true) => ({
  type: trellisProfileActions.GET_TRELLIS_PROFILES_LIST,
  filters,
  forceReload
});

export const trellisProfilesListLoadSuccessfullAction = (data: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILES_LIST_LOAD_SUCCESSFUL,
  data
});

export const trellisProfilesListLoadFailedAction = (error: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILES_LIST_LOAD_FAILED,
  error
});

export const trellisProfileListAlreadyPresentAction = () => ({
  type: trellisProfileActions.REUSE_EXISTING_TRELLIS_PROFILE_LIST
});

export const trellisProfileRequireIntegrationAction = () => ({ type: trellisProfileActions.SET_NO_INTEGRATIONS });

export const trellisProfilesUpdateAction = (id: string, data: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_UPDATE,
  id,
  data
});

export const trellisProfilesPartialUpdateAction = (id: string, data: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_PARTIAL_UPDATE,
  id,
  data
});

export const getTrellisProfileAction = (id: string) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_READ,
  id
});

export const trellisProfileLoadSuccessfulAction = (data: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_LOAD_SUCCESSFUL,
  data
});

export const trellisProfileLoadFailedAction = (error: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_LOAD_FAILED,
  error
});

export const createTrellisProfileAction = (item: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_CREATE,
  data: item
});

export const saveTrellisProfileSuccessfulAction = () => ({
  type: trellisProfileActions.TRELLIS_PROFILE_SAVE_SUCCESSFUL
});

export const saveTrellisProfileFailedAction = (error: any) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_SAVE_FAILED,
  error
});

export const trellisProfileClone = (cloneId: string) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_CLONE,
  uri,
  id: cloneId
});

export const deleteTrellisProfile = (id: string) => ({
  type: trellisProfileActions.TRELLIS_PROFILE_DELETE,
  uri,
  id
});

export const clearSavedTrellisProfile = () => ({ type: trellisProfileActions.TRELLIS_PROFILE_CLEAR });

export const getWorkspaceOUList = (workspaceId: string) => ({
  type: trellisProfileActions.GET_WORKSPACE_OU_LIST,
  workspaceId
});

export const saveWorkspaceOUList = (workspaceId: string, orgList: Array<any>) => ({
  type: trellisProfileActions.SAVE_WORKSPACE_OU_LIST,
  workspaceId,
  orgList
});

export const trellisProfileOuAssociationAction = (profileId: string, orgId: string, orgName: string) => ({
  type: trellisProfileActions.ASSOCIATE_OU_TO_PROFILE,
  profileId,
  orgId,
  orgName
});
