import { actions } from "@mrblenny/react-flow-chart";
import { Affix, Button, Col, Form, Icon, Input, Modal, notification, Popconfirm, Radio, Row, Tooltip } from "antd";
import { RadioChangeEvent } from "antd/lib/radio";
import { RestPropel } from "classes/RestPropel";
import Loader from "components/Loader/Loader";
import ConfirmationWrapper, { ConfirmationWrapperProps } from "hoc/confirmationWrapper";
import { cloneDeep, concat, debounce, difference, filter, get, map, reduce, set } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { usersSelector } from "reduxConfigs/selectors/usersSelector";
import { AntText, AntTitle, PillBoxZoom } from "shared-resources/components";
import { FlowChart } from "shared-resources/helpers";
import { validateEmail } from "utils/stringUtils";
import { v1 as uuid } from "uuid";
import {
  CanvasInnerCustom,
  CustomLink,
  CustomNode,
  CustomPort,
  ExportModal,
  GenericNewNode,
  GenericNode,
  LinkNode,
  NodeInner,
  TriggerNode
} from "workflow/components";
import NotifyFailModal, { NotifyFailType } from "workflow/components/notify-fail-modal/NotifyFailModal";
import { formClear, formUpdateObj } from "reduxConfigs/actions/formActions";
import { setPageSettings, setPageSwitchAction } from "reduxConfigs/actions/pagesettings.actions";
import { propelFetch, propelNew } from "reduxConfigs/actions/propelLoad.actions";
import { prepelsCreate, prepelsUpdate, usersGetOrCreate } from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { getPropelForm } from "reduxConfigs/selectors/formSelector";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { getPropelCreateSelector, getPropelUpdateSelector } from "reduxConfigs/selectors/propels.selectors";
import { getPropelNodeTemplateListSelector } from "reduxConfigs/selectors/propel_node_template.selectors";
import { getPropelTriggerTemplateListSelector } from "reduxConfigs/selectors/propel_trigger_template.selectors";
import { parseQueryParamsIntoKeys } from "../../../utils/queryUtils";
import { restAPILoadingState } from "../../../utils/stateUtil";
import "./workflow-editor.style.scss";
import StringsEn from "../../../locales/StringsEn";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, PropelsActions } from "dataTracking/analytics.constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { getBaseUrl } from "constants/routePaths";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

function getOffset(config: any, data: any, zoom: number = 0) {
  let offset = { x: data.x, y: data.y };
  if (config && config.snapToGrid) {
    offset = {
      x: Math.round(data.x / 20) * 20,
      y: Math.round(data.y / 20) * 20
    };
  }
  if (!!zoom) {
    offset.x = offset.x / zoom;
    offset.y = offset.y / zoom;
  }
  return offset;
}

interface WorkflowEditorPageProps extends RouteComponentProps, ConfirmationWrapperProps {
  className?: string;
}

const WorkflowEditorPage: React.FC<WorkflowEditorPageProps> = (props: WorkflowEditorPageProps) => {
  const PROPEL_FORM_NAME = "propel_form";

  const { setDirty, location, bindSaveAction, onSaveActionComplete } = props;
  const { trigger, propel } = parseQueryParamsIntoKeys(location.search, ["trigger", "propel"]);

  let propelId: undefined | string = undefined;
  let triggerId: string | undefined = undefined;
  let isEditMode = false;

  if (propel) {
    propelId = propel[0];

    // Tracking propel id in new relic
    if ("newrelic" in window) {
      (window as any).newrelic.setCustomAttribute("PropelId", propelId);
    }
  }
  if (trigger) {
    triggerId = trigger[0];
  }
  isEditMode = !!propelId;

  const [loading, setLoading] = useState(isEditMode);
  const [random, setRandom] = useState<string | undefined>(undefined);
  const [propel_id, setPropelId] = useState<string | undefined>(propelId);
  const [drawer_visible, setDrawerVisibility] = useState(false);
  const [drawer_type, setDrawerType] = useState<string | undefined>("node");
  const [selected_node, setSelectedNode] = useState<undefined | string>(undefined);
  const [selected_link, setSelectedLink] = useState<string | undefined>(undefined);
  const [first_drawer, setFirstDrawer] = useState(false);
  const [create_loading, setCreateLoading] = useState(false);
  const [show_errors, setShowErrors] = useState(false);
  const [reload, setReload] = useState(1);
  const [export_visible, setExportVisibility] = useState(false);
  const [settings_modal_visible, setSettingsModalVisibility] = useState(false);
  const [confirmation_modal_visible, setConfirmationModalVisibility] = useState(false);
  const [header, setHeader] = useState(false);
  const [container, setContainer] = useState(null);
  const [notifyFailModal, setNotifyFailModal] = useState<boolean>(false);
  const [creatingUsers, setCreatingUsers] = useState<boolean>(false);

  const dispatch = useDispatch();

  const pageSettingsState = useSelector(pageSettings);
  const propelForm = useSelector(getPropelForm);
  const propelCreateState = useSelector(getPropelCreateSelector);
  const propelUpdateState = useSelector(getPropelUpdateSelector);
  const propelNodeTemplateListState = useSelector(getPropelNodeTemplateListSelector);
  const propelTriggerListState = useSelector(getPropelTriggerTemplateListSelector);
  const usersRestState = useSelector(usersSelector);

  const entPropelsAddCountExceed = useHasEntitlements(Entitlement.PROPELS_COUNT_5, EntitlementCheckType.AND);

  useEffect(() => {
    setLoading(true);
    if (triggerId) {
      setDirty(true);
    }
    setRandom(uuid());

    dispatch(formClear(PROPEL_FORM_NAME));
    if (propel_id) {
      dispatch(propelFetch(propel_id));
    }
    if (triggerId) {
      dispatch(propelNew(triggerId));
    }

    return () => {
      dispatch(formClear(PROPEL_FORM_NAME));
      dispatch(restapiClear("propel_node_templates", "list", 0));
      dispatch(restapiClear("propel_node_categories", "get", "list"));
      dispatch(restapiClear("propel_trigger_templates", "list", 0));
      dispatch(restapiClear("configs", "list", "0"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (propel !== undefined && propelId !== propel_id) {
      setPropelId(propelId);
      dispatch(propelFetch(propelId));
    }
  }, [propelId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (loading && propelForm !== undefined) {
      dispatch(setPageSwitchAction(location.pathname, "enabled", { checked: propelForm.enabled }));
      dispatch(
        setPageSwitchAction(location.pathname, "notify_fail", {
          checked: !!propelForm.settings?.notifications?.enabled
        })
      );
      setLoading(false);
    }
  }, [propelForm]); // eslint-disable-line react-hooks/exhaustive-deps

  const [createAccess, editAccess] = useConfigScreenPermissions();
  const hasAccess = useMemo(() => (propel_id ? editAccess : createAccess), [propel_id]);

  // eslint-disable-next-line
  useEffect(() => {
    if (propelForm && !first_drawer && propel_id === undefined) {
      if (propelForm.nodes.hasOwnProperty("0")) {
        setDrawerType("edit_trigger");
        setFirstDrawer(true);
        setSelectedNode("0");
        setDrawerVisibility(true);
      }
    }

    if (!loading && !create_loading && !header && propelForm !== undefined) {
      dispatch(
        setPageSettings(props.location.pathname, {
          title: StringsEn.propelsEditor,
          select_buttons: {
            enabled: {
              label: "Enabled",
              checked: propelForm.enabled,
              hasToggled: false
            },
            notify_fail: {
              label: "Notify On Fail",
              checked: !!propelForm.settings?.notifications?.enabled,
              hasToggled: false
            }
          }
        })
      );
      setHeader(true);
    }

    if (create_loading) {
      const method = isEditMode ? "update" : "create";
      const id = propel_id || "0";
      const { loading, error } = restAPILoadingState(!isEditMode ? propelCreateState : propelUpdateState, id);
      if (!loading) {
        if (!error) {
          if (!creatingUsers) {
            const data = get(!isEditMode ? propelCreateState : propelUpdateState, [id, "data"], {});
            const newId = data.permanent_id;
            notification.info({
              message: "Propel Saved Successfully"
            });
            props.history.push(`${getBaseUrl()}/propels/propelseditor?propel=${newId}`);
            onSaveActionComplete && onSaveActionComplete(false);
            setCreateLoading(false);
            dispatch(restapiClear("propels", method, -1));
          }
        } else {
          onSaveActionComplete && onSaveActionComplete(true);
          notification.error({
            message: StringsEn.propelCouldnotSave
          });
          setDirty(true);
          setCreateLoading(false);
          dispatch(restapiClear("propels", method, -1));
        }
      }
    }

    if (creatingUsers) {
      const loading = get(usersRestState, ["getOrCreate", 0, "loading"], true);
      const error = get(usersRestState, ["getOrCreate", 0, "error"], false);
      if (!loading && !error) {
        const newUsers = get(usersRestState, ["getOrCreate", 0, "data"], []);
        dispatch(restapiClear("users", "getOrCreate", "-1"));
        const recipients = get(propelForm, ["settings", "notifications", "recipients"], []);
        const newRecipients = concat(
          filter(recipients, (recipient: any) => !recipient?.includes("create:")),
          map(newUsers, (user: any) => user.email)
        );
        set(propelForm, ["settings", "notifications", "recipients"], newRecipients);
        setCreatingUsers(false);
        if (propel_id) {
          setCreateLoading(true);
          setDirty(false);
          dispatch(prepelsUpdate(propel_id, propelForm));
        } else {
          setCreateLoading(true);
          setDirty(false);
          dispatch(prepelsCreate(propelForm));
        }
      }
    }
  });

  useEffect(() => {
    const hasToggled = get(pageSettingsState, [location.pathname, "select_buttons", "enabled", "hasToggled"], false);
    const hasNotifyFailToggled = get(
      pageSettingsState,
      [location.pathname, "select_buttons", "notify_fail", "hasToggled"],
      false
    );
    if (hasToggled) {
      propelForm.enabled = get(pageSettingsState, [location.pathname, "select_buttons", "enabled", "checked"], false);
      dispatch(setPageSwitchAction(location.pathname, "enabled", { hasToggled: false }));
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
    }
    if (hasNotifyFailToggled) {
      const checked = get(pageSettingsState, [location.pathname, "select_buttons", "notify_fail", "checked"], false);
      dispatch(setPageSwitchAction(location.pathname, "notify_fail", { hasToggled: false }));
      if (checked) {
        setNotifyFailModal(true);
        return;
      }
      propelForm.settings = {
        ...(propelForm?.settings || {}),
        notifications: { ...(propelForm?.settings?.notifications || {}), enabled: false }
      };
    }
  }, [pageSettingsState]); // eslint-disable-line react-hooks/exhaustive-deps

  const onDragCanvas = useCallback(
    ({ config, data }: any) => {
      if (!data) {
        return;
      }
      const chart = propelForm;
      chart.offset = getOffset(config, { x: data.positionX, y: data.positionY });
      chart.reload = chart.reload + 1;
      debounce(() => dispatch(formUpdateObj(PROPEL_FORM_NAME, chart)), 50);
      setDirty(true);
    },
    [propelForm]
  );

  const onSettingsClick = useCallback((nodeId: any) => {
    setDrawerType(nodeId === "0" ? "edit_trigger" : "edit_node");
    setSelectedNode(nodeId);
    setDrawerVisibility(true);
  }, []);

  const onUpdateNodeProperties = useCallback(
    (nodeId: any, values: any) => {
      let chart = propelForm;

      Object.keys(chart.nodes[nodeId].input).forEach(field => {
        if (values[field]) {
          chart.nodes[nodeId].input[field].values = values[field].value;
        }
      });
      chart.updateOutputContentTypes(nodeId);
      chart.reload = chart.reload + 1;
      chart.nodes_dirty = chart.nodes_dirty || nodeId !== "0";
      setDrawerVisibility(false);
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const onUpdateNodeName = useCallback(
    (nodeId: any, name: string) => {
      let chart = propelForm;
      chart.nodes_dirty = chart.nodes_dirty || nodeId !== "0";
      chart.nodes[nodeId].name = name;
      chart.reload = chart.reload + 1;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const onLinkStart = useCallback(
    (props: any) => {
      let chart = propelForm;
      chart.links[props.linkId] = {
        id: props.linkId,
        from: {
          nodeId: props.fromNodeId,
          portId: props.fromPortId
        },
        to: {}
      };
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
      return actions.onLinkStart(props);
    },
    [propelForm]
  );

  const onLinkCancel = useCallback(
    (props: any) => {
      let chart = propelForm;
      let links = chart.links;
      delete links[props.linkId];
      chart.links = links;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
      return actions.onLinkCancel(props);
    },
    [propelForm]
  );

  const onLinkComplete = useCallback(
    (props: any) => {
      const { linkId, fromNodeId, fromPortId, toNodeId, toPortId, config = {} } = props;
      let chart = propelForm;

      // validation
      if (fromPortId === toPortId) {
        notification.error({
          message: "Cannot add link",
          description: "Links can only connect input to output ports"
        });
        delete chart.links[linkId];
      } else {
        // fromPortId needs to be output, otherwise flip from and to
        const fromNode = fromPortId === "output" ? fromNodeId : toNodeId;
        const linkOptions = chart.nodes[fromNode].options;
        const linkObj = {
          id: linkId,
          from:
            fromPortId === "output"
              ? {
                  nodeId: fromNodeId,
                  portId: fromPortId
                }
              : {
                  nodeId: toNodeId,
                  portId: toPortId
                },
          to:
            fromPortId === "output"
              ? {
                  nodeId: toNodeId,
                  portId: toPortId
                }
              : {
                  nodeId: fromNodeId,
                  portId: fromPortId
                },
          properties: {
            option: linkOptions && linkOptions.length > 0 ? linkOptions[0] : undefined
          }
        };
        chart.links[linkId] = linkObj;
      }

      if (chart.links[linkId] && chart.links[linkId].to && chart.links[linkId].to.nodeId) {
        setDrawerType("edit_node");
        setSelectedNode(chart.links[linkId].to.nodeId);
        setDrawerVisibility(true);
      }

      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const onLinkClick = useCallback(
    (config: any) => {
      let chart = propelForm;
      const link = chart.links[config.linkId];
      handleLinkEdit(link);
    },
    [propelForm]
  );

  const handleLinkDelete = useCallback(
    (link: any) => {
      let chart = propelForm;
      delete chart.links[link.id];
      chart.sanitizeAllNodes(null, true);
      chart.reload = chart.reload + 1;
      setShowErrors(true);
      setDrawerType(undefined);
      setSelectedLink(undefined);
      setDrawerVisibility(false);
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const handleLinkEdit = useCallback(
    (link: any) => {
      const chart = propelForm;
      const linkObj = chart.links[link.id];
      if (linkObj) {
        const fromNodeObj = chart.nodes[linkObj.from.nodeId];
        if (fromNodeObj) {
          setDrawerType("link");
          setSelectedLink(link.id);
          setDrawerVisibility(true);
        }
      }
    },
    [propelForm]
  );

  const getLinkOptions = useCallback(
    (linkId: any) => {
      const chart = propelForm;
      const linkObj = chart.links[linkId];
      if (linkObj) {
        const fromNodeObj = chart.nodes[linkObj.from.nodeId];
        if (fromNodeObj) {
          const options = fromNodeObj.options || [];
          return options;
        }
      }
      return [];
    },
    [propelForm]
  );

  const onDragNodeStop = useCallback(
    (config: any, event: any, data: any, id: string) => {
      let chart = propelForm;
      const nodeId = config.id;
      chart.nodes[nodeId].position = { x: config.data.x, y: config.data.y };
      if (nodeId !== "0") {
        chart.nodes_dirty = true;
      }
      chart.reload = chart.reload + 1;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
      if (config.event && config.event.target) {
        if (config.event.target.id === "edit") {
          onSettingsClick(nodeId);
        }
        if (config.event.target.id === "delete") {
          Modal.confirm({
            title: `Are you sure you want to delete node ${chart.nodes[nodeId].name}?`,
            okText: "Yes",
            okType: "danger",
            cancelText: "No",
            onOk: () => onDeleteNode(nodeId)
          });
        }
      }
    },
    [propelForm]
  );

  const onDeleteNode = useCallback(
    (nodeId: string) => {
      let chart = propelForm;
      chart.deleteNode(nodeId);
      chart.reload = chart.reload + 1;
      setDrawerVisibility(false);
      setSelectedNode(undefined);
      setShowErrors(true);
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const onAddTemplateNode = useCallback(
    (type: string) => {
      let chart = propelForm;
      chart.nodes_dirty = true;

      const templates = get(propelNodeTemplateListState, ["0", "data", "records"], []);
      const template = templates.find((template: any) => template.type === type);
      if (template) {
        const newNode = chart.addNodeFromTemplate(template);
        chart.reload = chart.reload + 1;
        setShowErrors(true);
        dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
        setDirty(true);
      }
    },
    [propelForm, propelNodeTemplateListState]
  );

  const onReplaceTrigger = useCallback(
    (type: string) => {
      let chart = propelForm;
      if (chart.trigger_template_type === type) {
        notification.info({
          message: "Same as current trigger"
        });
        return;
      }
      const templates = get(propelTriggerListState, ["0", "data", "records"], []);
      const triggerRecord = templates.find((template: any) => template.type === type);
      chart.replaceTrigger(triggerRecord);
      chart.trigger_template_type = triggerRecord.type;
      chart.trigger_type = triggerRecord.trigger_type;
      chart.reload = chart.reload + 1;

      setShowErrors(true);
      setDrawerType(undefined);
      setDrawerVisibility(false);
      setDirty(true);
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
    },
    [propelForm, propelTriggerListState]
  );

  const handleNameChange = useCallback(
    (e: any) => {
      let chart = propelForm;
      chart.name = e.target.value;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const handleDescriptionChange = useCallback(
    (e: any) => {
      let chart = propelForm;
      chart.description = e.target.value;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const onUpdateTriggerType = useCallback(
    (value: any) => {
      let chart = propelForm;
      chart.nodes["0"].trigger_event = value;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  const onUpdateLinkOptions = useCallback(
    (link: any, option: any) => {
      let chart = propelForm;
      chart.links[link.id].properties.option = option;
      setDrawerType(undefined);
      setDrawerVisibility(false);
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
      setDirty(true);
    },
    [propelForm]
  );

  // Bind save function to parent confirmwation wrapper
  // Dependencies must equal updateGraph()'s dependencies
  useEffect(() => {
    bindSaveAction && bindSaveAction(onSaveClick);
  }, [propelForm, propel_id]);

  // Have commented code to show warning popup to user before saving the propel.
  function onSaveClick() {
    // const chart = propelForm;
    // if (!chart.propel_running) {
    updateGraph();
    // } else {
    //   setConfirmationModalVisibility(true);
    // }
  }

  const updateGraph = useCallback(() => {
    const chart = propelForm;
    if (!chart.valid.result) {
      setShowErrors(true);
      notification.error({
        message: "Cannot save propel. Please fix the below issues",
        description: (
          <>
            {chart.valid.errors.map((error: any) => (
              <>
                {error}
                <br />
              </>
            ))}
          </>
        )
      });
    } else {
      // do the actual saving update here
      let _mappedChart = new RestPropel(cloneDeep(chart.post_data));
      const notifications = get(_mappedChart, ["settings", "notifications"], []);
      let create: boolean = true;
      if (notifications?.enabled) {
        const newUsers = filter(notifications?.recipients, (option: any) => option?.includes("create:"));
        const existingUsers = filter(notifications?.recipients, (option: any) => !option?.includes("create:"));
        if (newUsers.length > 0) {
          create = false;
          const inValidEmails = reduce(
            newUsers,
            (acc: any, option: any) => {
              if (!validateEmail((option || "").replace("create:", ""))) {
                acc.push(option);
              }
              return acc;
            },
            []
          );
          const validNewEmails = difference(newUsers, inValidEmails);
          if (validNewEmails.length) {
            setCreatingUsers(true);
            dispatch(usersGetOrCreate(validNewEmails));
          }
        }
        set(_mappedChart, ["settings", "notifications", "recipients"], existingUsers);
      }
      if (propel_id) {
        setCreateLoading(true);
        setDirty(false);
        dispatch(prepelsUpdate(propel_id, _mappedChart));
      } else if (create) {
        setCreateLoading(true);
        setDirty(false);
        // GA event PROPEL_ADD
        emitEvent(AnalyticsCategoryType.PROPELS, PropelsActions.PROPEL_ADD);

        dispatch(prepelsCreate(_mappedChart));
      }
      props.history.goBack();
    }
  }, [propelForm, propel_id]);

  const handleZoom = useCallback(
    (zoom: number) => {
      const chart = propelForm;
      chart.scale = chart.scale + zoom;
      chart.reload = chart.reload + 1;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
    },
    [propelForm]
  );

  const handleConfirmationChange = useCallback(
    (e: RadioChangeEvent) => {
      let chart = propelForm;
      chart.stop_propel_runs = e.target.value;
      dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
    },
    [propelForm]
  );

  const showExportModal = useCallback(() => {
    setExportVisibility(true);
  }, []);

  const hideExportModal = useCallback(() => {
    setExportVisibility(false);
  }, []);

  const showSettingsModal = useCallback(() => {
    setSettingsModalVisibility(true);
  }, []);

  const hideSettingsModal = useCallback(() => {
    setSettingsModalVisibility(false);
  }, []);

  const hideConfirmationModal = useCallback(() => {
    setConfirmationModalVisibility(false);
  }, []);

  const hideDrawer = useCallback(() => {
    setDrawerVisibility(false);
  }, []);

  const handleArrangeNodes = useCallback(() => {
    chart.rearrangeNodes();
    chart.reload = chart.reload + 1;
    dispatch(formUpdateObj(PROPEL_FORM_NAME, chart));
    setReload(reload + 1);
  }, [propelForm]);

  const handleAddClick = useCallback(() => {
    setDrawerType("add_node");
    setDrawerVisibility(true);
  }, []);

  function settingsModal() {
    let chart = propelForm;
    return (
      <Modal
        title={isEditMode ? StringsEn.propelsRenameYour : StringsEn.propelSaveYour}
        visible={settings_modal_visible}
        okText="Save"
        onOk={hideSettingsModal}
        onCancel={hideSettingsModal}>
        <Form layout="vertical">
          <Form.Item className="mt-15" label={StringsEn.propelName} colon={false} required>
            <Input value={chart.name} onChange={handleNameChange} />
          </Form.Item>
          <Form.Item className="mt-15" label="description" colon={false}>
            <Input type="textarea" value={chart.description} onChange={handleDescriptionChange} />
          </Form.Item>
        </Form>
      </Modal>
    );
  }

  function confirmationModal() {
    const radioStyle = {
      display: "block",
      height: "30px",
      lineHeight: "30px"
    };
    let chart = propelForm;

    return (
      <Modal
        title={StringsEn.propelSave}
        visible={confirmation_modal_visible}
        okText={"Save"}
        className={"confirmation-modal"}
        onOk={() => {
          updateGraph();
          setConfirmationModalVisibility(false);
        }}
        onCancel={hideConfirmationModal}>
        <Radio.Group onChange={handleConfirmationChange} value={chart.stop_propel_runs}>
          <Radio value={false} style={radioStyle}>
            Apply new changes after the currently running instances complete
          </Radio>
          <Radio value={true} style={radioStyle}>
            Terminate existing instances of the propel and apply changes immediately
          </Radio>
        </Radio.Group>
      </Modal>
    );
  }

  const updateNotifyOnEmailSettings = useCallback(
    (data: NotifyFailType) => {
      let chart = propelForm.post_data;
      chart.settings = { ...(propelForm.settings || {}), notifications: { ...data, enabled: true } };
      setNotifyFailModal(false);
      dispatch(formUpdateObj(PROPEL_FORM_NAME, new RestPropel(chart)));
    },
    [propelForm]
  );

  const handleCancelNotifyFailModal = useCallback(() => {
    dispatch(
      setPageSwitchAction(location.pathname, "notify_fail", {
        checked: false
      })
    );
    setNotifyFailModal(false);
  }, [pageSettingsState]);

  const renderNotifyMailModal = useMemo(() => {
    if (!notifyFailModal) return null;
    return (
      <NotifyFailModal
        visible
        initialState={propelForm?.settings?.notifications}
        onCancel={handleCancelNotifyFailModal}
        onOk={updateNotifyOnEmailSettings}
      />
    );
  }, [notifyFailModal, propelForm]);

  const className = props.className || "workflow-editor";
  const chart = propelForm;
  if (propelForm === undefined || loading || create_loading) {
    return <Loader />;
  }

  const selectedNodeObj = selected_node ? chart.nodes[selected_node] : {};
  const predicates = selected_node ? chart.getPredecessorOutputs(selected_node) : [];
  const selectedLinkObj = selected_link ? chart.links[selected_link] : {};
  const linkOptions = getLinkOptions(selected_link);

  const renderDrawers = () => {
    return (
      <>
        {drawer_type === "add_node" && drawer_visible && (
          <GenericNewNode
            visible={drawer_visible}
            onClose={hideDrawer}
            onAdd={onAddTemplateNode}
            onAddTrigger={onReplaceTrigger}
          />
        )}
        {drawer_type === "edit_node" && drawer_visible && (
          <GenericNode
            visible={drawer_visible}
            onClose={hideDrawer}
            node={selectedNodeObj}
            predicates={predicates}
            onUpdate={onUpdateNodeProperties}
            onUpdateName={onUpdateNodeName}
          />
        )}
        {drawer_type === "edit_trigger" && drawer_visible && (
          <TriggerNode
            visible={drawer_visible}
            onClose={hideDrawer}
            node={selectedNodeObj}
            propelId={propel_id || random}
            predicates={predicates}
            onUpdate={onUpdateNodeProperties}
            onUpdateType={onUpdateTriggerType}
          />
        )}
        {drawer_type === "link" && drawer_visible && (
          <LinkNode
            visible={drawer_visible}
            onClose={hideDrawer}
            onDelete={handleLinkDelete}
            link={selectedLinkObj}
            options={linkOptions}
            onUpdate={onUpdateLinkOptions}
          />
        )}
      </>
    );
  };

  return (
    <div className="content">
      {settingsModal()}
      {confirmationModal()}
      {renderNotifyMailModal}
      <ExportModal chart={chart} visible={export_visible} onOK={hideExportModal} />
      <Row>
        <Col span={12} className="d-flex align-center">
          <AntTitle className="mb-0" level={4}>
            {chart.name || "Untitled"}
          </AntTitle>
          <Icon
            className="d-flex ml-10"
            style={{ fontSize: "20px", lineHeight: "1.4" }}
            onClick={showSettingsModal}
            type="edit"
          />
        </Col>
      </Row>
      <Row>
        <Col span={12} className="d-flex align-center">
          <AntText className="mb-0" ellipsis={true} style={{ maxWidth: "400px" }}>
            {chart.description || "Untitled"}
          </AntText>
        </Col>
        <Col span={12}>
          {
            // @ts-ignore
            <div align={"right"}>
              <Tooltip title={StringsEn.propelsExport}>
                <Button icon={"export"} shape={"circle"} className="ant-btn-outline mx-5" onClick={showExportModal} />
              </Tooltip>
              <Tooltip title={"Arrange nodes"}>
                <Button
                  icon={"apartment"}
                  shape={"circle"}
                  className="ant-btn-outline mx-5"
                  onClick={handleArrangeNodes}
                />
              </Tooltip>
              <Tooltip title={"New Node"}>
                <Button
                  icon={"plus"}
                  shape={"circle"}
                  onClick={handleAddClick}
                  style={{ marginRight: "10px", marginBottom: "5px" }}
                  type={"primary"}
                />
              </Tooltip>
              <Popconfirm
                title={"Propel has changed. Saving will terminate existing runs. Would you like to continue?"}
                okText="Yes"
                cancelText="No"
                //disabled={chart.id === undefined || !chart.nodes_dirty}
                onConfirm={onSaveClick}>
                <Tooltip title={`${entPropelsAddCountExceed || !hasAccess ? TOOLTIP_ACTION_NOT_ALLOWED : "Save"}`}>
                  <Button
                    type={"primary"}
                    icon={"save"}
                    shape={"circle"}
                    disabled={create_loading || entPropelsAddCountExceed || !hasAccess}
                    style={{ marginBottom: "5px", marginRight: "10px" }}
                  />
                </Tooltip>
              </Popconfirm>
            </div>
          }
        </Col>
      </Row>
      <Row>
        <Col span={24}>
          <div
            className="position-relative workflow-wrapper"
            style={{ overflowY: drawer_visible ? "hidden" : "scroll" }}
            ref={(node: any) => setContainer(node)}>
            <div className={`${className} ${className}__editor`}>
              <FlowChart
                initialValue={chart.flowchartRepr}
                reload={chart.reload}
                config={{
                  snapToGrid: false
                }}
                callbacks={{
                  onLinkClick: onLinkClick,
                  onLinkMouseEnter: () => {},
                  onLinkMouseLeave: () => {},
                  onDragNodeStop: onDragNodeStop,
                  onLinkComplete: onLinkComplete,
                  onLinkStart: onLinkStart,
                  onLinkCancel: onLinkCancel,
                  onPortPositionChange: () => {},
                  onDragCanvas: onDragCanvas
                }}
                Components={{
                  Node: CustomNode,
                  NodeInner: (props: any) => (
                    <NodeInner
                      {...props}
                      showErrors={show_errors}
                      onDelete={onDeleteNode}
                      onSetting={onSettingsClick}
                      ref={React.createRef()}
                    />
                  ),
                  CanvasInner: CanvasInnerCustom,
                  Port: CustomPort as any,
                  Link: (props: any) => (
                    <CustomLink {...props} onDeleteClick={handleLinkDelete} onEditClick={handleLinkEdit} />
                  )
                }}
              />
              <Affix offsetBottom={20} target={() => container}>
                {
                  // @ts-ignore
                  <div align={"right"} style={{ margin: "10px" }}>
                    <PillBoxZoom
                      style={{ backgroundColor: "white" }}
                      zoomValue={chart.scale * 100}
                      onZoomIn={() => handleZoom(0.1)}
                      onZoomOut={() => handleZoom(-0.1)}
                    />
                  </div>
                }
              </Affix>
            </div>
            {renderDrawers()}
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default ConfirmationWrapper(WorkflowEditorPage);
