import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Icon, notification, Upload } from "antd";
import { AntModal, AntText, TableRowActions } from "shared-resources/components";
import { readString } from "react-papaparse";
import { v1 as uuid } from "uuid";
import { get, debounce } from "lodash";
import FileSaver from "file-saver";
import { ServerPaginatedTable } from "shared-resources/containers";
import { tableColumns } from "./table-config";
import { RestQuestionnaire, RestSection } from "classes/RestQuestionnaire";
import { AssessmentService } from "services/assessment/assessment.service";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";
import { checkTemplateNameExists } from "../../../helpers/checkTemplateNameExits";
import { EditCloneModal } from "../../../../shared-resources/components";
import { NAME_EXISTS_ERROR } from "../../../../constants/formWarnings";
import { AssessmentExcelImportModal } from "./excelImport.modal";
import { ExcelImportDetailModal } from "./excelImportDetail.modal";
import Loader from "components/Loader/Loader";
import ErrorWrapper from "hoc/errorWrapper";
import { navigateToRoute } from "../../../../utils/routeUtils";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import {
  restapiClear,
  sectionsGet,
  sectionsCreate,
  qsImport,
  genericList,
  tagsList,
  qsExport,
  qsCreate,
  qsDelete,
  qsBulkDelete
} from "reduxConfigs/actions/restapi";
import {
  questionnairesStateSelector,
  questionnairesSectionStateSelector
} from "reduxConfigs/selectors/restapiSelector";
import { appendErrorMessagesHelper } from "utils/arrayUtils";

const { Dragger } = Upload;

const AssessmentTemplatesListPage: React.FC<any> = props => {
  const dispatch = useDispatch();
  const questionnairesState = useSelector((state: any) => questionnairesStateSelector(state));
  const questionnairesSectionState = useSelector((state: any) => questionnairesSectionStateSelector(state));

  const [showImportModal, setShowImportModal] = useState<boolean>(false);
  // eslint-disable-next-line
  const [moreFilters, setMoreFilters] = useState<any>({});
  // eslint-disable-next-line
  const [partialFilters, setPartialFilters] = useState<any>({});
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [qId, setQId] = useState<string | undefined>(undefined);
  const [cloneQLoading, setCloneQLoading] = useState<boolean>(false);
  const [cloningSections, setCloningSections] = useState<any>({});
  const [cloningQ, setCloningQ] = useState<boolean>(false);
  const [exportAssessmentId, setExportAssessmentId] = useState<string | undefined>(undefined);
  const [exportingAssessment, setExportingAssessment] = useState<boolean>(false);
  const [importingAssessment, setImportingAssessment] = useState<boolean>(false);
  const [checkingName, setCheckingName] = useState<boolean>(false);
  const [dataToClone, setDataToClone] = useState<any>(undefined);
  const [checkNameListId, setCheckNameListId] = useState<string | undefined>(undefined);
  const [openEditCloneModal, setOpenEditCloneModal] = useState<boolean>(false);
  const [cloneTemplateName, setCloneTemplateName] = useState<string>("");
  const [nameExist, setNameExist] = useState<any>(undefined);
  const [importingDataSet, setImportingDataSet] = useState<any[]>([]);
  const [showExcelImportModal, setShowExcelImportModal] = useState<boolean>(false);
  const [importMetaData, setImportMetaData] = useState<any>({});
  const [startImportingAssessment, setStartImportingAssessment] = useState<boolean>(false);
  const [checkingImportName, setCheckingImportName] = useState<boolean>(false);
  const [currentImportIndex, setCurrentImportIndex] = useState<any>(undefined);
  const [showImportDetailModal, setShowImportDetailModal] = useState<boolean>(false);
  const [cloneSectionLoading, setCloneSectionLoading] = useState<boolean>(false);
  const [cloneSectionCloning, setCloneSectionCloning] = useState<boolean>(false);
  const [selectedIds, setSelectedIds] = useState<any>([]);
  const [rowSelection, setRowSelection] = useState<any>({});
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);
  const [reload, setReload] = useState(1);

  useEffect(
    () => {
      dispatch(
        setPageSettings(props.location.pathname, {
          title: "Assessment Templates",
          action_buttons: {
            import_csv: {
              type: "primary",
              label: "Import from CSV",
              icon: "import",
              hasClicked: false,
              buttonHandler: () => {
                setShowImportModal(true);
              }
            },
            add_assessment: {
              type: "primary",
              label: "Add Assessment Template",
              hasClicked: false,
              buttonHandler: () => {
                navigateToRoute(getBaseUrl(), TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.CREATE);
              }
            }
          }
        })
      );

      return () => {
        // @ts-ignore
        dispatch(restapiClear("questionnaires", "list", "0"));
        // @ts-ignore
        dispatch(restapiClear("questionnaires", "get", "-1")); // Used by clone-feature
        // @ts-ignore
        dispatch(restapiClear("questionnaires", "create", "0")); // Used by clone-feature
        // @ts-ignore
        dispatch(restapiClear("sections", "get", "-1")); // Used by clone-feature
        // @ts-ignore
        dispatch(restapiClear("sections", "create", "-1")); // Used by clone-feature
        // @ts-ignore
        dispatch(restapiClear("questionnaires", "delete", "-1"));
        // @ts-ignore
        dispatch(restapiClear("tags", "bulk", "0"));
        // @ts-ignore
        dispatch(restapiClear("questionnaires", "bulkDelete", "-1"));
        dispatch(clearPageSettings(props.location.pathname));
      };
    },
    // eslint-disable-next-line
    []
  );

  useEffect(
    () => {
      if (cloneQLoading) {
        const { sections } = dataToClone;
        sections.forEach((section: any) => dispatch(sectionsGet(section)));
        setCloneQLoading(false);
        setCloneSectionLoading(true);
      }
    },
    // eslint-disable-next-line
    [cloneQLoading]
  );

  useEffect(
    () => {
      if (cloneSectionLoading) {
        // sections <=> sectionIds
        const { sections } = dataToClone;

        let updatedSections: any = {};

        sections.forEach((sectionId: string) => {
          const { sectionLoaded } = cloningSections[sectionId] || {};

          if (!sectionLoaded) {
            // Check whether loading is complete yet or not...
            const { loading, error } = get(questionnairesSectionState, ["get", sectionId], {
              loading: true,
              error: true
            });

            if (!loading && !error) {
              // Stripping id from new section obj to avoid any conflict.
              const { id: _oldId, ...sectionData } = get(questionnairesSectionState, ["get", sectionId, "data"], {});
              let newSectionData = { ...sectionData };
              const questions = newSectionData.questions;
              let newQuestions = questions.map((question: any) => {
                // Stripping id from new question obj to avoid any conflict.
                const { id, ...newQuestionData } = question;
                return newQuestionData;
              });

              newSectionData = {
                ...newSectionData,
                questions: newQuestions
              };

              dispatch(sectionsCreate(new RestSection(newSectionData), sectionId));

              updatedSections = {
                ...updatedSections,
                [sectionId]: {
                  ...(updatedSections[sectionId] || {}),
                  sectionLoaded: true
                }
              };
            }
          }
        });

        if (
          Object.keys(cloningSections).length === sections.length &&
          Object.values(cloningSections).every((item: any) => item.sectionLoaded)
        ) {
          setCloneSectionLoading(false);
          setCloneSectionCloning(true);
          setCloningSections((sections: any) => ({
            ...sections,
            ...updatedSections
          }));
          return;
        }

        if (Object.keys(updatedSections).length > 0) {
          setCloningSections((sections: any) => ({
            ...sections,
            ...updatedSections
          }));
        }
      }

      if (cloneSectionCloning) {
        const { sections } = dataToClone;

        let updatedSections: any = {};

        sections.forEach((sectionId: string) => {
          const { sectionLoaded, sectionCloned } = cloningSections[sectionId] || {};
          if (sectionLoaded && !sectionCloned) {
            // Check whether sections cloning is complete yet or not...
            const loading = get(questionnairesSectionState, ["create", sectionId, "loading"], true);
            const error = get(questionnairesSectionState, ["create", sectionId, "error"], true);

            if (!loading && !error) {
              const newSection = get(questionnairesSectionState, ["create", sectionId, "data"], {});
              updatedSections = {
                ...updatedSections,
                [sectionId]: {
                  ...(updatedSections[sectionId] || {}),
                  sectionCloned: true,
                  newId: newSection.id
                }
              };
            }
          }
        });

        if (Object.keys(updatedSections).length > 0) {
          setCloningSections((sections: any) => ({
            ...sections,
            ...updatedSections
          }));
        }

        const sectionCloneComplete = Object.keys(cloningSections).reduce((acc, item) => {
          return acc && cloningSections[item].sectionCloned;
        }, true);

        if (sectionCloneComplete) {
          // Trigger API Call to create qTemplate
          const { id: _oldId, sections, _tags, tag_ids, ...questionnaireData } = dataToClone;
          const newQuestionnaire = new RestQuestionnaire({ ...questionnaireData, name: cloneTemplateName });
          let qTagIds = [];
          if (_tags && _tags.length > 0) {
            qTagIds = _tags.map((tag: any) => tag.key);
          }
          newQuestionnaire.tags = qTagIds;
          newQuestionnaire.sections = sections.map((sectionId: string) => {
            return { id: cloningSections[sectionId].newId };
          });
          dispatch(qsCreate(newQuestionnaire));
          setCloningQ(true);
          setCloneSectionCloning(false);
          return;
        }
      }

      if (cloningQ) {
        const loading = get(questionnairesState, ["create", "0", "loading"], true);
        const error = get(questionnairesState, ["create", "0", "error"], true);
        if (!loading && !error) {
          const newQTemplateId = get(questionnairesState, ["create", "0", "data", "id"], "");
          notification.success({
            message: "Clone Complete",
            description: "Template cloned successfully"
          });
          props.history.push(
            `${getBaseUrl()}${TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.EDIT}?questionnaire=${newQTemplateId}`
          );
          setCloneTemplateName("");
          setCloningQ(false);
        }
      }

      if (deleteLoading && qId) {
        const loading = get(questionnairesState, ["delete", qId.toString(), "loading"], true);
        const error = get(questionnairesState, ["delete", qId.toString(), "error"], true);
        if (!loading) {
          if (!error) {
            const data = get(questionnairesState, ["delete", qId.toString(), "data"], {});
            const success = get(data, ["success"], true);
            if (!success) {
              notification.error({
                message: data["error"]
              });
            } else {
              setSelectedIds((ids: string[]) => ids.filter(id => id !== qId.toString()));
            }
          }
          setDeleteLoading(false);
          setQId(undefined);
        }
      }

      if (exportingAssessment && exportAssessmentId) {
        const { loading, error } = get(questionnairesState, ["export", exportAssessmentId], {
          loading: true,
          error: true
        });
        if (!loading) {
          if (error) {
            notification.error({
              message: "Error",
              description: "Failed to export assessment."
            });
            return setExportingAssessment(false);
          }
          const data = get(questionnairesState, ["export", exportAssessmentId, "data"], {});
          const csvData = AssessmentService.convertAssessmentIntoExportFormat(data);
          // @ts-ignore
          let file = new File([csvData], `${data.name}.csv`, { type: "text/csv" });
          FileSaver.saveAs(file);
          notification.success({
            message: "Success",
            description: "Downloading assessment."
          });
          setExportingAssessment(false);
          setExportAssessmentId(undefined);
        }
      }

      if (
        showExcelImportModal &&
        importMetaData &&
        importMetaData.templateNames &&
        importMetaData.templateNames.length > 0
      ) {
        let checking_import_names = false;
        const updatedTemplates = importMetaData.templateNames.map((template: any) => {
          if (template.currentName && template.validate && !template.validating) {
            const filters = {
              filter: {
                partial: {
                  name: template.currentName
                }
              }
            };
            dispatch(genericList("questionnaires", "list", filters, null, template.id));
            checking_import_names = true;
            return {
              ...template,
              validating: true
            };
          }
          return template;
        });
        if (checking_import_names) {
          setCheckingImportName(checking_import_names);
          setImportMetaData({
            ...importMetaData,
            templateNames: updatedTemplates
          });
        }
      }

      if (bulkDeleting) {
        const { loading, error } = questionnairesState.bulkDelete["0"];
        if (!loading) {
          if (!error) {
            const data = get(questionnairesState, ["bulkDelete", "0", "data", "records"], []);
            const errorMessages = appendErrorMessagesHelper(data);
            let errorOccurs = false;
            if (errorMessages.length) {
              errorOccurs = true;
              notification.error({
                message: errorMessages
              });
            }
            if (errorOccurs) {
              setReload(state => state + 1);
            } else {
              setSelectedIds([]);
              setReload(state => state + 1);
            }
          }
          setBulkDeleting(false);
        }
      }
    },
    // eslint-disable-next-line
    [
      importMetaData,
      exportingAssessment,
      cloneSectionLoading,
      cloneSectionCloning,
      deleteLoading,
      cloningQ,
      questionnairesState
    ]
  );

  useEffect(
    () => {
      if (startImportingAssessment) {
        if (importMetaData && importMetaData.rawData && importMetaData.importColumnDetails) {
          const { rawData, importColumnDetails, templateNames } = importMetaData;
          const datasets = AssessmentService.parseExcelDataIntoReadableFormat(
            rawData,
            importColumnDetails,
            templateNames
          );
          if (datasets && datasets.length > 1) {
            dispatch(qsImport(datasets[0]));
            setImportingAssessment(true);
            setStartImportingAssessment(false);
            setImportingDataSet(datasets);
            setCurrentImportIndex(0);
            setShowImportDetailModal(true);
            setShowExcelImportModal(false);
          } else {
            dispatch(qsImport(datasets[0], "0"));

            notification.success({
              message: "Importing Template",
              description: "Starting to import the template..."
            });
            setStartImportingAssessment(false);
            setImportingAssessment(true);
          }
        }
      }
    },
    // eslint-disable-next-line
    [startImportingAssessment]
  );

  useEffect(
    () => {
      if (importingAssessment) {
        if (importingDataSet.length && currentImportIndex >= 0 && currentImportIndex < importingDataSet.length) {
          const loading = get(questionnairesState, ["create", currentImportIndex, "loading"], true);
          const error = get(questionnairesState, ["create", currentImportIndex, "error"], true);
          if (!loading && !error) {
            const data = get(questionnairesState, ["create", currentImportIndex, "data"], {});
            if (currentImportIndex === importingDataSet.length - 1) {
              setImportingAssessment(false);
              setCurrentImportIndex((index: number) => index + 1);
              setImportMetaData({
                ...importMetaData,
                response: {
                  ...importMetaData.response,
                  [currentImportIndex]: data
                }
              });
            } else {
              const newIndex = currentImportIndex + 1;

              dispatch(qsImport(importingDataSet[newIndex], newIndex));

              setCurrentImportIndex(newIndex);
              setImportMetaData((importMD: any) => ({
                ...importMD,
                response: {
                  ...importMD.response,
                  [currentImportIndex]: data
                }
              }));
            }
          }
        } else {
          const loading = get(questionnairesState, ["import", "0", "loading"], true);
          const error = get(questionnairesState, ["import", "0", "error"], true);
          if (!loading && !error) {
            notification.success({
              message: "Success",
              description: "Assessment imported"
            });
            setImportingAssessment(false);
          }
        }
      }

      if (checkingName && checkNameListId) {
        const { loading, error } = get(questionnairesState, ["list", checkNameListId], { loading: true, error: true });
        if (!loading && !error) {
          const data = get(questionnairesState, ["list", checkNameListId, "data", "records"], []);
          const prevName = cloneTemplateName;
          // @ts-ignore
          dispatch(restapiClear("questionnaires", "list", checkNameListId));
          setCheckingName(false);
          setCheckNameListId(undefined);
          setNameExist(checkTemplateNameExists(prevName, data) ? NAME_EXISTS_ERROR : undefined);
        }
      }

      if (checkingImportName && showExcelImportModal && importMetaData && importMetaData.templateNames) {
        let templatesModified = false;
        const updatedTemplates = importMetaData.templateNames.map((template: any) => {
          if (!template.validating) {
            return template;
          }
          const loading = get(questionnairesState, ["list", template.id, "loading"], true);
          const error = get(questionnairesState, ["list", template.id, "error"], true);
          if (!loading && !error) {
            const data = get(questionnairesState, ["list", template.id, "data", "records"], []);
            templatesModified = true;
            dispatch(restapiClear("questionnaires", "list", template.id));
            return {
              ...template,
              validating: false,
              validate: false,
              valid: !checkTemplateNameExists(template.currentName, data)
            };
          }
          return template;
        });

        if (templatesModified) {
          setImportMetaData({
            ...importMetaData,
            templateNames: updatedTemplates
          });
        }
      }
    },
    // eslint-disable-next-line
    [questionnairesState]
  );

  useEffect(() => {
    setRowSelection((prev: any) => {
      return {
        ...prev,
        selectedRowKeys: selectedIds,
        onChange: onSelectChange,
        hideDefaultSelections: false
      };
    });
  }, [selectedIds]);

  const onRemoveHandler = (qId: string) => {
    setDeleteLoading(true);
    setQId(qId);
    dispatch(qsDelete(qId));
  };

  const handleImport = (file: any) => {
    const _fileName = file && file.name;
    const _extension = _fileName.split(".")[1];
    let _err = "";

    if (_extension !== "csv") {
      _err = "Upload a .csv file.";
      parseCSV(_err, { rows: [] });
      return;
    }

    const fileReader = new FileReader();
    fileReader.onload = (e: any) => {
      if (e && e.target) {
        const _dataarray = readString(e.target.result).data;
        const filteredData = _dataarray.filter((row: any) => row.length > 0 && row.some((value: any) => !!value));
        parseCSV(_err, { rows: filteredData });
      }
    };

    fileReader.readAsText(file);
  };

  const parseCSV = (err: any, resp: any) => {
    if (err) {
      notification.error({
        message: "Error",
        description: `Failed to import assessment. ${err}`
      });
      return;
    }

    const columnNames = AssessmentService.columnNameOptions(resp);

    const importColumnDetails = AssessmentService.initialColumns(resp);
    setShowExcelImportModal(true);
    setImportMetaData((metaData: any) => ({
      ...metaData,
      columnNames,
      rawData: resp,
      importColumnDetails
    }));
  };

  const onOKImportModal = (e: any) => {
    setShowExcelImportModal(false);
    setStartImportingAssessment(true);
  };

  const onCancelImportModal = () => {
    setShowExcelImportModal(false);
    setImportMetaData({ importColumnDetails: {} });
  };

  const closeImportDetailModal = () => {
    setShowImportDetailModal(false);
    setCurrentImportIndex(undefined);
    setImportingDataSet([]);
    setImportMetaData({ importColumnDetails: {} });
  };

  const buildActionOptions = (props: any) => {
    const actions = [
      {
        type: "copy",
        id: props.id,
        description: "Clone",
        onClickEvent: handleClone
      },
      {
        disabled: exportAssessmentId,
        type: "export",
        id: props.id,
        description: "Export",
        onClickEvent: handleExport
      },
      {
        type: "delete",
        id: props.id,
        description: "Delete",
        onClickEvent: onRemoveHandler
      }
    ];
    // @ts-ignore
    return <TableRowActions actions={actions} />;
  };

  const handleExport = (qId: string) => {
    notification.success({
      message: "Please wait...",
      description: "Generating CSV for assessment."
    });
    setExportingAssessment(true);
    setExportAssessmentId(qId);
    dispatch(qsExport(qId));
  };

  const onSelectChange = (rowKeys: any) => {
    setSelectedIds(rowKeys);
  };

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const onBulkDelete = () => {
    dispatch(qsBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  const tableSection = () => {
    const mappedColumns = tableColumns().map(column => {
      if (column.dataIndex === "id") {
        return {
          ...column,
          width: 100,
          render: (text: any, record: any, index: number) => buildActionOptions(record)
        };
      }
      if (column.dataIndex === "tag_ids") {
        return {
          ...column,
          apiCall: tagsList
        };
      }
      return column;
    });

    return (
      <ServerPaginatedTable
        pageName={"assessment_templates"}
        restCall="getQuestionnaires"
        uri={"questionnaires"}
        columns={mappedColumns}
        hasFilters
        moreFilters={moreFilters}
        partialFilters={partialFilters}
        clearSelectedIds={clearSelectedIds}
        rowSelection={rowSelection}
        onBulkDelete={onBulkDelete}
        reload={reload}
        hasDelete={true}
        bulkDeleting={bulkDeleting}
      />
    );
  };

  const importModel = () => {
    if (!showImportModal) {
      return null;
    }
    return (
      <AntModal
        visible
        title="Import from CSV"
        destroyOnClose
        width="60vw"
        closable
        footer={null}
        onCancel={() => {
          setShowImportModal(false);
        }}>
        <Dragger
          accept=".csv"
          multiple={false}
          showUploadList={false}
          height={320}
          beforeUpload={(file, fileList) => {
            setShowImportModal(false);
            handleImport(file);
            return false;
          }}>
          <p className="ant-upload-drag-icon">
            <Icon type="cloud-upload" />
          </p>
          <AntText style={{ fontSize: "20px" }} type="secondary">
            CSV upload
          </AntText>
          <p className="ant-upload-text">Drag and Drop to upload or click to browse</p>
        </Dragger>
      </AntModal>
    );
  };

  const onCancelEditCloneModal = () => {
    setOpenEditCloneModal(false);
    setDataToClone(undefined);
  };

  const onOkEditCloneModal = (name: string) => {
    notification.success({
      message: "Clone Template",
      description: "Starting to clone the template..."
    });

    setCloneQLoading(true);
    setCloningSections({});
    setCloningQ(false);
    setCloneTemplateName(name);
    setOpenEditCloneModal(false);
  };

  const handleClone = (id: string) => {
    const apiData = get(questionnairesState, ["list", "0", "data", "records"], []);
    let cloneData = undefined;
    if (apiData && apiData.length > 0) {
      cloneData = apiData.find((item: any) => item.id === id);
    }
    setDataToClone(cloneData);
    setOpenEditCloneModal(true);
  };

  const checkTemplateName = (name: string) => {
    const filters = {
      filter: {
        partial: {
          name: name
        }
      }
    };
    const checkNameListId = uuid();
    dispatch(genericList("questionnaires", "list", filters, null, checkNameListId));
    setCheckingName(true);
    setCheckNameListId(checkNameListId);
  };

  const onSearchEvent = useCallback((name: string) => {
    setCloneTemplateName(name);
    checkTemplateName(name);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const debounceSearch = useMemo(() => debounce(onSearchEvent, 300), []);

  const updateImportColumnDetails = (details: any) => {
    setImportMetaData((metaData: any) => ({
      ...metaData,
      importColumnDetails: details
    }));
  };

  const updateTemplateNames = (names: string) => {
    setImportMetaData((metaData: any) => ({
      ...metaData,
      templateNames: names
    }));
  };

  const showLoader = deleteLoading || importingAssessment;

  return (
    <>
      {showLoader && <Loader />}
      {!showLoader && tableSection()}
      <EditCloneModal
        visible={openEditCloneModal}
        title={"Clone Template"}
        onOk={onOkEditCloneModal}
        onCancel={onCancelEditCloneModal}
        nameExists={!!nameExist}
        searchEvent={debounceSearch}
        confirmButtonText={"Clone"}
      />
      <AssessmentExcelImportModal
        visible={showExcelImportModal}
        onOk={onOKImportModal}
        onCancel={onCancelImportModal}
        metaData={importMetaData}
        setTemplateDetails={data => updateImportColumnDetails(data)}
        setTemplateNames={data => updateTemplateNames(data)}
      />
      <ExcelImportDetailModal
        visible={showImportDetailModal}
        metaData={importMetaData}
        currentIndex={currentImportIndex}
        importList={importingDataSet}
        onOk={closeImportDetailModal}
      />
      {importModel()}
    </>
  );
};

export default ErrorWrapper(AssessmentTemplatesListPage);
