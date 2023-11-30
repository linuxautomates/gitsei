import React, { useMemo, useState } from "react";
import { Empty, Row } from "antd";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import { AntModalComponent as AntModal } from "shared-resources/components/ant-modal/ant-modal.component";
import { AntTableComponent as AntTable } from "shared-resources/components/ant-table/ant-table.component";
import { AlignmentTableProps } from "../chart-types";
import { alignmentTableConfig } from "./alignmentTableConfig";
import {
  alignmentStatusMapping,
  alignmentToSVGMapping
} from "dashboard/constants/bussiness-alignment-applications/constants";
import AlignmentModal from "./AlignmentModal";
import "./alignmentTable.styles.scss";
import { get } from "lodash";

const LIMITER = 4;
const AlignmentTable: React.FC<AlignmentTableProps> = (props: AlignmentTableProps) => {
  const [showGoalsModal, setShowGoalsModal] = useState<boolean>(false);
  const [showExtraCategoriesModal, setShowExtraCategoriesModal] = useState<boolean>(false);
  const { data } = props;
  const slicedCategories = (data?.categories || []).slice(0, LIMITER);
  const extraCategories = (data?.categories || []).length > LIMITER ? (data?.categories || []).slice(LIMITER) : [];
  const extraCategoriesLength = (data?.categories || []).length - LIMITER;
  const alignmentStatus = (alignmentStatusMapping as any)[data?.total_alignment_score];

  const handleToggleGoalsModal = (value: boolean) => {
    setShowGoalsModal(value);
  };

  const handleToggleExtraCategoriesModal = (value: boolean) => {
    setShowExtraCategoriesModal(value);
  };

  const handleMouseEnter = (record: any, index: number) => {
    const sliderRef = document.getElementsByClassName((record?.id || "").concat(index + ""));
    const idealRange = get(record, ["config", "ideal_range"], undefined);
    if (sliderRef?.length && idealRange) {
      const slider = sliderRef[0];
      const firstChild = slider?.firstElementChild;
      const min = get(idealRange, ["min"]),
        max = get(idealRange, ["max"]);
      if (firstChild && min !== undefined && max !== undefined) {
        let lengthOfRange = Math.abs(parseInt(max) - parseInt(min));
        let leftAcceptableRange = [Math.max(0, parseInt(min) - lengthOfRange).toString(), min];
        let rightAcceptableRange = [max, Math.min(100, parseInt(max) + lengthOfRange).toString()];
        (
          firstChild as any
        ).style.backgroundImage = `linear-gradient(to right, #e33f3f 0%, #e33f3f ${leftAcceptableRange[0]}%, #fcb132 ${leftAcceptableRange[0]}%, #fcb132 ${leftAcceptableRange[1]}%,#fcb132 ${rightAcceptableRange[0]}%, #fcb132 ${rightAcceptableRange[1]}%, #e33f3f ${rightAcceptableRange[1]}%)`;
      }
    }
  };

  const handleMouseLeave = (record: any, index: number) => {
    const sliderRef = document.getElementsByClassName((record?.id || "").concat(index + ""));
    const idealRange = get(record, ["config", "ideal_range"], undefined);
    if (sliderRef?.length && idealRange) {
      const slider = sliderRef[0];
      const firstChild = slider?.firstElementChild;
      if (firstChild) {
        (firstChild as any).style.backgroundImage = "";
      }
    }
  };

  const renderExtraCategoriesModal = useMemo(() => {
    return (
      <AntModal
        title="Effort Alignment"
        visible={showExtraCategoriesModal}
        footer={null}
        className="extra-category-modal"
        onCancel={(e: any) => handleToggleExtraCategoriesModal(false)}>
        <div className="alignment-table">
          <AntTable
            columns={alignmentTableConfig}
            dataSource={extraCategories}
            hasPagination={false}
            rowClassName="alignment-row"
            onRow={(record: any, rowIndex: number) => {
              return {
                onMouseEnter: (e: any) => handleMouseEnter(record, rowIndex), // mouse enter row
                onMouseLeave: (e: any) => handleMouseLeave(record, rowIndex) // mouse leave row
              };
            }}
          />
        </div>
      </AntModal>
    );
  }, [extraCategories]);

  if (!(data?.categories ?? []).length) return <Empty />;

  return (
    <>
      <div className="alignment-table-container">
        <Row className="alignment-table-row">
          <div className="total-score">
            <div className="text">Score</div>
            <div className="flex justify-center align-center h-100p" style={{ paddingBottom: "5rem" }}>
              <img src={(alignmentToSVGMapping() as any)[alignmentStatus]} />
            </div>
          </div>
          <div className="alignment-table">
            <AntTable
              columns={alignmentTableConfig}
              dataSource={slicedCategories}
              hasPagination={false}
              rowClassName="alignment-row"
              onRow={(record: any, rowIndex: number) => {
                return {
                  onMouseEnter: (e: any) => handleMouseEnter(record, rowIndex), // mouse enter row
                  onMouseLeave: (e: any) => handleMouseLeave(record, rowIndex) // mouse leave row
                };
              }}
            />
          </div>
        </Row>
        <Row>
          <div className="footer">
            <AntButton onClick={handleToggleGoalsModal}>Edit Goals</AntButton>
            {extraCategoriesLength > 0 && (
              <AntButton onClick={handleToggleExtraCategoriesModal}>{`+${extraCategoriesLength} more`}</AntButton>
            )}
          </div>
        </Row>
      </div>
      <AlignmentModal
        reloadWidget={props.reload}
        visible={showGoalsModal}
        profileId={data?.profileId}
        handleShowAlignmentModalToggle={handleToggleGoalsModal}
      />
      {renderExtraCategoriesModal}
    </>
  );
};

export default AlignmentTable;
