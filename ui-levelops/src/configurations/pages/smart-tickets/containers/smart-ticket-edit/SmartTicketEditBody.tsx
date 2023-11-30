import { Col, Row } from "antd";
import { RestWorkItem } from "classes/RestWorkItem";
import { map } from "lodash";
import React from "react";
import { AntMultiUpload } from "shared-resources/components";
import { AttachmentItem } from "shared-resources/components/attachment-item/attachment-item.component";
import { SmartTicketHeader, TriageJobRuns } from "../../components";
import { WORKITEM_ACTION } from "./helper";
import PreviewerComponent from "shared-resources/components/previewer/previewer";
import SmartTicketsTabsContainer from "../tabs/smartTicketTabs";

interface SmartTicketEditBodyProps {
  workItem: RestWorkItem;
  attachments: Array<any>;
  showPreviewer: boolean;
  currentPreviewIndex: number;
  onReroute: (url: string) => void;
  onUpdate: (action: WORKITEM_ACTION, payload: any) => void;
  onRefresh: () => void;
}

const SmartTicketEditBody: React.FC<SmartTicketEditBodyProps> = (props: SmartTicketEditBodyProps) => {
  const { onUpdate, workItem, showPreviewer, currentPreviewIndex, onReroute } = props;

  const header = (
    <SmartTicketHeader
      workItem={workItem}
      handleDeleteConfirm={() => onUpdate(WORKITEM_ACTION.WORKITEM_DELETE, {})}
      handleMenuClick={(e: any) => onUpdate(WORKITEM_ACTION.WORKITEM_MENU_CLICK, e)}
      handleNameChange={(name: string) => onUpdate(WORKITEM_ACTION.WORKITEM_NAME_UPDATE, name)}
      handleDescriptionChange={(desc: string) => onUpdate(WORKITEM_ACTION.WORKITEM_DESCRIPTION_UPDATE, desc)}
    />
  );

  const attachments = (
    <Row style={{ marginTop: "10px", marginBottom: "10px" }}>
      <Col span={24} className="flex direction-column">
        <div>
          <strong>Attachments</strong>
        </div>
        <AntMultiUpload
          onChange={(attachments: any) => onUpdate(WORKITEM_ACTION.WORKITEM_FILE_CHANGE, attachments)}
          onAddFile={(file: any) => onUpdate(WORKITEM_ACTION.WORKITEM_FILE_ADD, file)}
          onDownloadFile={(file: any) => onUpdate(WORKITEM_ACTION.WORKITEM_FILE_DOWNLOAD, file)}
          files={[]}
          onRemove={(file: any) => onUpdate(WORKITEM_ACTION.WORKITEM_FILE_REMOVE, file)}
          showRemove={false}
          multiple={true}
        />
        {props.attachments.length > 0 &&
          map(props.attachments, (file: { file_name: any }) => (
            <AttachmentItem
              fileName={file.file_name}
              previewFile={() => onUpdate(WORKITEM_ACTION.WORKITEM_FILE_PREVIEW, file)}
              removeFile={() => onUpdate(WORKITEM_ACTION.WORKITEM_FILE_REMOVE, file)}
            />
          ))}
      </Col>
    </Row>
  );

  const previewer = (
    <PreviewerComponent
      onClose={() => onUpdate(WORKITEM_ACTION.WORKITEM_FILE_CLOSE_PREVIEW, undefined)}
      onDownload={(e: any, id: any, name: any) =>
        onUpdate(WORKITEM_ACTION.WORKITEM_FILE_DOWNLOAD, { fileId: id, fileName: name })
      }
      list={map(props.attachments, (file: any, index: number) => ({
        fileId: file.upload_id,
        fileName: file.file_name || `Attachment ${index}`,
        fetchFileId: `tickets/${workItem.id}/${file.upload_id}`
      }))}
      currentIndex={currentPreviewIndex}
    />
  );
  return (
    <Col span={18} className="main-panel">
      {header}
      {workItem.default_fields.attachments === false ? null : attachments}
      <TriageJobRuns workItem={workItem} />
      {showPreviewer && previewer}
      <Row gutter={[16, 16]} type={"flex"}>
        <Col span={24}>
          <SmartTicketsTabsContainer
            workItemId={workItem.id}
            workItemParentId={workItem.parent_id}
            workItemChildren={workItem.child_ids || []}
            productId={workItem.product_id}
            onReroute={onReroute}
            onRefresh={props.onRefresh}
          />
        </Col>
      </Row>
    </Col>
  );
};

export default SmartTicketEditBody;
