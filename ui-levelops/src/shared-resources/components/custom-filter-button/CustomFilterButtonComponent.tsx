import { Icon } from "antd";
import React, { useCallback, useMemo, useState } from "react";
import { AntBadgeComponent as AntBadge } from "../ant-badge/ant-badge.component";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { AntModalComponent as AntModal } from "../ant-modal/ant-modal.component";

interface customFilterButtonProps {
  count: number;
  onClearFilter: () => void;
  onFilterClick: () => void;
}

const CustomFilterButton: React.FC<customFilterButtonProps> = ({ count, onClearFilter, onFilterClick }) => {
  const [showModal, setShowModal] = useState<boolean>(false);

  const handleToggleModal = useCallback(() => {
    setShowModal(prev => !prev);
  }, []);

  const handleClearFilters = useCallback(() => {
    setShowModal(false);
    onClearFilter();
  }, [onClearFilter]);

  const filterButtonStyle = useMemo(
    () => ({
      width: count > 0 ? "75%" : "100%",
      borderRadius: "0",
      border: "1px solid #d7d0d0"
    }),
    [count]
  );

  const clearButtonStyle = useMemo(() => ({ borderRadius: "0", border: "1px solid #d7d0d0", borderLeft: "0px" }), []);

  return (
    <>
      <AntModal
        visible={showModal}
        onOk={handleClearFilters}
        onCancel={handleToggleModal}
        closable={false}
        title="Remove Filters?">
        <p>{`${count} ${count > 1 ? "filters" : "filter"} will be removed.`}</p>
      </AntModal>
      <div
        className="flex"
        style={{ width: count > 0 ? "9rem" : "6rem", marginLeft: "0.75rem", marginRight: "0.75rem" }}>
        <AntButton type="default" onClick={onFilterClick} icon="filter" style={filterButtonStyle}>
          Filters
        </AntButton>
        {count > 0 && (
          <AntBadge count={count} style={{ width: "1rem", background: "blue" }}>
            <AntButton type="default" onClick={handleToggleModal} style={clearButtonStyle}>
              <Icon type="close" style={{ textAlign: "center", fontSize: "small" }} />
            </AntButton>
          </AntBadge>
        )}
      </div>
    </>
  );
};

export default CustomFilterButton;
