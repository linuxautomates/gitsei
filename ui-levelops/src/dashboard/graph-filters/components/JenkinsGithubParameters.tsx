import React, { useEffect, useState } from "react";
import { Form, Row, Col, Input, Tag, Icon, Button } from "antd";
import "./JenkinsGithubParameters.style.scss";
import { scmCicdReportTypes, scmCicdStatReportTypes } from "dashboard/constants/applications/names";
interface JenkinsGithubParametersProps {
  data: Array<any>;
  onFilterValueChange: (value: any, type?: any) => void;
  filters: any;
  application: string;
  reportType: string;
}

// just need filters.parameters and send an on change for parameters
const JenkinsGithubParametersComponent: React.FC<JenkinsGithubParametersProps> = (
  props: JenkinsGithubParametersProps
) => {
  const [inputVisible, setInputVisible] = useState(999999);
  const { onFilterValueChange, filters } = props;
  let parameters = [...(filters.parameters || [])];
  const [tags, setTags] = useState(parameters.map((param: any) => [...(param.values || [])]));
  const [names, setNames] = useState(parameters.map((param: any) => param.name));

  useEffect(() => {
    const paramNames = parameters.map((param: any) => param.name);
    // @ts-ignore
    setNames(paramNames);
    const paramTags = parameters.map((param: any) => [...(param.values || [])]);
    // @ts-ignore
    setTags(paramTags);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  const onParamterNameChange = (index: number, update: boolean = false) => {
    return (e: any) => {
      // @ts-ignore
      let paramNames = [...names];
      // @ts-ignore
      paramNames[index] = e.currentTarget.value;
      setNames(paramNames);
      if (update) {
        parameters[index].name = e.currentTarget.value;
        onFilterValueChange(parameters, "parameters");
      }
    };
  };

  const onTagClose = (index: number, tagIndex: number) => {
    return () => {
      let paramTags = parameters[index].values || [];
      paramTags.splice(tagIndex, 1);
      parameters[index].values = [...paramTags];
      // @ts-ignore
      setTags(parameters.map((param: any) => [...(param.values || [])]));
      onFilterValueChange(parameters, "parameters");
    };
  };

  const onTagAdd = (index: number) => {
    return (e: any) => {
      let paramTags = parameters[index].values || [];
      paramTags.push(e.currentTarget.value);
      parameters[index].values = paramTags;
      setInputVisible(999999);
      onFilterValueChange(parameters, "parameters");
    };
  };

  const onDelete = (index: number) => {
    return (e: any) => {
      parameters.splice(index, 1);
      console.log(parameters);
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

  // @ts-ignore
  return (
    <Form.Item
      label={
        [...scmCicdReportTypes, ...scmCicdStatReportTypes].includes(props.reportType as any)
          ? "JOB RUN PARAMETERS"
          : "Jenkins Parameters"
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
                <Input
                  value={names[index]}
                  key={index}
                  onChange={onParamterNameChange(index, false)}
                  onPressEnter={onParamterNameChange(index, true)}
                  onBlur={onParamterNameChange(index, true)}
                />
              </Col>
              <Col span={9}>
                <h5>Values</h5>
                {parameter.values.map((value: any, tagIndex: number) => (
                  <Tag key={tags[index][tagIndex]} closable={true} onClose={onTagClose(index, tagIndex)}>
                    {tags[index][tagIndex]}
                  </Tag>
                ))}
                {inputVisible !== index && (
                  <Tag
                    onClick={e => setInputVisible(index)}
                    style={{ background: "transparent", borderStyle: "dashed" }}>
                    <Icon type={"plus"} /> New Value
                  </Tag>
                )}
                {inputVisible === index && <Input onPressEnter={onTagAdd(index)} />}
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
export default JenkinsGithubParametersComponent;
