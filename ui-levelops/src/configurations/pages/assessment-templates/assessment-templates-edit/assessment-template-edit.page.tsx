import * as React from "react";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { cloneDeep, debounce, get } from "lodash";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import queryString from "query-string";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { qsExport, templateCreateOrUpdate, templateGet } from "reduxConfigs/actions/restapi/questionnaireActions";
import { getStateLoadingStatus } from "../../../../utils/loadingUtils";
import { tagsList } from "reduxConfigs/actions/restapi/tagActions";
import { RestQuestionnaire, RestSection } from "classes/RestQuestionnaire";
import { AntButton, AntCard, AntCol, AntInput, AntRow } from "../../../../shared-resources/components";
import { Empty, Form, Modal, notification, Slider, Switch } from "antd";
import { Section, SectionsList } from "configurations/containers/questions";
import Loader from "components/Loader/Loader";
import "./assessment-templates-edit.style.scss";
import { getBaseUrl, TEMPLATE_ROUTES } from "../../../../constants/routePaths";
import { NAME_EXISTS_ERROR } from "../../../../constants/formWarnings";
import { SelectRestapi } from "shared-resources/helpers";
import { checkTemplateNameExists } from "../../../helpers/checkTemplateNameExits";
import { AssessmentService } from "../../../../services/assessment/assessment.service";
import FileSaver from "file-saver";
import { genericList } from "reduxConfigs/actions/restapi";

const AssessmentTemplatesEditPage: React.FC<any> = props => {
  const dispatch = useDispatch();
  const completeQuestionnairesState = useSelector((state: any) => {
    return get(state.restapiReducer, ["complete-questionnaires"], {});
  });
  const questionnairesState = useSelector((state: any) => {
    return get(state.restapiReducer, ["questionnaires"], {});
  });
  const page = useSelector((state: any) => getPageSettingsSelector(state));

  const values = queryString.parse(props.location?.search);
  const paramTemplateId = values.questionnaire;

  const [loading, setLoading] = useState<boolean>(paramTemplateId !== undefined);
  const [templateId, setTemplateId] = useState<string | undefined>(paramTemplateId as string);
  const [restQuestionnaire, setRestQuestionnaire] = useState<RestQuestionnaire>(new RestQuestionnaire());
  const [selectedTags, setSelectedTags] = useState<any[]>([]);
  const [selectedKbs, setSelectedKbs] = useState<any[]>([]);
  const [selectedSection, setSelectedSection] = useState<number | undefined>(0);
  const [setHeader, setSetHeader] = useState(false);
  const [settingsVisible, setSettingsVisible] = useState<boolean>(paramTemplateId === undefined);
  const [deletedSectionIds, setDeletedSectionIds] = useState<string[]>([]);
  const [exportTemplateId, setExportTemplateId] = useState<string | undefined | null>(null);
  const [exporting, setExporting] = useState<boolean>(false);
  const [createLoading, setCreateLoading] = useState<boolean>(false);
  const [nameExists, setNameExists] = useState<string | undefined>(undefined);
  const [checkingName, setCheckingName] = useState<boolean>(false);
  const [autoSaving, setAutoSave] = useState<boolean>(false);
  const [updateHeader, setUpdateHeader] = useState<boolean>(false);

  const sanitizeSections = (sections: Array<RestSection | undefined>) => {
    return (sections ?? []).filter((v: RestSection | undefined) => !!v);
  };

  const updateFormField = (
    field: "name" | "sections" | "tag_ids" | "kb_ids" | "low_risk_boundary" | "mid_risk_boundary" | "risk_enabled",
    value: any
  ) => {
    if (field === "sections") {
      setRestQuestionnaire(
        (state: any) => new RestQuestionnaire({ ...state.json(), [field]: sanitizeSections(value) })
      );
    } else {
      setRestQuestionnaire(
        (state: any) =>
          new RestQuestionnaire({
            ...state.json(),
            [field]: value,
            sections: sanitizeSections(state.sections)
          })
      );
    }
  };

  const saveQuestionnaire = () => {
    dispatch(templateCreateOrUpdate("0", { ...restQuestionnaire, deletedSectionIds }));
    setCreateLoading(true);
  };

  useEffect(() => {
    if (templateId) {
      dispatch(templateGet(templateId));
    }

    return () => {
      // @ts-ignore
      dispatch(restapiClear("questionnaires", "list", "0"));
      // @ts-ignore
      dispatch(restapiClear("complete-questionnaires", "get", templateId || "0"));
      // @ts-ignore
      dispatch(restapiClear("complete-questionnaires", "createOrUpdate", "0"));
      dispatch(clearPageSettings(props.location?.pathname));
    };
  }, []);

  useEffect(() => {
    if (autoSaving) {
      if (templateId) {
        saveQuestionnaire();
      } else {
        setAutoSave(false);
      }
    }
  }, [autoSaving]);

  useEffect(() => {
    if (updateHeader) {
      let valid = true;
      restQuestionnaire.sections.forEach((section: any) => {
        valid = valid && section.valid();
      });
      const btnStatus =
        restQuestionnaire.sections.length === 0 || restQuestionnaire.name === "" || !valid || nameExists || autoSaving;
      // dispatch(setPageButtonAction(props.location?.pathname, "save", { disabled: btnStatus }));
      dispatch(setPageButtonAction(props.location?.pathname, "save", { disabled: !!templateId && btnStatus }));
      setUpdateHeader(false);
    }
  }, [updateHeader]);

  useEffect(() => {
    if (loading) {
      const { loading: _loading, error } = getStateLoadingStatus(completeQuestionnairesState, "get", templateId);
      if (!_loading && !error && templateId) {
        const data = get(completeQuestionnairesState, ["get", templateId, "data"]);
        const template = data.template;
        const tags = data.tags;
        const kbs = data.kbs;
        const sections = data.sections.filter((section: any) => !!section && section?.id);

        setRestQuestionnaire(new RestQuestionnaire(template));
        setSelectedKbs(kbs.map((kb: any) => ({ label: kb.data.name, key: kb.data.id })));
        setSelectedTags(tags.map((tag: any) => ({ label: tag.name, key: tag.id })));
        updateFormField(
          "sections",
          sections.map((section: any) => new RestSection(section))
        );
        setLoading(false);
        setSetHeader(true);
      }
    }
  }, [loading, completeQuestionnairesState]);

  useEffect(() => {
    if (checkingName) {
      const { loading, error } = getStateLoadingStatus(questionnairesState, "list");
      if (!loading && !error) {
        const data = get(questionnairesState, ["list", "0", "data"], { records: [] }).records;
        const prevName = restQuestionnaire.name;
        setCheckingName(false);
        setNameExists(checkTemplateNameExists(prevName, data) ? NAME_EXISTS_ERROR : undefined);
      }
    }

    if (exporting && exportTemplateId) {
      const { error, loading } = getStateLoadingStatus(questionnairesState, "export", exportTemplateId);
      if (!loading) {
        if (error) {
          notification.error({
            message: "Error",
            description: "Failed to export assessment."
          });
          setExporting(false);
        } else {
          const data = get(questionnairesState, ["export", exportTemplateId, "data"], {});
          const csvData = AssessmentService.convertAssessmentIntoExportFormat(data);
          // @ts-ignore
          let file = new File([csvData], `${data.name}.csv`, { type: "text/csv" });
          FileSaver.saveAs(file);

          notification.success({
            message: "Success",
            description: "Downloading assessment."
          });
          setExporting(false);
          setExportTemplateId(null);
        }
      }
    }
  }, [completeQuestionnairesState, questionnairesState]);

  useEffect(() => {
    if (createLoading) {
      const { loading, error } = getStateLoadingStatus(completeQuestionnairesState, "createOrUpdate", "0");
      if (!loading && !error) {
        if (autoSaving) {
          setAutoSave(false);
        } else {
          props.history.push(`${getBaseUrl()}${TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.LIST}`);
        }
        setCreateLoading(false);
      }
    }
  }, [createLoading, completeQuestionnairesState]);

  useEffect(() => {
    if (setHeader) {
      dispatch(
        setPageSettings(props.location?.pathname, {
          title: restQuestionnaire.name || "",
          action_buttons: {
            settings: {
              type: "secondary",
              label: "Settings",
              icon: "setting",
              hasClicked: false
            },
            save: {
              type: "primary",
              label: "Save",
              icon: "save",
              hasClicked: false
            },
            export: {
              type: "primary",
              label: "Export",
              hasClicked: false,
              disabled: !templateId
            }
          }
        })
      );
      setSetHeader(false);
    }
  }, [setHeader]);

  useEffect(() => {
    if (page && Object.keys(page).length > 0) {
      const _page = page[props.location?.pathname];
      if (_page && _page.hasOwnProperty("action_buttons")) {
        if (_page.action_buttons.settings && _page.action_buttons.settings.hasClicked === true) {
          dispatch(setPageButtonAction(props.location?.pathname, "settings", { hasClicked: false }));
          setSettingsVisible(true);
        }

        if (_page.action_buttons.export && _page.action_buttons.export.hasClicked === true) {
          notification.success({
            message: "Please wait...",
            description: "Generating CSV for assessment."
          });

          dispatch(qsExport(templateId));
          dispatch(setPageButtonAction(props.location?.pathname, "export", { hasClicked: false }));

          setExporting(true);
          setExportTemplateId(templateId);
        }

        if (_page.action_buttons.save && _page.action_buttons.save.hasClicked === true) {
          // to prevent the saving of empty questions
          dispatch(setPageButtonAction(props.location?.pathname, "save", { hasClicked: false }));
          if (restQuestionnaire.sections.filter((f: any) => !!f._name).length > 0) {
            saveQuestionnaire();
          }
        }
      }
    }
  }, [page, completeQuestionnairesState]);

  const checkTemplateName = (name: string) => {
    const filters = {
      filter: {
        partial: {
          name: name
        }
      }
    };
    dispatch(genericList("questionnaires", "list", filters));
    setCheckingName(true);
  };

  const debounceCheckName = debounce(checkTemplateName, 300);

  const handleDeleteSection = (index: number) => {
    return (e: any) => {
      let sections = restQuestionnaire.sections;
      let deletedIds = deletedSectionIds;
      if (sections[index].id !== undefined) {
        deletedIds.push(sections[index].id);
      }
      setDeletedSectionIds(deletedIds);
      setSelectedSection(index === selectedSection ? 0 : selectedSection);
      sections.splice(index, 1);
      updateFormField("sections", sections);
      setAutoSave(true);
      setUpdateHeader(true);
    };
  };

  const handleReorder = (dragIndex: number, hoverIndex: number) => {
    let sections = restQuestionnaire.sections;
    let element = sections[dragIndex];

    sections.splice(dragIndex, 1);
    sections.splice(hoverIndex, 0, element);
    updateFormField("sections", sections);
    setUpdateHeader(true);
  };

  const handleAddSection = (e: any) => {
    setSelectedSection(undefined);
    let sections = restQuestionnaire.sections;
    // console.log("stringify sections", JSON.stringify(sections));

    let newSection = new RestSection();
    // console.log("newSection", JSON.stringify(newSection));
    // sections.push(newSection);
    const newSections = sections.concat(newSection);

    // console.log("newSections", JSON.stringify(newSections));
    updateFormField("sections", newSections);
    setSelectedSection(newSections.length - 1);
    setUpdateHeader(true);
  };

  const handleUpdateSection = (index: number) => {
    return (section: any) => {
      let sections = restQuestionnaire.sections;
      sections[index] = section;
      updateFormField("sections", sections);
      // setAutoSave(true);
      setUpdateHeader(true);
    };
  };

  const handleRange = (value: any) => {
    updateFormField("low_risk_boundary", value[0]);
    updateFormField("mid_risk_boundary", value[1]);
  };

  const onCancel = () => {
    setSettingsVisible(false);
    if (props.location?.pathname === `${getBaseUrl()}${TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.CREATE}`) {
      props.history.push(`${getBaseUrl()}${TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.LIST}`);
    }
  };

  const getBtnStatus = () => {
    const atForm = restQuestionnaire;
    return !(atForm && atForm.name && atForm.name.length > 0 && !nameExists);
  };

  const onOk = () => {
    setSettingsVisible(false);
    setSetHeader(true);
  };

  const handleFieldUpdate = (field: string) => {
    return (e: any) => {
      // setSetHeader(true);
      // setUpdateHeader(true);
      if (field === "name") {
        debounceCheckName(e.target.value);
      }
      // @ts-ignore
      updateFormField(field, e.target.value);
    };
  };

  const templateForm = () => {
    const questionnaire = restQuestionnaire;
    // const { getFieldDecorator, getFieldsError, getFieldError, isFieldTouched, resetFields } = props.form;
    const marks = {
      0: 0,
      10: "10",
      20: "20",
      30: "30",
      40: "40",
      50: "50",
      60: "60",
      70: "70",
      80: "80",
      90: "90",
      100: "100"
    };

    return (
      <Modal
        title={"Assessment Template Settings"}
        visible={settingsVisible}
        maskClosable={true}
        // @ts-ignore
        icon={"setting"}
        onOk={e => {
          // resetFields();
          onOk();
        }}
        onCancel={e => {
          // resetFields();
          onCancel();
        }}
        footer={[
          <AntButton key="back" onClick={onCancel}>
            Cancel
          </AntButton>,
          <AntButton key="submit" type="primary" disabled={getBtnStatus()} onClick={onOk}>
            Ok
          </AntButton>
        ]}
        cancelText={""}
        closable={false}>
        <Form layout={"vertical"}>
          <Form.Item
            label={"name"}
            key={"name"}
            validateStatus={nameExists ? "error" : ""}
            colon={false}
            help={
              <>
                {nameExists && nameExists}
                {/*{isFieldTouched("name") && getFieldError("name") && REQUIRED_FIELD}*/}
              </>
            }>
            <AntInput
              placeholder="New Assessment Template"
              value={questionnaire.name}
              onChange={handleFieldUpdate("name")}
            />
          </Form.Item>

          <Form.Item label={"Knowledge Base"}>
            <SelectRestapi
              style={{ width: "100%" }}
              placeholder="KB"
              uri="bestpractices"
              method={"list"}
              searchField="name"
              value={selectedKbs}
              allowClear={false}
              mode={"multiple"}
              onChange={(option: any) => {
                setSelectedKbs(option);
                updateFormField(
                  "kb_ids",
                  (option || []).map((opt: any) => opt.key)
                );
              }}
            />
          </Form.Item>

          <Form.Item label={"tags"}>
            <SelectRestapi
              style={{ width: "100%" }}
              placeholder="tags"
              uri="tags"
              fetchData={tagsList}
              searchField="name"
              value={selectedTags}
              allowClear={false}
              mode={"multiple"}
              createOption={true}
              onChange={(options: any) => {
                const tagOptions = options || [];
                setSelectedTags(tagOptions);
                const tags = tagOptions.map((option: any) => option.key);
                updateFormField("tag_ids", tags);
              }}
            />
          </Form.Item>

          <Form.Item label={"Risk Scoring Enabled"}>
            <Switch
              checked={questionnaire.risk_enabled}
              onChange={value => {
                let sections = questionnaire.sections;
                sections.forEach((section: any) => {
                  section.risk_enabled = value;
                });
                updateFormField("risk_enabled", value);
                updateFormField("sections", sections);
              }}
            />
          </Form.Item>

          {questionnaire.risk_enabled && (
            <Form.Item label={"Risk Range"}>
              <Slider
                range
                min={0}
                max={100}
                value={[questionnaire.low_risk_boundary, questionnaire.mid_risk_boundary]}
                marks={marks}
                step={10}
                onChange={handleRange}
              />
            </Form.Item>
          )}
        </Form>
      </Modal>
    );
  };

  if (!autoSaving && (loading || createLoading)) {
    return <Loader />;
  }

  return (
    <>
      {templateForm()}
      <AntRow type="flex" gutter={[10, 10]}>
        <AntCol span={6}>
          <AntCard
            title={"Sections"}
            className={`${!restQuestionnaire.sections.length ? "no-section-added " : ""}ant-card-section-list`}>
            {restQuestionnaire.sections.length > 0 && (
              <SectionsList
                sections={[...restQuestionnaire.sections]}
                onDelete={handleDeleteSection}
                onSelect={(index: number) => setSelectedSection(index)}
                selectedSection={selectedSection}
                onReorder={handleReorder}
              />
            )}
            {restQuestionnaire.sections.length === 0 && (
              <Empty description={"Add at least one Section"} style={{ margin: "10px" }} />
            )}
            <div className="mx-15">
              <AntButton onClick={handleAddSection} icon={"plus"} block type={"dashed"} size={"large"}>
                Add Section
              </AntButton>
            </div>
          </AntCard>
        </AntCol>
        {selectedSection !== undefined && restQuestionnaire.sections[selectedSection] !== undefined && (
          <AntCol span={18}>
            <Section
              // @ts-ignore
              cardClassNames="ant-card-section-list ant-card-question-list"
              section={restQuestionnaire.sections[selectedSection]}
              onChange={handleUpdateSection(selectedSection)}
              onDelete={handleDeleteSection(selectedSection)}
            />
          </AntCol>
        )}
      </AntRow>
    </>
  );
};

export default AssessmentTemplatesEditPage;
