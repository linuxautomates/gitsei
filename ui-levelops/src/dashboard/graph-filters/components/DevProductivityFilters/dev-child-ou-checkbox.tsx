import { Checkbox, Form } from "antd";
import { CheckboxChangeEvent } from "antd/lib/checkbox";
import { get } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { AntText } from "shared-resources/components";
import { DEV_CHILD_CHECKBOX_LABEL } from "./constants";

interface DevChildCheckboxFilterProps {
  value: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
}

const DevChildCheckboxFilter: React.FC<DevChildCheckboxFilterProps> = ({ value, onFilterValueChange }) => {
  const dispatch = useDispatch();
  const [orgUnitLoading, setOrgUnitLoading] = useState<boolean>(true);
  const selectedOrgUnit = useSelector(getSelectedOU);
  const childOrgUnitdata = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: "childs_data"
  });
  const [childOrgData, setChildOrgData] = useState<any[]>([]);

  useEffect(() => {
    if (orgUnitLoading && selectedOrgUnit) {
      dispatch(
        genericList(
          "organization_unit_management",
          "list",
          { filter: { ou_category_id: [selectedOrgUnit?.ou_category_id], parent_ref_id: selectedOrgUnit?.id } },
          null,
          "childs_data"
        )
      );
    }
  }, []);

  useEffect(() => {
    if (orgUnitLoading) {
      const loading = get(childOrgUnitdata, ["loading"], true);
      const error = get(childOrgUnitdata, ["error"], true);
      if (!loading && !error) {
        const data = get(childOrgUnitdata, ["data", "records"], []);
        setChildOrgData(data);
        setOrgUnitLoading(false);
      }
    }
  }, [childOrgUnitdata]);

  const disable = useMemo(() => {
    return childOrgData.length > 0 ? false : value ? false : true;
  }, [childOrgData, value]);

  return (
    <Form.Item className="mt-10" label={"Collection Display"}>
      <Checkbox
        disabled={disable}
        checked={value}
        onChange={(e: CheckboxChangeEvent) => onFilterValueChange(e.target.checked, "is_immediate_child_ou")}>
        {<AntText className="mr-1">{DEV_CHILD_CHECKBOX_LABEL}</AntText>}
      </Checkbox>
    </Form.Item>
  );
};

export default DevChildCheckboxFilter;
