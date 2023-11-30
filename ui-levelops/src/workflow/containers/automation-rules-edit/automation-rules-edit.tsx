import React, { useEffect, useRef, useState, useMemo } from "react";
import { Col, Form, Input, Row } from "antd";
import { get } from "lodash";
import { useSelector } from "react-redux";
import { v1 as uuid } from "uuid";

import { validateEmail } from "utils/stringUtils";
import { EMAIL_WARNING, EMPTY_FIELD_WARNING, ERROR, SUCCESS } from "constants/formWarnings";
import { SelectRestapi } from "shared-resources/helpers";
import { AntCard, AntText } from "shared-resources/components";
import { getObjectsListSelector } from "reduxConfigs/selectors/objectsSelector";
import AutomationRulesCriteria from "./automation-rules-criteria";
import { usePrevious } from "shared-resources/hooks/usePrevious";

import "./automation-rules-edit.style.scss";

interface AutomationRulesEditContainerProps {
  automationRuleForm: any;
  formUpdateField: (rule: any) => void;
  onApplicationChange: (application: any) => void;
  dirty: boolean;
  width?: string;
  className?: string;
}

const AutomationRulesEditContainer: React.FC<AutomationRulesEditContainerProps> = ({
  automationRuleForm,
  formUpdateField,
  onApplicationChange,
  dirty,
  width = "100%",
  className
}) => {
  const ownerFieldRef = useRef<any>(undefined);
  const applicationFieldRef = useRef<any>(undefined);
  const [userSelect, setUserSelect] = useState<any>(undefined);
  const [applicationSelect, setApplicationSelect] = useState<any>(undefined);
  const [applicationObj, setApplicationObj] = useState(undefined);

  const objectListState = useSelector(getObjectsListSelector);
  const randomUUID = useMemo(() => uuid(), []);

  const prevApplicationObj = usePrevious(applicationObj);

  useEffect(
    () => {
      if (automationRuleForm.owner !== userSelect?.label) {
        setUserSelect({ key: automationRuleForm.owner, label: automationRuleForm.owner });
      }
      if (automationRuleForm.object_type !== applicationSelect?.label) {
        setApplicationSelect({ key: automationRuleForm.object_type, label: automationRuleForm.object_type });
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [automationRuleForm]
  );

  useEffect(
    () => {
      if (automationRuleForm.object_type && automationRuleForm.object_type !== prevApplicationObj) {
        const applications = get(objectListState, [randomUUID, "data", "records"]);
        if (applications) {
          const application = applications.find(
            (application: any) => application.id === automationRuleForm.object_type
          );
          setApplicationObj(application);
        }
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [automationRuleForm, objectListState]
  );

  const nameChangeHandler = (name: string) => {
    const rule = automationRuleForm;
    rule.name = name;
    formUpdateField(rule);
  };

  const descriptionChangeHandler = (description: string) => {
    const rule = automationRuleForm;
    rule.description = description;
    formUpdateField(rule);
  };

  const applicationChangeHandler = (value: any) => {
    const rule = automationRuleForm;
    value = value || { key: "", label: "" };
    const objType = value.key;
    rule.object_type = objType;
    setApplicationSelect(value);

    const applications = get(objectListState, [randomUUID, "data", "records"]);
    const application = applications.find((application: any) => application.id === objType);
    setApplicationObj(application);
    // onApplicationChange(application);
    formUpdateField(rule);
  };

  const ownerChangeHandler = (value: any) => {
    const rule = automationRuleForm;
    value = value || { key: "", label: "" };
    rule.owner = value.label;
    setUserSelect(value);
    formUpdateField(rule);
  };

  const criteriaChangeHandler = (value: any[]) => {
    const rule = automationRuleForm;
    value = value || [];
    rule.critereas = value;
    formUpdateField(rule);
  };

  const validateField = (field: string, value: string) => {
    if (!dirty) {
      return SUCCESS;
    }
    switch (field) {
      case "name":
        if (!value) {
          return ERROR;
        }
        return "";
      case "application":
        if (!value) {
          return ERROR;
        }
        return "";
      case "owner":
        if (validateEmail(value)) {
          return "";
        } else {
          return ERROR;
        }
      case "field_name": {
        if (value !== undefined && value !== null && value) {
          return "";
        }
        return ERROR;
      }
      case "regexes": {
        if (Array.isArray(value) && value.length) {
          for (let str of value) {
            if (!str) {
              return ERROR;
            }
          }
          return "";
        } else {
          return ERROR;
        }
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
        return "Application cannot be empty";
      case "owner":
        if (automationRuleForm.owner === undefined || automationRuleForm.owner === "") {
          return "Owner cannot be empty";
        }
        return EMAIL_WARNING;
      case "field_name": {
        return "Field Type cannot be empty";
      }
      case "regexes": {
        return "Regular Expression cannot be empty";
      }
      default:
        return EMPTY_FIELD_WARNING;
    }
  };

  return (
    <AntCard style={{ width: width, height: "500px" }} className="automation-rules-edit">
      <Form className={className} layout={"vertical"} style={{}}>
        <Row type={"flex"} justify={"center"} style={{ width: "100%" }}>
          <Col style={{ width: "40%" }}>
            <Row>
              <Col className="d-flex align-center">
                <AntText strong style={{ fontSize: "16px", textTransform: "uppercase", marginBottom: "1rem" }}>
                  Basic Info
                </AntText>
              </Col>
            </Row>
            <Form.Item
              label={"Name"}
              required
              validateStatus={validateField("name", automationRuleForm.name)}
              help={validateField("name", automationRuleForm.name) === ERROR && helperText("name")}>
              <Row>
                <Col>
                  <Input value={automationRuleForm.name} onChange={e => nameChangeHandler(e.currentTarget.value)} />
                </Col>
              </Row>
            </Form.Item>
            <Form.Item
              label={"Application"}
              required
              validateStatus={validateField("application", automationRuleForm.object_type)}
              help={
                validateField("application", automationRuleForm.object_type) === ERROR && helperText("application")
              }>
              <Row>
                <Col>
                  <SelectRestapi
                    placeholder={"Application"}
                    value={applicationSelect}
                    mode={"single"}
                    labelInValue={true}
                    uri={"objects"}
                    searchField={"name"}
                    uuid={randomUUID}
                    onChange={(e: any) => applicationChangeHandler(e)}
                    onBlur={(e: any) => {
                      applicationFieldRef.current = true;
                    }}
                  />
                </Col>
              </Row>
            </Form.Item>
            <Form.Item label={"Description"}>
              <Row>
                <Col>
                  <Input.TextArea
                    value={automationRuleForm.description}
                    onChange={e => descriptionChangeHandler(e.currentTarget.value)}
                    autoSize={{ minRows: 2, maxRows: 4 }}
                  />
                </Col>
              </Row>
            </Form.Item>
            <Form.Item
              label={"Owner"}
              required
              validateStatus={validateField("owner", automationRuleForm.owner)}
              help={validateField("owner", automationRuleForm.owner) === ERROR && helperText("owner")}>
              <Row>
                <Col>
                  <SelectRestapi
                    placeholder={"Owner"}
                    value={userSelect}
                    mode={"single"}
                    labelInValue={true}
                    uri={"users"}
                    searchField={"email"}
                    onChange={(e: any) => ownerChangeHandler(e)}
                    onBlur={(e: any) => {
                      ownerFieldRef.current = true;
                    }}
                  />
                </Col>
              </Row>
            </Form.Item>
          </Col>
          <Col className="scolling-column" style={{ width: "60%", height: "100%" }}>
            {
              // @ts-ignore
              applicationObj && Array.isArray(applicationObj.fields) ? (
                <AutomationRulesCriteria
                  // @ts-ignore
                  applicationFields={applicationObj ? applicationObj.fields : []}
                  // @ts-ignore
                  selectedApplicationId={automationRuleForm.object_type}
                  value={automationRuleForm.critereas}
                  validateField={validateField}
                  helperText={helperText}
                  onChange={criteriaChangeHandler}
                />
              ) : (
                <React.Fragment>
                  <Row style={{ textAlign: "left", marginBottom: "1rem" }}>
                    <AntText strong style={{ fontSize: "16px", textTransform: "uppercase", marginBottom: "1rem" }}>
                      Match Conditions
                    </AntText>
                  </Row>
                  <Row style={{ textAlign: "left" }}>
                    <div>Select an application</div>
                  </Row>
                </React.Fragment>
              )
            }
          </Col>
        </Row>
      </Form>
    </AntCard>
  );
};

export default AutomationRulesEditContainer;
