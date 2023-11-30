import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { AntCard, AntFormItem, AntInput, AntSwitch, Tagtip } from "shared-resources/components";
import ErrorWrapper from "hoc/errorWrapper";
import "./templates-add.style.scss";
import { RestCommTemplate } from "classes/RestCommTemplate";
import Loader from "components/Loader/Loader";
import { Col, Form, Row, Select } from "antd";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { debounce, get } from "lodash";
import { checkTemplateNameExists } from "../../../helpers/checkTemplateNameExits";
import { NAME_EXISTS_ERROR, REQUIRED_FIELD } from "constants/formWarnings";
import { parseQueryParamsIntoKeys } from "utils/queryUtils";
import { RouteComponentProps } from "react-router-dom";
import { getTemplateForm } from "reduxConfigs/selectors/formSelector";
import {
  getTemplateCreateSelector,
  getTemplateGetSelector,
  getTemplateListSelector,
  getTemplateUpdateSelector
} from "reduxConfigs/selectors/templatesSelector";
import { formClear, formInitialize, formUpdateField, formUpdateObj } from "reduxConfigs/actions/formActions";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { cTemplatesCreate, cTemplatesGet, cTemplatesUdpate, genericList } from "reduxConfigs/actions/restapi";
import { restAPILoadingState } from "utils/stateUtil";

const validateForm = (form: any) => {
  if (!form.type) {
    return true;
  } else {
    if (form.type === "EMAIL") {
      return !form.name || !form.email_subject || !form.message;
    } else if (form.type === "SLACK") {
      return !form.name || !form.bot_name || !form.message;
    }
  }
};

interface TemplatesAddPageProps extends RouteComponentProps {
  className?: string;
  form?: any;
}

export const TemplatesAddPage: React.FC<TemplatesAddPageProps> = (props: TemplatesAddPageProps) => {
  const [template, setTemplate] = useState<RestCommTemplate | undefined>(undefined);

  const TEMPLATE_FORM_NAME = "template_form";

  let templateId: undefined | string = undefined;
  let isEditMode = false;
  const { pathname } = props.location;
  if (pathname.includes("edit")) {
    const { template } = parseQueryParamsIntoKeys(props.location.search, ["template"]);
    if (template) {
      templateId = template[0];
    }
    isEditMode = !!templateId;
  }

  const [loading, setLoading] = useState(isEditMode);
  const [create_loading, setCreateLoading] = useState(false);
  const [created, setCreated] = useState(false);
  const [set_header, setHeader] = useState(false);
  const [update_btn_status, setUpdateBtnStatus] = useState(false);
  const [checking_name, setCheckingName] = useState(false);
  const [nameExists, setNameExists] = useState<any>(undefined);

  const dispatch = useDispatch();
  const templateForm = useSelector(getTemplateForm);
  const pageSettingsState = useSelector(pageSettings);
  const templateCreate = useSelector(getTemplateCreateSelector);
  const templateUpdate = useSelector(getTemplateUpdateSelector);
  const templateGet = useSelector(getTemplateGetSelector);
  const templateList = useSelector(getTemplateListSelector);

  const debounceCheckName = debounce(() => checkTemplateName(), 500);

  useEffect(() => {
    dispatch(formInitialize(TEMPLATE_FORM_NAME, {}));
    if (isEditMode) {
      dispatch(cTemplatesGet(templateId));
    }
    return () => {
      dispatch(formClear(TEMPLATE_FORM_NAME));
      dispatch(restapiClear("ctemplates", "get", -1));
      dispatch(clearPageSettings(pathname));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const addBtnClicked = get(pageSettingsState, [pathname, "action_buttons", "create_update", "hasClicked"], false);
    if (addBtnClicked) {
      let newTemplate = new RestCommTemplate(templateForm);
      if (template) {
        newTemplate.system = template.system;
      }
      setCreateLoading(true);
      if (isEditMode) {
        console.log(newTemplate);
        console.log(newTemplate.json());
        dispatch(cTemplatesUdpate(templateId, newTemplate));
      } else {
        dispatch(cTemplatesCreate(newTemplate));
      }
      dispatch(setPageButtonAction(pathname, "create_update", { hasClicked: false }));
    }
  }, [pageSettingsState]); // eslint-disable-line react-hooks/exhaustive-deps

  // eslint-disable-next-line
  useEffect(() => {
    if (loading && templateId !== undefined) {
      const { loading, error } = restAPILoadingState(templateGet, templateId);
      if (!loading && !error) {
        const template = new RestCommTemplate(get(templateGet, [templateId, "data"], {}));
        setTemplate(template);
        dispatch(formUpdateField(TEMPLATE_FORM_NAME, "name", template.name));
        dispatch(formUpdateField(TEMPLATE_FORM_NAME, "type", template.type));
        dispatch(formUpdateField(TEMPLATE_FORM_NAME, "bot_name", template.botname));
        dispatch(formUpdateField(TEMPLATE_FORM_NAME, "message", template.message));
        dispatch(formUpdateField(TEMPLATE_FORM_NAME, "email_subject", template.email_subject));
        dispatch(formUpdateField(TEMPLATE_FORM_NAME, "default", template.default));
        dispatch(formUpdateField(TEMPLATE_FORM_NAME, "event_type", template.event_type));
        setLoading(false);
      }
    }

    if (!loading && !create_loading && !set_header) {
      dispatch(
        setPageSettings(pathname, {
          title: "Templates",
          action_buttons: {
            create_update: {
              type: "primary",
              label: isEditMode ? "Update Template" : "Create Template",
              hasClicked: false
            }
          }
        })
      );
      setHeader(true);
    }

    if (set_header && !update_btn_status) {
      const btnStatus = validateForm(templateForm);
      dispatch(setPageButtonAction(pathname, "create_update", { disabled: btnStatus || nameExists }));
      setUpdateBtnStatus(true);
    }

    if (create_loading) {
      let method = !isEditMode ? "create" : "update";
      const id = templateId || "0";
      const { loading, error } = restAPILoadingState(!isEditMode ? templateCreate : templateUpdate, id);
      if (!loading) {
        if (error) {
          setCreated(false);
        } else {
          setCreated(true);
          dispatch(formClear(TEMPLATE_FORM_NAME));
        }
        setCreateLoading(false);
        dispatch(restapiClear("ctemplates", method, -1));
      }
    }
  });

  useEffect(() => {
    if (checking_name) {
      const { loading, error } = restAPILoadingState(templateList);
      if (!loading && !error) {
        const data = get(templateList, ["0", "data", "records"], []);
        const prevName = templateForm.name;
        setCheckingName(false);
        setNameExists(checkTemplateNameExists(prevName, data) ? NAME_EXISTS_ERROR : undefined);
        validateForm(templateForm);
      }
    }
  }, [checking_name, templateList]); // eslint-disable-line react-hooks/exhaustive-deps

  const onFieldChangeHandler = (field: string) => {
    return (e: any) => {
      const template = Object.assign(Object.create(Object.getPrototypeOf(templateForm)), templateForm);
      template[field] = e.target ? e.target.value : e;
      update_btn_status && setUpdateBtnStatus(false);
      dispatch(formUpdateObj(TEMPLATE_FORM_NAME, template));
      if (field === "name") {
        debounceCheckName();
      }
    };
  };

  const handleDefaultToggle = (e: any) => {
    const currentValue = templateForm["default"];
    const template = Object.assign(Object.create(Object.getPrototypeOf(templateForm)), templateForm);
    template["default"] = !currentValue;
    dispatch(formUpdateObj(TEMPLATE_FORM_NAME, template));
    update_btn_status && setUpdateBtnStatus(false);
  };

  const checkTemplateName = () => {
    const filters = {
      filter: {
        partial: {
          name: templateForm.name
        }
      }
    };
    dispatch(genericList("ctemplates", "list", filters));
    setCheckingName(true);
  };

  if (created) {
    props.history.push(getBaseUrl() + TEMPLATE_ROUTES.COMMUNICATION_TEMPLATES.LIST);
  }

  const { className } = props;
  const { getFieldDecorator, getFieldError, isFieldTouched } = props.form;
  const { Option } = Select;

  if (loading || create_loading) {
    return <Loader />;
  }

  const tokens = [
    { name: "$link ", description: "Assessment link" },
    { name: "$sender", description: "Email of the sender" },
    { name: "$info", description: "Additional information" },
    { name: "$artifact", description: "Artifact" },
    { name: "$title", description: "Title of the item" },
    { name: "$text", description: "Body of the item" },
    { name: "$issue_id", description: "ID of Issue" },
    { name: "$issue_title", description: "Title of the Issue" },
    { name: "$issue_link", description: "Link to the Issue" },
    { name: "$assessment_id", description: "ID of the Assessment" },
    { name: "$assessment_title", description: "Title of the Assessment" },
    { name: "$assessment_link", description: "Link to the Assessment" }
  ];

  return (
    <div className={"flex direction-column align-center"}>
      <div className={`${className}__content`}>
        <AntCard title="Template">
          <Form layout="vertical">
            <AntFormItem label="type" required>
              <Select
                value={templateForm.type}
                disabled={!!templateForm.default}
                style={{ width: "30%" }}
                onChange={onFieldChangeHandler("type")}>
                <Option key="SLACK">Slack</Option>
                <Option key="EMAIL">Email</Option>
              </Select>
            </AntFormItem>
            <AntFormItem
              label={"Name"}
              validateStatus={(isFieldTouched("name") && getFieldError("name")) || nameExists ? "error" : ""}
              help={
                <>
                  {nameExists && nameExists}
                  {isFieldTouched("name") && getFieldError("name") && REQUIRED_FIELD}
                </>
              }>
              {getFieldDecorator("name", {
                initialValue: templateForm.name || "",
                validateTrigger: "onBlur",
                rules: [
                  {
                    required: true,
                    message: "This field cannot be empty"
                  }
                ]
              })(<AntInput onChange={onFieldChangeHandler("name")} />)}
            </AntFormItem>
            {templateForm.type === "SLACK" && (
              <AntFormItem
                label={"Bot Name"}
                validateStatus={isFieldTouched("bot_name") && getFieldError("bot_name") ? "error" : ""}>
                {getFieldDecorator("bot_name", {
                  initialValue: templateForm.bot_name || "",
                  validateTrigger: "onBlur",
                  rules: [
                    {
                      required: true,
                      message: "This field cannot be empty"
                    }
                  ]
                })(<AntInput onChange={onFieldChangeHandler("bot_name")} />)}
              </AntFormItem>
            )}
            {templateForm.type === "EMAIL" && (
              <AntFormItem
                label={"Subject"}
                validateStatus={isFieldTouched("subject") && getFieldError("subject") ? "error" : ""}>
                {getFieldDecorator("subject", {
                  initialValue: templateForm.email_subject || "",
                  validateTrigger: "onBlur",
                  rules: [
                    {
                      required: true,
                      message: "This field cannot be empty"
                    }
                  ]
                })(<AntInput onChange={onFieldChangeHandler("email_subject")} />)}
              </AntFormItem>
            )}
            <AntFormItem label={"Event Type"} required={true}>
              <Select
                value={templateForm.event_type}
                style={{ width: "30%" }}
                onChange={onFieldChangeHandler("event_type")}>
                <Option key="all">All</Option>
                <Option key="smart_ticket_created">Smart Ticket Created</Option>
                <Option key="smart_ticket_new_assignee">Smart Ticket New Assignee</Option>
                <Option key="assessment_submitted">Assessment Submitted</Option>
                <Option key="assessment_created">Assessment Created</Option>
                <Option key={"assessment_notified"}>Assessment Notifi</Option>
              </Select>
            </AntFormItem>
            <AntFormItem label={"Default"}>
              <AntSwitch id="default" checked={templateForm.default} onChange={handleDefaultToggle} />
            </AntFormItem>
            <AntFormItem
              label={"Message"}
              validateStatus={isFieldTouched("message") && getFieldError("message") ? "error" : ""}>
              {getFieldDecorator("message", {
                initialValue: templateForm.message || "",
                validateTrigger: "onBlur",
                rules: [
                  {
                    required: true,
                    message: "This field cannot be empty"
                  }
                ]
              })(<AntInput type="textarea" onChange={onFieldChangeHandler("message")} />)}
            </AntFormItem>
            <div className="tokens">
              <span className="tokenLabel">{"Tokens allowed: "}</span>
              <Row type={"flex"} justify={"start"} gutter={[10, 10]}>
                {tokens.map(token => (
                  <Col span={6}>
                    <Tagtip tagText={token.name} tooltipText={token.description} />
                  </Col>
                ))}
              </Row>
            </div>
            <br />
          </Form>
        </AntCard>
      </div>
    </div>
  );
};

const TemplatesAddForm = Form.create({ name: "templates_add" })(TemplatesAddPage);

export default ErrorWrapper(TemplatesAddForm);
