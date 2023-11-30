import { get } from "lodash";
import { TrellisProfileState } from "reduxConfigs/reducers/trellisProfileReducer";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

const trellisProfileSelector = (state: any) => state.trellisProfileReducer;
const getWorkspaceId = createParameterSelector((params: any) => params.workspaceId);
const getOuId = createParameterSelector((params: any) => params.currentOrgUnit);

export const trellisProfileListSelector = createSelector(
  trellisProfileSelector,
  (data: TrellisProfileState) => data.profiles
);

export const trellisProfileDetailsSelector = createSelector(
  trellisProfileSelector,
  (data: TrellisProfileState) => data.selectedProfile
);

export const trellisProfileSavingStatusSelector = createSelector(
  trellisProfileSelector,
  (data: TrellisProfileState) => data.savingStatus
);

export const getOUsForWorkspace = createSelector(
  trellisProfileSelector,
  getWorkspaceId,
  (data: TrellisProfileState, workspaceId: string) => get(data.workspaceOUList, [workspaceId], { isloading: true })
);

export const getOUsForWorkspaceExCludeOU = createSelector(
  trellisProfileSelector,
  getWorkspaceId,
  getOuId,
  (data: TrellisProfileState, workspaceId: string, currentOrgUnit: string) => {
    const updatedData: any = get(data.workspaceOUList, [workspaceId], { isloading: true });
    if (updatedData?.OUlist?.length) {
      updatedData.OUlist = updatedData?.OUlist?.filter((org: any) => org?.id !== currentOrgUnit);
      return updatedData;
    } else {
      return updatedData;
    }
  }
);
