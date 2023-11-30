import React, { ChangeEvent, useRef, useState } from "react";
import { Form, Input, Modal } from "antd";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { AntColComponent as AntCol } from "../ant-col/ant-col.component";
import { default as AntRow } from "../ant-row/ant-row.component";
import { NAME_EXISTS_ERROR, REQUIRED_FIELD } from "../../../constants/formWarnings";
import OrgUnitCategorySelectorComponent from "dashboard/pages/OrgUnitCategorySelectorComponent";

interface EditCloneModalProps {
  visible: boolean;
  title: string;
  onOk: (name: string, categories: string[]) => void;
  onCancel: () => void;
  confirmButtonText?: string;
  searchEvent?: (name: string) => void;
  nameExists?: boolean;
}

const EditCloneModal: React.FC<EditCloneModalProps> = (props: EditCloneModalProps) => {
  const [newName, setNewName] = useState<string>("");
  const [categories, setOUCategories] = useState<string[]>([]);
  const categoriesRef = useRef<string[] | undefined>(undefined);
  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);

  const getBtnStatus = () => {
    return !(nameFieldBlur && newName && newName.length > 0 && !props.nameExists && categories?.length);
  };

  const getValidateStatus = () => {
    if (!nameFieldBlur) {
      return "";
    } else if (nameFieldBlur && newName.length > 0 && !props.nameExists) {
      return "success";
    } else return "error";
  };

  const getError = () => {
    if (props.nameExists === true) {
      return NAME_EXISTS_ERROR;
    } else return REQUIRED_FIELD;
  };

  const onCancel = () => {
    clearField();
    props.onCancel();
  };

  const onOk = () => {
    const name = newName;
    clearField();
    props.onOk(name, categories);
  };

  const clearField = () => {
    setNewName("");
    setNameFieldBlur(false);
    setOUCategories(categoriesRef.current ?? []);
  };

  const onFieldChange = (e: ChangeEvent<HTMLInputElement>) => {
    setNewName(e.target.value);
    props.searchEvent && props.searchEvent(e.target.value);
  };

  const handleOUCategoryChange = (value: string[]) => {
    if (!categoriesRef.current) {
      categoriesRef.current = value;
    }
    setOUCategories(value);
  };

  return (
    <Modal
      visible={props.visible}
      title={props.title}
      onCancel={onCancel}
      maskClosable={false}
      onOk={onOk}
      footer={[
        <AntButton key="back" onClick={onCancel}>
          Cancel
        </AntButton>,
        <AntButton key="submit" type="primary" disabled={getBtnStatus()} onClick={onOk}>
          {props.confirmButtonText || "Clone"}
        </AntButton>
      ]}>
      <AntRow gutter={[10, 10]}>
        <AntCol span={24}>
          <Form layout={"vertical"}>
            <Form.Item
              label="Name"
              validateStatus={getValidateStatus()}
              required
              hasFeedback={true}
              help={getValidateStatus() === "error" && getError()}>
              <Input
                placeholder="Name"
                value={newName}
                onFocus={() => setNameFieldBlur(true)}
                onChange={onFieldChange}
              />
            </Form.Item>
            <OrgUnitCategorySelectorComponent
              ouCategories={categories}
              handleOUCategoryChange={handleOUCategoryChange}
            />
          </Form>
        </AntCol>
      </AntRow>
    </Modal>
  );
};

export default EditCloneModal;
