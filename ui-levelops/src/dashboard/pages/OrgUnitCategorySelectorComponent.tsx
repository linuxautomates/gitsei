import React, { useEffect, useState } from "react";
import { OUCategoryOptionsType, PivotType } from "configurations/configuration-types/OUTypes";
import { DASHBOARD_CREATE_PIVOT_UUID } from "dashboard/constants/uuid.constants";
import { get, uniq, uniqBy } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { orgUnitPivotsList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import { Form } from "antd";
import {
  getOUHeaderSelectedWorkspace,
  getSelectedWorkspace
} from "reduxConfigs/selectors/workspace/workspace.selector";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { sanitizeObject } from "utils/commonUtils";
import OrgUnitCategoryDropdownComponent from "./org-unit-category-dropdown/OrgUnitCategoryDropdownComponent";

interface OrgUnitCategorySelectorProps {
  handleOUCategoryChange: (value: string[]) => void;
  ouCategories: string[];
}
const OrgUnitCategorySelectorComponent: React.FC<OrgUnitCategorySelectorProps> = ({
  ouCategories,
  handleOUCategoryChange
}) => {
  const [pivotsLoading, setPivotsLoading] = useState<boolean>(false);
  const [pivots, setPivots] = useState<Array<OUCategoryOptionsType>>([]);
  const selectedWorkspace: WorkspaceModel = useSelector(getSelectedWorkspace);
  const pivotsListState = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots_list",
    method: "list",
    uuid: DASHBOARD_CREATE_PIVOT_UUID
  });
  const dispatch = useDispatch();
  const ou_workspace_id = useSelector(getOUHeaderSelectedWorkspace);
  const fetchPivotsList = () => {
    setPivotsLoading(true);
    dispatch(
      orgUnitPivotsList(DASHBOARD_CREATE_PIVOT_UUID, {
        filter: sanitizeObject({ workspace_id: [ou_workspace_id ?? selectedWorkspace?.id] })
      })
    );
  };

  useEffect(() => {
    fetchPivotsList();
    return () => {
      dispatch(restapiClear("pivots_list", "list", DASHBOARD_CREATE_PIVOT_UUID));
    };
  }, []);

  useEffect(() => {
    if (pivotsLoading) {
      const loading = get(pivotsListState, ["loading"], true);
      const error = get(pivotsListState, ["error"], true);
      if (!loading) {
        let nrecords: Array<OUCategoryOptionsType> = [];
        if (!error) {
          let records: Array<PivotType> = get(pivotsListState, ["data", "records"], []);
          nrecords = records.reduce((acc: OUCategoryOptionsType[], curValue: PivotType) => {
            if (curValue.enabled) {
              acc.push({ label: curValue.name, value: curValue.id, ouCount: curValue.count_of_ous });
            }
            return acc;
          }, []);
          setPivots(uniqBy(nrecords, "value"));
        }
        setPivotsLoading(false);
        if (!ouCategories?.length) handleOUCategoryChange(nrecords.map(rec => rec.value));
      }
    }
  }, [pivotsListState, pivotsLoading, ouCategories]);

  return (
    <Form.Item label="COLLECTION CATEGORIES" className="ou-category-selector">
      <AntText style={{ color: "var(--text-secondary-color)" }} className="mt-20">
        {
          "Select Collection Categories for this insight (all child collection nodes under the selected category will inherit this insight). For more targeted associations, go to Settings > Collections after creating this insight."
        }
      </AntText>
      <OrgUnitCategoryDropdownComponent
        categoryLoading={pivotsLoading}
        categoryOptions={pivots}
        selectedCategories={uniq(ouCategories)}
        onChange={handleOUCategoryChange}
      />
    </Form.Item>
  );
};

export default OrgUnitCategorySelectorComponent;
