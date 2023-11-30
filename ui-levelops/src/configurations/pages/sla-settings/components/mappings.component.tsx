import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { Row, Col } from "antd";
import { integrationsList, restapiClear } from "reduxConfigs/actions/restapi";
import { FiltersType, optionType } from "dashboard/dashboard-types/common-types";
import { WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";
import { AntSelect } from "shared-resources/components";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";

interface MappingsComponentProps {
  onChange: (filters: FiltersType) => void;
}

export const MappingsComponent: React.FC<MappingsComponentProps> = (props: MappingsComponentProps) => {
  const dispatch = useDispatch();
  const [productId, setProductId] = useState<string>("");
  const [integrationOptions, setIntegrationOptions] = useState<optionType[]>([]);
  const [integrationIds, setIntegrationIds] = useState<string[]>([]);
  const integrationListState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "list",
    uuid: "sla_integration"
  });

  const { workSpaceListData } = useWorkSpaceList();

  useEffect(() => {
    //@ts-ignore
    dispatch(integrationsList({}, "sla_integration" as string, "sla_integration"));
  }, []);

  useEffect(() => {
    const loading = get(integrationListState, "loading", true);
    const error = get(integrationListState, "error", true);
    if (!loading && !error) {
      const data = get(integrationListState, ["data", "records"], []);
      const integrationOptions = data.map((integration: any) => ({ label: integration?.name, value: integration?.id }));
      setIntegrationOptions(integrationOptions);
    }
  }, [integrationListState]);

  const workspaceOptions = useMemo(() => {
    if (workSpaceListData?.length) {
      return workSpaceListData.map(workspace => ({ label: workspace.name, value: workspace.id }));
    }
    return [];
  }, [workSpaceListData]);

  const handleWorkspaceChanges = useCallback(
    (value: string) => {
      if (!value) {
        dispatch(restapiClear("sla_module_data", "list", "sla_data"));
        props.onChange({
          product_id: undefined,
          integration_ids: []
        });
        setIntegrationIds([]);
      } else {
        const workspace: WorkspaceModel | undefined = workSpaceListData.find(
          (workspace: WorkspaceModel) => workspace?.id === value
        );
        const integrations = (workspace?.integration_ids || []).map((integration: number) => integration?.toString());
        props.onChange({
          product_id: workspace?.id,
          integration_ids: integrations
        });
        setProductId(value);
        setIntegrationIds(integrations);
      }
    },
    [props.onChange, workSpaceListData]
  );

  const onOptionFilter = useCallback((value: string, option: any) => {
    if (!value) return true;
    return (option?.label || "").toLowerCase().includes(value.toLowerCase());
  }, []);

  return (
    <Row>
      <Col span={8}>
        <h4>{`SEI ${WORKSPACE_NAME_MAPPING[WORKSPACES]}`}</h4>
        <AntSelect
          style={{ width: "100%" }}
          showArrow={true}
          value={productId}
          showSearch
          onOptionFilter={onOptionFilter}
          mode={"single"}
          options={workspaceOptions}
          onChange={handleWorkspaceChanges}
        />
      </Col>
      <Col span={16}>
        <h4>Integrations</h4>
        <AntSelect
          style={{ width: "100%" }}
          value={integrationIds}
          showArrow={true}
          options={integrationOptions}
          mode={"multiple"}
          showSearch
          onOptionFilter={onOptionFilter}
          onChange={(value: string[]) => {
            setIntegrationIds(value || []);
            props.onChange({
              product_id: productId,
              integration_ids: value
            });
          }}
        />
      </Col>
    </Row>
  );
};

export default MappingsComponent;
