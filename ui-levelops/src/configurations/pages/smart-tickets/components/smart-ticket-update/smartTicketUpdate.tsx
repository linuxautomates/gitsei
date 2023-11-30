import React, { useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { DatePicker, Popconfirm, Typography } from "antd";
import moment from "moment";
import { SelectRestapi } from "shared-resources/helpers";
import { AntCol, AntRow } from "shared-resources/components";
import { AssigneeSelect } from "../index";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";

const { Text } = Typography;

interface SmartTicketUpdateProps {
  status: any;
  due: number;
  onAssigneesSelect: (assignee: any[]) => void;
  onStatusSelect: (options: any) => void;
  onDueDateChange: (value: any) => void;
  direction?: string;
  product: any;
  onTagsChange: (updatedTags: any) => void;
  onProductChange: (value: any) => void;
  tags: any[];
  defaultFields: any;
  assignees: any[];
}

export const SmartTicketUpdateComponent: React.FC<SmartTicketUpdateProps> = ({
  status = {},
  tags = [],
  onAssigneesSelect = assignee => {},
  onStatusSelect = () => {},
  onDueDateChange = () => {},
  direction = "column",
  ...props
}) => {
  const sessions = useSelector(state => mapSessionStatetoProps(state));
  const selectStyle = { width: "100%" };

  const [showProductPopConfirm, setShowProductPopConfirm] = useState(false);
  const [newProduct, setNewProduct] = useState<null | any>(null);

  const assigneeSelect = () => {
    return (
      // @ts-ignore
      <AssigneeSelect
        assignees={props.assignees || null}
        onAssigneesChange={onAssigneesSelect}
        placeholder="UNASSIGNED"
        allowCreateAssignee={true}
      />
    );
  };

  const ticketAssignees = () => {
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol className="gutter-row" span={24}>
          <Text type="secondary">ASSIGNEES</Text>
          {assigneeSelect()}
        </AntCol>
      </AntRow>
    );
  };

  const ticketStatus = () => {
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol className="gutter-row" span={24}>
          <Text type="secondary">STATUS</Text>
          <SelectRestapi
            style={selectStyle}
            placeholder="Select Status..."
            mode="single"
            uri="states"
            uuid={"workitems"}
            fetchOnMount={false}
            searchField="name"
            value={status}
            onChange={onStatusSelect}
            labelinValue
            allowClear={false}
            label_case="title_case"
          />
        </AntCol>
      </AntRow>
    );
  };

  const dueOn = () => {
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol className="gutter-row" span={24}>
          <Text type="secondary">DUE DATE</Text>
          <DatePicker
            style={selectStyle}
            onChange={onDueDateChange}
            // @ts-ignore
            value={props.due ? moment.unix(props.due) : props.due}
          />
        </AntCol>
      </AntRow>
    );
  };

  const ticketTags = () => {
    if (props.defaultFields.tags === false) {
      return null;
    }
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol className="gutter-row" span={24}>
          <Text type="secondary">TAGS</Text>{" "}
          <SelectRestapi
            className="w-100"
            value={tags || null}
            placeholder="Select tags"
            mode="multiple"
            labelInValue
            uri="tags"
            uuid={"workitems"}
            fetchOnMount={false}
            createOption
            allowClear={false}
            searchField="name"
            onChange={props.onTagsChange}
          />
        </AntCol>
      </AntRow>
    );
  };

  const handleConfirm = () => {
    props.onProductChange(newProduct);
    setShowProductPopConfirm(false);
  };

  const handleCancel = () => {
    setShowProductPopConfirm(false);
  };

  const handleChange = (product: any) => {
    setShowProductPopConfirm(true);
    setNewProduct(product);
  };

  const smartTicketProjectUpdateDisabled = useMemo(
    () => !getRBACPermission(PermeableMetrics.SMART_TICKET_PROJECT_UPDATE),
    []
  );

  const product_dropdown = () => {
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol className="gutter-row" span={24}>
          <Text type="secondary">PROJECT</Text>
          <Popconfirm
            title={"Changing project will change the Issue ID. Are you sure you want to do it"}
            visible={showProductPopConfirm}
            onConfirm={handleConfirm}
            onCancel={handleCancel}>
            <SelectRestapi
              searchField="name"
              disabled={smartTicketProjectUpdateDisabled}
              style={{ width: "100%" }}
              mode={"single"}
              placeholder="Select Project"
              uri="workspace"
              uuid={"workitems"}
              fetchOnMount={false}
              value={props.product || {}}
              onChange={handleChange}
              labelinValue={true}
              allowClear={false}
            />
          </Popconfirm>
        </AntCol>
      </AntRow>
    );
  };

  return (
    <>
      {ticketStatus()}
      {product_dropdown()}
      {dueOn()}
      {ticketAssignees()}
      {ticketTags()}
    </>
  );
};

export default SmartTicketUpdateComponent;
