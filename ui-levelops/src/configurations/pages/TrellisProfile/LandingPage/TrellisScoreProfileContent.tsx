import React, { useCallback, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import { Empty } from "antd";
import { WebRoutes } from "routes/WebRoutes";
import { ProfilesPaginatedTable } from "shared-resources/containers";
import {
  actionColumn,
  categoriesColumn,
  descriptionColumn,
  nameColumn,
  PROFILE_ACTIONS,
  modalNameColumn
} from "../../ticket-categorization/helper/profiles.helper";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import { deleteTrellisProfile, trellisProfileClone } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { sortProfiles } from "./helper";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface TrellisScoreProfileContentProps {
  isModalView?: boolean;
}

const TrellisScoreProfileContent: React.FC<TrellisScoreProfileContentProps> = ({ isModalView }) => {
  const history = useHistory();
  const dispatch = useDispatch();

  const trellisScoreProfilesListState: TrellisProfilesListState = useSelector(trellisProfileListSelector);

  const trellisScoreProfilesList: RestTrellisScoreProfile[] | undefined = trellisScoreProfilesListState.data?.records;

  const editUrl = (profileId: string) => WebRoutes.trellis_profile.scheme.edit(profileId);

  const handleMenuClick = useCallback((key: string, profileId: string) => {
    switch (key) {
      case PROFILE_ACTIONS.EDIT:
        history.push(editUrl(profileId));
        break;
      case PROFILE_ACTIONS.DELETE:
        dispatch(deleteTrellisProfile(profileId || ""));
        break;
      case PROFILE_ACTIONS.CLONE:
        dispatch(trellisProfileClone(profileId));
        break;
      default:
        return;
    }
  }, []);

  const access = useConfigScreenPermissions();

  const actionColumns = useMemo(() => {
    let columns = [PROFILE_ACTIONS.EDIT];
    if (window.isStandaloneApp) {
      if (!getRBACPermission(PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY)) {
        columns.push(PROFILE_ACTIONS.CLONE, PROFILE_ACTIONS.DELETE);
      }
    } else if (access) {
      access[0] && columns.push(PROFILE_ACTIONS.CLONE);
      access[2] && columns.push(PROFILE_ACTIONS.DELETE);
    }
    return columns;
  }, [access]);

  const columns = useMemo(
    () => [
      nameColumn(trellisScoreProfilesList as any, editUrl),
      descriptionColumn(),
      categoriesColumn("sections", "FACTORS", (category: any) => !!category.enabled),
      actionColumn(actionColumns, handleMenuClick)
    ],
    [handleMenuClick, trellisScoreProfilesList]
  );

  const sortedTrellisProfilesList = useMemo(() => sortProfiles(trellisScoreProfilesList), [trellisScoreProfilesList]);

  if (!sortedTrellisProfilesList.length) return <Empty />;

  return (
    <ProfilesPaginatedTable
      dataSource={sortedTrellisProfilesList}
      columns={isModalView ? [modalNameColumn(trellisScoreProfilesList as any, editUrl, "Trellis")] : columns}
      isModalView={isModalView}
    />
  );
};

export default TrellisScoreProfileContent;
