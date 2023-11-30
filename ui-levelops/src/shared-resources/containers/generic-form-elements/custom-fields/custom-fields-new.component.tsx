import React, { useState, useEffect, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { genericGet, widgetFilterValuesGet } from "reduxConfigs/actions/restapi";
import { integrationsGetState } from "reduxConfigs/selectors/integrationSelectors";
import { get } from "lodash";
import { genericApiState } from "reduxConfigs/selectors/generic.selector";
import { AntFormItem, AntRow, AntCol, AntSelect, AntButton } from "shared-resources/components";
import { usePrevious } from "shared-resources/hooks/usePrevious";

// We need an integration id props.
// We need to be prepared for mid-mount integration_id updates.
// When we get a new integration id, we need to make an API
// request to get its associated custom fields.
// Once the custom fields are loaded, enable the dropdowns for it.

const CustomFieldsComponent: React.FC<any> = props => {
  const dispatch = useDispatch();
  const integrationIdProp = props?.field_values?.integration_id?.value?.[0];
  const previousIntegrationIdProp = usePrevious(integrationIdProp);
  const [integrationLoading, setIntegrationLoading] = useState<boolean>(false);
  const [customFieldsLoading, setCustomFieldsLoading] = useState<boolean>(false);
  const integrationState = useSelector(integrationsGetState) || { loading: true, error: true };
  const jiraFilterValuesState = useSelector(state =>
    genericApiState(state, {
      uri: "jira_filter_values",
      method: "list",
      id: "0"
    })
  );
  const [localValue, setLocalValue] = useState<any>(props.value);
  const localValueMemory = useRef<{ [key: string]: any }>({});
  // const debouncedLocalValue = useDebounce(localValue, 500);

  // Load integration from integration id prop.
  useEffect(() => {
    if (integrationIdProp) {
      dispatch(genericGet("integrations", integrationIdProp.key));
      setIntegrationLoading(true);

      // @ts-ignore
      if (previousIntegrationIdProp?.key !== undefined) {
        // When changing from one integration to another, it doesn't
        // really make sense to keep the previous selected custom fields,
        // so change them, but also store them locally so we can restore them
        // if the user switches back while the component is still mounted.
        const newMemoryValue = {
          ...localValueMemory.current,
          //@ts-ignore
          [previousIntegrationIdProp.key]: localValue
        };
        setLocalValue(localValueMemory.current[integrationIdProp?.key] || { custom_fields: [] });
        localValueMemory.current = newMemoryValue;
      }
    }
  }, [integrationIdProp]);

  // Load custom field options after loading integration
  useEffect(() => {
    if (integrationLoading && integrationIdProp) {
      const loading = get(integrationState, [integrationIdProp.key, "loading"], true);
      const error = get(integrationState, [integrationIdProp.key, "error"], false);
      if (!loading && error) {
        setIntegrationLoading(false);
      } else if (!loading && !error) {
        setIntegrationLoading(false);
        const integration = get(integrationState, [integrationIdProp.key, "data"], {});
        if (integration && integration.id !== undefined) {
          const filters = {
            fields: ["status", "priority", "issue_type", "assignee", "project", "component", "label"],
            integration_ids: [integration.id],
            filter: {
              integration_ids: [integration.id]
            }
          };
          dispatch(widgetFilterValuesGet("jira_filter_values", filters));
          setCustomFieldsLoading(true);
        }
      }
    }
  }, [integrationLoading, integrationState, integrationIdProp]);

  // Turn off loading flag after custom fields
  // finishes loading
  useEffect(() => {
    if (customFieldsLoading) {
      const loading = get(jiraFilterValuesState, ["loading"], true);
      if (!loading) {
        setCustomFieldsLoading(false);
      }
    }
  }, [jiraFilterValuesState, customFieldsLoading]);

  // When localValue changes, call the update prop
  // to update value in parent.
  useEffect(() => {
    props.onChange(localValue);
  }, [localValue]);

  const addCustomField = () => {
    let propsVal = { ...props.value };
    if (propsVal.custom_fields) {
      propsVal.custom_fields.push({ key: undefined, value: undefined });
      setLocalValue(propsVal);
    } else {
      propsVal.custom_fields = [{ key: undefined, value: undefined }];
      setLocalValue(propsVal);
    }
  };

  const removeCustomField = (index: number) => {
    let propsVal = { ...props.value };
    propsVal.custom_fields.splice(index, 1);
    setLocalValue(propsVal);
  };

  const customFieldOptions = get(jiraFilterValuesState, ["data", "custom_fields"], []).map((field: any) => {
    const mappedField = {
      key: field.key,
      value: field.key,
      label: field.name
    };

    return mappedField;
  });

  const customFieldValues = get(props, ["value", "custom_fields"], []).map((field: any) => {
    const mappedValue = {
      ...field,
      name: (customFieldOptions.find((option: any) => option.key === field.key) || {}).label
    };
    return mappedValue;
  });

  const customFieldOptionValues = (index: number) => {
    const key = get(props, ["value", "custom_fields", index, "key"], undefined);
    if (!key) {
      return [];
    }
    const filterValues = get(jiraFilterValuesState, ["data", "records"], []);
    const filterRecord = filterValues.find((val: any) => Object.keys(val)[0] === key);
    if (filterRecord) {
      const optionValues = (filterRecord[key] || []).map((val: any) => val.key);
      return optionValues;
    }
    return [];
  };

  return (
    <AntFormItem>
      {customFieldValues.map((field: any, index: number) => {
        // Note: The purpose of key={`${field.key}-${field.value}-${index}`
        // is to disable the animation of the Select component
        // when its value changes.
        return (
          <AntRow gutter={[10, 10]} key={`${field.key}-${field.value}-${index}` || index}>
            <AntCol span={12}>
              <AntSelect
                loading={customFieldsLoading}
                disabled={integrationIdProp?.key === undefined}
                mode={"default"}
                labelInValue={true}
                showSearch={true}
                allowClear={false}
                options={customFieldOptions}
                value={field.key ? { key: field.key, label: field.name } : undefined}
                onChange={(value: any) => {
                  let propsVal = { ...props.value };
                  // on question change, the answers selected have to be reset
                  propsVal.custom_fields[index] = { key: value.key, value: undefined };
                  setLocalValue(propsVal);
                }}
              />
            </AntCol>
            <AntCol span={11}>
              <AntSelect
                loading={customFieldsLoading}
                disabled={
                  integrationIdProp?.key === undefined ||
                  get(props, ["value", "custom_fields", index, "key"], undefined) === undefined
                }
                mode={"multiple"}
                labelInValue={true}
                showSearch={true}
                onOptionFilter={(value: string, option: any) => {
                  if (!value || typeof value !== "string") return true;

                  return (option?.label || "").toLowerCase().includes(value.toLowerCase());
                }}
                allowClear={false}
                value={field.value ? [{ key: field.value, label: field.value }] : []}
                //onSearch={value => this.setState({search_string: value})}
                options={customFieldOptionValues(index) || []}
                onDeselect={(value: any) => {
                  let propsVal = { ...props.value };
                  propsVal.custom_fields[index] = { ...propsVal.custom_fields[index], value: undefined };
                  setLocalValue(propsVal);
                }}
                onSelect={(value: any) => {
                  let propsVal = { ...props.value };
                  // on question change, the answers selected have to be reset
                  propsVal.custom_fields[index] = { ...propsVal.custom_fields[index], value: value.key };
                  setLocalValue(propsVal);
                }}
              />
            </AntCol>
            <AntCol span={1}>
              <AntButton type={"link"} icon={"delete"} onClick={(e: any) => removeCustomField(index)} />
            </AntCol>
          </AntRow>
        );
      })}
      {integrationIdProp?.key !== undefined && (
        <AntRow>
          <AntCol span={24}>
            <AntButton icon={"plus"} onClick={addCustomField} type={"link"}>
              Add Custom Field
            </AntButton>
          </AntCol>
        </AntRow>
      )}
    </AntFormItem>
  );
};

export default CustomFieldsComponent;
