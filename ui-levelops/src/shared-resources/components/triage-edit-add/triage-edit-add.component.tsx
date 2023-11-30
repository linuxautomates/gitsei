import React, { useEffect, useRef, useState } from "react";
import { Button, Col, Form, Input, Row, Typography } from "antd";
import { RestTriageRule } from "classes/RestTriageRule";
import { validateEmail } from "utils/stringUtils";
import { EMAIL_WARNING, EMPTY_FIELD_WARNING, ERROR, SUCCESS, URL_WARNING } from "constants/formWarnings";
import { SelectRestapiHelperWrapper as SelectRestapi } from "shared-resources/helpers/select-restapi/select-restapi.helper.wrapper";

const { Text } = Typography;
const REGEX_HELP = "Rule matches when any of the regexes are discovered in the artifact";

interface TriageEditAddProps {
  triageRule: RestTriageRule;
  setTriageRule: (data: RestTriageRule) => void;
  dirty: boolean;
  onNameChanged: (name: string) => void;
  width?: string;
  className?: string;
}

const TriageEditAddComponent: React.FC<TriageEditAddProps> = ({
  triageRule,
  setTriageRule,
  dirty,
  onNameChanged,
  width = "100%",
  className
}) => {
  const ownerFieldRef = useRef<any>(undefined);
  const [userSelect, setUserSelect] = useState<any>(undefined);

  useEffect(() => {
    if (triageRule.owner !== userSelect?.label) {
      setUserSelect({ key: triageRule.owner, label: triageRule.owner });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [triageRule]);

  const onAddRegex = () => {
    let rule = new RestTriageRule(triageRule.json);
    let regexes = rule.regexes;
    regexes.push("");
    rule.regexes = regexes;
    setTriageRule(rule);
  };

  const onDeleteRegex = (index: number) => {
    let rule = new RestTriageRule(triageRule.json);
    rule.removeRegex(index);
    setTriageRule(rule);
  };

  const onUpdateRegex = (index: number, value: string) => {
    let rule = new RestTriageRule(triageRule.json);
    let regexes = rule.regexes;
    regexes[index] = value;
    rule.regexes = regexes;
    setTriageRule(rule);
  };

  const onNameChange = (name: string) => {
    let rule = new RestTriageRule(triageRule.json);
    rule.name = name;
    setTriageRule(rule);
    onNameChanged(name);
  };

  const onDescriptionChange = (description: string) => {
    let rule = new RestTriageRule(triageRule.json);
    rule.description = description;
    setTriageRule(rule);
  };

  const onApplicationChange = (application: string) => {
    let rule = new RestTriageRule(triageRule.json);
    rule.application = application;
    setTriageRule(rule);
  };

  const onOwnerChange = (e: any) => {
    let rule = new RestTriageRule(triageRule.json);
    rule.owner = e ? e.label : undefined;
    setUserSelect(e || {});
    setTriageRule(rule);
  };

  const validateField = (field: string, value: string) => {
    if (!dirty) {
      return SUCCESS;
    }
    switch (field) {
      case "name":
        if (value !== undefined && value !== null) {
          return "";
        } else {
          return "error";
        }
      // LEV-2105 Remove application validation.
      // case "application":
      // if (!!value && value !== "" && value !== "all" && !validateURL(value)) {
      //   return "error";
      // }
      // return "";
      case "owner":
        if (validateEmail(value)) {
          return "";
        } else {
          return "error";
        }
      default:
        return SUCCESS;
    }
  };

  const helperText = (field: string) => {
    if (!dirty) {
      return "";
    }
    switch (field) {
      case "name":
        return "Name cannot be empty";
      case "application":
        return URL_WARNING;
      case "owner":
        if (triageRule.owner === undefined || triageRule.owner === "") {
          return "Owner cannot be empty";
        }
        return EMAIL_WARNING;
      default:
        return EMPTY_FIELD_WARNING;
    }
  };

  return (
    <Form className={className} layout={"vertical"} style={{ width: width }}>
      {
        // @ts-ignore
        <Form.Item
          label={"Name"}
          validateStatus={validateField("name", triageRule.name)}
          help={validateField("name", triageRule.name) === ERROR && helperText("name")}>
          <Row>
            <Col span={22}>
              <Input value={triageRule.name} onChange={e => onNameChange(e.currentTarget.value)} />
            </Col>
          </Row>
        </Form.Item>
      }
      <Form.Item label={"Description"}>
        <Row>
          <Col span={22}>
            <Input value={triageRule.description} onChange={e => onDescriptionChange(e.currentTarget.value)} />
          </Col>
        </Row>
      </Form.Item>
      <Form.Item
        label={"Owner"}
        validateStatus={validateField("owner", triageRule.owner)}
        help={validateField("owner", triageRule.owner) === ERROR && helperText("owner")}>
        <Row>
          <Col span={22}>
            <SelectRestapi
              placeholder={"Owner"}
              value={userSelect}
              mode={"single"}
              labelInValue={true}
              uri={"users"}
              searchField={"email"}
              onChange={(e: any) => onOwnerChange(e)}
              onBlur={(e: any) => {
                ownerFieldRef.current = true;
              }}
            />
          </Col>
        </Row>
      </Form.Item>
      <Form.Item
        label={"Application"}
        validateStatus={validateField("application", triageRule.application)}
        help={validateField("application", triageRule.application) === ERROR && helperText("application")}>
        <Row>
          <Col span={22}>
            <Input value={triageRule.application} onChange={e => onApplicationChange(e.currentTarget.value)} />
          </Col>
        </Row>
      </Form.Item>
      <Form.Item label={"Regexes"} help={REGEX_HELP}>
        {triageRule.regexes.map((regex: string, index: number) => (
          <Row key={index} type={"flex"} justify={"space-between"} gutter={[10, 10]} align={"middle"}>
            <Col span={22}>
              <Input value={regex} onChange={e => onUpdateRegex(index, e.currentTarget.value)} />
            </Col>
            <Col span={2}>
              <Button type={"link"} icon={"delete"} onClick={e => onDeleteRegex(index)} />
            </Col>
            {index !== triageRule.regexes.length - 1 && (
              <Col span={24}>
                <Text className={"m-10"}>OR</Text>
              </Col>
            )}
          </Row>
        ))}
      </Form.Item>
      <Form.Item>
        <Row justify={"start"}>
          <Col span={22}>
            <Button icon={"plus"} type={"default"} block={true} onClick={e => onAddRegex()}>
              Add Regex
            </Button>
          </Col>
        </Row>
      </Form.Item>
    </Form>
  );
};

export default TriageEditAddComponent;
