import { Form, Modal, notification, Select } from "antd";
import React, { useCallback, useMemo, useState } from "react";
import { SelectRestapi } from "shared-resources/helpers";

const { Option } = Select;

export type NotifyFailType = {
  type: "email" | "slack";
  recipients: string[];
};

const TypeSelectOptions = [
  { label: "Email", value: "email" },
  { label: "Slack", value: "slack" },
  { label: "MS Teams", value: "ms_teams" }
];

interface NotifyFailModalProps {
  visible: boolean;
  initialState?: NotifyFailType;
  onCancel: () => void;
  onOk: (data: NotifyFailType) => void;
}

const NotifyFailModal: React.FC<NotifyFailModalProps> = props => {
  const { visible, onCancel, initialState, onOk } = props;
  const [formState, setFormState] = useState<NotifyFailType | undefined>({
    ...(initialState || {}),
    recipients: initialState?.recipients?.map((item: any) => ({
      label: item.includes("create:") ? item.split(":")[1] : item,
      key: item
    }))
  } as any);

  const isFormValidated: boolean = useMemo(() => {
    return !!(formState && formState.type && formState.recipients?.length > 0);
  }, [formState]);

  const save = useCallback(() => {
    if (isFormValidated) {
      onOk({
        ...(formState || {}),
        recipients: formState?.recipients?.map((item: any) => (item.key?.includes("create:") ? item.key : item.label))
      } as any);
      return;
    }
    notification.error({ message: "Please select type and recipients" });
  }, [formState]);

  const onTypeSelectChange = useCallback(
    (value: "email" | "slack") => {
      setFormState(prev => ({ ...prev, type: value } as any));
    },
    [formState]
  );

  const onRecepientSelectChange = useCallback(
    (values: { key: string; label: string }[]) => {
      setFormState(prev => ({ ...prev, recipients: values } as any));
    },
    [formState]
  );

  return (
    <Modal
      visible={visible}
      title={"Notify On Fail"}
      onCancel={onCancel}
      onOk={save}
      okText={"Save"}
      okButtonProps={{ disabled: !isFormValidated }}>
      <Form>
        <Form.Item label={"Type"} colon={false}>
          <Select value={formState?.type} onChange={onTypeSelectChange}>
            {TypeSelectOptions.map(item => {
              return (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              );
            })}
          </Select>
        </Form.Item>
        <Form.Item label="Recipients" colon={false}>
          <SelectRestapi
            searchField="email"
            uri="users"
            method="list"
            isMulti
            closeMenuOnSelect
            value={formState?.recipients || []}
            createOption
            mode={"multiple"}
            labelInValue
            onChange={onRecepientSelectChange}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default NotifyFailModal;
