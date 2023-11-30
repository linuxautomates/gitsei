import { isEqual } from "lodash";
import React, { useEffect, useState } from "react";
import { AntModal } from "shared-resources/components";
import { ServerPaginatedTable } from "shared-resources/containers";
import { tableColumns } from "./table.config";

interface AssigneeModalSheetProps {
  visible: boolean;
  onCancel: () => void;
  selectedValue: { key: string; label: string }[] | null;
  onSave: (assignees: { key: string; label: string }[]) => void;
  className?: string;
}

const AssigneeModalSheet: React.FC<AssigneeModalSheetProps> = (props: AssigneeModalSheetProps) => {
  const [selectedEmails, setSelectedEmails] = useState<{ key: string; label: string }[]>(props?.selectedValue || []);
  const onSelectChange = (rowKeys: any, record: any) => {
    let _selectedEmails = rowKeys
      ?.map((key: string) => {
        const data = selectedEmails.find((item: any) => item?.key === key);
        if (data) {
          return data;
        } else {
          const data = record.find((item: any) => item.id === key);
          return { key: data?.id, label: data?.email };
        }
      })
      .filter((item: any) => !!item);
    setSelectedEmails(_selectedEmails);
  };

  useEffect(() => {
    if (!isEqual(props.selectedValue, selectedEmails)) {
      setSelectedEmails(props?.selectedValue || []);
    }
  }, [props.selectedValue]);

  const rowSelection = {
    selectedRowKeys: selectedEmails.map((item: any) => item?.key).filter((item: any) => !item.includes("create:")),
    onChange: onSelectChange
  };
  return (
    <AntModal
      visible={props.visible}
      className={props.className}
      title={"Assignee"}
      width={700}
      closable={true}
      maskClosable={false}
      mask={true}
      onOk={() => {
        props.onSave(selectedEmails);
      }}
      onCancel={props.onCancel}>
      <ServerPaginatedTable
        pageName={"Assignee"}
        uri={"users"}
        uuid={"workitems"}
        columns={tableColumns}
        hasSearch={true}
        hasFilters={false}
        generalSearchField={"email"}
        pageSize={10}
        rowSelection={rowSelection}
        skipConfirmationDialog={true}
      />
    </AntModal>
  );
};

export default AssigneeModalSheet;
