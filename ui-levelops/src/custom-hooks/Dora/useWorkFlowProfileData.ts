import { get } from "lodash";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { DORA_REPORT_TO_KEY_MAPPING } from "dashboard/graph-filters/components/helper";
import { getWorkflowProfileFilters } from "reduxConfigs/actions/restapi/workFlowNewAction";
import { CombinedInfoDataType } from "dashboard/graph-filters/containers/Dora/typing";

export const useWorkFlowProfileFilters = (report: string) => {
  const dispatch = useDispatch();
  const selectedOUState = useSelector(getSelectedOU);
  const workflowProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState.id });
  const reportNameKey = DORA_REPORT_TO_KEY_MAPPING[report];
  const [workflowDataList, setWorkflowDataList] = useState<CombinedInfoDataType[]>([]);
  const [workflowLoading, setWorkflowLoading] = useState<boolean>(false);
  const uri = `workflowInfo_dora_${workflowProfile?.name}`;
  const WorkflowInfoData = useParamSelector(getGenericUUIDSelector, {
    uri: uri,
    method: "list",
    uuid: reportNameKey
  });

  const [dataCount, setDataCount] = useState<number>(0);

  useEffect(() => {
    if (workflowProfile) {
      setWorkflowLoading(true);
      dispatch(getWorkflowProfileFilters(uri, report, reportNameKey));
    }
  }, [workflowProfile]);

  useEffect(() => {
    const loading = get(WorkflowInfoData, ["loading"], true);
    const error = get(WorkflowInfoData, ["error"], true);
    if (!loading && !error) {
      const data = get(WorkflowInfoData, ["data"], {});
      setDataCount(data?.moreCount);
      setWorkflowDataList(data?.records);
      setWorkflowLoading(false);
    }
  }, [WorkflowInfoData?.data]);

  return { data: workflowDataList, loading: workflowLoading, count: dataCount };
};
