import { Col, Divider, Form, Icon, Row, Typography } from "antd";
import { RestWorkItem } from "classes/RestWorkItem";
import { GenericFormComponent } from "../../../../../shared-resources/containers/generic-form-component/generic-form.component";
import React from "react";
import { SmartTicketUpdate } from "../../components";
import { WORKITEM_ACTION } from "./helper";
import { AntSelect, AvatarWithText } from "../../../../../shared-resources/components";
import { TICKET_TYPES } from "../../../../../constants/workitem";

interface SmartTicketEditFieldsProps {
  workItem: RestWorkItem;
  templateFields: any;
  tags: Array<any>;
  product: { key: string; label: string };
  values: any;
  showMore: boolean;
  firstShow: boolean;
  ticketType: string;
  assignees: Array<any>;
  onUpdate: (action: WORKITEM_ACTION, payload: any) => void;
}

const SmartTicketEditFields: React.FC<SmartTicketEditFieldsProps> = (props: SmartTicketEditFieldsProps) => {
  const { Text } = Typography;
  const { templateFields, workItem, tags, product, values, showMore, firstShow, assignees, ticketType, onUpdate } =
    props;
  const fields = templateFields.map((field: any) => ({
    ...field,
    label: field.display_name,
    key: field.ticket_field_id,
    values: values[field.ticket_field_id]
  }));

  const smartTicketupdate = (
    <SmartTicketUpdate
      defaultFields={workItem.default_fields}
      assignees={assignees}
      onAssigneesSelect={(options: any) => onUpdate(WORKITEM_ACTION.ASSIGNEE_CHANGE, options)}
      status={{
        key: workItem.state_id,
        label: ""
      }}
      product={product}
      tags={tags}
      onStatusSelect={(option: any) => onUpdate(WORKITEM_ACTION.WORKITME_STATUS_CHANGE, option)}
      due={workItem.due_at ? workItem.due_at : undefined}
      onDueDateChange={(value: any) => onUpdate(WORKITEM_ACTION.WORKITEM_DUE_DATE_CHANGE, value)}
      onTagsChange={(updatedTags: any) => onUpdate(WORKITEM_ACTION.UPDATED_TAGS, updatedTags)}
      onProductChange={(product: any) => onUpdate(WORKITEM_ACTION.WORKITEM_PRODUCT_CHANGE, product)}
    />
  );

  return (
    <Col span={6} className="side-panel">
      <div className="fields">
        <Form layout={"vertical"}>
          {workItem.reporter && (
            <Form.Item label={"OWNER"}>
              <AvatarWithText text={workItem.reporter} avatarText={workItem.reporter} />
            </Form.Item>
          )}
          {workItem.default_fields &&
            workItem.default_fields.type !== undefined &&
            workItem.default_fields.type !== false && (
              <Form.Item label={"TICKET TYPE"}>
                <AntSelect
                  options={TICKET_TYPES}
                  disabled={ticketType === "FAILURE_TRIAGE"}
                  onChange={(option: any) => onUpdate(WORKITEM_ACTION.TICKET_TYPE_CHANGE, option)}
                  value={ticketType || RestWorkItem.DEFAULT_TICKET_TYPE}
                />
              </Form.Item>
            )}
        </Form>
        {smartTicketupdate}
        <Divider />
        {workItem.ticket_data_values.length > 0 && (
          <Text>
            <a
              href={"#"}
              onClick={e => {
                e.preventDefault();
                onUpdate(WORKITEM_ACTION.TOGGLE_MORE_CUSTOM_FIELDS, showMore);
              }}>
              <Icon
                type={"down"}
                className={["icon-transition", "pl-5", "pr-5", !!showMore ? "expanded" : "collapsed"].join(" ")}
              />
              {showMore ? "Hide Custom Fields" : "Show Custom Fields "}
            </a>
          </Text>
        )}

        {firstShow === true && (
          <div style={{ marginTop: "1rem", display: showMore ? "block" : "none" }}>
            <GenericFormComponent
              layout="vertical"
              onChange={(values: any) => onUpdate(WORKITEM_ACTION.UPDATE_CUSTOM_FIELDS, values)}
              elements={fields}
            />
          </div>
        )}
      </div>
    </Col>
  );
};

export default SmartTicketEditFields;
