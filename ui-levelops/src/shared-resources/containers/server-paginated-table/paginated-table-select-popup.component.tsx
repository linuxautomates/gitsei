import * as React from "react";
import { Button, Col, Modal, Row } from "antd";
import WarningTriangle from "shared-resources/assets/svg-icons/warningTriangle.svg";
import { AntText, AntTitle } from "../../components";
import AntIconComponent from "../../components/ant-icon/ant-icon.component";
import Loader from "components/Loader/Loader";
import "./paginated-table-select-popup.style.scss";

interface PaginatedSelectPopupProps {
  selectedItemCount?: number;
  onCancel: any;
  onOk: any;
  deleteVisible: boolean;
  warningVisible: boolean;
  bulkDeleting?: boolean;
}

export const PaginatedSelectPopup: React.FC<PaginatedSelectPopupProps> = ({
  selectedItemCount,
  onCancel,
  onOk,
  deleteVisible = false,
  warningVisible = false,
  bulkDeleting,
  ...props
}) => {
  let line1;
  let line2 = `${selectedItemCount} record(s) selected`;

  if (deleteVisible) {
    line1 = "Do you want to delete these items?";
  } else {
    line1 = `Changing the filter will deselect the ${selectedItemCount} items you selected`;
  }

  return (
    <Modal
      visible={deleteVisible || warningVisible}
      onOk={onOk}
      onCancel={onCancel}
      closable={false}
      maskClosable={false}
      className="paginated-table-select-popup"
      bodyStyle={{ paddingBottom: 0 }}
      footer={[
        <Button key="back" onClick={onCancel}>
          Cancel
        </Button>,
        <>
          {(deleteVisible || warningVisible) && (
            <>
              {bulkDeleting && (
                <div className="loader">
                  <Loader />
                </div>
              )}
              {!bulkDeleting && (
                <Button key="submit" type={deleteVisible ? "danger" : "primary"} onClick={onOk}>
                  {deleteVisible ? "Delete" : "Yes, Clear Selections"}
                </Button>
              )}
            </>
          )}
        </>
      ]}>
      <div style={{ padding: "2rem" }}>
        {deleteVisible && (
          <>
            <Row>
              <Col span={3}>
                <AntIconComponent type="question-circle" className="delete-icon" />
              </Col>
              <Col span={21}>
                <AntTitle level={4} type={"primary"}>
                  {line1}
                </AntTitle>
              </Col>
            </Row>
            <Row>
              <Col span={23} offset={3}>
                <AntText>{line2}</AntText>
              </Col>
            </Row>
          </>
        )}
        {warningVisible && (
          <div className="warning-container">
            <WarningTriangle />
            <span className="warning-container__line">{line1}</span>
          </div>
        )}
      </div>
    </Modal>
  );
};
