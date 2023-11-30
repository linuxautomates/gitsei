import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { get } from "lodash";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericWorkSpaceUUIDSelector } from "reduxConfigs/selectors/workspace/workspace.selector";
import { workspaceRead } from "reduxConfigs/actions/workspaceActions";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { sessionUserWorkspacesSelections } from "reduxConfigs/selectors/session_current_user.selector";

export function useWorkSpaceList(filterWorspacesBasedOnRBAC: boolean = false) {
  const dispatch = useDispatch();

  const [list, setList] = useState<WorkspaceModel[]>([]);
  const [loading, setLoading] = useState(true);

  const workSpaceListState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "list",
    uuid: "workspace_list"
  });

  const allowedWorkspaces = useSelector(sessionUserWorkspacesSelections);

  useEffect(() => {
    const data = get(workSpaceListState, ["data", "records"], []);
    if (data.length) {
      data.sort(stringSortingComparator("name"));
      setList(data);
      setLoading(false);
    } else {
      dispatch(workspaceRead("workspace_list", "list", {}));
    }
  }, []);

  useEffect(() => {
    const loading = get(workSpaceListState, ["loading"], true);
    const error = get(workSpaceListState, ["error"], true);
    if (!loading && !error) {
      let data: WorkspaceModel[] = get(workSpaceListState, ["data", "records"], []).sort(
        stringSortingComparator("name")
      );
      const workspacesRBACBased = Object.keys(allowedWorkspaces);
      if (workspacesRBACBased.length && filterWorspacesBasedOnRBAC) {
        data = (data ?? []).filter((w: WorkspaceModel) => workspacesRBACBased.includes(w.id));
      }
      setList(data);
      setLoading(false);
    }
    if (!loading && error) {
      setLoading(false);
    }
  }, [workSpaceListState, allowedWorkspaces, filterWorspacesBasedOnRBAC]);

  return { loading, workSpaceListData: list };
}
