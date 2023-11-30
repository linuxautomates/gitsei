import React, { useCallback, useMemo } from "react";
import { Form, Row, Col, Button } from "antd";
import "dashboard/graph-filters/components/JenkinsGithubParameters.style.scss";
import { CustomSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { APIFilterConfigType, ApiDropDownData, DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { get, isArray } from "lodash";
import { showInfoProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

interface JenkinsGithubParametersProps {
  data: Array<any>;
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  handleRemoveFilter: (key: string) => void;
}

// just need filters.parameters and send an on change for parameters
const JenkinsParametersFilterWrapper: React.FC<JenkinsGithubParametersProps> = (
  props: JenkinsGithubParametersProps
) => {
  const { onFilterValueChange, filterProps } = props;
  const {
    label,
    beKey,
    apiFilterProps,
    allFilters,
    filterMetaData,
    placeholder,
    labelCase,
    disabled,
    filterInfo
  } = filterProps;
  const {
    options,
    selectMode,
    createOption,
  } = filterMetaData as DropDownData;

  let parameters = [...(allFilters.parameters || [])];
  let parametersKeyNames = useMemo(() => parameters.map(data => data.name), [parameters]);

  const getOptions = useMemo(() => {
    let _options: Array<{ label: string; value: string | number }> = [];
    if (isArray(options)) _options = options;
    if (options instanceof Function) _options = options({ ...filterProps });

    let finalKeyData = (Object.keys(_options) as Array<any>)
      .filter((keyOption: string) =>
        !parametersKeyNames.includes(keyOption));

    return finalKeyData;
  }, [filterProps, options, parametersKeyNames]);

  const getOptionsValue = useCallback((key: any) => {
    let _options: Array<{ label: string; value: string | number }> = [];
    if (isArray(options)) _options = options;
    if (options instanceof Function) _options = options({ ...filterProps });
    if (key) {
      return (get(_options, key, []) as Array<any>);
    }
    return [];

  }, [filterProps, options]);

  const isDisabled = useMemo(() => {
    if (typeof disabled === "boolean") return disabled;
    if (disabled instanceof Function) return disabled({ filters: allFilters });
    return false;
  }, [disabled, allFilters]);

  const onDelete = (index: number) => {
    return (e: any) => {
      parameters.splice(index, 1);
      onFilterValueChange(parameters, "parameters");
    };
  };

  const onAddParameter = (e: any) => {
    parameters.push({
      name: "",
      values: []
    });
    onFilterValueChange(parameters, "parameters");
  };

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter: props.handleRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  const apiConfig = useMemo(() => (filterMetaData as ApiDropDownData)?.apiConfig, [filterMetaData]) as
    | APIFilterConfigType
    | undefined;

  const loading = useMemo(() => !!apiConfig?.loading, [apiConfig]);

  const onChangeKey = (value: any, index: number) => {
    parameters[index].name = value.pop();
    parameters[index].values = [];
    onFilterValueChange(parameters, "parameters");
  };

  const onChangeValue = (value: any, index: number) => {
    parameters[index].values = value;
    onFilterValueChange(parameters, "parameters");
  };

  const filterInfoConfig: showInfoProps = useMemo(() => {
    if (typeof filterInfo === "function")
      return {
        showInfo: !!filterInfo({ filters: allFilters }),
        description: filterInfo({ filters: allFilters }) || ""
      };
    return { showInfo: !!filterInfo, description: filterInfo || "" };
  }, [filterInfo, allFilters]);

  // @ts-ignore
  return (
    <Form.Item key={beKey}
      label={
        <NewCustomFormItemLabel
          label={label || ""}
          withDelete={apiFilters.withDelete}
          withInfo={filterInfoConfig}
        />
      }>
      <Row gutter={[10, 10]} align={"middle"} type={"flex"} justify={"start"} style={{ margin: 0 }}>
        {parameters.length === 0 && (
          <Col span={24}>
            <Button
              size={"small"}
              type={"link"}
              icon={"plus"}
              onClick={onAddParameter}
              className="add-parameter-button"
            />
          </Col>
        )}
        {parameters.map((parameter: any, index: number) => (
          <Col span={24}>
            <Row gutter={[10, 10]} type={"flex"} justify={"start"} align={"top"}>
              <Col span={10}>
                <h5>Name</h5>
                <CustomSelect
                  dataFilterNameDropdownKey={"filter-key"}
                  createOption={!!createOption}
                  labelKey={"label"}
                  placeholder={placeholder}
                  valueKey={"value"}
                  labelCase={labelCase ?? "title_case"}
                  options={getOptions}
                  mode={selectMode}
                  showArrow={true}
                  value={parameter.name || []}
                  loading={loading}
                  disabled={isDisabled || loading}
                  dropdownVisible={true}
                  onChange={(values: string) => onChangeKey(values, index)}
                />
              </Col>
              <Col span={9}>
                <h5>Values</h5>
                <CustomSelect
                  dataFilterNameDropdownKey={"filter-value"}
                  createOption={!!createOption}
                  labelKey={"label"}
                  placeholder={placeholder}
                  valueKey={"value"}
                  labelCase={labelCase ?? "title_case"}
                  options={getOptionsValue(parameter.name)}
                  mode={selectMode}
                  showArrow={true}
                  value={parameter.values}
                  loading={loading}
                  disabled={isDisabled || loading}
                  dropdownVisible={true}
                  onChange={(values: string) => onChangeValue(values, index)}
                />
              </Col>
              <Col span={2}>
                <Button
                  size={"small"}
                  type={"link"}
                  icon={"delete"}
                  onClick={onDelete(index)}
                  className="add-parameter-button"
                />
              </Col>
              {index === parameters.length - 1 && (
                <Col span={2}>
                  <Button
                    size={"small"}
                    type={"link"}
                    icon={"plus"}
                    onClick={onAddParameter}
                    className="add-parameter-button"
                  />
                </Col>
              )}
            </Row>
          </Col>
        ))}
      </Row>
    </Form.Item>
  );
};

// @ts-ignore
export default JenkinsParametersFilterWrapper;
