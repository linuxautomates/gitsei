import { OUCategoryOptionsType } from "configurations/configuration-types/OUTypes";
import OrgUnitTreeViewComponent from "configurations/pages/Organization/organization-unit/components/org-unit-tree-view/OrgUnitTreeViewComponent";
import React, { useMemo, useState } from "react";
import { AntBadge, AntButton, AntModal } from "shared-resources/components";

interface OrgUnitCategoryRowProps {
  categoryRecord: OUCategoryOptionsType;
}
const OrgUnitCategoryRowComponent: React.FC<OrgUnitCategoryRowProps> = ({ categoryRecord }) => {
  const [viewOUCategoryId, setViewOUCategoryId] = useState<string | undefined>();

  const handlePopoverClose = () => {
    setViewOUCategoryId(undefined);
  };

  const handleViewOUClick = () => {
    setViewOUCategoryId(categoryRecord.value);
  };

  const renderTitle = useMemo(
    () => (
      <div className="flex align-center">
        {categoryRecord.label}
        <AntBadge
          style={{ backgroundColor: "var(--harness-blue)", marginLeft: "0.5rem" }}
          count={categoryRecord.ouCount || 0}
          overflowCount={categoryRecord.ouCount || 0}
        />
      </div>
    ),
    [categoryRecord]
  );

  const popOverContentModal = useMemo(
    () => (
      <AntModal
        visible={!!viewOUCategoryId}
        closable
        onCancel={handlePopoverClose}
        footer={null}
        mask={true}
        maskClosable={false}
        wrapClassName={"ou-category-tree-modal"}
        centered
        className="ou-category-tree-modal">
        <OrgUnitTreeViewComponent title={renderTitle} ouGroupId={viewOUCategoryId ?? ""} disableSelect={true} />
      </AntModal>
    ),
    [renderTitle, viewOUCategoryId]
  );
  return (
    <>
      {popOverContentModal}
      <div className="ou-category-selection-row">
        {categoryRecord.label}
        <AntButton className="view-ou-action" size="small" onClick={handleViewOUClick}>
          View Collections
        </AntButton>
      </div>
    </>
  );
};

export default OrgUnitCategoryRowComponent;
