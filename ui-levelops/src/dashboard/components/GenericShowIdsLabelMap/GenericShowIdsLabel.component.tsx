import { Tag } from "antd";
import { get, isArray } from "lodash";
import React, { useCallback, useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getStoredIdsMap } from "reduxConfigs/actions/restapi/genericIdsMap.actions";
import { LabelIdMap } from "reduxConfigs/sagas/genericIdsMap.saga";
import { getformState } from "reduxConfigs/selectors/formSelector";

interface GenericShowIdsLabelComponentProps {
  filterKey: "ticket_categorization_scheme" | "velocity_config_id";
  label: string;
  value: string | string[];
}

const GenericShowIdsLabelComponent: React.FC<GenericShowIdsLabelComponentProps> = props => {
  const { filterKey, label, value } = props;

  const dispatch = useDispatch();

  const getFormName: string = useMemo(() => {
    if (isArray(value)) return value.join();
    return value;
  }, [value]);

  const getFormKey: string = useMemo(() => {
    if (filterKey === "ticket_categorization_scheme") return "effort_investment_profile_ids";
    return "lead_time_profile_ids";
  }, [filterKey]);

  const getFormIds: string[] = useMemo(() => {
    if (isArray(value)) return value;
    return [value];
  }, [value]);

  const formState = useSelector(state => getformState(state, getFormName));

  useEffect(() => {
    const filter = {
      [getFormKey]: getFormIds
    };
    dispatch(getStoredIdsMap(getFormName, filter));
  }, []);

  const getLabel = useCallback((): string => {
    const restState = get(formState, getFormKey, []);
    if (isArray(value)) {
      const names: string[] = [];
      value.forEach((id: string) => {
        const data: LabelIdMap = restState.find((val: LabelIdMap) => val.id === id);
        if (data) names.push(data.name);
      });
      return names.join();
    }
    const data: LabelIdMap = restState.find((val: LabelIdMap) => val.id === value);
    if (data) return data.name;
    return value;
  }, [formState, getFormKey, value]);

  return (
    <Tag key={getFormName} className="widget-filter_tags">
      {getLabel()}
    </Tag>
  );
};

export default GenericShowIdsLabelComponent;
