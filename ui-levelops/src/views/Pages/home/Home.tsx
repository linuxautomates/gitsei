import React, { useCallback, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { AntRow, AntCol } from "shared-resources/components";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { setSelectedWorkspace, workspaceRead } from "reduxConfigs/actions/workspaceActions";
import { TILE_COLORS } from "./constant";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import "./home.scss";
import Tile from "shared-resources/components/tile/Tile";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import { WorkspaceCreateEdit } from "products/containers";
import { isSelfOnboardingUser, sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { userProfileUpdate } from "reduxConfigs/actions/restapi";
import { RestUsers } from "classes/RestUsers";
import { sessionCurrentUser } from "reduxConfigs/actions/sessionActions";

interface HomeProps {
  workspaces: WorkspaceModel[];
}

const Home: React.FC<HomeProps> = ({ workspaces }) => {
  const dispatch = useDispatch();
  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();
  const hasManageAccess = userRole?.toLowerCase() === USERROLES.ADMIN;
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [selectedWorkspace, setWorkspace] = useState<WorkspaceModel | undefined>(undefined);
  const sessionCurrentUserData = useSelector(sessionCurrentUserState);
  const isTrialUser = useSelector(isSelfOnboardingUser);

  const generateColor = () => {
    const no = Math.floor(Math.random() * (11 - 0 + 1) + 0);
    return `${TILE_COLORS?.[no]}`;
  };

  const setSelectedWorkSpace = (space: WorkspaceModel) => {
    dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, space));
    const sessionCurrentData = {
      ...sessionCurrentUserData,
      metadata: { ...sessionCurrentUserData?.metadata, selected_workspace: space }
    };
    const restUser = new RestUsers({
      ...sessionCurrentData
    });
    dispatch(userProfileUpdate(restUser));
    dispatch(sessionCurrentUser({ loading: false, error: false, data: sessionCurrentData }));
  };

  const handleWorkspaceUpdate = useCallback(() => {
    dispatch(workspaceRead("workspace_list", "list", {}));
    setModalVisible(false);
  }, [selectedWorkspace]);

  return (
    <div className="landing-home">
      <AntRow className="nav-group m-2">
        {workspaces.map((space, index) => {
          return (
            <AntCol key={`${space.name}-${index}`} xs={12} sm={12} md={9} lg={8} xl={6} span={9}>
              <Tile
                key={`${space.name}-${index}`}
                handleSelect={setSelectedWorkSpace}
                tile={space}
                icon={{ name: "workspace", bgColor: generateColor() }}
                hasManageAccess={hasManageAccess}
                handleManage={() => {
                  setWorkspace(space);
                  setModalVisible(true);
                }}
                isTrialUser={isTrialUser}
              />
            </AntCol>
          );
        })}
        {modalVisible && (
          <WorkspaceCreateEdit
            onUpdate={handleWorkspaceUpdate}
            display={modalVisible}
            onCancel={() => setModalVisible(false)}
            product_id={selectedWorkspace?.id}
          />
        )}
      </AntRow>
    </div>
  );
};

export default Home;
