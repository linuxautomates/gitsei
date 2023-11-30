import React, { useState } from "react";
import { List, Dropdown, Menu, Icon } from "antd";
import { DragDrop } from "./dragDrop";
import { ReorderableList } from "shared-resources/containers";

interface SectionsListContainerProps {
  sections: any;
  onSelect: (index: number) => void;
  onDelete: (index: number) => any;
  selectedSection: number | undefined;
  onReorder: (dragIndex: number, hoverIndex: number) => any;
}
const SectionsListContainer: React.FC<SectionsListContainerProps> = (props: SectionsListContainerProps) => {
  const [selected_index, setSelectedIndex] = useState<any>(undefined);

  const { sections } = props;

  return (
    <ReorderableList
      //@ts-ignore
      className="bg-white ant-list-custom m-0 "
      style={{ margin: "0" }}
      dataSource={sections}
      moveCard={props.onReorder}
      renderItem={(item: any, index: any) => {
        let listClass = "ant-card-section-list-item";
        if (index === selected_index) {
          listClass = "ant-card-section-list-item selected";
        }
        return (
          <div
            id={index}
            key={index}
            onClick={e => {
              e.preventDefault();
              setSelectedIndex(index);
              props.onSelect(index);
            }}>
            <List.Item
              className={listClass}
              key={index}
              actions={[
                <Dropdown
                  overlay={
                    <Menu>
                      <Menu.Item key={index} onClick={props.onDelete(index)}>
                        <Icon type={"delete"} /> Delete
                      </Menu.Item>
                    </Menu>
                  }
                  placement="bottomRight">
                  <Icon type={"more"} style={{ fontSize: "14px" }} />
                </Dropdown>
              ]}>
              <div className="flex-1 flex align-items-start overflow-hidden">
                <DragDrop className="drag-icon" />
                {/*eslint-disable-next-line  jsx-a11y/anchor-is-valid*/}
                <a
                  href={"#"}
                  className="section-item-text flex-1 ant-typography-ellipsis ant-typography-ellipsis-single-line">
                  {item.name}
                </a>
              </div>
            </List.Item>
          </div>
        );
      }}
    />
  );
};

SectionsListContainer.defaultProps = {
  sections: [],
  onSelect: () => {}
};

export default React.memo(SectionsListContainer);
