import * as React from "react";
import { Col, Icon, Modal, Popover, Row, Select } from "antd";
import { ModalProps } from "antd/es/modal";
import { useEffect, useState } from "react";
import "./excelImportModal.style.scss";
import { uniq } from "lodash";
import { TemplatesNameValidator } from "./template-name.component";
import { AntButton, AntText } from "../../../../shared-resources/components";
import { RestQuestion } from "classes/RestQuestionnaire";
import { AssessmentService } from "services/assessment/assessment.service";

const requiredColumns = ["sectionName", "questionName"];

interface AssessmentExcelImportModalProps extends ModalProps {
  metaData: any;
  setTemplateDetails: (data: any) => void;
  setTemplateNames: (name: any) => void;
}

export const AssessmentExcelImportModal: React.FC<AssessmentExcelImportModalProps> = props => {
  const { setTemplateDetails } = props;
  const templateDetails: any = props.metaData.importColumnDetails || {};
  const [options, setOptions] = useState<{ label: string; value: string; type: string }[]>([]);
  const [showError, setShowError] = useState<boolean>(false);
  const [isImportValid, setIsImportValid] = useState<boolean>(true);
  const [showNameValidator, setShowNameValidator] = useState<boolean>(false);
  const [columnValidationList, setColumnValidationList] = useState<any>({});

  useEffect(() => {
    if (props.metaData && props.metaData.columnNames && props.metaData.columnNames.length > 0) {
      console.log(props.metaData.columnNames);
      const newOptions = props.metaData.columnNames.map((col: any) => ({
        ...col,
        // @ts-ignore
        type: AssessmentService.DEFAULT_HEADER_MAPPING[col.label.toString().toLowerCase()] || ""
      }));
      setOptions(newOptions);
      const colDetails = props.metaData.importColumnDetails || {};
      Object.keys(colDetails).forEach((key: string) => validateColumnData(key, colDetails[key]));
    }
  }, [props.metaData.columnNames]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (props.metaData.templateNames && props.metaData.templateNames.length > 0) {
      setIsImportValid(props.metaData.templateNames.every((item: any) => !!item.currentName && item.valid));
    }
  }, [props.metaData]);

  useEffect(() => {
    if (!props.visible) {
      setColumnValidationList({});
    }
  }, [props.visible]);

  const selectChanged = (key: string, value: string | Array<string>) => {
    // let currentValue = templateDetails[key];
    // let newValue = value;

    // if (typeof newValue === "object") {
    //   if (currentValue && currentValue.length < (newValue as Array<string>).length) {
    //     // add
    //     newValue = difference(newValue, currentValue)[0];
    //   } else if (currentValue && currentValue.length > (newValue as Array<string>).length) {
    //     // remove
    //     currentValue = difference(currentValue, newValue)[0];
    //   } else if (!currentValue) {
    //     // first entry
    //     newValue = value[0];
    //   }
    // }

    // const currentIndex = options.findIndex((option: any) => option.value === currentValue);
    // const newIndex = options.findIndex((option: any) => option.value === newValue);

    setTemplateDetails({ ...templateDetails, [key]: value });
    // setOptions((options: any) => {
    //   return options.map((opt: any, index: number) => {
    //     let option = { ...opt };
    //     if (newIndex !== -1 && index === newIndex) {
    //       option.type = key;
    //     }
    //
    //     if (currentIndex !== -1 && index === currentIndex) {
    //       option.type = "";
    //     }
    //
    //     return option;
    //   });
    // });
    validateColumnData(key, value);
  };

  const onOkClick = (e: any) => {
    if (!showNameValidator) {
      if (!requiredColumns.every((col: any) => !!templateDetails[col] && !!templateDetails[col].length)) {
        setShowError(true);
        return;
      }

      if (showError) {
        setShowError(false);
      }
      setShowNameValidator(true);
      getInitialTemplateNames();
    } else {
      if (!isImportValid) {
        return;
      }

      setShowNameValidator(false);
      props.onOk && props.onOk(e);
    }
  };

  const hasError = (key: string) => {
    return showError && requiredColumns.includes(key) && !templateDetails[key];
  };

  const getInitialTemplateNames = () => {
    if (props.metaData.rawData && templateDetails.templateName) {
      const nameIndex = props.metaData.rawData.rows[0].findIndex((col: string) => col === templateDetails.templateName);
      const names = uniq(props.metaData.rawData.rows.slice(1).map((row: any) => row[nameIndex]))
        .filter(val => !!val)
        .map(value => ({
          id: value,
          initialName: value as string,
          currentName: value as string,
          valid: false,
          blur: true,
          validating: false,
          validate: true
        }));
      props.setTemplateNames(names);
    } else {
      props.setTemplateNames([
        {
          id: "singleTemplate",
          initialName: "",
          currentName: "",
          valid: false,
          blur: true,
          validating: false,
          validate: true
        }
      ]);
    }
  };

  const validateColumnData = (columnName: string, csvColumnName: string | string[]) => {
    if (!csvColumnName && requiredColumns.includes(columnName)) {
      setColumnValidationList((list: any) => ({ ...list, [columnName]: "Invalid column name" }));
      return;
    }
    const rows = props.metaData.rawData.rows.slice(1).filter((row: string[]) => row.length > 0);

    if (columnName === "sectionDescription" || (columnName === "tagNames" && typeof csvColumnName === "object")) {
      return;
      // let error = "";
      // csvColumnName.forEach((col: string) => {
      //   if (error) {
      //     return;
      //   }
      //   const csvIndex = props.metaData.rawData.rows[0].findIndex((item: string) => item === col);
      //   // filtering empty rows
      //   const values = rows.map((row: string[]) => row[csvIndex]);
      //   for (let i = 0; i < values.length; i++) {
      //     if (!values[i]) {
      //       error = `Invalid value at row number ${i + 2} and col ${csvIndex + 1}`;
      //       break;
      //     }
      //   }
      // });
      // if (error) {
      //   setColumnValidationList((list: any) => ({ ...list, [columnName]: error }));
      //   return;
      // }
      // setColumnValidationList((list: any) => ({ ...list, [columnName]: "" }));
      // return;
    }

    const csvIndex = props.metaData.rawData.rows[0].findIndex((item: string) => item === csvColumnName);
    const values = rows.map((row: string[]) => (!!row[csvIndex] ? row[csvIndex] : ""));

    let error = "";
    for (let i = 0; i < values.length; i++) {
      if (!values[i]) {
        error = `Invalid value at row number ${i + 2} and col ${csvIndex + 1}`;
        break;
      }
    }
    if (error && requiredColumns.includes(columnName)) {
      setColumnValidationList((list: any) => ({ ...list, [columnName]: error }));
      return;
    }

    if ([...requiredColumns, "templateName"].includes(columnName)) {
      setColumnValidationList((list: any) => ({ ...list, [columnName]: "" }));
      return;
    }

    if (columnName === "requiredName") {
      let error = "";
      for (let i = 0; i < values.length; i++) {
        if (!["yes", "no"].includes(values[i].toString().toLowerCase())) {
          error = `Invalid value at row number ${i + 2} and col ${csvIndex + 1}`;
          break;
        }
      }
      if (error) {
        setColumnValidationList((list: any) => ({ ...list, requiredName: error }));
        return;
      }
      setColumnValidationList((list: any) => ({ ...list, requiredName: "" }));
    }

    if (columnName === "typeName") {
      let error = "";
      for (let i = 0; i < values.length; i++) {
        if (!RestQuestion.TYPES.includes(values[i].toString().toLowerCase())) {
          error = `Invalid value at row number ${i + 2} and col ${csvIndex + 1}`;
          break;
        }
      }
      if (error) {
        setColumnValidationList((list: any) => ({ ...list, typeName: error }));
        return;
      }
      setColumnValidationList((list: any) => ({ ...list, typeName: "" }));
    }

    if (columnName === "severityName") {
      // validate all col names
      let error = "";
      for (let i = 0; i < values.length; i++) {
        if (!["low", "medium", "high"].includes(values[i].toString().toLowerCase())) {
          error = `Invalid value at row number ${i + 2} and col ${csvIndex + 1}`;
          break;
        }
      }

      if (error) {
        setColumnValidationList((list: any) => ({ ...list, severityName: error }));
        return;
      }
      setColumnValidationList((list: any) => ({ ...list, severityName: "" }));
    }
  };

  return (
    <Modal
      className="excelImport"
      visible={props.visible}
      title="Import templates"
      onCancel={(e: any) => {
        setShowNameValidator(false);
        props.onCancel && props.onCancel(e);
      }}
      footer={
        <>
          <AntButton
            key="cancel"
            onClick={(e: any) => {
              setShowNameValidator(false);
              props.onCancel && props.onCancel(e);
            }}>
            Cancel
          </AntButton>
          {showNameValidator && (
            <AntButton key="back" onClick={() => setShowNameValidator(false)}>
              Back
            </AntButton>
          )}
          <AntButton
            key="submit"
            type="primary"
            disabled={
              (showNameValidator && !isImportValid) ||
              !Object.keys(columnValidationList).every((key: string) => !columnValidationList[key])
            }
            onClick={(e: any) => onOkClick(e)}>
            {showNameValidator ? "Import" : "Next"}
          </AntButton>
        </>
      }
      bodyStyle={{ maxHeight: "60vh", overflowY: "auto", margin: "1rem 0" }}
      width="50vw">
      {!showNameValidator && (
        <div className="excelImportContent">
          <Row>
            <Col span={11}>
              Template Name Column
              <AntText type="danger">{requiredColumns.includes("templateName") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <AntText type="danger">{requiredColumns.includes("templateName") ? "*" : " "}</AntText>
              <Select
                allowClear={true}
                className={hasError("templateName") || !!columnValidationList["templateName"] ? "selectError" : ""}
                style={{ width: "100%" }}
                value={templateDetails.templateName}
                onChange={(val: string) => selectChanged("templateName", val)}>
                {options
                  // .filter(option => option.type === "templateName" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            <Col span={2}>
              <Popover
                content={
                  !!columnValidationList["templateName"]
                    ? columnValidationList["templateName"]
                    : "Select this field to import multiple templates"
                }
                title="Note">
                <Icon
                  type="info-circle"
                  className="importInfo"
                  style={{ color: !!columnValidationList["templateName"] ? "red" : "" }}
                />
              </Popover>
            </Col>
          </Row>
          <Row>
            <Col span={11}>
              Tag Name Columns<AntText type="danger">{requiredColumns.includes("tagNames") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                className={hasError("tagNames") || !!columnValidationList["tagNames"] ? "selectError" : ""}
                mode="multiple"
                style={{ width: "100%" }}
                value={templateDetails.tagNames}
                onChange={(val: string) => selectChanged("tagNames", val)}>
                {options
                  // .filter(option => option.type === "tagNames" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            {!!columnValidationList["tagNames"] && (
              <Col span={2}>
                <Popover content={columnValidationList["tagNames"]} title="Note">
                  <Icon type="info-circle" className="importInfo" style={{ color: "red" }} />
                </Popover>
              </Col>
            )}
          </Row>
          <Row>
            <Col span={11}>
              Section Name Column<AntText type="danger">{requiredColumns.includes("sectionName") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                className={hasError("sectionName") || !!columnValidationList["sectionName"] ? "selectError" : ""}
                style={{ width: "100%" }}
                value={templateDetails.sectionName}
                onChange={(val: string) => selectChanged("sectionName", val)}>
                {options
                  // .filter(option => option.type === "sectionName" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            {!!columnValidationList["sectionName"] && (
              <Col span={2}>
                <Popover content={columnValidationList["sectionName"]} title="Note">
                  <Icon type="info-circle" className="importInfo" style={{ color: "red" }} />
                </Popover>
              </Col>
            )}
          </Row>
          <Row>
            <Col span={11}>
              Section Description Column
              <AntText type="danger">{requiredColumns.includes("sectionDescription") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                className={
                  hasError("sectionDescription") || !!columnValidationList["sectionDescription"] ? "selectError" : ""
                }
                style={{ width: "100%" }}
                value={templateDetails.sectionDescription}
                onChange={(val: string) => selectChanged("sectionDescription", val)}>
                {options
                  // .filter(option => option.type === "sectionDescription" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            {!!columnValidationList["sectionDescription"] && (
              <Col span={2}>
                <Popover content={columnValidationList["sectionDescription"]} title="Note">
                  <Icon type="info-circle" className="importInfo" style={{ color: "red" }} />
                </Popover>
              </Col>
            )}
          </Row>
          <Row>
            <Col span={11}>
              Questions Column<AntText type="danger">{requiredColumns.includes("questionName") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                className={hasError("questionName") || !!columnValidationList["questionName"] ? "selectError" : ""}
                style={{ width: "100%" }}
                value={templateDetails.questionName}
                onChange={(val: string) => selectChanged("questionName", val)}>
                {options
                  // .filter(option => option.type === "questionName" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            {!!columnValidationList["questionName"] && (
              <Col span={2}>
                <Popover content={columnValidationList["questionName"]} title="Note">
                  <Icon type="info-circle" className="importInfo" style={{ color: "red" }} />
                </Popover>
              </Col>
            )}
          </Row>
          <Row>
            <Col span={11}>
              Question Required<AntText type="danger">{requiredColumns.includes("requiredName") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                allowClear={true}
                className={hasError("requiredName") || !!columnValidationList["requiredName"] ? "selectError" : ""}
                style={{ width: "100%" }}
                value={templateDetails.requiredName}
                onChange={(val: string) => selectChanged("requiredName", val)}>
                {options
                  // .filter(option => option.type === "requiredName" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            <Col span={2}>
              <Popover
                content={
                  !!columnValidationList["requiredName"]
                    ? columnValidationList["requiredName"]
                    : "If not selected default value will be 'Not required'"
                }
                title="Note">
                <Icon
                  type="info-circle"
                  className="importInfo"
                  style={{ color: !!columnValidationList["requiredName"] ? "red" : "" }}
                />
              </Popover>
            </Col>
          </Row>
          <Row>
            <Col span={11}>
              Question Type<AntText type="danger">{requiredColumns.includes("typeName") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                allowClear={true}
                className={hasError("typeName") || !!columnValidationList["typeName"] ? "selectError" : ""}
                style={{ width: "100%" }}
                value={templateDetails.typeName}
                onChange={(val: string) => selectChanged("typeName", val)}>
                {options
                  // .filter(option => option.type === "typeName" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            <Col span={2}>
              <Popover
                content={
                  !!columnValidationList["typeName"]
                    ? columnValidationList["typeName"]
                    : "If not selected default value will be 'Text'"
                }
                title="Note">
                <Icon
                  type="info-circle"
                  className="importInfo"
                  style={{ color: !!columnValidationList["typeName"] ? "red" : "" }}
                />
              </Popover>
            </Col>
          </Row>
          <Row>
            <Col span={11}>
              Question Severity<AntText type="danger">{requiredColumns.includes("severityName") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                allowClear={true}
                className={hasError("severityName") || !!columnValidationList["severityName"] ? "selectError" : ""}
                style={{ width: "100%" }}
                value={templateDetails.severityName}
                onChange={(val: string) => selectChanged("severityName", val)}>
                {options
                  // .filter(option => option.type === "severityName" || option.type === "")
                  .map((opt: any) => (
                    <Select.Option key={opt.value} value={opt.value}>
                      {opt.label}
                    </Select.Option>
                  ))}
              </Select>
            </Col>
            <Col span={2}>
              <Popover
                content={
                  !!columnValidationList["severityName"]
                    ? columnValidationList["severityName"]
                    : "If not selected default value will be 'Medium'"
                }
                title="Note">
                <Icon
                  type="info-circle"
                  className="importInfo"
                  style={{ color: !!columnValidationList["severityName"] ? "red" : "" }}
                />
              </Popover>
            </Col>
          </Row>
          <Row>
            <Col span={11}>
              Knowledge Base<AntText type="danger">{requiredColumns.includes("knowledgeBase") ? "*" : " "}</AntText>
            </Col>
            <Col span={11}>
              <Select
                allowClear={true}
                className={hasError("knowledgeBase") || !!columnValidationList["knowledgeBase"] ? "selectError" : ""}
                style={{ width: "100%" }}
                value={templateDetails.knowledgeBase}
                onChange={(val: string) => selectChanged("knowledgeBase", val)}>
                {options.map((opt: any) => (
                  <Select.Option key={opt.value} value={opt.value}>
                    {opt.label}
                  </Select.Option>
                ))}
              </Select>
            </Col>
          </Row>
        </div>
      )}
      {showNameValidator && (
        <TemplatesNameValidator
          templateNames={props.metaData.templateNames}
          setTemplateNames={props.setTemplateNames}
        />
      )}
    </Modal>
  );
};
