import React, { useEffect, useState } from "react";
import { Row, Col, Typography, Button, Input } from "antd";

const { Paragraph } = Typography;
const { TextArea } = Input;

interface DescriptionProps {
  workItemId: string;
  description: string;
  displayDescription: boolean;
  onChange: (value: string) => void;
}

const DescriptionComponent: React.FC<DescriptionProps> = ({ displayDescription = true, ...props }) => {
  const [edit_desc, setEditDesc] = useState(false);
  const [description, setDescription] = useState("");
  const [work_item_id, setWorkItemId] = useState<undefined | string>(undefined);

  useEffect(() => {
    if (props.workItemId !== work_item_id) {
      setEditDesc(false);
      setDescription(props.description);
      setWorkItemId(props.workItemId);
    }
  }, [props]);

  const { onChange } = props;
  if (!displayDescription) {
    return null;
  }
  return (
    <Row style={{ marginTop: "10px", marginBottom: "10px" }}>
      <Col span={24}>
        <div className="flex direction-column">
          <div>
            <strong>Description</strong>
            <Button icon={"edit"} type={"link"} onClick={e => setEditDesc(true)}>
              Edit
            </Button>
          </div>
          {edit_desc && (
            <TextArea
              autoFocus={true}
              value={description}
              autoSize={{ minRows: 4, maxRows: 6 }}
              onBlur={e => {
                setEditDesc(false);
                onChange(description);
              }}
              onChange={e => setDescription(e.target.value)}
            />
          )}
          {props.description === "" ||
            (props.description === undefined && !edit_desc && (
              <TextArea
                disabled={true}
                autoSize={{ minRows: 4, maxRows: 6 }}
                placeholder={"Enter detailed description here"}
                onChange={e => onChange(e.target.value)}
              />
            ))}
          {!edit_desc && (
            <Paragraph className="desc-normal" style={{ overflowWrap: "break-word" }}>
              {description}
            </Paragraph>
          )}
        </div>
      </Col>
    </Row>
  );
};

export default DescriptionComponent;
