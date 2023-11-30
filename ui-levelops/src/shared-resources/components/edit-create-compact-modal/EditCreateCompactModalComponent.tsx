import React, { useState, useEffect, ChangeEvent } from "react";
import { Input, Modal, Form, Switch } from "antd";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { AntColComponent as AntCol } from "../ant-col/ant-col.component";
import { default as AntRow } from "../ant-row/ant-row.component";
import { NAME_EXISTS_ERROR, REQUIRED_FIELD } from "../../../constants/formWarnings";
import TextArea from "antd/lib/input/TextArea";
import "./EditCreateCompactModalComponent.style.scss";
import { isEqual } from "lodash";
import Loader from "components/Loader/Loader";
import { EditCreateModalFormInfo, EditCreateModalPayload } from "model/modal/EditCreateModal";

interface EditCreateCompactModalProps {
  onOk: (item: EditCreateModalPayload | undefined) => void;
  data?: EditCreateModalPayload;
  title: string;
  visible: boolean;
  loading: boolean;
  formInfo: EditCreateModalFormInfo;
  onCancel: () => void;
  nameExists: boolean;
  hasDefault?: boolean;
  searchEvent: (value: string) => void;
  initialData?: EditCreateModalPayload;
  hasUniqueName?: boolean;
  hasDescription?: boolean;
  disableNameEdit?: boolean;
}

const EditCreateCompactModalComponent: React.FC<EditCreateCompactModalProps> = ({
  data,
  title,
  visible,
  loading,
  formInfo,
  nameExists,
  hasDefault = true,
  searchEvent,
  initialData,
  hasUniqueName = true,
  hasDescription = true,
  disableNameEdit = false,
  ...props
}) => {
  const [formData, setFormData] = useState<EditCreateModalPayload | undefined>(initialData);
  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);

  useEffect(() => {
    if (data && Object.keys(data).length > 0 && !isEqual(formData, data)) {
      setFormData(data as any);
    }
  }, [data]);

  const getBtnStatus = () => {
    return !(formData && formData.name && !nameExists);
  };

  const getValidateStatus = () => {
    if (!nameFieldBlur) {
      return "";
    } else if (nameFieldBlur && formData && formData.name?.length > 0 && !nameExists) {
      return "success";
    } else return "error";
  };

  const getError = () => {
    if (nameExists === true) {
      return NAME_EXISTS_ERROR;
    } else return REQUIRED_FIELD;
  };

  const onCancel = () => {
    props.onCancel();
  };

  const onOk = () => {
    props.onOk(formData);
    setNameFieldBlur(false);
  };

  const onFieldChange = (e: ChangeEvent<HTMLInputElement>) => {
    const name = e.target.value;
    setFormData(form => ({ ...form, name }));
    if (hasUniqueName) {
      searchEvent(name);
    }
  };

  const onDescriptionFieldChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    const description = e.target.value;
    setFormData(form => ({ ...form, description }));
  };

  const onDefaultToggle = (value: boolean) => {
    setFormData(form => ({ ...form, default: value }));
  };

  const footer = [
    <AntButton key="back" className="mr-10" onClick={onCancel}>
      Cancel
    </AntButton>
  ];

  if (!loading) {
    footer.push(
      <AntButton key="submit" type="primary" disabled={getBtnStatus()} onClick={onOk}>
        Save
      </AntButton>
    );
  } else {
    footer.push(<Loader />);
  }

  return (
    <Modal
      visible={visible}
      title={title}
      onCancel={onCancel}
      onOk={onOk}
      className="edit-create-compact-modal"
      footer={footer}>
      <AntRow gutter={[10, 10]}>
        <AntCol span={24}>
          <Form layout={"vertical"}>
            {hasUniqueName && (
              <Form.Item
                label={formInfo.nameLabel || "Name"}
                validateStatus={getValidateStatus()}
                required
                hasFeedback={true}
                help={getValidateStatus() === "error" && getError()}>
                <Input
                  placeholder={formInfo.namePlaceholder}
                  value={formData?.name}
                  onFocus={() => setNameFieldBlur(true)}
                  onChange={onFieldChange}
                  disabled={disableNameEdit}
                />
              </Form.Item>
            )}
            {!hasUniqueName && (
              <Form.Item label={formInfo.nameLabel || "Name"}>
                <Input
                  placeholder={formInfo.namePlaceholder}
                  value={formData?.name}
                  onChange={onFieldChange}
                  disabled={disableNameEdit}
                />
              </Form.Item>
            )}
            {hasDescription && (
              <Form.Item label={formInfo.descriptionLabel || "Description"}>
                <TextArea
                  className="description-input"
                  rows={2}
                  name={"Description"}
                  value={formData?.description}
                  placeholder={formInfo.descriptionPlaceholder}
                  onChange={onDescriptionFieldChange}
                />
              </Form.Item>
            )}
            {hasDefault && (
              <Form.Item label={formInfo.defaultLabel || "Default"} colon={false}>
                <Switch checked={formData?.default} onChange={onDefaultToggle} disabled={formInfo?.disableDefault} />
              </Form.Item>
            )}
          </Form>
        </AntCol>
      </AntRow>
    </Modal>
  );
};

export default React.memo(EditCreateCompactModalComponent);
