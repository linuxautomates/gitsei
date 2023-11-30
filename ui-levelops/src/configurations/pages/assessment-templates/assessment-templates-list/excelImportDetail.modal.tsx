import { ModalProps } from "antd/lib/modal";
import * as React from "react";
import { Button, Col, Icon, Modal, Row } from "antd";
import { AntText } from "../../../../shared-resources/components";
import "./excelImportModal.style.scss";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";

interface ExcelImportDetailModalProps extends ModalProps {
  metaData: any;
  currentIndex: any;
  importList: any[];
}

export const ExcelImportDetailModal: React.FC<ExcelImportDetailModalProps> = (props: ExcelImportDetailModalProps) => {
  const {
    utils: { getLocationPathName }
  } = useParentProvider();
  const navigateToDetail = (index: number) => {
    window.open(
      `${removeLastSlash(getLocationPathName?.())}${getBaseUrl()}${
        TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.EDIT
      }?questionnaire=${props.metaData.response[index].id}`,
      "_blank"
    );
  };

  return (
    <Modal
      className="excelImportDetails"
      bodyStyle={{ maxHeight: "60vh", overflowY: "auto", margin: "2rem 0" }}
      visible={props.visible}
      closable={false}
      footer={
        <>
          {props.metaData.response && Object.keys(props.metaData.response).length === props.importList.length && (
            <Button type={"primary"} onClick={props.onOk}>
              Close
            </Button>
          )}
        </>
      }
      width="45vw"
      title={
        props.metaData.response && Object.keys(props.metaData.response).length === props.importList.length ? (
          <>
            <Icon style={{ fontSize: "40px", color: "green", marginRight: "1rem" }} type="check-circle" /> Imported
            templates
          </>
        ) : (
          <>
            <Icon style={{ fontSize: "40px", marginRight: "1rem" }} type="loading" /> Importing Templates...
          </>
        )
      }>
      <>
        <Row style={{ marginTop: "1rem" }}>
          <Col span={10}>
            <AntText style={{ fontWeight: 600 }}>Template Name</AntText>
          </Col>
          <Col span={4}>
            <AntText style={{ fontWeight: 600 }}>Import Status</AntText>
          </Col>
          <Col span={10}>
            <AntText style={{ fontWeight: 600, paddingLeft: "15px" }}>Detail link</AntText>
          </Col>
        </Row>
        {props.metaData &&
          (props.importList || []).map((item: any, index: number) => (
            <Row>
              <Col span={10}>
                <AntText>{item.name}</AntText>
              </Col>
              <Col span={4}>
                <AntText>
                  {index < props.currentIndex ? (
                    <>
                      <Icon style={{ fontSize: "20px", color: "green" }} type="check-circle" /> Done
                    </>
                  ) : (
                    <>
                      <Icon style={{ fontSize: "20px" }} type="loading" /> Loading...
                    </>
                  )}
                </AntText>
              </Col>
              <Col span={10}>
                <Button type={"link"} onClick={() => navigateToDetail(index)} disabled={index >= props.currentIndex}>
                  Go to detail
                </Button>
              </Col>
            </Row>
          ))}
      </>
    </Modal>
  );
};
