import React, { useCallback, useEffect, useRef, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import queryString from "query-string";
import { Checkbox, Collapse, Dropdown, Form, Icon, List, Menu, Modal, notification, Switch } from "antd";
import { FILE_UPLOAD_TYPE } from "../../../constants/fieldTypes";
import { DefaultFieldComponent } from "./default-fields.container";
import Loader from "../../../components/Loader/Loader";
import {
  AntButton,
  AntCol,
  AntInput,
  AntRow,
  AntSelect,
  AntText,
  AntTitle
} from "../../../shared-resources/components";
import { debounce, get, groupBy } from "lodash";
import { SelectRestapi } from "../../../shared-resources/helpers";
import { FieldTypes } from "../../../classes/FieldTypes";
import { ReorderableList } from "../../../shared-resources/containers";
import { TicketField } from "../../containers";
import {
  integrationsSelectorState,
  questionnariesSelectorState,
  SmartTicketSelectorState
} from "reduxConfigs/selectors/restapiSelector";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { useDispatch, useSelector } from "react-redux";
import { getBaseUrl, getSettingsPage, TEMPLATE_ROUTES } from "../../../constants/routePaths";
import { RestSmartTicketTemplate } from "../../../classes/RestSmartTicketTemplate";
import "./smart-ticket-template.style.scss";
import {
  genericList,
  qsGet,
  qsList,
  restapiClear,
  smartTicketTemplatesCreate,
  smartTicketTemplatesGet,
  smartTicketTemplatesUpdate
} from "reduxConfigs/actions/restapi";
import { formClear, formUpdateObj } from "reduxConfigs/actions/formActions";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { getSmartTicketTemplateForm } from "reduxConfigs/selectors/formSelector";
import { checkTemplateNameExists } from "../../../configurations/helpers/checkTemplateNameExits";
import { NAME_EXISTS_ERROR } from "../../../constants/formWarnings";
import { cTemplatesList } from "reduxConfigs/actions/restapi";
import { getTemplateListSelector } from "reduxConfigs/selectors/templatesSelector";
import { restAPILoadingState } from "utils/stateUtil";
import { toTitleCase } from "utils/stringUtils";
import { Link } from "react-router-dom";
import { IntegrationTypes } from "constants/IntegrationTypes";

const { Panel } = Collapse;

const SmartTicketTemplateEditPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const dispatch = useDispatch();

  const values = queryString.parse(props.location.search);

  const templateIdRef = useRef<any>(values.template);
  const slackLoadingRef = useRef<boolean>(true);
  const headerRef = useRef<boolean>(false);
  const checkingNameRef = useRef<boolean>(false);
  const updateBtnStatusRef = useRef<boolean>(false);

  const [loading, setLoading] = useState<boolean>(values.template !== undefined);
  const [questionnaires_loading, setQuestionnairesLoading] = useState<boolean>(false);
  const [slack_available, setSlackAvailable] = useState<boolean>(false);
  const [update_loading, setUpdateLoading] = useState<boolean>(false);
  const [settings_visible, setSettingsVisible] = useState<boolean>(false);
  const [settings_index, setSettingsIndex] = useState<any>();
  const [initial_default, setInitialDefault] = useState<any>();
  const [name_exits, setNameExits] = useState<any>();
  const [notificationEmailList, setNotificationEmailList] = useState<Array<string>>([]);
  const [notificationSlackList, setNotificationSlackList] = useState<Array<string>>([]);
  const [notificationId, setNotificationId] = useState<any>({});

  const sttApiState = useSelector(state => SmartTicketSelectorState(state, templateIdRef.current));
  const integrationsApiState = useSelector(state => integrationsSelectorState(state, "slack"));
  const questionnaireApiState = useSelector(state => questionnariesSelectorState(state));
  const templateList = useSelector(getTemplateListSelector);
  const pageState = useSelector(state => pageSettings(state));
  const sttFormState = useSelector(state => getSmartTicketTemplateForm(state));
  const messageIdRef = useRef<any>(sttFormState.message_template_ids);

  useEffect(() => {
    const values = queryString.parse(props.location.search);
    if (values.template) {
      dispatch(smartTicketTemplatesGet(values.template));
    }
    dispatch(genericList("integrations", "list", { page_size: 100, filter: { application: "slack" } }, null, "slack"));
    setLoading(values.template !== undefined);
    dispatch(cTemplatesList());
    return () => {
      dispatch(restapiClear("ticket_templates", "get", -1));
      dispatch(restapiClear("ticket_templates", "create", 0));
      dispatch(restapiClear("ticket_templates", "update", -1));
      dispatch(formClear("stt_form"));
      dispatch(clearPageSettings(props.location.pathname));
      dispatch(restapiClear("message_templates", "list", -1));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (checkingNameRef.current) {
      const { loading, error } = sttApiState?.list?.["0"];
      if (loading !== undefined && error !== undefined && !loading && !error) {
        const data = sttApiState?.list?.["0"]?.data?.records;
        const prevName = sttFormState.name;
        checkingNameRef.current = false;
        setNameExits(checkTemplateNameExists(prevName, data) ? NAME_EXISTS_ERROR : undefined);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [checkingNameRef.current, sttApiState?.list]);

  useEffect(() => {
    const { loading, error } = restAPILoadingState(templateList);
    const data: any = groupBy(get(templateList, ["0", "data", "records"], []), "type");
    if (Object.keys(data).length > 0 && !loading && !error) {
      setNotificationEmailList(
        (data.EMAIL || [])
          .map((option: any) => {
            if ((sttFormState.message_template_ids || []).includes(option.id)) {
              return option.id;
            }
          })
          .filter((val: any) => val)
      );
      setNotificationSlackList(
        (data.SLACK || [])
          .map((option: any) => {
            if ((sttFormState.message_template_ids || []).includes(option.id)) {
              return option.id;
            }
          })
          .filter((val: any) => val)
      );
      setNotificationId(data);
    }
  }, [templateList, messageIdRef.current]);

  useEffect(() => {
    if (slackLoadingRef.current && integrationsApiState?.list) {
      const { loading, error } = integrationsApiState?.list;
      if (loading !== undefined && error !== undefined && !loading && !error) {
        const data = integrationsApiState?.list?.data;
        const records = data?.records || [];
        const slack = records.find((integration: any) => integration.application === IntegrationTypes.SLACK);
        slackLoadingRef.current = false;
        setSlackAvailable(slack !== undefined);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slackLoadingRef.current, integrationsApiState]);

  useEffect(() => {
    if (loading) {
      const { loading, error } = sttApiState?.get;
      if (loading !== undefined && error !== undefined && !loading && !error) {
        const sttForm = new RestSmartTicketTemplate(sttApiState?.get?.data);
        console.log(1);
        dispatch(formUpdateObj("stt_form", sttForm));
        sttForm.questionnaire_templates.forEach((questionnaire: any) =>
          dispatch(qsGet(questionnaire.questionnaire_template_id))
        );
        messageIdRef.current = sttForm.message_template_ids;
        setLoading(false);
        setQuestionnairesLoading(true);
        setInitialDefault(sttForm.default);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, sttApiState?.get]);

  useEffect(() => {
    if (questionnaires_loading) {
      let allLoading = false;
      let questionnairesSelect: { key: any; label: string }[] = [];
      sttFormState?.questionnaire_templates.forEach((questionnaire: any) => {
        const { loading, error } = questionnaireApiState.get?.[questionnaire.questionnaire_template_id] || {};
        if (loading !== undefined && error !== undefined && !loading && !error) {
          questionnairesSelect.push({
            key: questionnaireApiState.get?.[questionnaire.questionnaire_template_id].data.id,
            label: questionnaireApiState.get?.[questionnaire.questionnaire_template_id].data.name
          });
        } else {
          allLoading = true;
        }
      });
      if (!allLoading) {
        let sttForm = sttFormState;
        sttForm.questionnaires_select = questionnairesSelect;
        console.log(1);
        dispatch(formUpdateObj("stt_form", sttForm));
        setQuestionnairesLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [questionnaires_loading, questionnaireApiState.get]);

  useEffect(() => {
    if (!loading && !questionnaires_loading && sttFormState && !headerRef.current) {
      console.log(1);
      dispatch(
        setPageSettings(props.location.pathname, {
          title: sttFormState.name || "",
          action_buttons: {
            settings: {
              type: "secondary",
              label: "Settings",
              icon: "setting",
              hasClicked: false,
              color: sttFormState.name === "" ? "red" : "black"
            },
            save: {
              type: "primary",
              label: "Save",
              icon: "save",
              hasClicked: false,
              disabled: !sttFormState.valid
            }
          }
        })
      );
      headerRef.current = true;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, questionnaires_loading, sttFormState, headerRef.current]);

  useEffect(() => {
    if (headerRef.current && !updateBtnStatusRef.current) {
      console.log(1);
      dispatch(setPageButtonAction(props.location.pathname, "save", { disabled: !sttFormState.valid }));
      updateBtnStatusRef.current = true;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [headerRef.current, updateBtnStatusRef.current]);

  useEffect(() => {
    if (headerRef.current && pageState && Object.keys(pageState).length > 0) {
      const page = pageState[props.location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page.action_buttons.settings && page.action_buttons.settings.hasClicked === true) {
          console.log(1);
          dispatch(setPageButtonAction(props.location.pathname, "settings", { hasClicked: false }));
          setSettingsVisible(true);
          return;
        }

        if (page.action_buttons.save && page.action_buttons.save.hasClicked === true) {
          if (templateIdRef.current) {
            console.log(1);
            dispatch(smartTicketTemplatesUpdate(templateIdRef.current, sttFormState));
          } else {
            console.log(1);
            dispatch(smartTicketTemplatesCreate(sttFormState));
          }
          console.log(1);
          dispatch(setPageButtonAction(props.location.pathname, "save", { hasClicked: false }));
          setUpdateLoading(true);
          return;
        }
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [headerRef.current, pageState]);

  useEffect(() => {
    if (update_loading) {
      const method = templateIdRef.current ? "update" : "create";
      const { loading, error } = sttApiState?.[method];
      if (!loading && !error) {
        props.history.push(getBaseUrl() + TEMPLATE_ROUTES.ISSUE_TEMPLATE.LIST);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [update_loading, pageState]);

  const onChangeHandler = (field: string) => {
    return (e: any) => {
      let sttForm = sttFormState;
      sttForm[field] = e.target.value;
      console.log(1);
      dispatch(formUpdateObj("stt_form", sttForm));
      if (field === "name") {
        debounceCheckName(e.target.value);
      }
    };
  };

  const onDefaultHandler = (value: any) => {
    let sttForm = sttFormState;
    sttForm.default = value;
    console.log(1);
    dispatch(formUpdateObj("stt_form", sttForm));
  };

  const onEnabledHandler = (value: boolean) => {
    let sttForm = sttFormState;
    if (!value && sttForm.default === true) {
      notification.error({
        message: "Cannot disable Default Issue Template"
      });
    } else {
      sttForm.enabled = value;
      console.log(1);
      dispatch(formUpdateObj("stt_form", sttForm));
    }
  };

  const checkTemplateName = (name = null) => {
    const filters = { filter: { partial: { name: name || sttFormState.name } } };
    console.log(1);
    dispatch(genericList("ticket_templates", "list", filters));
    checkingNameRef.current = true;
  };
  const debounceCheckName = useCallback(
    // preserve the same instance on all re-renders
    debounce(checkTemplateName, 500),
    []
  );
  const onOk = () => {
    setSettingsVisible(false);
    headerRef.current = false;
  };

  const settingsModal = () => {
    const sttForm = sttFormState;
    return (
      <Modal
        title="Settings"
        visible={settings_visible}
        maskClosable={true}
        // icon={"setting"}
        footer={[
          <AntButton key="submit" type="primary" disabled={name_exits} onClick={onOk}>
            Ok
          </AntButton>
        ]}
        cancelButtonProps={{ disabled: true }}
        cancelText={""}
        closable={false}>
        <Form layout={"vertical"}>
          <Form.Item label={"enabled"} colon={false}>
            <Switch disabled={initial_default === true} onChange={onEnabledHandler} checked={sttForm.enabled} />
          </Form.Item>
          <Form.Item label={"default"} colon={false} extra={"Enable template to set default"}>
            <Switch
              disabled={!sttForm.enabled || initial_default === true}
              onChange={onDefaultHandler}
              checked={sttForm.default}
            />
          </Form.Item>
          <Form.Item
            label={"name"}
            colon={false}
            required={true}
            validateStatus={name_exits ? "error" : ""}
            help={<>{name_exits && name_exits}</>}>
            <AntInput placeholder={"Template Name"} value={sttForm.name} onChange={onChangeHandler("name")} />
          </Form.Item>
          <Form.Item label={"description"} colon={false}>
            <AntInput
              placeholder={"Template Description"}
              value={sttForm.description}
              onChange={onChangeHandler("description")}
            />
          </Form.Item>
        </Form>
      </Modal>
    );
  };

  const onQuestionnairesSelect = (value: any) => {
    let sttForm = sttFormState;
    sttForm.questionnaires_select = value;
    console.log(1);
    dispatch(formUpdateObj("stt_form", sttForm));
  };

  const onAddField = (type: string) => {
    let sttForm = sttFormState;
    let fields = sttForm.ticket_fields;
    let newField = new FieldTypes();
    newField.type = type;
    newField.key = `Field ${fields.length + 1}`;
    newField.display_name = `Field ${fields.length + 1}`;
    fields.push(newField);
    sttForm.ticket_fields = fields;
    console.log(1);
    dispatch(formUpdateObj("stt_form", sttForm));
    updateBtnStatusRef.current = false;
  };

  const onNotifyChange = (value: any) => {
    let sttForm = sttFormState;
    let message_template_ids: any = [];
    sttForm.notify_by = { all: value };
    if (!value.includes("EMAIL") && notificationEmailList.length > 0) {
      setNotificationEmailList([]);
      message_template_ids = notificationSlackList;
    }
    if (!value.includes("SLACK") && notificationSlackList.length > 0) {
      setNotificationSlackList([]);
      message_template_ids = notificationEmailList;
    }
    sttForm.message_template_ids = message_template_ids;
    dispatch(formUpdateObj("stt_form", sttForm));
  };

  const templateIdChanges = (value: any, type: string) => {
    let sttForm = sttFormState;
    if (type === "EMAIL") {
      setNotificationEmailList(value);
      value = value.concat(notificationSlackList);
    } else {
      setNotificationSlackList(value);
      value = value.concat(notificationEmailList);
    }
    sttForm.message_template_ids = value;
    dispatch(formUpdateObj("stt_form", sttForm));
  };

  const getNotification = () => {
    const sttForm = sttFormState;
    const options = [
      { label: "EMAIL", value: "EMAIL" },
      { label: "SLACK", value: "SLACK", disabled: !slack_available }
    ];
    const notifyValues = sttForm?.notify_by?.all || [];
    return (
      <AntRow>
        {!slack_available && (
          <AntCol span={24}>
            <AntText>
              Please add Slack Integration to choose it as a notification option. You can add integrations
              <Link to={`${getSettingsPage()}/integrations`}> here</Link>
            </AntText>
          </AntCol>
        )}
        <AntCol span={24}>
          <Checkbox.Group onChange={onNotifyChange} value={notifyValues} options={options} />
          <AntSelect
            id="select-email-notification"
            options={(notificationId?.EMAIL || []).map((option: any) => ({
              label: `${option.name} (${
                option.event_type && typeof option.event_type === "string"
                  ? toTitleCase(option.event_type.replace(/_/g, " "))
                  : ""
              })`,
              value: option.id
            }))}
            onChange={(value: any) => {
              templateIdChanges(value, "EMAIL");
            }}
            value={notificationEmailList || []}
            mode="multiple"
            style={{ width: "100%" }}
            placeholder={"Select Email Notification"}
            disabled={!notifyValues.includes("EMAIL")}
          />
          <AntSelect
            id="select-slack-notification"
            options={(notificationId?.SLACK || []).map((option: any) => ({
              label: `${option.name} (${
                option.event_type && typeof option.event_type === "string"
                  ? toTitleCase(option.event_type.replace(/_/g, " "))
                  : ""
              })`,
              value: option.id
            }))}
            onChange={(value: any) => {
              templateIdChanges(value, "SLACK");
            }}
            value={notificationSlackList || []}
            mode="multiple"
            style={{ width: "100%" }}
            placeholder={"Select Slack Notification"}
            disabled={!slack_available || !notifyValues.includes("SLACK")}
          />
        </AntCol>
      </AntRow>
    );
  };

  const onUpdateDefaultFields = (values: any) => {
    let sttForm = sttFormState;
    sttForm.default_fields = values;
    console.log(1);
    dispatch(formUpdateObj("stt_form", sttForm));
  };

  const onFieldChange = (index: number) => {
    return (field: any) => {
      let sttForm = sttFormState;
      let fields = sttForm.ticket_fields;
      fields[index] = field;
      sttForm.ticket_fields = fields;
      console.log(1);
      dispatch(formUpdateObj("stt_form", sttForm));
      updateBtnStatusRef.current = false;
    };
  };

  const fieldSettingsModal = () => {
    return (
      <Modal
        title={"Field Settings"}
        visible={settings_index !== undefined}
        maskClosable={true}
        // icon={"setting"}
        onOk={e => {
          setSettingsIndex(undefined);
        }}
        cancelButtonProps={{ disabled: true }}
        cancelText={""}
        closable={false}
        destroyOnClose={true}>
        {settings_index !== undefined && (
          <TicketField field={sttFormState.ticket_fields[settings_index]} onChange={onFieldChange(settings_index)} />
        )}
      </Modal>
    );
  };

  const moveRow = (dragIndex: number, hoverIndex: number) => {
    let sttForm = sttFormState;
    let fields = sttForm.ticket_fields;
    let element = sttForm.ticket_fields[dragIndex];

    fields.splice(dragIndex, 1);
    fields.splice(hoverIndex, 0, element);
    sttForm.ticket_fields = fields;
    console.log(1);
    dispatch(formUpdateObj("stt_form", sttForm));
  };

  const onDeleteField = (index: number) => {
    let sttForm = sttFormState;
    let fields = sttForm.ticket_fields;
    if (fields[index].id === undefined) {
      fields.splice(index, 1);
    } else {
      fields[index].deleted = true;
    }
    sttForm.ticket_fields = fields;
    console.log(1);
    dispatch(formUpdateObj("stt_form", sttForm));
  };

  const getFieldIcon = (item: string): string => {
    // @ts-ignore
    return FieldTypes.TYPES[item].icon;
  };

  const getFieldName = (item: string): string => {
    // @ts-ignore
    return FieldTypes.TYPES[item].name;
  };

  if (loading || questionnaires_loading || update_loading) {
    return <Loader />;
  }

  return (
    <div className="overflow-hidden-wrapper flex direction-column h-100 edit-questionaire issue-template-configure-container">
      {settingsModal()}
      <AntRow type={"flex"} justify={"space-between"} style={{ marginBottom: "10px" }}>
        <AntCol>
          <AntTitle level={4}>Configure Template</AntTitle>
          <AntText type={"secondary"}>Add custom fields and assessments to your ticket template</AntText>
        </AntCol>
      </AntRow>
      <AntRow className="h-100 flex-1" gutter={[10, 10]} style={{ minHeight: "32rem", overflowY: "scroll" }}>
        <AntCol className="h-100" span={8}>
          <Collapse defaultActiveKey={["assessments"]} className="collapsible-panel-container">
            <Panel key={"assessments"} header={"Assessments"}>
              <SelectRestapi
                style={{ width: "100%" }}
                placeholder={"Assessments"}
                //rest_api={props.rest_api}
                uri={"questionnaires"}
                method={"list"}
                fetchData={(filters: any) => dispatch(qsList(filters))}
                value={sttFormState.questionnaires_select}
                //value={filter.selected || ''}
                onChange={(value: any) => onQuestionnairesSelect(value)}
                createOption={false}
                mode={"multiple"}
                labelinValue={true}
              />
            </Panel>
            <Panel key={"custom_fields"} header={"Workflow Metadata"}>
              <List
                className="bg-white ant-list-custom issue-template-list"
                //header="Add Field"
                //bordered
                dataSource={Object.keys(FieldTypes.TYPES).filter(type => type !== FILE_UPLOAD_TYPE)}
                renderItem={(item: string, index: number) => (
                  <List.Item key={index} onClick={e => onAddField(item)}>
                    <AntRow justify={"start"} type={"flex"} align={"middle"}>
                      <Icon type={getFieldIcon(item)} style={{ marginRight: "10px", fontSize: "14px" }} />
                      <AntText ellipsis>{getFieldName(item)}</AntText>
                    </AntRow>
                  </List.Item>
                )}
              />
            </Panel>
            <Panel key={"notification"} header={"Notifications"}>
              {getNotification()}
            </Panel>
          </Collapse>
        </AntCol>
        <DefaultFieldComponent
          data={sttFormState.default_fields}
          onUpdateValues={(default_fields: any) => onUpdateDefaultFields(default_fields)}
        />
        <AntCol
          //className="h-100"
          span={16}>
          {settings_index !== undefined && fieldSettingsModal()}
          {/*<DndProvider backend={HTML5Backend}>*/}
          <ReorderableList
            // @ts-ignore
            className="bg-white ant-list-custom issue-template-list"
            header="Add Additional Metadata fields"
            bordered
            onReorder={() => {}}
            dataSource={sttFormState.ticket_fields}
            moveCard={moveRow}
            renderItem={(item: any, index: any) => {
              if (item.deleted === true) {
                return null;
              }
              return (
                <div id={index}>
                  <List.Item
                    key={index}
                    actions={[
                      <Dropdown
                        overlay={
                          <Menu>
                            <Menu.Item key={"settings"} onClick={e => setSettingsIndex(index)}>
                              <Icon type={"setting"} /> Settings
                            </Menu.Item>
                            <Menu.Item key={"delete"} onClick={e => onDeleteField(index)}>
                              <Icon type={"delete"} /> Delete
                            </Menu.Item>
                          </Menu>
                        }
                        placement="bottomRight">
                        <Icon type={"more"} style={{ fontSize: "14px" }} />
                      </Dropdown>
                    ]}>
                    <AntRow type={"flex"} justify={"start"} align={"middle"}>
                      <Icon type={getFieldIcon(item.type)} style={{ marginRight: "10px", fontSize: "14px" }} />
                      <AntText ellipsis>{item.display_name}</AntText>
                      {!item.valid && (
                        <Icon
                          type="exclamation-circle"
                          theme={"filled"}
                          style={{ paddingLeft: "10px", color: "red", fontSize: "14px" }}
                        />
                      )}
                    </AntRow>
                  </List.Item>
                </div>
              );
            }}
          />
        </AntCol>
      </AntRow>
    </div>
  );
};

export default SmartTicketTemplateEditPage;
// npm run build
