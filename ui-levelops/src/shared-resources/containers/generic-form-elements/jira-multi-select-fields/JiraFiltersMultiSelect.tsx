import React, { useEffect, useState } from "react";
import { Col, Row } from "antd";
import CustomSelect from "shared-resources/components/custom-select/CustomSelect";
import { get, uniqBy } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { integrationConfigState } from "./helper";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { v1 as uuid } from "uuid";

const JiraCustomSelectWrapper: React.FC = (props: any) => {
  const { value, onChange, field_values } = props;

  const integrationId = get(field_values, ["integration_id", "value", 0, "key"], undefined);

  const [apiId, setApiId] = useState<string>("");
  const [options, setOptions] = useState<Array<any>>([]);
  const [filterValue, setFilterValue] = useState<any>(undefined);

  const dispatch = useDispatch();
  const rest_api = useSelector(state => integrationConfigState(state, apiId));

  useEffect(() => {
    if (!integrationId) {
      setFilterValue(undefined);
      onChange && onChange(undefined);
    } else {
      setFilterValue(value);
    }

    const integrationIds = integrationId ? [integrationId] : [];
    const newId = uuid();
    dispatch(
      actionTypes.genericList(
        "jira_integration_config",
        "list",
        { filter: { integration_ids: integrationIds } },
        null,
        newId
      )
    );
    setApiId(newId);
  }, [integrationId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const loading = get(rest_api, ["loading"], true);
    const error = get(rest_api, ["error"], true);
    if (!loading && !error) {
      const records = get(rest_api, ["data", "records"], []);
      const aggFields = records
        .filter((record: { integration_id: any }) => record.integration_id === integrationId)
        .reduce((agg: any[], obj: any) => {
          const fields = get(obj, ["config", "agg_custom_fields"], []);
          agg.push(...fields);
          return agg;
        }, [])
        .filter((field: { key: string }) => field.key.includes("customfield_"));

      setOptions(uniqBy(aggFields, "key"));
    }
  }, [rest_api]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    return () => {
      //@ts-ignore
      dispatch(actionTypes.restapiClear("jira_integration_config", "list", apiId));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleFilterSelectChange = (value: any) => {
    setFilterValue(value);
    onChange && onChange(value);
  };

  return (
    <Row>
      <Col span={24}>
        <CustomSelect
          labelKey="name"
          valueKey="key"
          // @ts-ignore
          disabled={!integrationId}
          createOption={false}
          labelCase={"title_case"}
          options={options}
          mode={"default"}
          showArrow={true}
          value={filterValue}
          truncateOptions={true}
          onChange={handleFilterSelectChange}
        />
      </Col>
    </Row>
  );
};

export default JiraCustomSelectWrapper;
