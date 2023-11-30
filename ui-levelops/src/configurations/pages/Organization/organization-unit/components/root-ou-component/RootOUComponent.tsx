import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { get } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { Link, useParams } from "react-router-dom";
import { OrganizationUnitList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { WebRoutes } from "routes/WebRoutes";
import { AntTooltip } from "shared-resources/components";
import { ROOT_OU_HINT_TEXT } from "./constant";
import { ProjectPathProps } from "classes/routeInterface";

interface RootOUComponentProps {
  category_id: string;
  workspace_id: string;
  currentWorkspaceIsDemo: boolean;
}
const RootOUComponent: React.FC<RootOUComponentProps> = ({ category_id, workspace_id, currentWorkspaceIsDemo }) => {
  const [rootOULoading, setRootOULoading] = useState<boolean>(true);
  const [root, setRoot] = useState<orgUnitJSONType | undefined>(undefined);
  const dispatch = useDispatch();
  const rootOUID = `ROOT_OU_ID-${category_id}`;
  const orgUnitListState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: rootOUID
  });
  const projectParams = useParams<ProjectPathProps>();

  useEffect(() => {
    dispatch(OrganizationUnitList({ filter: { ou_category_id: [category_id], parent_ref_id: null } }, rootOUID));
  }, []);

  useEffect(() => {
    if (rootOULoading) {
      const loading = get(orgUnitListState, ["loading"], true);
      const error = get(orgUnitListState, ["error"], false);
      if (!loading) {
        if (!error) {
          const records: Array<orgUnitJSONType> = get(orgUnitListState, ["data", "records"], []);
          if (records.length) {
            setRoot(records[0]);
          }
        }
        setRootOULoading(false);
      }
    }
  }, [orgUnitListState]);

  const getEditURL = useMemo(
    () => WebRoutes.organization_page.edit(projectParams, root?.id, workspace_id, category_id),
    [root, category_id]
  );

  const oldReadOnly = getRBACPermission(PermeableMetrics.ORG_UNIT_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : root?.access_response?.edit;

  return (
    <div>
      <AntTooltip title={ROOT_OU_HINT_TEXT} className="flex align-center">
        Root Collection :
        {!currentWorkspaceIsDemo && !isReadOnly ? (
          <Link to={getEditURL} className="link ml-10">
            {root?.name}
          </Link>
        ) : (
          <span className="ml-10">{root?.name}</span>
        )}
      </AntTooltip>
    </div>
  );
};

export default RootOUComponent;
